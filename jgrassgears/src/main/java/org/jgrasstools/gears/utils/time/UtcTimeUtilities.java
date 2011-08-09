package org.jgrasstools.gears.utils.time;

import org.jgrasstools.gears.libs.modules.JGTConstants;
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
    private static DateTimeFormatter withMinutesformatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;

    /**
     * The formatter for YYYY-MM-DD HH:MM:SS.
     */
    private static DateTimeFormatter withSecondsformatter = JGTConstants.utcDateFormatterYYYYMMDDHHMMSS;

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

}
