package org.jgrasstools.server.jetty.providers.tiles;

public class EsriTmsProvider extends ATilesProvider {

    private String name;

    public EsriTmsProvider( String name ) {
        super();
        this.name = name;
        valuesMap.put(URL, "http://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}");
        valuesMap.put(ATTRIBUTION, "Tiles &copy; Esri");
    }

    @Override
    public String getName() {
        return name;
    }

}
