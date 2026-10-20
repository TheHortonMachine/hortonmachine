/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.server.notebooks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

/**
 * Makes the cells of a notebook behave as one script:
 * <ul>
 * <li>the variables declared at the top level of a cell, as <code>def x = 1</code>, become variables of the
 *     binding, so that the next cells see them;</li>
 * <li>the imports of a cell are added to the next cells.</li>
 * </ul>
 *
 * <p>Methods declared in a cell stay in that cell: closures assigned to variables work in all of them.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
class CellCustomizer extends CompilationCustomizer {

    private final ImportCustomizer imports;
    private final Set<String> known = new HashSet<>();

    /**
     * @param imports the imports given to all the cells, where the imports of the cells are added.
     */
    CellCustomizer( ImportCustomizer imports ) {
        // before the variables are resolved, which makes the declarations local
        super(CompilePhase.CONVERSION);
        this.imports = imports;
    }

    @Override
    public void call( SourceUnit source, GeneratorContext context, ClassNode classNode ) {
        if (!classNode.isScript()) {
            return;
        }
        ModuleNode module = source.getAST();
        List<Statement> statements = module.getStatementBlock().getStatements();
        for( int i = 0; i < statements.size(); i++ ) {
            Statement statement = statements.get(i);
            if (statement instanceof ExpressionStatement
                    && ((ExpressionStatement) statement).getExpression() instanceof DeclarationExpression) {
                DeclarationExpression declaration = (DeclarationExpression) ((ExpressionStatement) statement).getExpression();
                // @Field and def (a, b) = ... stay as they are
                if (declaration.isMultipleAssignmentDeclaration() || !declaration.getAnnotations().isEmpty()) {
                    continue;
                }
                Expression right = declaration.getRightExpression();
                if (right instanceof EmptyExpression) {
                    right = ConstantExpression.NULL;
                }
                VariableExpression variable = new VariableExpression(declaration.getVariableExpression().getName());
                variable.setSourcePosition(declaration.getVariableExpression());
                BinaryExpression assignment = new BinaryExpression(variable,
                        Token.newSymbol(Types.ASSIGN, declaration.getLineNumber(), declaration.getColumnNumber()), right);
                assignment.setSourcePosition(declaration);
                ExpressionStatement replacement = new ExpressionStatement(assignment);
                replacement.setSourcePosition(statement);
                replacement.copyStatementLabels(statement);
                statements.set(i, replacement);
            }
        }
        rememberImports(module);
    }

    private void rememberImports( ModuleNode module ) {
        for( ImportNode node : module.getImports() ) {
            if (known.add("class:" + node.getAlias() + ":" + node.getClassName())) {
                imports.addImport(node.getAlias(), node.getClassName());
            }
        }
        for( ImportNode node : module.getStarImports() ) {
            String packageName = node.getPackageName();
            if (packageName.endsWith(".")) {
                packageName = packageName.substring(0, packageName.length() - 1);
            }
            if (known.add("star:" + packageName)) {
                imports.addStarImports(packageName);
            }
        }
        for( Map.Entry<String, ImportNode> entry : module.getStaticImports().entrySet() ) {
            ImportNode node = entry.getValue();
            if (known.add("static:" + entry.getKey() + ":" + node.getClassName() + ":" + node.getFieldName())) {
                imports.addStaticImport(entry.getKey(), node.getClassName(), node.getFieldName());
            }
        }
        for( ImportNode node : module.getStaticStarImports().values() ) {
            if (known.add("staticstar:" + node.getClassName())) {
                imports.addStaticStars(node.getClassName());
            }
        }
    }
}
