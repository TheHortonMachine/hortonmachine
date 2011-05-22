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
        codeBuilder.append("import org.jgrasstools.gears.libs.modules.JGTModel;").append("\n");
        codeBuilder.append("import java.io.File;").append("\n");
        codeBuilder.append("import java.lang.reflect.Field;").append("\n");
        codeBuilder.append("import java.util.ArrayList;").append("\n");
        codeBuilder.append("import java.util.List;").append("\n");
        codeBuilder.append("import org.jgrasstools.gears.libs.exceptions.ModelsIOException;").append("\n");
        codeBuilder.append("import org.jgrasstools.grass.utils.GrassRunner;").append("\n");
        codeBuilder.append("import org.jgrasstools.grass.utils.GrassUtils;").append("\n");
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
        codeBuilder.append("public class ").append(classSafeName).append(" extends JGTModel {").append("\n");
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
        executeGenetration();
        codeBuilder.append(INDENT).append("}").append("\n\n");


        codeBuilder.append("}").append("\n");
    }

    private void executeGenetration(  ) {
        codeBuilder.append("        String gisBase = System.getProperty(GrassUtils.GRASS_ENVIRONMENT_GISBASE_KEY);                                           ").append("\n");
        codeBuilder.append("        File gisBasefile = new File(gisBase);                                                                                    ").append("\n");
        codeBuilder.append("        if (!gisBasefile.exists()) {                                                                                             ").append("\n");
        codeBuilder.append("            throw new ModelsIOException(\"Gisbase variable not properly set. Check your settings!\", this);                        ").append("\n");
        codeBuilder.append("        }                                                                                                                        ").append("\n");
        codeBuilder.append("        String className = this.getClass().getSimpleName();                                                                      ").append("\n");
        codeBuilder.append("        className = className.replaceAll(GrassUtils.VARIABLE_DOT_SUBSTITUTION, \".\");                                             ").append("\n");
        codeBuilder.append("        File grassCommandFile = new File(gisBase, \"bin/\" + className);                                                           ").append("\n");
        codeBuilder.append("        if (!grassCommandFile.exists()) {                                                                                        ").append("\n");
        codeBuilder.append("            throw new ModelsIOException(\"Command does not exist: \" + grassCommandFile.getAbsolutePath(), this);                  ").append("\n");
        codeBuilder.append("        }                                                                                                                        ").append("\n");
        codeBuilder.append("                                                                                                                                 ").append("\n");
        codeBuilder.append("        GrassRunner runner = new GrassRunner(System.out, System.err, false);                                                     ").append("\n");
        codeBuilder.append("                                                                                                                                 ").append("\n");
        codeBuilder.append("        List<String> args = new ArrayList<String>();                                                                             ").append("\n");
        codeBuilder.append("        args.add(grassCommandFile.getAbsolutePath());                                                                            ").append("\n");
        codeBuilder.append("                                                                                                                                 ").append("\n");
        codeBuilder.append("        Field[] fields = this.getClass().getFields();                                                                            ").append("\n");
        codeBuilder.append("        // first flags                                                                                                           ").append("\n");
        codeBuilder.append("        for( Field field : fields ) {                                                                                            ").append("\n");
        codeBuilder.append("            String flagName = field.getName();                                                                                   ").append("\n");
        codeBuilder.append("            if (!flagName.endsWith(GrassUtils.VARIABLE_FLAG_SUFFIX)) {                                                           ").append("\n");
        codeBuilder.append("                continue;                                                                                                        ").append("\n");
        codeBuilder.append("            }                                                                                                                    ").append("\n");
        codeBuilder.append("                                                                                                                                 ").append("\n");
        codeBuilder.append("            Object valueObj = field.get(this);                                                                                   ").append("\n");
        codeBuilder.append("            if (valueObj instanceof Boolean) {                                                                                   ").append("\n");
        codeBuilder.append("                Boolean flagBoolean = (Boolean) valueObj;                                                                        ").append("\n");
        codeBuilder.append("                if (flagBoolean) {                                                                                               ").append("\n");
        codeBuilder.append("                    flagName = flagName.replaceFirst(GrassUtils.VARIABLE_FLAG_PREFIX_REGEX, \"\");                                 ").append("\n");
        codeBuilder.append("                    flagName = flagName.replaceFirst(GrassUtils.VARIABLE_FLAG_SUFFIX, \"\");                                       ").append("\n");
        codeBuilder.append("                                                                                                                                 ").append("\n");
        codeBuilder.append("                    args.add(\"-\" + flagName);                                                                                    ").append("\n");
        codeBuilder.append("                }                                                                                                                ").append("\n");
        codeBuilder.append("            }                                                                                                                    ").append("\n");
        codeBuilder.append("        }                                                                                                                        ").append("\n");
        codeBuilder.append("                                                                                                                                 ").append("\n");
        codeBuilder.append("        // and parameters                                                                                                        ").append("\n");
        codeBuilder.append("        for( Field field : fields ) {                                                                                            ").append("\n");
        codeBuilder.append("            String parameterName = field.getName();                                                                              ").append("\n");
        codeBuilder.append("            if (!parameterName.endsWith(GrassUtils.VARIABLE_PARAMETER_SUFFIX)) {                                                 ").append("\n");
        codeBuilder.append("                continue;                                                                                                        ").append("\n");
        codeBuilder.append("            }                                                                                                                    ").append("\n");
        codeBuilder.append("            parameterName = parameterName.replaceFirst(GrassUtils.VARIABLE_PARAMETER_PREFIX_REGEX, \"\");                          ").append("\n");
        codeBuilder.append("            parameterName = parameterName.replaceFirst(GrassUtils.VARIABLE_PARAMETER_SUFFIX, \"\");                              ").append("\n");
        codeBuilder.append("                                                                                                                                 ").append("\n");
        codeBuilder.append("            Object valueObj = field.get(this);                                                                                   ").append("\n");
        codeBuilder.append("            if (valueObj != null) {                                                                                              ").append("\n");
        codeBuilder.append("                StringBuilder sb = new StringBuilder();                                                                          ").append("\n");
        codeBuilder.append("                sb.append(parameterName);                                                                                        ").append("\n");
        codeBuilder.append("                sb.append(\"=\");                                                                                                  ").append("\n");
        codeBuilder.append("                sb.append(valueObj.toString());                                                                                  ").append("\n");
        codeBuilder.append("                args.add(sb.toString());                                                                                         ").append("\n");
        codeBuilder.append("            }                                                                                                                    ").append("\n");
        codeBuilder.append("        }                                                                                                                        ").append("\n");
        codeBuilder.append("                                                                                                                                 ").append("\n");
        codeBuilder.append("        String[] argsArray = args.toArray(new String[0]);                                                                        ").append("\n");
        codeBuilder.append("        System.out.println(\"Command launched: \");                                                                                ").append("\n");
        codeBuilder.append("        for( String arg : argsArray ) {                                                                                          ").append("\n");
        codeBuilder.append("            System.out.print(arg + \" \");                                                                                         ").append("\n");
        codeBuilder.append("        }                                                                                                                        ").append("\n");
        codeBuilder.append("        System.out.println();                                                                                                    ").append("\n");
        codeBuilder.append("        System.out.println();                                                                                                    ").append("\n");
        codeBuilder.append("                                                                                                                                 ").append("\n");
        codeBuilder.append("        String[] mapsetForRun = GrassUtils.prepareMapsetForRun(false);                                                           ").append("\n");
        codeBuilder.append("        runner.runModule(argsArray, mapsetForRun[0], mapsetForRun[1]);                                                           ").append("\n");
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
