package org.jgrasstools.server.jetty.providers.tiles;

import java.util.HashMap;

import org.jgrasstools.server.jetty.providers.tilesgenerator.ITilesObject;

public abstract class ATilesProvider implements ITilesObject {

    public static final String URL = "URL";
    public static final String LAYERNAME = "LAYERNAME";
    public static final String ATTRIBUTION = "ATTRIBUTION";
    public static final String PARAMS = "PARAMS";
    public static final String SERVERTYPE = "SERVERTYPE";
    public static final String SOURCE = "SOURCE";
    public static final String IMAGERYSET = "IMAGERYSET";
    public static final String IMAGERYSET_KEY = "IMAGERYSET_KEY";
    public static final String MINZOOM = "MINZOOM";
    public static final String MAXZOOM = "MAXZOOM";
    public static final String MAXNATIVEZOOM = "MAXNATIVEZOOM";

    protected HashMap<String, String> valuesMap = new HashMap<>();
    
    public ATilesProvider() {
        valuesMap.put(MAXZOOM, "25");
        valuesMap.put(MAXNATIVEZOOM, "19");
        valuesMap.put(MINZOOM, "3");
    }
    
    public String getParameter( String key ) {
        String value = valuesMap.get(key);
        if (value == null) {
            value = "";
        }
        return value;
    }

}
