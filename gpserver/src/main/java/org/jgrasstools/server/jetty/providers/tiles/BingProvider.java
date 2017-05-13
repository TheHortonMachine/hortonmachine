package org.jgrasstools.server.jetty.providers.tiles;

public class BingProvider extends ATilesProvider {

    private String name;

    public BingProvider( String name, String imagerySet, String key ) {
        super();
        this.name = name;
        valuesMap.put(IMAGERYSET, imagerySet);
        valuesMap.put(IMAGERYSET_KEY, key);
        valuesMap.put(ATTRIBUTION, "Copyright by Bing");
    }

    @Override
    public String getName() {
        return name;
    }

}
