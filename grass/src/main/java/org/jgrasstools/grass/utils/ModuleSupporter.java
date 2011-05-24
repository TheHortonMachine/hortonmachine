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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.UI;

import org.jgrasstools.gears.libs.exceptions.ModelsIOException;

/**
 * Module supporter for execution.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ModuleSupporter {
    public static void processModule( Object owner ) throws ModelsIOException, IOException, IllegalAccessException, Exception {
        String gisBase = System.getProperty(GrassUtils.GRASS_ENVIRONMENT_GISBASE_KEY);
        File gisBasefile = new File(gisBase);
        if (!gisBasefile.exists()) {
            throw new ModelsIOException("Gisbase variable not properly set. Check your settings!", owner);
        }
        String className = owner.getClass().getSimpleName();
        className = className.replaceAll(GrassUtils.VARIABLE_DOT_SUBSTITUTION, ".");
        File grassCommandFile = new File(gisBase, "bin/" + className);
        if (!grassCommandFile.exists()) {
            throw new ModelsIOException("Command does not exist: " + grassCommandFile.getAbsolutePath(), owner);
        }

        String[] mapsetForRun = GrassUtils.prepareMapsetForRun(false);

        GrassRunner runner = new GrassRunner(System.out, System.err);

        List<String> args = new ArrayList<String>();
        args.add(grassCommandFile.getAbsolutePath());

        Field[] fields = owner.getClass().getFields();
        // first flags
        for( Field field : fields ) {
            String flagName = field.getName();
            if (!flagName.endsWith(GrassUtils.VARIABLE_FLAG_SUFFIX)) {
                continue;
            }

            Object valueObj = field.get(owner);
            if (valueObj instanceof Boolean) {
                Boolean flagBoolean = (Boolean) valueObj;
                if (flagBoolean) {
                    flagName = flagName.replaceFirst(GrassUtils.VARIABLE_FLAG_PREFIX_REGEX, "");
                    flagName = flagName.replaceFirst(GrassUtils.VARIABLE_FLAG_SUFFIX, "");

                    args.add("-" + flagName);
                }
            }
        }

        // and parameters
        for( Field field : fields ) {
            String parameterName = field.getName();
            if (!parameterName.endsWith(GrassUtils.VARIABLE_PARAMETER_SUFFIX)) {
                continue;
            }
            parameterName = parameterName.replaceFirst(GrassUtils.VARIABLE_PARAMETER_PREFIX_REGEX, "");
            parameterName = parameterName.replaceFirst(GrassUtils.VARIABLE_PARAMETER_SUFFIX, "");

            Object valueObj = field.get(owner);
            if (valueObj != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(parameterName);
                sb.append("=");
                sb.append(valueObj.toString());
                args.add(sb.toString());

                // if parameter is input file use r.external
                UI uiAnnotation = field.getAnnotation(UI.class);
                if (uiAnnotation != null) {
                    String value = uiAnnotation.value();
                    if (value.toLowerCase().contains("infile")) {
                        GrassRunner tmpRunner = new GrassRunner(System.out, System.err);
                        String inPath = valueObj.toString();
                        File inFile = new File(inPath);
                        tmpRunner.runModule(new String[]{"r.external", inPath, inFile.getName()}, mapsetForRun[0],
                                mapsetForRun[1]);
                    }
                }
            }

        }

        String[] argsArray = args.toArray(new String[0]);
        System.out.println("Command launched: ");
        for( String arg : argsArray ) {
            System.out.print(arg + " ");
        }
        System.out.println();
        System.out.println();

        runner.runModule(argsArray, mapsetForRun[0], mapsetForRun[1]);
    }
}
