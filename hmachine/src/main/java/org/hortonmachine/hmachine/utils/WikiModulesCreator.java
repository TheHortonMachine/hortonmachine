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
package org.hortonmachine.hmachine.utils;

import java.io.File;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Unit;

import org.hortonmachine.gears.JGrassGears;
import org.hortonmachine.gears.libs.modules.ClassField;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.HortonMachine;

/**
 * Wiki documentation creation helper class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class WikiModulesCreator {

    private static final String outputWikiFolder = "/home/hydrologis/TMP/hortonmachinedocs/";

    private static final String NEWLINE = "\n";
    private static final String IMAGES_HM_BASEURL = "http://wiki.hortonmachine.googlecode.com/git/images/hortonmachine/";
    private static final String TESTCASES_HM_BASEURL = "http://code.google.com/p/hortonmachine/source/browse/hortonmachine/src/test/java/org/hortonmachine/hortonmachine/models/hm/";
    private static final String TESTCASES_HM_BASEPACKAGE = "org.hortonmachine.hmachine.models.hm.";

    // private static final String IMAGES_JG_BASEURL =
    // "http://wiki.hortonmachine.googlecode.com/hg/images/jgrassgears/";
    private static final String TESTCASES_JG_BASEURL = "http://code.google.com/p/hortonmachine/source/browse/jgrassgears/src/test/java/org/hortonmachine/gears/modules/";
    private static final String TESTCASES_JG_BASEPACKAGE = "org.hortonmachine.gears.modules.";

    private static final String DOCSSUFFIX = ".html";

    public static void createModulesPages() throws Exception {

        Map<String, List<ClassField>> hmModules = HortonMachine.getInstance().moduleName2Fields;
        Map<String, List<ClassField>> jggModules = JGrassGears.getInstance().moduleName2Fields;
        Map<String, Class< ? >> hmModulesClasses = HortonMachine.getInstance().moduleName2Class;
        Map<String, Class< ? >> jggModulesClasses = JGrassGears.getInstance().moduleName2Class;

        dump(hmModules, hmModulesClasses, IMAGES_HM_BASEURL, TESTCASES_HM_BASEURL, TESTCASES_HM_BASEPACKAGE);

        dump(jggModules, jggModulesClasses, null, TESTCASES_JG_BASEURL, TESTCASES_JG_BASEPACKAGE);

    }

    private static void dump( Map<String, List<ClassField>> modulesMap,
            Map<String, Class< ? >> modulesClassesMap, String imagesBaseurl, String testcasesBaseurl,
            String testcasesHmBasepackage ) throws Exception {

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
                try {
                    File resourceFile = new File(resource.toURI());
                    documentationStr = FileUtilities.readFile(resourceFile);
                } catch (Exception e) {
                    System.err.println("Error in: " + moduleName);
                }
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
            // general info: status
            sb.append(" Module status: " + parentClassStatus).append(NEWLINE);
            sb.append(NEWLINE);

            // general info: script name
            Name name = abClass.getAnnotation(Name.class);
            if (name != null) {
                String nameStr = name.value();
                sb.append(" Name to use in a script: <b>" + nameStr + "</b>").append(NEWLINE);
                sb.append(NEWLINE);
            }
            // general info: authors
            Author author = abClass.getAnnotation(Author.class);
            if (author != null) {
                String authorNameStr = author.name();
                String authorContactStr = author.contact();
                sb.append(" Authors: " + authorNameStr).append(NEWLINE);
                sb.append(NEWLINE);
                sb.append(" Contacts: " + authorContactStr).append(NEWLINE);
                sb.append(NEWLINE);
            }
            // general info: license
            License license = abClass.getAnnotation(License.class);
            if (license != null) {
                String licenseStr = license.value();
                sb.append(" License: " + licenseStr).append(NEWLINE);
                sb.append(NEWLINE);
            }
            // general info: keywords
            Keywords keywords = abClass.getAnnotation(Keywords.class);
            if (keywords != null) {
                String keywordsStr = keywords.value();
                sb.append(" Keywords: " + keywordsStr).append(NEWLINE);
                sb.append(NEWLINE);
            }
            sb.append(NEWLINE);

            // parameters
            sb.append("<h2>Parameters</h2>").append(NEWLINE);
            sb.append(NEWLINE);
            // parameters: input
            StringBuilder sbTmp = new StringBuilder();
            for( ClassField classField : fieldsList ) {
                if (classField.isOut || classField.fieldName.equals("pm")) {
                    // ignore progress monitor
                    continue;
                }
                Unit unitAnn = abClass.getField(classField.fieldName).getAnnotation(Unit.class);
                String fieldDescription = classField.fieldDescription;
                if (unitAnn != null) {
                    fieldDescription = fieldDescription + " [" + unitAnn.value() + "]";
                }

                sbTmp.append("<tr>").append(NEWLINE);
                sbTmp.append("<td width=\"50%\"> <b>").append(classField.fieldName).append("</b> </td><td width=\"50%\"> ");
                sbTmp.append(fieldDescription).append(" </td>").append(NEWLINE);
                sbTmp.append("</tr>").append(NEWLINE);
            }
            toTable(sb, sbTmp, "Input parameters");

            sb.append(NEWLINE);
            // parameters: output data
            sbTmp = new StringBuilder();
            for( ClassField classField : fieldsList ) {
                if (classField.isIn) {
                    // ignore progress monitor
                    continue;
                }
                Unit unitAnn = abClass.getField(classField.fieldName).getAnnotation(Unit.class);
                String fieldDescription = classField.fieldDescription;
                if (unitAnn != null) {
                    fieldDescription = fieldDescription + " [" + unitAnn.value() + "]";
                }
                sbTmp.append("<tr>").append(NEWLINE);
                sbTmp.append("<td width=\"50%\"> <b>").append(classField.fieldName).append("</b> </td><td width=\"50%\"> ");
                sbTmp.append(fieldDescription).append(" </td>").append(NEWLINE);
                sbTmp.append("</tr>").append(NEWLINE);
            }
            toTable(sb, sbTmp, "Output parameters");
            sb.append(NEWLINE);

            // example result
            if (imagesBaseurl != null) {
                sb.append("<h2>Example result</h2>").append(NEWLINE);
                sb.append(NEWLINE);
                sb.append("<img src=\"" + imagesBaseurl + moduleName.toLowerCase() + ".png" + "\" alt=\"" + moduleName + "\"/>")
                        .append(NEWLINE);
                sb.append("<br>").append(NEWLINE);
            }

            // developer example
            String testName = "Test" + moduleName + ".java";
            String testClassName = testcasesHmBasepackage + "Test" + moduleName;

            boolean doTest = false;
            try {
                Class.forName(testClassName);
                doTest = true;
            } catch (Exception e) {
                // ignore if no testcase
                System.err.println("TESTCASE missign for: " + testName);
            }
            if (doTest) {
                sb.append("<h2>Developer example</h2>").append(NEWLINE);
                sb.append(NEWLINE);
                sb.append("An example usage of the algorithm can be found in the testcases suite: ").append(NEWLINE);

                sb.append("[");
                sb.append(testcasesBaseurl).append(testName);
                sb.append(" ").append(moduleName).append("]");
            }

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
