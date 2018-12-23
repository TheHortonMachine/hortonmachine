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

public class CurrentGpsInfo {
    private GpsFixStatus gpsFixStatus;
    private double horizontalPrecision;
    private double verticalPrecision;
    private double positionPrecision;
    private String[] satelliteIds;
    private List<SatelliteInfo> satelliteInfo;
    private Date date;
    private Time time;
    private double correctedCourse;
    private FaaMode faaMode;
    private Position position;
    private DataStatus dataStatus;
    private double kmHSpeed;
    private GpsFixQuality gpsFixQuality;
    private int fieldCount = -1;

    public void addGSA( GSASentence gsa ) {
        gpsFixStatus = gsa.getFixStatus();
        horizontalPrecision = gsa.getHorizontalDOP();
        verticalPrecision = gsa.getVerticalDOP();
        positionPrecision = gsa.getPositionDOP();
        satelliteIds = gsa.getSatelliteIds();
    }

    public void addGSV( GSVSentence gsv ) {
        satelliteInfo = gsv.getSatelliteInfo();
    }

    public void addGLL( GLLSentence gll ) {
        position = gll.getPosition();
    }

    public void addGGA( GGASentence gga ) {
        gpsFixQuality = gga.getFixQuality();
        position = gga.getPosition();
        if (time == null) {
            time = gga.getTime();
        }
    }

    public void addRMC( RMCSentence rmc ) {
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
    }

    protected boolean isValid() {

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
        sb.append("GPS fix status: ").append(gpsFixStatus).append("\n");
        sb.append("GPS fix quality: ").append(gpsFixQuality).append("\n");
        sb.append("Data status: ").append(dataStatus).append("\n");
        sb.append("Faa mode: ").append(faaMode).append("\n");
        sb.append("Horizontal precision: ").append(horizontalPrecision).append("\n");
        sb.append("Vertical precision: ").append(verticalPrecision).append("\n");
        sb.append("Position precision: ").append(positionPrecision).append("\n");

        sb.append("Date: ").append(date).append("\n");
        sb.append("Time: ").append(time).append("\n");
        sb.append("Speed [Km/h]: ").append(kmHSpeed).append("\n");
        sb.append("Course: ").append(correctedCourse).append("\n");
        sb.append("Position: ").append(position).append("\n");

        sb.append("Satellites count:").append(satelliteIds.length).append("\n");
        sb.append("--> ids: ");
        for( String sid : satelliteIds ) {
            sb.append(" ").append(sid);
        }
        sb.append("\n");
        if (satelliteInfo != null) {
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
        return sb.toString();
    }

}
