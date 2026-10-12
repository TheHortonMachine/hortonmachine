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
package org.hortonmachine.geoscript.console;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ClosureListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.LambdaExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

/**
 * Instruments the scripts for the {@link ScriptDebugger}: before the first statement of each line
 * it adds
 *
 * <pre>
 * if (ScriptDebugger.shouldPause(line)) ScriptDebugger.pause(line, [a: a, b: b, ...])
 * </pre>
 *
 * <p>
 * with the local variables declared up to there. It works on the syntax tree before the variables
 * are resolved, so that the added references are resolved like the ones of the script.
 *
 * <p>
 * It stays installed in the compiler configuration of the console and does nothing unless enabled,
 * so that normal runs are compiled as they always were.
 *
 * <p>
 * Constructors are not instrumented, since nothing can come before their <code>super()</code>
 * call.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class DebugCustomizer extends CompilationCustomizer {

    private static final ClassNode DEBUGGER = ClassHelper.make(ScriptDebugger.class);

    private volatile boolean enabled = false;
    private volatile int lineOffset = 0;

    public DebugCustomizer() {
        super(CompilePhase.CONVERSION);
    }

    /**
     * Instrument the next compilations, or stop doing it.
     *
     * @param enabled if <code>true</code>, the scripts are instrumented.
     * @param lineOffset the lines that come before the compiled text in the editor (when a selection
     *          is run), to report the lines of the editor.
     */
    public void setEnabled( boolean enabled, int lineOffset ) {
        this.enabled = enabled;
        this.lineOffset = lineOffset;
    }

    @Override
    public void call( SourceUnit source, GeneratorContext context, ClassNode classNode ) {
        if (!enabled) {
            return;
        }
        for( MethodNode method : classNode.getMethods() ) {
            if (method.getCode() != null && !method.isAbstract()) {
                Instrumenter instrumenter = new Instrumenter();
                instrumenter.scopes.push(names(method.getParameters()));
                method.getCode().visit(instrumenter);
            }
        }
    }

    private static List<String> names( Parameter[] parameters ) {
        List<String> names = new ArrayList<>();
        if (parameters != null) {
            for( Parameter parameter : parameters ) {
                names.add(parameter.getName());
            }
        }
        return names;
    }

    private static BlockStatement asBlock( Statement statement ) {
        BlockStatement block = new BlockStatement(new ArrayList<>(List.of(statement)), new VariableScope());
        block.setSourcePosition(statement);
        return block;
    }

    private class Instrumenter extends CodeVisitorSupport {
        /** The names of the local variables, one list per nested scope. */
        private final Deque<List<String>> scopes = new ArrayDeque<>();

        @Override
        public void visitBlockStatement( BlockStatement block ) {
            List<String> scope = new ArrayList<>();
            scopes.push(scope);
            List<Statement> instrumented = new ArrayList<>();
            int lastLine = -1;
            for( Statement statement : block.getStatements() ) {
                int line = statement.getLineNumber();
                if (line > 0 && line != lastLine) {
                    instrumented.add(hook(statement));
                    lastLine = line;
                }
                statement.visit(this);
                instrumented.add(statement);
                addDeclared(statement, scope);
            }
            block.getStatements().clear();
            block.getStatements().addAll(instrumented);
            scopes.pop();
        }

        @Override
        public void visitIfElse( IfStatement ifElse ) {
            if (!(ifElse.getIfBlock() instanceof BlockStatement)) {
                ifElse.setIfBlock(asBlock(ifElse.getIfBlock()));
            }
            Statement elseBlock = ifElse.getElseBlock();
            // an else if stays as it is, it is instrumented as an if
            if (!(elseBlock instanceof BlockStatement || elseBlock instanceof IfStatement || elseBlock instanceof EmptyStatement)) {
                ifElse.setElseBlock(asBlock(elseBlock));
            }
            super.visitIfElse(ifElse);
        }

        @Override
        public void visitForLoop( ForStatement forLoop ) {
            if (!(forLoop.getLoopBlock() instanceof BlockStatement)) {
                forLoop.setLoopBlock(asBlock(forLoop.getLoopBlock()));
            }
            List<String> scope = new ArrayList<>();
            if (forLoop.getVariable() != ForStatement.FOR_LOOP_DUMMY) {
                scope.add(forLoop.getVariable().getName());
            } else if (forLoop.getCollectionExpression() instanceof ClosureListExpression) {
                // the classic for (int i = 0; ...)
                for( Expression init : ((ClosureListExpression) forLoop.getCollectionExpression()).getExpressions() ) {
                    addDeclared(init, scope);
                }
            }
            scopes.push(scope);
            super.visitForLoop(forLoop);
            scopes.pop();
        }

        @Override
        public void visitWhileLoop( WhileStatement loop ) {
            if (!(loop.getLoopBlock() instanceof BlockStatement)) {
                loop.setLoopBlock(asBlock(loop.getLoopBlock()));
            }
            super.visitWhileLoop(loop);
        }

        @Override
        public void visitDoWhileLoop( DoWhileStatement loop ) {
            if (!(loop.getLoopBlock() instanceof BlockStatement)) {
                loop.setLoopBlock(asBlock(loop.getLoopBlock()));
            }
            super.visitDoWhileLoop(loop);
        }

        @Override
        public void visitCatchStatement( CatchStatement statement ) {
            scopes.push(new ArrayList<>(List.of(statement.getVariable().getName())));
            super.visitCatchStatement(statement);
            scopes.pop();
        }

        @Override
        public void visitClosureExpression( ClosureExpression closure ) {
            List<String> scope = names(closure.getParameters());
            // an empty parameters list means the implicit it, while { -> } has null
            if (closure.getParameters() != null && closure.getParameters().length == 0 && !(closure instanceof LambdaExpression)) {
                scope.add("it");
            }
            scopes.push(scope);
            super.visitClosureExpression(closure);
            scopes.pop();
        }

        private void addDeclared( Statement statement, List<String> scope ) {
            if (statement instanceof ExpressionStatement) {
                addDeclared(((ExpressionStatement) statement).getExpression(), scope);
            }
        }

        private void addDeclared( Expression expression, List<String> scope ) {
            if (!(expression instanceof DeclarationExpression)) {
                return;
            }
            DeclarationExpression declaration = (DeclarationExpression) expression;
            if (!declaration.getAnnotations().isEmpty()) {
                return; // ex. @Field, that turns it into something else
            }
            if (declaration.isMultipleAssignmentDeclaration()) {
                for( Expression variable : declaration.getTupleExpression().getExpressions() ) {
                    scope.add(((VariableExpression) variable).getName());
                }
            } else {
                scope.add(declaration.getVariableExpression().getName());
            }
        }

        private Statement hook( Statement statement ) {
            Set<String> visible = new LinkedHashSet<>();
            scopes.descendingIterator().forEachRemaining(visible::addAll);
            MapExpression locals = new MapExpression();
            for( String name : visible ) {
                locals.addMapEntryExpression(new ConstantExpression(name), new VariableExpression(name));
            }
            int line = statement.getLineNumber() + lineOffset;
            Expression shouldPause = new StaticMethodCallExpression(DEBUGGER, "shouldPause",
                    new ArgumentListExpression(new ConstantExpression(line, true)));
            Expression pause = new StaticMethodCallExpression(DEBUGGER, "pause",
                    new ArgumentListExpression(new ConstantExpression(line, true), locals));
            ExpressionStatement pauseStatement = new ExpressionStatement(pause);
            pauseStatement.setSourcePosition(statement);
            IfStatement hook = new IfStatement(new BooleanExpression(shouldPause), pauseStatement, EmptyStatement.INSTANCE);
            hook.setSourcePosition(statement);
            return hook;
        }
    }
}
