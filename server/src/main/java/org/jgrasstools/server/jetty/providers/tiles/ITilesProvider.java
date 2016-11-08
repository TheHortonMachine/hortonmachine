package org.jgrasstools.server.jetty.providers.tiles;

public interface ITilesProvider {
    
    String getName();
    
    boolean isVisible();
    
    String getUrl();
    
    String getParams();
    
    String getServerType();
    
    default String getMaxZoom(){
        return "19";
    }

    default String getMinZoom(){
        return "2";
    }
    
    String getKey();

    String getImagerySet();
    
    /**
     * If the source is not null, the other values apart of the name are ignored.
     * 
     * @return the source or null.
     */
    String getSource();
    
    boolean isDefault();

}
