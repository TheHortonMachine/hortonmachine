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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hortonmachine.gears.JGrassGears;
import org.hortonmachine.gears.libs.modules.ClassField;
import org.hortonmachine.hmachine.HortonMachine;

/**
 * Documentation creation helper class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class WikiPageModulesOverviewCreator {

    /**
     * Creates an overview of all the OMS modules for the wiki site.
     */
    public static void createModulesOverview() {

        Map<String, List<ClassField>> hmModules = HortonMachine.getInstance().moduleName2Fields;
        Map<String, List<ClassField>> jggModules = JGrassGears.getInstance().moduleName2Fields;
        Map<String, Class< ? >> hmModulesClasses = HortonMachine.getInstance().moduleName2Class;
        Map<String, Class< ? >> jggModulesClasses = JGrassGears.getInstance().moduleName2Class;

        Set<String> hmNames = hmModules.keySet();
        String[] hmNamesArray = (String[]) hmNames.toArray(new String[hmNames.size()]);

        Set<String> jggNames = jggModules.keySet();
        String[] jggNamesArray = (String[]) jggNames.toArray(new String[jggNames.size()]);

        Arrays.sort(hmNamesArray);
        Arrays.sort(jggNamesArray);

        StringBuilder sb = new StringBuilder();

        sb.append("#summary An overview of the modules implemented in the HortonMachine\n\n");
        sb.append("<wiki:toc max_depth=\"4\" />\n\n");
        sb.append("= HortonMachine Modules Overview =\n");
        sb.append("== !HortonMachine Modules ==\n");

        String status = "CERTIFIED";
        sb.append("=== Release ready ==\n");
        dumpModules(hmModules, hmModulesClasses, hmNamesArray, sb, status);
        status = "TESTED";
        sb.append("=== Tested but not for upcoming release ==\n");
        dumpModules(hmModules, hmModulesClasses, hmNamesArray, sb, status);
        status = "DRAFT";
        sb.append("=== Module that are not passing the QA rules yet ==\n");
        dumpModules(hmModules, hmModulesClasses, hmNamesArray, sb, status);

        sb.append("\n<BR/><BR/><BR/>\n\n");
        sb.append("== Gears Modules ==\n");
        status = "CERTIFIED";
        sb.append("=== Release ready ==\n");
        dumpModules(jggModules, jggModulesClasses, jggNamesArray, sb, status);
        status = "TESTED";
        sb.append("=== Tested but not for upcoming release ==\n");
        dumpModules(jggModules, jggModulesClasses, jggNamesArray, sb, status);
        status = "DRAFT";
        sb.append("=== Module that are not passing the QA rules yet ==\n");
        dumpModules(jggModules, jggModulesClasses, jggNamesArray, sb, status);

        System.out.println(sb.toString());
    }

    private static void dumpModules( Map<String, List<ClassField>> modulesMap,
            Map<String, Class< ? >> modulesClasses, String[] modulesNamesArray, StringBuilder sb, String status ) {
        for( String moduleName : modulesNamesArray ) {
            List<ClassField> fieldsList = modulesMap.get(moduleName);
            if (fieldsList == null) {
                throw new RuntimeException("fieldsList == null in module: " + moduleName);
            }
            if (fieldsList.size() > 0) {
                ClassField tmp = fieldsList.get(0);
                String parentClassStatus = tmp.parentClassStatus;
                if (!parentClassStatus.equals(status)) {
                    continue;
                }
            }

            // check if it is camelcase
            boolean isCamelCase = false;
            int length = moduleName.length();
            for( int i = 1; i < length; i++ ) {
                char charAt = moduleName.charAt(i);
                String charAtStr = new String(new char[]{charAt});
                if (charAtStr.matches("[A-Z]")) {
                    isCamelCase = true;
                }
                if (i == 1 && isCamelCase) {
                    isCamelCase = false;
                    break;
                }
            }

            sb.append("\n<BR/><BR/>\n----\n<BR/> ");
            sb.append("\n\n\n==== ");
            if (isCamelCase)
                sb.append("!");
            sb.append(moduleName).append(" ====\n");

            sb.append("Name to use in scripts: *").append(modulesClasses.get(moduleName).getCanonicalName()).append("*\n\n");

            sb.append("Parameters\n");
            // input parameters
            for( ClassField classField : fieldsList ) {
                if (classField.fieldName.startsWith("p") || classField.fieldName.startsWith("do")) {
                    sb.append("|| *").append(classField.fieldName).append("* || ").append(classField.fieldDescription)
                            .append(" ||\n");
                }
            }
            // input data
            sb.append("\nInput Data\n");
            for( ClassField classField : fieldsList ) {
                if (classField.isIn && !classField.fieldName.startsWith("p") && !classField.fieldName.startsWith("do")) {
                    sb.append("|| *").append(classField.fieldName).append("* || ").append(classField.fieldDescription)
                            .append(" ||\n");
                }
            }
            // input data
            sb.append("\nOutput Data\n");
            for( ClassField classField : fieldsList ) {
                if (classField.isOut && !classField.fieldName.startsWith("p") && !classField.fieldName.startsWith("do")) {
                    sb.append("|| *").append(classField.fieldName).append("* || ").append(classField.fieldDescription)
                            .append(" ||\n");
                }
            }
        }
    }

    public static void main( String[] args ) {
        createModulesOverview();
    }

}
