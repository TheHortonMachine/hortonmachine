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

import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.GLLSentence;
import net.sf.marineapi.nmea.sentence.GSASentence;
import net.sf.marineapi.nmea.sentence.GSVSentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.util.DataStatus;
import net.sf.marineapi.nmea.util.Date;
import net.sf.marineapi.nmea.util.FaaMode;
import net.sf.marineapi.nmea.util.GpsFixQuality;
import net.sf.marineapi.nmea.util.GpsFixStatus;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.SatelliteInfo;
import net.sf.marineapi.nmea.util.Time;

/**
 * An object that holds and parses GPS information.
 */
public class CurrentGpsInfo {
    private GpsFixStatus gpsFixStatus = GpsFixStatus.GPS_NA;
    private double horizontalPrecision = -1;
    private double verticalPrecision = -1;
    private double positionPrecision = -1;
    private String[] satelliteIds;
    private List<SatelliteInfo> satelliteInfo;
    private Date date;
    private Time time;
//    private double correctedCourse = -1;
    private FaaMode faaMode = FaaMode.NONE;
    private Position position;
    private DataStatus dataStatus = DataStatus.VOID;
    private double kmHSpeed = -1;
    private GpsFixQuality gpsFixQuality = GpsFixQuality.INVALID;
    private int fieldCount = -1;

    private static long count = 1;

    /**
     * Method to add a new {@link GSASentence}.
     * 
     * @param gsa the sentence to add.
     */
    public void addGSA( GSASentence gsa ) {
        try {
            if (gsa.isValid()) {
                gpsFixStatus = gsa.getFixStatus();
                horizontalPrecision = gsa.getHorizontalDOP();
                verticalPrecision = gsa.getVerticalDOP();
                positionPrecision = gsa.getPositionDOP();
                satelliteIds = gsa.getSatelliteIds();
            }
        } catch (Exception e) {
            // ignore it, this should be handled in the isValid,
            // if an exception is thrown, we can't deal with it here.
        }
    }

    /**
     * Method to add a new {@link GSVSentence}.
     * 
     * @param gsv the sentence to add.
     */
    public void addGSV( GSVSentence gsv ) {
        try {
            if (gsv.isValid())
                satelliteInfo = gsv.getSatelliteInfo();
        } catch (Exception e) {
            // ignore it, this should be handled in the isValid,
            // if an exception is thrown, we can't deal with it here.
        }
    }

    /**
     * Method to add a new {@link GLLSentence}.
     * 
     * @param gll the sentence to add.
     */
    public void addGLL( GLLSentence gll ) {
        try {
            if (gll.isValid())
                position = gll.getPosition();
        } catch (Exception e) {
            // ignore it, this should be handled in the isValid,
            // if an exception is thrown, we can't deal with it here.
        }
    }

    /**
     * Method to add a new {@link GGASentence}.
     * 
     * @param gaa the sentence to add.
     */
    public void addGGA( GGASentence gga ) {
        try {
            if (gga.isValid()) {
                gpsFixQuality = gga.getFixQuality();
                position = gga.getPosition();
                if (time == null) {
                    time = gga.getTime();
                }
            }
        } catch (Exception e) {
            // ignore it, this should be handled in the isValid,
            // if an exception is thrown, we can't deal with it here.
        }
    }

    /**
     * Method to add a new {@link RMCSentence}.
     * 
     * @param rmc the sentence to add.
     */
    public void addRMC( RMCSentence rmc ) {
        try {
            if (rmc.isValid()) {
                date = rmc.getDate();
                time = rmc.getTime();
//            correctedCourse = rmc.getCorrectedCourse();
                faaMode = rmc.getMode();
                position = rmc.getPosition();
                dataStatus = rmc.getStatus();
                kmHSpeed = rmc.getSpeed();
                fieldCount = rmc.getFieldCount();
            }
        } catch (Exception e) {
            // ignore it, this should be handled in the isValid,
            // if an exception is thrown, we can't deal with it here.
        }
    }

    public boolean isValid() {
        if (dataStatus == null || faaMode == null || DataStatus.VOID.equals(dataStatus)
                || (fieldCount > 11 && FaaMode.NONE.equals(faaMode))) {
            return false;
        } else if (GpsFixQuality.INVALID.equals(gpsFixQuality)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================\n");
        sb.append("Point number from app start: ").append(count++).append("\n");
        sb.append("THE CURRENT INFO IS VALID: ").append(isValid()).append("\n");
        sb.append("GPS fix status: ").append(gpsFixStatus).append("\n");
        sb.append("GPS fix quality: ").append(gpsFixQuality).append("\n");
        sb.append("Data status: ").append(dataStatus).append("\n");
        sb.append("Faa mode: ").append(faaMode).append("\n");
        sb.append("Horizontal precision: ").append(horizontalPrecision).append("\n");
        sb.append("Vertical precision: ").append(verticalPrecision).append("\n");
        sb.append("Position precision: ").append(positionPrecision).append("\n");

        if (date != null && time != null)
            sb.append("Timestamp: ").append(date.toISO8601(time)).append("\n");
        sb.append("Speed [Km/h]: ").append(kmHSpeed).append("\n");
//        sb.append("Course: ").append(correctedCourse).append("\n");
        if (position != null)
            sb.append("Position: ").append(position).append("\n");

        sb.append("Satellites count:").append(satelliteIds.length).append("\n");
        sb.append("--> ids: ");
        for( String sid : satelliteIds ) {
            sb.append(" ").append(sid);
        }
        sb.append("\n");
        if (satelliteInfo != null && satelliteInfo.size() > 0) {
            sb.append("Satellites info:").append("\n");
            for( SatelliteInfo sInfo : satelliteInfo ) {
                String id = sInfo.getId();
                int azimuth = sInfo.getAzimuth();
                int noise = sInfo.getNoise();
                int elevation = sInfo.getElevation();

                sb.append("--> id: ").append(id);
                sb.append(" azimuth: ").append(azimuth);
                sb.append(" noise: ").append(noise);
                sb.append(" elevation: ").append(elevation);
                sb.append("\n");
            }
        }
        sb.append("========================================================================\n");
        return sb.toString();
    }

}
