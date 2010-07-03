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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.jgrasstools.gears.JGrassGears;
import org.jgrasstools.gears.libs.modules.ClassField;
import org.jgrasstools.hortonmachine.HortonMachine;

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

        LinkedHashMap<String, List<ClassField>> hmModules = HortonMachine.getInstance().moduleName2Fields;
        LinkedHashMap<String, List<ClassField>> jggModules = JGrassGears.getInstance().moduleName2Fields;

        Set<String> hmNames = hmModules.keySet();
        String[] hmNamesArray = (String[]) hmNames.toArray(new String[hmNames.size()]);

        Set<String> jggNames = jggModules.keySet();
        String[] jggNamesArray = (String[]) jggNames.toArray(new String[jggNames.size()]);

        Arrays.sort(hmNamesArray);
        Arrays.sort(jggNamesArray);

        StringBuilder sb = new StringBuilder();

        sb.append("#summary An overview of the modules implemented in the jgrasstools\n\n");
        sb.append("<wiki:toc max_depth=\"3\" />\n\n");
        sb.append("= JGrassTools Modules Overview =\n");
        sb.append("== !HortonMachine Modules ==\n");

        dumpModules(hmModules, hmNamesArray, sb);

        sb.append("\n<BR/><BR/><BR/>\n\n");
        sb.append("== Gears Modules ==\n");
        dumpModules(jggModules, jggNamesArray, sb);

        System.out.println(sb.toString());
    }

    private static void dumpModules( LinkedHashMap<String, List<ClassField>> modulesMap, String[] modulesNamesArray,
            StringBuilder sb ) {
        for( String moduleName : modulesNamesArray ) {
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
            sb.append("\n\n\n=== ");
            if (isCamelCase)
                sb.append("!");
            sb.append(moduleName).append(" ===\n");

            List<ClassField> fieldsList = modulesMap.get(moduleName);

            sb.append("Parameters\n");
            // input parameters
            for( ClassField classField : fieldsList ) {
                if (classField.fieldName.startsWith("p") || classField.fieldName.startsWith("do")) {
                    sb.append("|| *").append(classField.fieldName).append("* || ").append(classField.fieldDescription).append(
                            " ||\n");
                }
            }
            // input data
            sb.append("\nInput Data\n");
            for( ClassField classField : fieldsList ) {
                if (classField.isIn && !classField.fieldName.startsWith("p") && !classField.fieldName.startsWith("do")) {
                    sb.append("|| *").append(classField.fieldName).append("* || ").append(classField.fieldDescription).append(
                            " ||\n");
                }
            }
            // input data
            sb.append("\nOutput Data\n");
            for( ClassField classField : fieldsList ) {
                if (classField.isOut && !classField.fieldName.startsWith("p") && !classField.fieldName.startsWith("do")) {
                    sb.append("|| *").append(classField.fieldName).append("* || ").append(classField.fieldDescription).append(
                            " ||\n");
                }
            }
        }
    }

    public static void main( String[] args ) {
        createModulesOverview();
    }

}
