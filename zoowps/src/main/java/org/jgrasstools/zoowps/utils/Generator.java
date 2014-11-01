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
package org.jgrasstools.zoowps.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.UI;

import org.jgrasstools.Modules;
import org.jgrasstools.gears.libs.modules.ClassField;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.files.FileUtilities;

/**
 * This class generates the wps classes and config files.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class Generator {

    private static final String EMPTY_DOC = " - ";

    public static void generate() throws IOException {
        File classesPackageFile = new File("./src/main/java/org/jgrasstools/zoowps");
        if (!classesPackageFile.exists()) {
            throw new IOException("Output package doesn't exist: " + classesPackageFile.getAbsolutePath());
        }

        File configFolderFile = new File("./config");
        if (!configFolderFile.exists()) {
            throw new IOException("Config folder doesn't exist: " + configFolderFile.getAbsolutePath());
        }

        String[] allClasses = Modules.getInstance().allClasses;
        LinkedHashMap<String, List<ClassField>> moduleName2Fields = Modules.getInstance().moduleName2Fields;
        LinkedHashMap<String, Class< ? >> moduleName2Class = Modules.getInstance().moduleName2Class;

        for( String className : allClasses ) {
            Class< ? > clazz = moduleName2Class.get(className);
            String builderAndConfigName = className + "Builder";
            String newClassCanonicalName = "org.jgrasstools.zoowps." + className + "Wps";
            String newClassSimpleName = className + "Wps";

            StringBuilder sb = new StringBuilder();
            sb.append("[" + builderAndConfigName + "]").append("\n");
            Description description = clazz.getAnnotation(Description.class);
            String descriptionStr = description.value();
            sb.append(" Title = " + descriptionStr).append("\n");
            Documentation documentation = clazz.getAnnotation(Documentation.class);
            String documentationStr = EMPTY_DOC;
            if (documentation != null && !documentation.value().endsWith("html")) {
                documentationStr = documentation.value();
            }
            sb.append(" Abstract = " + documentationStr).append("\n");
            sb.append(" processVersion = 1").append("\n");
            sb.append(" storeSupported = true").append("\n");
            sb.append(" statusSupported = true").append("\n");
            sb.append(" serviceProvider = " + newClassCanonicalName).append("\n");
            sb.append(" serviceType = Java").append("\n");
            sb.append(" <MetaData>").append("\n");
            sb.append("   title = " + descriptionStr).append("\n");
            sb.append(" </MetaData>").append("\n");


            sb.append(" <DataInputs>").append("\n");
            List<ClassField> fieldsList = moduleName2Fields.get(className);
            for( ClassField classField : fieldsList ) {
                String fieldName = classField.fieldName;
                if (!acceptField(fieldName)) {
                    continue;
                }

                String fieldDescription = classField.fieldDescription;

                String outputPrefix = "";
                String uiString = classField.uiString;
                if (uiString != null) {
                    if (uiString.equals(JGTConstants.FILEOUT_UI_HINT) || uiString.equals(JGTConstants.FOLDEROUT_UI_HINT)) {
                        outputPrefix = "Output: ";
                    }
                }

                sb.append("  [" + fieldName + "]").append("\n");
                sb.append("   Title = " + outputPrefix + fieldDescription).append("\n");
                sb.append("   Abstract = " + outputPrefix + fieldDescription).append("\n");
                sb.append("   minOccurs = 0").append("\n");
                sb.append("   maxOccurs = 1").append("\n");
                sb.append("   <LiteralData>").append("\n");
                String fieldType = classField.fieldClass.getSimpleName().toLowerCase();
                sb.append("    DataType = " + fieldType).append("\n");
                if (classField.rangeString != null) {
                    sb.append("    range = [" + classField.rangeString + "]").append("\n");
                }
                sb.append("    <Default>").append("\n");
                if (classField.unitsString != null) {
                    sb.append("    uom = " + classField.unitsString).append("\n");
                }
                sb.append("    </Default>").append("\n");
                sb.append("   </LiteralData>").append("\n");
            }
            sb.append(" </DataInputs>").append("\n");
            // sb.append(" <DataOutputs>");
            // sb.append("  [Result]");
            // sb.append("   Title = The hello string");
            // sb.append("   Abstract = The Hello message string.");
            // sb.append("   <LiteralOutput>");
            // sb.append("    DataType = string");
            // sb.append("    <Default>");
            // sb.append("    </Default>");
            // sb.append("   </LiteralOutput>");
            // sb.append(" </DataOutputs>  ");

            File outConfigFile = new File(configFolderFile, builderAndConfigName + ".zcfg");
            FileUtilities.writeFile(sb.toString(), outConfigFile);

            // GENERATE CLASS
            sb = new StringBuilder();
            sb.append("// THIS FILE IS GENERATED, DO NOT EDIT, IT WILL BE OVERWRITTEN \n");
            sb.append("package org.jgrasstools.zoowps; \n");
            sb.append(" \n");
            sb.append("import java.util.Collection; \n");
            sb.append("import java.util.HashMap; \n");
            sb.append(" \n");
            sb.append("import oms3.Access; \n");
            sb.append("import oms3.ComponentAccess; \n");
            sb.append("import oms3.annotations.Execute; \n");
            sb.append("import oms3.annotations.Finalize; \n");
            sb.append("import oms3.annotations.Initialize; \n");
            sb.append(" \n");
            sb.append("import org.geotools.process.ProcessException; \n");
            sb.append("import org.jgrasstools.modules." + className + "; \n");
            sb.append(" \n");
            sb.append("public class " + newClassSimpleName + " { \n");
            sb.append("    public static int " + builderAndConfigName + "( HashMap conf, HashMap inputs, HashMap outputs ) { \n");
            sb.append("        try { \n");
            sb.append("            " + className + " tmpModule = new " + className + "(); \n");
            sb.append(" \n");
            sb.append("            // set the inputs to the model \n");
            sb.append("            ComponentAccess.setInputData(inputs, tmpModule, null); \n");
            sb.append(" \n");
            sb.append("            // trigger execution of the module \n");
            sb.append("            ComponentAccess.callAnnotated(tmpModule, Initialize.class, true); \n");
            sb.append("            ComponentAccess.callAnnotated(tmpModule, Execute.class, false); \n");
            sb.append("            ComponentAccess.callAnnotated(tmpModule, Finalize.class, true); \n");
            sb.append(" \n");
            sb.append("            // get the results \n");
            sb.append("            ComponentAccess cA = new ComponentAccess(tmpModule); \n");
            sb.append("            Collection<Access> outputsCollection = cA.outputs(); \n");
            sb.append(" \n");
            sb.append("            // and put them into the output map \n");
            sb.append("            HashMap<String, Object> outputMap = new HashMap<String, Object>(); \n");
            sb.append("            for( Access access : outputsCollection ) { \n");
            sb.append("                try { \n");
            sb.append("                    String fieldName = access.getField().getName(); \n");
            sb.append("                    Object fieldValue = access.getFieldValue(); \n");
            sb.append("                    outputMap.put(fieldName, fieldValue); \n");
            sb.append("                } catch (Exception e) { \n");
            sb.append("                    throw new ProcessException(e.getLocalizedMessage()); \n");
            sb.append("                } \n");
            sb.append("            } \n");
            sb.append(" \n");
            sb.append("            outputs.put(\"Result\", outputMap); \n");
            sb.append("        } catch (Exception e) { \n");
            sb.append("            e.printStackTrace(); \n");
            sb.append("            outputs.clear(); \n");
            sb.append("            outputs.put(\"Result\", \"ERROR: \" + e.getLocalizedMessage()); \n");
            sb.append("            return 4; \n");
            sb.append("        } \n");
            sb.append("        return 3; \n");
            sb.append("    } \n");
            sb.append("} \n");
            sb.append(" \n");

            File outJavaFile = new File(classesPackageFile, newClassSimpleName + ".java");
            FileUtilities.writeFile(sb.toString(), outJavaFile);

        }

    }

    private static boolean acceptField( String fieldName ) {
        if (fieldName.equals("pm")) {
            return false;
        }
        return true;
    }

    public static void main( String[] args ) throws IOException {
        Generator.generate();
    }

}
