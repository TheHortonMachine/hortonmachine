/*
 * $Id:$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 */
package oms3.util;

import java.util.Calendar;

/**
 *
 * @author Olaf David
 */
public class Times {

    /** Solar year (Dec 21st - Dec20th).  */
    public static final int SOLAR_YEAR = 1;
    /** Water year (Oct 1st - Sept 31st)  */
    public static final int WATER_YEAR = 2;
    /** Calendar year (Jan 1st - Dec 31st)  */
    public static final int CALENDAR_YEAR = 3;
    private static final long SERIAL_BASE_1900 = -2209050000000l;
    /**
     * All minutes have this many milliseconds except the last minute of the day on a day defined with
     * a leap second.
     */
    public static final long MILLISECS_PER_MINUTE = 60 * 1000;
    /**
     * Number of milliseconds per hour, except when a leap second is inserted.
     */
    public static final long MILLISECS_PER_HOUR = 60 * MILLISECS_PER_MINUTE;
    /**
     * Number of leap seconds per day expect on 
     * <BR/>1. days when a leap second has been inserted, e.g. 1999 JAN  1.
     * <BR/>2. Daylight-savings "spring forward" or "fall back" days.
     */
    protected static final long MILLISECS_PER_DAY = 24 * MILLISECS_PER_HOUR;
    /**
     * Value to add to the day number returned by this calendar to find the Julian Day number.
     * This is the Julian Day number for 1/1/1970.
     * Note: Since the unix Day number is the same from local midnight to local midnight adding
     * JULIAN_DAY_OFFSET to that value results in the chronologist, historians, or calenderists
     * Julian Day number.
     */
    public static final long EPOCH_UNIX_ERA_DAY = 2440588L;

    /**
     * @param cal
     * @return Day number where day 0 is 1/1/1970, as per the Unix/Java date/time epoch.
     */
    public static long getUnixDay(Calendar cal) {
        long offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        long day = (long) Math.floor((double) (cal.getTime().getTime() + offset) / ((double) MILLISECS_PER_DAY));
        return day;
    }

    /**
     * @return LOCAL Chronologists Julian day number each day starting from midnight LOCAL TIME.
     */
    public static long getJulianDay(Calendar cal) {
        return getUnixDay(cal) + EPOCH_UNIX_ERA_DAY;
    }

    /**
     * find the number of days from this date to the given end date.
     * later end dates result in positive values.
     * Note this is not the same as subtracting day numbers.  Just after midnight subtracted from just before
     * midnight is 0 days for this method while subtracting day numbers would yields 1 day.
     * @param end - any Calendar representing the moment of time at the end of the interval for calculation.
     */
    public static long diffDayPeriods(Calendar start, Calendar end) {
        long endL = end.getTimeInMillis() + end.getTimeZone().getOffset(end.getTimeInMillis());
        long startL = start.getTimeInMillis() + start.getTimeZone().getOffset(start.getTimeInMillis());
        return (endL - startL) / MILLISECS_PER_DAY;
    }

    /** Check if a year is a leap year.
     *
     * @param year the calendar year to check
     * @return true is the given year is a leap year.
     */
    public static boolean isLeapYear(int year) {
        return year >= 1582 ? ((year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0))) : // Gregorian
                (year % 4 == 0); // Julian
    }

    /** Get the Day of the year in WATER, SOLAR, or CALENDAR year.
     */
    public static int getDayOfYear(Calendar cal, int type) {
        int jday = cal.get(Calendar.DAY_OF_YEAR);
        int mo = cal.get(java.util.Calendar.MONTH) + 1;
        if (type == CALENDAR_YEAR) {
            return jday;
        } else if (type == SOLAR_YEAR) {
            int day = cal.get(Calendar.DAY_OF_MONTH);
            return (mo == 12 && day > 21) ? (day - 21) : (jday + 10);
        } else if (type == WATER_YEAR) {
            return (mo > 9) ? (jday - (isLeapYear(cal.get(Calendar.YEAR)) ? 274 : 273)) : (jday + 92);
        }
        throw new IllegalArgumentException("getDayOfYear() type argument unknown");
    }

    /**
     * Convert a calendar to a serial date value. a serial date is the number
     *  of days since Jan 01 1900 plus a fractional.
     * @param cal the calendar object
     * @return the serial date
     */
    public static double toSerialDate(Calendar cal) {
        long calTime = cal.getTimeInMillis();
        return (((double) (calTime - SERIAL_BASE_1900) / 86400000l) + 1.0);
    }

    /** Set a calendar object to a serial date value.
     *
     * @param cal the calendar to set the value
     * @param serialDate the serial date to apply
     */
    public static void setSerialDate(Calendar cal, double serialDate) {
        cal.setTimeInMillis(SERIAL_BASE_1900 + (long) ((serialDate - 1.0) * 86400000l));
    }
    
      /** This used to be 'deltim' in MMS.
     */
    public static double deltaHours(int calUnit, int increments) {
        if (calUnit == Calendar.DATE) {
            return 24 * increments;
        } else if (calUnit == Calendar.HOUR) {
            return increments;
        } else if (calUnit == Calendar.MINUTE) {
            return increments / 60;
        } else if (calUnit == Calendar.SECOND) {
            return increments / 3600;
        }
        return -1;
    }


}
