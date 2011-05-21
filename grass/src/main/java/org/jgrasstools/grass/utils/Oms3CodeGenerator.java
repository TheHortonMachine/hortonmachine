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

import org.jgrasstools.grass.dtd64.Flag;
import org.jgrasstools.grass.dtd64.Parameter;
import org.jgrasstools.grass.dtd64.Task;
import org.jgrasstools.grass.dtd64.Value;
import org.jgrasstools.grass.dtd64.Values;

/**
 * OMS3 Code generation class. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class Oms3CodeGenerator {

    private StringBuilder codeBuilder = new StringBuilder();

    private String INDENT = "\t";

    public Oms3CodeGenerator( Task grassTask ) {

        String name = grassTask.getName().trim();
        String classSafeName = name.replaceAll("\\.", "_");
        String description = grassTask.getDescription();
        String keyWords = grassTask.getKeywords();
        String category = GrassUtils.name2GrassCategory(name);

        codeBuilder.append("import org.jgrasstools.gears.libs.modules.JGTModel;").append("\n");
        codeBuilder.append("").append("\n");
        codeBuilder.append("import oms3.annotations.Author;").append("\n");
        codeBuilder.append("import oms3.annotations.Documentation;").append("\n");
        codeBuilder.append("import oms3.annotations.Label;").append("\n");
        codeBuilder.append("import oms3.annotations.Description;").append("\n");
        codeBuilder.append("import oms3.annotations.Execute;").append("\n");
        codeBuilder.append("import oms3.annotations.In;").append("\n");
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
        codeBuilder.append("@Name(\"").append(name).append("\")").append("\n");
        codeBuilder.append("@Status(Status.CERTIFIED)").append("\n");
        codeBuilder.append("@License(\"General Public License Version >=2)\")").append("\n");
        codeBuilder.append("public class ").append(classSafeName).append(" extends JGTModel {").append("\n");
        codeBuilder.append("").append("\n");

        List<Parameter> parameterList = grassTask.getParameter();
        if (parameterList.size() > 0) {
            for( Parameter parameter : parameterList ) {
                String parameterName = parameter.getName().trim();
                String parameterDescription = parameter.getDescription().trim();
                String isRequired = parameter.getRequired().trim();
                String defaultValue = parameter.getDefault();

                codeBuilder.append(INDENT).append("@Description(\"").append(parameterDescription);
                if (isRequired.trim().equals("no")) {
                    codeBuilder.append(" (optional)");
                }
                codeBuilder.append("\")\n");
                codeBuilder.append(INDENT).append("@In\n");
                codeBuilder.append(INDENT).append("public String ").append(parameterName);
                if (defaultValue != null) {
                    codeBuilder.append(" = ").append(defaultValue.trim());
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

        List<Flag> flagList = grassTask.getFlag();
        for( Flag flag : flagList ) {
            String flagName = flag.getName().trim();
            String descr = flag.getDescription().trim();
            codeBuilder.append(INDENT).append("@Description(\"").append(descr).append("\")\n");
            codeBuilder.append(INDENT).append("@In\n");
            codeBuilder.append(INDENT).append("public String ").append(flagName).append(";\n\n");
        }

        codeBuilder.append("}").append("\n");
    }
    public String getOms3Class() {
        return codeBuilder.toString();
    }

}
