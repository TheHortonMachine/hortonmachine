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
package org.hortonmachine.gears.utils.time;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

/**
 * An utility class for time related issues, all in UTC timezone.
 * 
 * <p>This class is supposed to handle only the string 
 * format YYYY-MM-DD HH:MM:SS. If not explicitly defined, 
 * that format, with or without seconds, is used. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.0
 */
public class UtcTimeUtilities {

    /**
     * The formatter for YYYY-MM-DD HH:MM.
     */
    private static DateTimeFormatter withMinutesformatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;

    /**
     * The formatter for YYYY-MM-DD HH:MM:SS.
     */
    private static DateTimeFormatter withSecondsformatter = HMConstants.utcDateFormatterYYYYMMDDHHMMSS;

    /**
     * Getter for the current time.
     * 
     * @return the current time in utc.
     */
    public static DateTime newDateTime() {
        DateTime dt = new DateTime().withZone(DateTimeZone.UTC);
        return dt;
    }

    /**
     * Get {@link DateTime} from date string of format: YYYY-MM-DD HH:MM:SS.
     * 
     * @param dateTimeString the date string.
     * @return the parsed datetime. 
     */
    public static DateTime fromStringWithSeconds( String dateTimeString ) {
        DateTime dt = withSecondsformatter.parseDateTime(dateTimeString);
        return dt;
    }

    /**
     * Get {@link DateTime} from date string of format: YYYY-MM-DD HH:MM.
     * 
     * @param dateTimeString the date string.
     * @return the parsed datetime. 
     */
    public static DateTime fromStringWithMinutes( String dateTimeString ) {
        DateTime dt = withMinutesformatter.parseDateTime(dateTimeString);
        return dt;
    }

    /**
     * Get String of format: YYYY-MM-DD HH:MM:SS from {@link DateTime}.
     * 
     * @param dateTime the {@link DateTime}.
     * @return the date string. 
     */
    public static String toStringWithSeconds( DateTime dateTime ) {
        String dtStr = dateTime.toString(withSecondsformatter);
        return dtStr;
    }

    /**
     * Get String of format: YYYY-MM-DD HH:MM from {@link DateTime}.
     * 
     * @param dateTime the {@link DateTime}.
     * @return the date string. 
     */
    public static String toStringWithMinutes( DateTime dateTime ) {
        String dtStr = dateTime.toString(withMinutesformatter);
        return dtStr;
    }

    /**
     * Quick long to timestamp string formatter, UTC.
     * 
     * @param unixEpoch the unix epoch to convert.
     * @return the timestamp string as yyyy-MM-dd HH:mm:ss
     */
    public static String quickToString( long unixEpoch ) {
        return new DateTime(unixEpoch).toString(withSecondsformatter);
    }

    /**
     * Quick long to timestamp string formatter.
     * 
     * @param unixEpoch the unix epoch to convert.
     * @return the timestamp string as yyyy-MM-dd HH:mm:ss
     */
    public static String quickToStringLocal( long unixEpoch ) {
        return new DateTime(unixEpoch).toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS);
    }

}
