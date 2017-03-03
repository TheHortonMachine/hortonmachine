package org.jgrasstools.osm.examples;

import de.westnordost.osmapi.OsmConnection;
import oauth.signpost.OAuthConsumer;

public class OsmConnectionHandler {

    private static final String DEFAULT_URL = "https://api.openstreetmap.org/api/0.6/";
    private static final String DEVEL_TEST_URL = "http://api06.dev.openstreetmap.org/api/0.6/";

    private OsmConnection osm;

    public OsmConnectionHandler( String serverPath, String userAgent, OAuthConsumer authConsumer ) {
        if (serverPath == null) {
            serverPath = DEFAULT_URL;
        }
        if (userAgent == null) {
            userAgent = "my user agent";
        }
        osm = new OsmConnection(serverPath, userAgent, authConsumer);
    }

    public OsmConnectionHandler() {
        this(null, null, null);
    }
    
    public OsmConnection getOsmConnection() {
        return osm;
    }
}
