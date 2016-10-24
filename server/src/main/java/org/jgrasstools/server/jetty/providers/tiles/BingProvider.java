package org.jgrasstools.server.jetty.providers.tiles;

public class BingProvider implements ITilesProvider {

    private String name;
    private boolean visible;
    private String url;
    private String params;
    private String serverType;
    private String imagerySet;
    private String key;
    private String source;
    private boolean isDefault;

    public BingProvider( String name, String source, String imagerySet, String key, boolean isDefault ) {
        this.name = name;
        this.source = source;
        this.imagerySet = imagerySet;
        this.key = key;
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
        return source;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

}
