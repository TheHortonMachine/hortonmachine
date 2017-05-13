package org.jgrasstools.server.jetty.providers.css;

import java.util.List;

public interface ICssProvider {
    
    default String getName(){
        return "cssprovider";
    }
    
    /**
     * Getter for the full url as for example: <link rel="stylesheet" type="text/css" href="huberg.css" />
     * 
     * @return the list of urls.
     */
    List<String> getUrls();
}
