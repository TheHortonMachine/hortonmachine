/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package org.hortonmachine.nww.layers.defaults.raster;

import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.Logging;

/**
 * @author Tom Gaskins
 * @version $Id: HTTPRetriever.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class FileRetriever extends URLRetriever {

    public FileRetriever( URL url, RetrievalPostProcessor postProcessor ) {
        super(url, postProcessor);
    }

    protected ByteBuffer doRead( URLConnection connection ) throws Exception {
        if (connection == null) {
            String msg = Logging.getMessage("nullValue.ConnectionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return super.doRead(connection);

    }
}
