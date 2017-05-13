package org.jgrasstools.server.jetty.providers.tiles;

public class OsmTmsProvider extends ATilesProvider {

    private String name;

    public OsmTmsProvider( String name ) {
        super();
        this.name = name;
        valuesMap.put(URL, "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png");
        valuesMap.put(ATTRIBUTION, "&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>");
    }

    @Override
    public String getName() {
        return name;
    }

}
