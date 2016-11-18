package org.jgrasstools.server.jetty.providers.tiles;

import org.jgrasstools.server.jetty.providers.tilesgenerator.ITilesObject;

public interface ITilesProvider  extends ITilesObject{
    
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
}
