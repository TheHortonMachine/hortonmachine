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
package org.hortonmachine.gears.io.las.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Gps Time converter
 * 
 * <p>Adapted from the javascript version of: https://losc.ligo.org/gps/</p>
 * <p>Following may be helpful for conversion when new leap seconds are
 * announced.
 * <pre>
 * gnu 'date':
 * date --date='2012-06-30 23:59:59' +%s
 * </pre>
 * 
 * <p>source: https://losc.ligo.org/s/js/gpstimeutil.js</p>
 * 
 * <p>GPS time was born at the 1/5/1980 to 1/6/1980 midnight. 
 * January 6, 1980 is a Sunday. GPS Time counts in weeks and seconds of a week 
 * from this instant. The weeks begin at the Saturday/Sunday transition. 
 * The days of the week are numbered, with Sunday being 0, Saturday is day 6
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GpsTimeConverter {

    public static String ISO8601_pattern = "yyyy-MM-dd'T'HH:mm:ss";
    public static DateTimeFormatter ISO8601Formatter = DateTimeFormat.forPattern(ISO8601_pattern).withZone(DateTimeZone.UTC);

    private static double[] getleaps() {
        return new double[]{46828800, 78364801, 109900802, 173059203, 252028804, 315187205, 346723206, 393984007, 425520008,
                457056009, 504489610, 551750411, 599184012, 820108813, 914803214, 1025136015, 1119744016, 1341118800, 1167264017};
    }

    // Test to see if a GPS second is a leap second
    private static boolean isleap( double gpsTime ) {
        boolean isLeap = false;
        double[] leaps = getleaps();
        for( int i = 0; i < leaps.length; i += 1 ) {
            if (gpsTime == leaps[i]) {
                isLeap = true;
                break;
            }
        }
        return isLeap;
    }

    // Count number of leap seconds that have passed
    private static int countleaps( double gpsTime, boolean accum_leaps ) {
        int i, nleaps;
        double[] leaps = getleaps();
        nleaps = 0;

        if (accum_leaps) {
            for( i = 0; i < leaps.length; i += 1 ) {
                if (gpsTime + i >= leaps[i]) {
                    nleaps += 1;
                }
            }
        } else {
            for( i = 0; i < leaps.length; i += 1 ) {
                if (gpsTime >= leaps[i]) {
                    nleaps += 1;
                }
            }
        }
        return nleaps;
    }

    // Test to see if a unixtime second is a leap second
    private static boolean isunixtimeleap( double unixTime ) {
        double gpsTime = unixTime - 315964800;
        gpsTime += countleaps(gpsTime, true) - 1;

        return isleap(gpsTime);
    }

    /**
     * Convert {@link DateTime} to GPS Time.
     * 
     * @param dateTimeMillis the datetime to convert (millis).
     * @return the gps time.
     */
    public static double dateTime2gps( double dateTimeMillis ) {
        dateTimeMillis = dateTimeMillis / 1000;
        double ipart = Math.floor(dateTimeMillis);
        double fpart = dateTimeMillis % 1;
        double gpsTime = ipart - 315964800;
        if (isunixtimeleap(Math.ceil(dateTimeMillis))) {
            fpart *= 2;
        }
        return gpsTime + fpart + countleaps(gpsTime, true);
    }

    /**
     * Convert GPS Week Time to {@link DateTime}.
     * 
     * @param gpsWeekTime the gps time.
     * @return the {@link DateTime} of the gps time.
     */
    public static DateTime gpsWeekTime2DateTime( double gpsWeekTime ) {
        double fpart, ipart, unixTime;
        fpart = gpsWeekTime % 1;
        ipart = Math.floor(gpsWeekTime);
        unixTime = ipart + 315964800 - countleaps(ipart, false);

        if (isleap(ipart + 1)) {
            unixTime = unixTime + fpart / 2;
        } else if (isleap(ipart)) {
            unixTime = unixTime + (fpart + 1) / 2;
        } else {
            unixTime = unixTime + fpart;
        }
        DateTime dt = new DateTime((long) unixTime * 1000);
        return dt;
    }

    /**
     * Convert GPS Week Time to the day of the week.
     * 
     * @param gpsWeekTime the gps time.
     * @return the day of the week.
     */
    public static EGpsWeekDays gpsWeekTime2WeekDay( double gpsWeekTime ) {
        int seconds = (int) gpsWeekTime;
        // week starts with Sunday
        EGpsWeekDays day4Seconds = EGpsWeekDays.getDay4Seconds(seconds);
        return day4Seconds;
    }

    /**
     * Convert GPS Time to ISO8601 time string.
     * 
     * @param gpsWeekTime the gps time.
     * @return the ISO8601 string of the gps time.
     */
    public static String gpsWeekTime2ISO8601( double gpsWeekTime ) {
        DateTime gps2unix = gpsWeekTime2DateTime(gpsWeekTime);
        return gps2unix.toString(ISO8601Formatter);
    }

}
