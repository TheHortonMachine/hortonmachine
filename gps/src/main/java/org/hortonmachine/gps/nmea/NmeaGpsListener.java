package org.hortonmachine.gps.nmea;

import org.hortonmachine.gps.utils.CurrentGpsInfo;

public interface NmeaGpsListener {
    public void onGpsEvent( CurrentGpsInfo gpsInfo );
}