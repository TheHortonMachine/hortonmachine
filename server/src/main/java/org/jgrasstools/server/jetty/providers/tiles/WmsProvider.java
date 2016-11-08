package org.jgrasstools.server.jetty.providers.tiles;

public class WmsProvider implements ITilesProvider {

    private String name;
    private boolean visible;
    private String url;
    private String params;
    private String serverType;
    private String source;
    private boolean isDefault;

    public WmsProvider( String name, String source, boolean visible, String url, String params, String serverType,
            boolean isDefault ) {
        this.name = name;
        this.source = source;
        this.visible = visible;
        this.url = url;
        this.params = params;
        this.serverType = serverType;
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
        return null;
    }

    @Override
    public String getImagerySet() {
        return null;
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
