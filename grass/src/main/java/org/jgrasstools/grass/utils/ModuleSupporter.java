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

/**
 * Module supporter for execution.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ModuleSupporter {
    private ModuleSupporter() {}

    public static void processModule( Object owner ) throws IOException, IllegalAccessException, Exception {

        String gisBase = System.getProperty(GrassUtils.GRASS_ENVIRONMENT_GISBASE_KEY);
        if (gisBase == null || !new File(gisBase).exists()) {
            throw new IOException("Gisbase variable not properly set. Check your settings!");
        }
        String className = owner.getClass().getSimpleName();
        className = className.replaceAll(GrassUtils.VARIABLE_DOT_SUBSTITUTION, ".");
        File grassCommandFile = new File(gisBase, "bin/" + className);

        GrassModuleRunnerWithScript runner = new GrassModuleRunnerWithScript(System.out, System.err);

        List<String> args = new ArrayList<>();
        args.add(grassCommandFile.getName());

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

                    String cleanFlag = flagName.trim().toLowerCase();
                    if (cleanFlag.equals("overwrite") || cleanFlag.equals("verbose") || cleanFlag.equals("quiet")) {
                        args.add("--" + flagName);
                    } else {
                        args.add("-" + flagName);
                    }
                }
            }
        }

        String mapset = null;

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

                String valueString = valueObj.toString();

                // if parameter is input file use r.external
                UI uiAnnotation = field.getAnnotation(UI.class);
                if (uiAnnotation != null) {
                    String value = uiAnnotation.value();
                    if (value.toLowerCase().contains("infile")) {
                        String inPath = valueObj.toString();
                        File inFile = new File(inPath);
                        if (isGrassFile(inPath)) {
                            String name = getGrassRasterName(inPath);
                            File mapsetFile = getMapsetFile(inPath);
                            if (mapset == null)
                                mapset = mapsetFile.getAbsolutePath();
                            valueString = name + "@" + mapsetFile.getName();
                        } else {
                            // TODO
                            throw new RuntimeException("Non grass files are not supported yet!");
                            // String[] mapsetForRun = GrassUtils.prepareMapsetForRun(false);
                            // GrassRunner tmpRunner = new GrassRunner(System.out, System.err);
                            // tmpRunner.runModule(new String[]{"r.external", inPath,
                            // inFile.getName()}, mapsetForRun[0],
                            // mapsetForRun[1]);
                            // mapset = mapsetForRun[0];
                        }
                    } else if (value.toLowerCase().contains("outfile")) {
                        String outPath = valueObj.toString();
                        if (isGrassFile(outPath)) {
                            String name = getGrassRasterName(outPath);
                            File mapsetFile = getMapsetFile(outPath);
                            if (mapset == null)
                                mapset = mapsetFile.getAbsolutePath();
                            valueString = name + "@" + mapsetFile.getName();
                        }
                    }
                }

                sb.append(valueString);
                args.add(sb.toString());

            }

        }

        String[] argsArray = args.toArray(new String[0]);
        System.out.println("Command launched: ");
        for( String arg : argsArray ) {
            System.out.print(arg + " ");
        }
        System.out.println();
        System.out.println();

        if (mapset == null) {
            mapset = GrassUtils.prepareMapsetForRun(false);
        }
        runner.runModule(argsArray, mapset);
    }
    /**
     * Checks if the given path is a GRASS raster file.
     * 
     * <p>Note that there is no check on the existence of the file.
     * 
     * @param path the path to check.
     * @return true if the file is a grass raster.
     */
    public static boolean isGrassFile( String path ) {
        File file = new File(path);
        File cellFolderFile = file.getParentFile();
        File mapsetFile = cellFolderFile.getParentFile();
        File windFile = new File(mapsetFile, "WIND");
        return cellFolderFile.getName().equalsIgnoreCase("cell") && windFile.exists();
    }

    public static String getLocationPath( String path ) {
        File file = new File(path);
        File cellFolderFile = file.getParentFile();
        File mapsetFile = cellFolderFile.getParentFile();
        return mapsetFile.getParent();
    }

    public static File getMapsetFile( String path ) {
        File file = new File(path);
        File cellFolderFile = file.getParentFile();
        return cellFolderFile.getParentFile();
    }

    public static String getGrassRasterName( String path ) {
        File file = new File(path);
        return file.getName();
    }
}
