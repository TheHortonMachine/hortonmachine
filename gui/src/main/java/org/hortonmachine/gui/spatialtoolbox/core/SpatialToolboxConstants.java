/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gui.spatialtoolbox.core;

import java.text.SimpleDateFormat;
import java.util.HashMap;

public class SpatialToolboxConstants {
    public static String LIBS_MAIN_FOLDER_NAME = "spatialtoolbox";
    public static String LIBS_SUBFOLDER_NAME = "libs";
    public static String MODULES_SUBFOLDER_NAME = "modules";


    public static String dateTimeFormatterYYYYMMDDHHMMSS_string = "yyyy-MM-dd HH:mm:ss";
    public static SimpleDateFormat dateTimeFormatterYYYYMMDDHHMMSS = new SimpleDateFormat(dateTimeFormatterYYYYMMDDHHMMSS_string);

    public static String LOGLEVEL_GUI_ON = "ON";
    public static String LOGLEVEL_GUI_OFF = "OFF";
    public static String[] LOGLEVELS_GUI = {LOGLEVEL_GUI_OFF, LOGLEVEL_GUI_ON};
    public static HashMap<String, String> LOGLEVELS_MAP = new HashMap<String, String>(2);
    static {
        LOGLEVELS_MAP.put(LOGLEVEL_GUI_OFF, "OFF");
        LOGLEVELS_MAP.put(LOGLEVEL_GUI_ON, "ALL");
    }

    public static String[] HEAPLEVELS = {"64", "128", "250", "500",//
            "1000", "1500", "2000", "4000", //
            "6000", "8000", "10000", "12000", "16000", //
            "18000", "24000"};

    public static final int LISTHEIGHT = 8;

    // vars labels
    public static final String GRASSFILE_UI_HINT = "grassfile";

    /**
     * Key used to set and retrieve the grass installation location.
     * 
     * <p>Example on linux: /usr/lib/grass64
     */
    public static String GRASS_ENVIRONMENT_GISBASE_KEY = "jgt-grass.gisbase";
    public static String GRASS_ENVIRONMENT_SHELL_KEY = "jgt-grass.shell";

    public static String MAPCALCHISTORY_KEY = "mapcalc-history";
    public static String MAPCALCHISTORY_SEPARATOR = "@@@";

}
