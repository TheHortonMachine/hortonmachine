/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
 */
package org.hortonmachine.gears.io.nmea;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Representation of a complete gps point info.
 * 
 * <pre>
 * RMC - NMEA has its own version of essential gps pvt (position, velocity, time) data. It is called RMC, The Recommended Minimum, which will look similar to:
 * $GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
 * Where:
 *      RMC          Recommended Minimum sentence C
 *      123519       Fix taken at 12:35:19 UTC
 *      A            Status A=active or V=Void.
 *      4807.038,N   Latitude 48 deg 07.038' N
 *      01131.000,E  Longitude 11 deg 31.000' E
 *      022.4        Speed over the ground in knots
 *      084.4        Track angle in degrees True
 *      230394       Date - 23rd of March 1994
 *      003.1,W      Magnetic Variation
 *      *6A          The checksum data, always begins with *
 * 
 * GGA - essential fix data which provide 3D location and accuracy data.
 *  $GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
 * Where:
 *      GGA          Global Positioning System Fix Data
 *      123519       Fix taken at 12:35:19 UTC
 *      4807.038,N   Latitude 48 deg 07.038' N
 *      01131.000,E  Longitude 11 deg 31.000' E
 *      1            Fix quality: 0 = invalid
 *                                1 = GPS fix (SPS)
 *                                2 = DGPS fix
 *                                3 = PPS fix
 *                    4 = Real Time Kinematic
 *                    5 = Float RTK
 *                                6 = estimated (dead reckoning) (2.3 feature)
 *                    7 = Manual input mode
 *                    8 = Simulation mode
 *      08           Number of satellites being tracked
 *      0.9          Horizontal dilution of position
 *      545.4,M      Altitude, Meters, above mean sea level
 *      46.9,M       Height of geoid (mean sea level) above WGS84
 *                       ellipsoid
 *      (empty field) time in seconds since last DGPS update
 *      (empty field) DGPS station ID number
 *      *47          the checksum data, always begins with *
 * </pre>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NmeaGpsPoint {
    public static final String GPGGA = "$GPGGA";
    public static final String GPRMC = "$GPRMC";
    public final static String strLatitude = "northing";
    public final static String strLongitude = "easting";
    public final static String strLat = "lat";
    public final static String strLon = "lon";
    public final static String strSpeed = "speed";
    public final static String strAltitude = "elev";
    public final static String strQuality = "quality";
    public final static String strSat = "satnum";
    public final static String strHdop = "hdop";
    public final static String strMsl = "dif_ell_msl";
    public final static String strUtctime = "utctime";
    public final static String strMag_var = "mag_var";
    public final static String strAngle = "angle";
    public final static String strValidity1 = "THIS POINT IS NOT VALID"; // "Validity";
    public final static String strValidity2 = "";

    public double latitude = -1.0;
    public double longitude = -1.0;
    public double speed = -1.0;
    public double altitude = -1.0;
    public double quality = -1.0;
    public double sat = -1.0;
    public double hdop = -1.0;
    public double ellipsoidVsMsl = -1.0;
    public DateTime utcDateTime = null;
    public double mag_var = -1.0;
    public double angle = -1.0;
    public String altitudeUnit = "M";
    public String ellipsoidVsMslUnit = "M";
    public boolean isValid = false;

    private static DateTimeFormatter nmeaDateFormatter = DateTimeFormat.forPattern("ddMMyyHHmmss.SSS"); //$NON-NLS-1$
    private static DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("ddMMyyHHmmss"); //$NON-NLS-1$

    public NmeaGpsPoint() {
    }

    // public NmeaGpsPoint( double latitude, double longitude, double altitude ) {
    // this.originalLatitude = latitude;
    // this.originalLongitude = longitude;
    // this.altitude = altitude;
    // }

    public NmeaGpsPoint( NmeaGpsPoint gpsPoint ) {
        this.latitude = gpsPoint.latitude;
        this.longitude = gpsPoint.longitude;
        this.speed = gpsPoint.speed;
        this.quality = gpsPoint.quality;
        this.altitude = gpsPoint.altitude;
        this.sat = gpsPoint.sat;
        this.hdop = gpsPoint.hdop;
        this.ellipsoidVsMsl = gpsPoint.ellipsoidVsMsl;
        this.utcDateTime = gpsPoint.utcDateTime;
        this.mag_var = gpsPoint.mag_var;
        this.angle = gpsPoint.angle;
        this.altitudeUnit = gpsPoint.altitudeUnit;
        this.ellipsoidVsMslUnit = gpsPoint.ellipsoidVsMslUnit;
        this.isValid = gpsPoint.isValid;
    }

    /**
     * Creates a {@link NmeaGpsPoint} from the data strings.
     * 
     * @param currentGPGGAsentence the GPGGA Nmea sentence. If null it is discarded.
     * @param currentGPRMCsentence the GPRMC Nmea sentence. If null it is discarded.
     */
    public NmeaGpsPoint( String currentGPGGAsentence, String currentGPRMCsentence ) {
        // $GPGGA,173416.033,2500.0001,N,12159.9999,E,0,0,,80.9,M,16.1,M,,*7A
        if (currentGPGGAsentence != null && currentGPGGAsentence.startsWith(GPGGA)) {
            String[] dataBlocks = currentGPGGAsentence.split(","); //$NON-NLS-1$
            if (dataBlocks[6].length() > 0)
                quality = Double.parseDouble(dataBlocks[6]);
            if (dataBlocks[7].length() > 0)
                sat = Double.parseDouble(dataBlocks[7]);
            if (dataBlocks[8].length() > 0)
                hdop = Double.parseDouble(dataBlocks[8]);
            if (dataBlocks[9].length() > 0) {
                altitude = Double.parseDouble(dataBlocks[9]);
                altitudeUnit = dataBlocks[10].toLowerCase();
            }
            if (dataBlocks[11].length() > 0) {
                ellipsoidVsMsl = Double.parseDouble(dataBlocks[11]);
                ellipsoidVsMslUnit = dataBlocks[12].toLowerCase();
            }
        }
        if (currentGPRMCsentence != null && currentGPRMCsentence.startsWith(GPRMC)) {
            String[] dataBlocks = currentGPRMCsentence.split(","); //$NON-NLS-1$
            if (dataBlocks[1].length() > 0 && dataBlocks[9].length() > 0) {
                try {
                    utcDateTime = nmeaDateFormatter.parseDateTime(dataBlocks[9] + dataBlocks[1]);
                } catch (Exception e) {
                    utcDateTime = DateTime.now();
                }
            }
            if (dataBlocks[2].length() > 0 && dataBlocks[2].trim().equals("A")) //$NON-NLS-1$
                isValid = true;
            double latitude_in = 0.0;
            if (dataBlocks[3].length() > 0)
                latitude_in = Double.parseDouble(dataBlocks[3]);
            double longitude_in = 0.0;
            if (dataBlocks[5].length() > 0)
                longitude_in = Double.parseDouble(dataBlocks[5]);
            if (dataBlocks[10].length() > 0)
                mag_var = Double.parseDouble(dataBlocks[10]);
            double speed_in = 0.0;
            if (dataBlocks[7].length() > 0)
                speed_in = Double.parseDouble(dataBlocks[7]);
            if (dataBlocks[8].length() > 0)
                angle = Double.parseDouble(dataBlocks[8]);

            double latitude_degrees = Math.floor(latitude_in / 100.0);
            double latitude_minutes = latitude_in - latitude_degrees * 100.0;
            double longitude_degrees = Math.floor(longitude_in / 100.0);
            double longitude_minutes = longitude_in - longitude_degrees * 100;
            latitude = latitude_degrees + (latitude_minutes / 60.0);
            longitude = longitude_degrees + (longitude_minutes / 60.0);
            if (dataBlocks[4].equals("S")) //$NON-NLS-1$
                latitude = -latitude;
            if (dataBlocks[6].equals("W")) //$NON-NLS-1$
                longitude = -longitude;

            speed = (int) (speed_in * 1.852);
        }

    }

    public String toString() {
        final String TAB = "/";

        String retValue = "";

        String time = utcDateTime != null ? utcDateTime.toString(dateFormatter) : "-";
        retValue = "GpsPoint ( " + "latitude = " + this.latitude + TAB + "longitude = " + this.longitude + TAB + "speed = "
                + this.speed + TAB + "altitude = " + this.altitude + TAB + "quality = " + this.quality + TAB + "sat = " + this.sat
                + TAB + "hdop = " + this.hdop + TAB + "msl = " + this.ellipsoidVsMsl + TAB + "utctime = " + time + TAB
                + "mag_var = " + this.mag_var + TAB + "angle = " + this.angle + TAB + " )";

        return retValue;
    }

}
