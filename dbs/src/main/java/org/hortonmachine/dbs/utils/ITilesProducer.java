package org.hortonmachine.dbs.utils;

import org.locationtech.jts.geom.Envelope;

public interface ITilesProducer {

    int getMinZoom();

    int getMaxZoom();

    boolean cancelled();

    int getTileSize();

    byte[] getTileData( Envelope tileBounds3857 );

    void startWorkingOnZoomLevel( int zoomLevel, int workCount );

    void worked();

    void done();

}
