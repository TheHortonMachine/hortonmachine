/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.hortonmachine.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Name;

import org.jgrasstools.gears.JGrassGears;
import org.jgrasstools.gears.libs.modules.ClassField;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.hortonmachine.HortonMachine;

/**
 * Wiki documentation creation helper class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class WikiModulesCreator {

    private static final String outputWikiFolder = "/home/moovida/TMP/jgt_wiki_generated/";

    private static final String NEWLINE = "\n";
    private static final String IMAGES_HM_BASEURL = "http://wiki.jgrasstools.googlecode.com/hg/images/hortonmachine/";
    private static final String TESTCASES_HM_BASEURL = "http://code.google.com/p/jgrasstools/source/browse/hortonmachine/src/test/java/org/jgrasstools/hortonmachine/models/hm/";

    private static final String IMAGES_JG_BASEURL = "http://wiki.jgrasstools.googlecode.com/hg/images/jgrassgears/";
    private static final String TESTCASES_JG_BASEURL = "http://code.google.com/p/jgrasstools/source/browse/jgrassgears/src/test/java/org/jgrasstools/gears/modules/";

    private static final String DOCSSUFFIX = ".html";

    public static void createModulesPages() throws Exception {

        LinkedHashMap<String, List<ClassField>> hmModules = HortonMachine.getInstance().moduleName2Fields;
        LinkedHashMap<String, List<ClassField>> jggModules = JGrassGears.getInstance().moduleName2Fields;
        LinkedHashMap<String, Class< ? >> hmModulesClasses = HortonMachine.getInstance().moduleName2Class;
        LinkedHashMap<String, Class< ? >> jggModulesClasses = JGrassGears.getInstance().moduleName2Class;

        dump(hmModules, hmModulesClasses, IMAGES_HM_BASEURL, TESTCASES_HM_BASEURL);

        dump(jggModules, jggModulesClasses, IMAGES_JG_BASEURL, TESTCASES_JG_BASEURL);

    }

    private static void dump( LinkedHashMap<String, List<ClassField>> modulesMap,
            LinkedHashMap<String, Class< ? >> modulesClassesMap, String imagesBaseurl, String testcasesBaseurl ) throws Exception {

        Set<String> nameSet = modulesClassesMap.keySet();
        for( String moduleName : nameSet ) {

            StringBuilder sb = new StringBuilder();

            Class< ? > abClass = modulesClassesMap.get(moduleName);

            // summary line
            Description description = abClass.getAnnotation(Description.class);
            String descriptionStr = description.value();

            sb.append("#summary " + descriptionStr);
            sb.append(NEWLINE);
            sb.append(NEWLINE);

            // modules documentation
            sb.append("<h2>Description</h2>").append(NEWLINE);
            sb.append(NEWLINE);

            Documentation documentation = abClass.getAnnotation(Documentation.class);
            if (documentation == null) {
                System.out.println("Jumping " + moduleName);
                continue;
            }
            String documentationStr = documentation.value();
            if (documentationStr.endsWith(DOCSSUFFIX)) {
                // have to get the file
                URL resource = abClass.getResource(documentationStr);
                File resourceFile = new File(resource.toURI());
                documentationStr = FileUtilities.readFile(resourceFile);
            }
            sb.append(documentationStr);

            // general info
            sb.append(NEWLINE);
            sb.append(NEWLINE);
            String parentClassStatus = "not defined";
            List<ClassField> fieldsList = modulesMap.get(moduleName);
            if (fieldsList.size() > 0) {
                ClassField tmp = fieldsList.get(0);
                parentClassStatus = tmp.parentClassStatus;
            }
            sb.append("<h2>General Information</h2>").append(NEWLINE);
            sb.append(NEWLINE);
            sb.append("Module status: " + parentClassStatus).append(NEWLINE);
            sb.append(NEWLINE);
            sb.append(NEWLINE);
            Name name = abClass.getAnnotation(Name.class);
            if (name == null) {
                System.out.println("Jumping " + moduleName);
                continue;
            }
            String nameStr = name.value();
            sb.append("Name to use in a script: <b>" + nameStr + "</b>").append(NEWLINE);
            sb.append(NEWLINE);

            // parameters
            sb.append("<h2>Parameters</h2>").append(NEWLINE);
            sb.append(NEWLINE);
            // parameters: fields
            StringBuilder sbTmp = new StringBuilder();
            for( ClassField classField : fieldsList ) {
                if (classField.fieldName.startsWith("p") || classField.fieldName.startsWith("do")) {
                    sbTmp.append("<tr>").append(NEWLINE);
                    sbTmp.append("<td width=\"50%\"> *").append(classField.fieldName).append("* </td><td width=\"50%\"> ");
                    sbTmp.append(classField.fieldDescription).append(" </td>").append(NEWLINE);
                    sbTmp.append("</tr>").append(NEWLINE);
                }
            }
            toTable(sb, sbTmp, "Input parameters");

            sb.append(NEWLINE);
            // parameters: input data
            sbTmp = new StringBuilder();
            for( ClassField classField : fieldsList ) {
                if (classField.isIn && !classField.fieldName.startsWith("p") && !classField.fieldName.startsWith("do")) {
                    sbTmp.append("<tr>").append(NEWLINE);
                    sbTmp.append("<td width=\"50%\"> *").append(classField.fieldName).append("* </td><td width=\"50%\"> ");
                    sbTmp.append(classField.fieldDescription).append(" </td>").append(NEWLINE);
                    sbTmp.append("</tr>").append(NEWLINE);
                }
            }
            toTable(sb, sbTmp, "Input data");

            sb.append(NEWLINE);
            // parameters: output data
            sbTmp = new StringBuilder();
            for( ClassField classField : fieldsList ) {
                if (classField.isOut && !classField.fieldName.startsWith("p") && !classField.fieldName.startsWith("do")) {
                    sbTmp.append("<tr>").append(NEWLINE);
                    sbTmp.append("<td width=\"50%\"> *").append(classField.fieldName).append("* </td><td width=\"50%\"> ");
                    sbTmp.append(classField.fieldDescription).append(" </td>").append(NEWLINE);
                    sbTmp.append("</tr>").append(NEWLINE);
                }
            }
            toTable(sb, sbTmp, "Output data");
            sb.append(NEWLINE);

            // example result
            sb.append("<h2>Example result</h2>").append(NEWLINE);
            sb.append(NEWLINE);
            sb.append("<img src=\"" + imagesBaseurl + moduleName.toLowerCase() + ".png" + "\" alt=\"" + moduleName + "\"/>")
                    .append(NEWLINE);
            sb.append("<br>").append(NEWLINE);

            // developer example
            sb.append("<h2>Developer example</h2>").append(NEWLINE);
            sb.append(NEWLINE);
            sb.append("An example usage of the algorithm can be found in the testcases suite: ").append(NEWLINE);

            String testName = "Test" + moduleName + ".java";
            sb.append("[");
            sb.append(testcasesBaseurl).append(testName);
            sb.append(" ").append(moduleName).append("]");

            FileUtilities.writeFile(sb.toString(), new File(outputWikiFolder, moduleName + ".wiki"));
        }
    }

    private static void toTable( StringBuilder sbToAppendTo, StringBuilder tableContentSb, String tableTitle ) {
        if (tableContentSb.length() > 0) {
            sbToAppendTo.append("<h3>" + tableTitle + "</h3>").append(NEWLINE);
            sbToAppendTo.append("<table width=\"70%\" border=\"1\" cellpadding=\"10\">").append(NEWLINE);
            sbToAppendTo.append(tableContentSb);
            sbToAppendTo.append("</table>").append(NEWLINE);
        }
    }

    public static void main( String[] args ) throws Exception {
        createModulesPages();
    }

}
