/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.grass.utils;

import java.util.List;
import java.util.TreeSet;

import static org.jgrasstools.grass.utils.GrassUtils.*;
import org.jgrasstools.grass.dtd64.Flag;
import org.jgrasstools.grass.dtd64.Gisprompt;
import org.jgrasstools.grass.dtd64.Parameter;
import org.jgrasstools.grass.dtd64.Task;

/**
 * OMS3 Code generation class. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class Oms3CodeWrapper {

    private StringBuilder codeBuilder = new StringBuilder();

    private String INDENT = "\t";

    private String classSafeName;

    private String name;

    private String description;

    private String category;

    public Oms3CodeWrapper( Task grassTask ) {

        name = grassTask.getName().trim();
        classSafeName = name.replaceAll("\\.", VARIABLE_DOT_SUBSTITUTION);
        description = grassTask.getDescription();
        description = cleanDescription(description);
        String keyWords = grassTask.getKeywords();
        category = GrassUtils.name2GrassCategory(name);
        String modulePackage = GrassUtils.getModulePackage(classSafeName);

        /*
         * the main class
         */
        codeBuilder.append("package ").append(modulePackage).append(";\n");
        codeBuilder.append("").append("\n");
        codeBuilder.append("import org.jgrasstools.grass.utils.ModuleSupporter;").append("\n");
        codeBuilder.append("").append("\n");
        codeBuilder.append("import oms3.annotations.Author;").append("\n");
        codeBuilder.append("import oms3.annotations.Documentation;").append("\n");
        codeBuilder.append("import oms3.annotations.Label;").append("\n");
        codeBuilder.append("import oms3.annotations.Description;").append("\n");
        codeBuilder.append("import oms3.annotations.Execute;").append("\n");
        codeBuilder.append("import oms3.annotations.In;").append("\n");
        codeBuilder.append("import oms3.annotations.UI;").append("\n");
        codeBuilder.append("import oms3.annotations.Keywords;").append("\n");
        codeBuilder.append("import oms3.annotations.License;").append("\n");
        codeBuilder.append("import oms3.annotations.Name;").append("\n");
        codeBuilder.append("import oms3.annotations.Out;").append("\n");
        codeBuilder.append("import oms3.annotations.Status;").append("\n");
        codeBuilder.append("").append("\n");
        if (description != null)
            codeBuilder.append("@Description(\"").append(description.trim()).append("\")").append("\n");
        codeBuilder.append("@Author(name = \"Grass Developers Community\", contact = \"http://grass.osgeo.org\")").append("\n");
        if (keyWords != null)
            codeBuilder.append("@Keywords(\"").append(keyWords.trim()).append("\")").append("\n");
        if (category != null)
            codeBuilder.append("@Label(\"").append(category).append("\")").append("\n");
        codeBuilder.append("@Name(\"").append(classSafeName).append("\")").append("\n");
        codeBuilder.append("@Status(Status.CERTIFIED)").append("\n");
        codeBuilder.append("@License(\"General Public License Version >=2)\")").append("\n");
        codeBuilder.append("public class ").append(classSafeName).append(" {").append("\n");
        codeBuilder.append("").append("\n");

        TreeSet<String> namesMap = new TreeSet<String>();

        /*
         * parameters
         */
        List<Parameter> parameterList = grassTask.getParameter();
        if (parameterList.size() > 0) {
            for( Parameter parameter : parameterList ) {
                String parameterName = parameter.getName().trim();
                parameterName = parameterName.replaceAll("\\.", VARIABLE_DOT_SUBSTITUTION);
                parameterName = VARIABLE_PARAMETER_PREFIX + parameterName + VARIABLE_PARAMETER_SUFFIX;
                if (!namesMap.add(parameterName)) {
                    System.err.println(INDENT + "Found double parameter " + parameterName + " in " + name);
                    continue;
                }

                String parameterDescription = parameter.getDescription().trim();
                parameterDescription = cleanDescription(parameterDescription);
                String isRequired = parameter.getRequired().trim();
                String defaultValue = parameter.getDefault();
                Gisprompt gisprompt = parameter.getGisprompt();
                String guiHints = null;
                if (gisprompt != null)
                    guiHints = GrassUtils.getGuiHintsFromGisprompt(gisprompt);

                if (guiHints != null)
                    codeBuilder.append(INDENT).append("@UI(\"").append(guiHints).append("\")\n");
                codeBuilder.append(INDENT).append("@Description(\"").append(parameterDescription);
                if (isRequired.trim().equals("no")) {
                    codeBuilder.append(" (optional)");
                }
                codeBuilder.append("\")\n");
                codeBuilder.append(INDENT).append("@In\n");
                codeBuilder.append(INDENT).append("public String ").append(parameterName);
                if (defaultValue != null) {
                    codeBuilder.append(" = \"").append(defaultValue.trim()).append("\"");
                }
                codeBuilder.append(";\n\n");

                // String multiple = parameter.getMultiple().trim();
                // System.out.println("\t\tMultiple: " + multiple);

                // Values values = parameter.getValues();
                // if (values != null) {
                // System.out.println("\t\tValues:");
                // List<Value> value = values.getValue();
                // for( Value v : value ) {
                // String name2 = v.getName().trim();
                // System.out.print("\t\t\t" + name2 + " - ");
                // String description = v.getDescription().trim();
                // System.out.println(description);
                // }
                // }
            }
        }

        /*
         * flags
         */
        List<Flag> flagList = grassTask.getFlag();
        for( Flag flag : flagList ) {
            String flagName = flag.getName().trim();
            flagName = flagName.replaceAll("\\.", VARIABLE_DOT_SUBSTITUTION);
            flagName = VARIABLE_FLAG_PREFIX + flagName + VARIABLE_FLAG_SUFFIX;
            if (!namesMap.add(flagName)) {
                System.err.println(INDENT + "Found double flag " + flagName + " in " + name);
                continue;
            }

            String descr = flag.getDescription().trim();
            descr = cleanDescription(descr);
            codeBuilder.append(INDENT).append("@Description(\"").append(descr).append("\")\n");
            codeBuilder.append(INDENT).append("@In\n");
            codeBuilder.append(INDENT).append("public boolean ").append(flagName).append(" = false;\n\n");
        }

        /*
         * execution method
         */
        codeBuilder.append("\n");
        codeBuilder.append(INDENT).append("@Execute").append("\n");
        codeBuilder.append(INDENT).append("public void process() throws Exception {").append("\n");
        codeBuilder.append(INDENT).append(INDENT).append("ModuleSupporter.processModule(this);").append("\n");
        codeBuilder.append(INDENT).append("}").append("\n\n");

        codeBuilder.append("}").append("\n");
    }

    /**
     * Clean description from quotes and linefeeds. 
     * 
     * @param description the description to clean.
     * @return the cleaned description or <code>null</code> if the input value is null.
     */
    public String cleanDescription( String description ) {
        if (description == null) {
            return null;
        }
        description = description.replaceAll("\"", "\\\\\"");
        description = description.replaceAll("\n", " ");
        return description;
    }

    public String getGeneratedOms3Class() {
        return codeBuilder.toString();
    }

    public String getName() {
        return name;
    }

    public String getClassSafeName() {
        return classSafeName;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

}
