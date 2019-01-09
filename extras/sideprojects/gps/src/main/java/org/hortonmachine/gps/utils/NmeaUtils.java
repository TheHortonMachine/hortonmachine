/*******************************************************************************
 * Copyright (C) 2018 HydroloGIS S.r.l. (www.hydrologis.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Author: Antonello Andrea (http://www.hydrologis.com)
 ******************************************************************************/
package org.hortonmachine.gps.utils;

import java.util.List;

import net.sf.marineapi.nmea.sentence.GLLSentence;
import net.sf.marineapi.nmea.sentence.GSASentence;
import net.sf.marineapi.nmea.sentence.GSVSentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.util.DataStatus;
import net.sf.marineapi.nmea.util.Date;
import net.sf.marineapi.nmea.util.GpsFixQuality;
import net.sf.marineapi.nmea.util.GpsFixStatus;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.SatelliteInfo;
import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.SatelliteInfoEvent;

public class NmeaUtils {

    public static String toString( SatelliteInfoEvent satInfoEvent ) {
        GpsFixStatus gpsFixStatus = satInfoEvent.getGpsFixStatus();
        double horizontalPrecision = satInfoEvent.getHorizontalPrecision();
        double verticalPrecision = satInfoEvent.getVerticalPrecision();
        double positionPrecision = satInfoEvent.getPositionPrecision();
        StringBuilder sb = new StringBuilder();
        sb.append("GPS fix status: ").append(gpsFixStatus).append("\n");
        sb.append("Horizontal precision").append(horizontalPrecision).append("\n");
        sb.append("Vertical precision").append(verticalPrecision).append("\n");
        sb.append("Position precision").append(positionPrecision).append("\n");

        sb.append("Satellites:").append(positionPrecision).append("\n");
        List<SatelliteInfo> satelliteInfo = satInfoEvent.getSatelliteInfo();
        for( SatelliteInfo sInfo : satelliteInfo ) {
            String id = sInfo.getId();
            int azimuth = sInfo.getAzimuth();
            int noise = sInfo.getNoise();
            int elevation = sInfo.getElevation();

            sb.append("--> id:").append(id);
            sb.append("azimuth:").append(azimuth);
            sb.append("noise:").append(noise);
            sb.append("elevation:").append(elevation);
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String toString( GSASentence satInfoEvent ) {
        GpsFixStatus gpsFixStatus = satInfoEvent.getFixStatus();
        double horizontalPrecision = satInfoEvent.getHorizontalDOP();
        double verticalPrecision = satInfoEvent.getVerticalDOP();
        double positionPrecision = satInfoEvent.getPositionDOP();
        StringBuilder sb = new StringBuilder();
        sb.append("GPS fix status: ").append(gpsFixStatus).append("\n");
        sb.append("Horizontal precision: ").append(horizontalPrecision).append("\n");
        sb.append("Vertical precision: ").append(verticalPrecision).append("\n");
        sb.append("Position precision: ").append(positionPrecision).append("\n");

        String[] satelliteIds = satInfoEvent.getSatelliteIds();
        sb.append("Satellites count:").append(satelliteIds.length).append("\n");
        sb.append("--> ids: ");
        for( String sid : satelliteIds ) {
            sb.append(" ").append(sid);
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String toString( PositionEvent pEvent ) {
        GpsFixQuality fixQuality = pEvent.getFixQuality();
        Date date = pEvent.getDate();
        Time time = pEvent.getTime();
        Double kmHSpeed = pEvent.getSpeed();
        Double course = pEvent.getCourse();
        Position position = pEvent.getPosition();

        StringBuilder sb = new StringBuilder();
        sb.append("GPS fix quality: ").append(fixQuality).append("\n");
        sb.append("Date: ").append(date).append("\n");
        sb.append("Time: ").append(time).append("\n");
        sb.append("Speed [Km/h]: ").append(kmHSpeed).append("\n");
        sb.append("Course: ").append(course).append("\n");
        sb.append("Position: ").append(position).append("\n");

        return sb.toString();
    }

    public static String toString( GLLSentence pEvent ) {
        DataStatus dataStatus = pEvent.getStatus();
        Time time = pEvent.getTime();
        Position position = pEvent.getPosition();

        StringBuilder sb = new StringBuilder();
        sb.append("Data status: ").append(dataStatus).append("\n");
        sb.append("Time: ").append(time).append("\n");
        sb.append("Position: ").append(position).append("\n");

        return sb.toString();
    }


    public static char[] toString( RMCSentence rmc ) {
        // TODO Auto-generated method stub
        return null;
    }

    public static char[] toString( GSVSentence gsv ) {
        // TODO Auto-generated method stub
        return null;
    }

}
