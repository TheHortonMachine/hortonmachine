/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package org.jgrasstools.gui.spatialtoolbox.core;

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
