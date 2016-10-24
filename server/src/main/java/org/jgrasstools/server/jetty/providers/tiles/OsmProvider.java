package org.jgrasstools.server.jetty.providers.tiles;

public class OsmProvider implements ITilesProvider {

    private String name;
    private boolean visible;
    private String url;
    private String params;
    private String serverType;
    private String imagerySet;
    private String key;
    private boolean isDefault;

    public OsmProvider( String name, boolean isVisible, boolean isDefault ) {
        this.name = name;
        visible = isVisible;
        this.isDefault = isDefault;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getParams() {
        return params;
    }

    @Override
    public String getServerType() {
        return serverType;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getImagerySet() {
        return imagerySet;
    }

    @Override
    public String getSource() {
        return "new ol.source.OSM()";
    }
    
    @Override
    public boolean isDefault() {
        return isDefault;
    }

}
