package org.jgrasstools.server.jetty.providers.tiles;

public class OsmOLProvider extends ATilesProvider {

    private String name;

    public OsmOLProvider( String name ) {
        super();
        this.name = name;
        valuesMap.put(SOURCE, "new ol.source.OSM()");
    }

    @Override
    public String getName() {
        return name;
    }


}
