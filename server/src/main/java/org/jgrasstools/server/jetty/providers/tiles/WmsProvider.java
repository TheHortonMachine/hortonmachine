package org.jgrasstools.server.jetty.providers.tiles;

public class WmsProvider extends ATilesProvider {

    private String name;

    public WmsProvider( String name, String url, String layerName, String attribution, String params, String serverType ) {
        super();
        this.name = name;

        valuesMap.put(URL, url);
        valuesMap.put(ATTRIBUTION, attribution);
        valuesMap.put(LAYERNAME, layerName);
        valuesMap.put(PARAMS, params);
        valuesMap.put(SERVERTYPE, serverType);
    }

    @Override
    public String getName() {
        return name;
    }

}
