/*
 * $Id: Dates.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 * @author od
 */
public class Dates {

    /**
     * Solar year (Dec 21st - Dec20th).
     */
    public static final int SOLAR_YEAR = 1;
    /**
     * Water year (Oct 1st - Sept 31st)
     */
    public static final int WATER_YEAR = 2;
    /**
     * Calendar year (Jan 1st - Dec 31st)
     */
    public static final int CALENDAR_YEAR = 3;
    private static final long SERIAL_BASE_1900 = -2209050000000l;
    /**
     * All minutes have this many milliseconds except the last minute of the day
     * on a day defined with a leap second.
     */
    public static final long MILLISECS_PER_MINUTE = 60 * 1000;
    /**
     * Number of milliseconds per hour, except when a leap second is inserted.
     */
    public static final long MILLISECS_PER_HOUR = 60 * MILLISECS_PER_MINUTE;
    /**
     * Number of leap seconds per day expect on {@literal <BR/>1}. days when a leap second
     * has been inserted, e.g. 1999 JAN 1. {@literal <BR/>2}. Daylight-savings "spring
     * forward" or "fall back" days.
     */
    protected static final long MILLISECS_PER_DAY = 24 * MILLISECS_PER_HOUR;
    /**
     * Value to add to the day number returned by this calendar to find the
     * Julian Day number. This is the Julian Day number for 1/1/1970. Note:
     * Since the unix Day number is the same from local midnight to local
     * midnight adding JULIAN_DAY_OFFSET to that value results in the
     * chronologist, historians, or calenderists Julian Day number.
     */
    public static final long EPOCH_UNIX_ERA_DAY = 2440588L;

    private Dates() {
    }

    /**
     * Get Unix format day
     *
     * @param cal the src calendar
     * @return Day number where day 0 is 1/1/1970, as per the Unix/Java
     * date/time epoch.
     */
    public static long getUnixDay(Calendar cal) {
        long offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        long day = (long) Math.floor((double) (cal.getTime().getTime() + offset) / ((double) MILLISECS_PER_DAY));
        return day;
    }

    /**
     * Get Julian format day
     * 
     * @param cal the src calendar
     * @return LOCAL Chronologists Julian day number each day starting from
     * midnight LOCAL TIME.
     */
    public static long getJulianDay(Calendar cal) {
        return getUnixDay(cal) + EPOCH_UNIX_ERA_DAY;
    }

    /**
     * find the number of days from this date to the given end date. later end
     * dates result in positive values. Note this is not the same as subtracting
     * day numbers. Just after midnight subtracted from just before midnight is
     * 0 days for this method while subtracting day numbers would yields 1 day.
     *
     * @param start calendar representing the time at the start of the interval
     * @param end - any Calendar representing the moment of time at the end of
     * the interval for calculation.
     * @return the number of days between start and end
     */
    public static long diffDayPeriods(Calendar start, Calendar end) {
        long endL = end.getTimeInMillis() + end.getTimeZone().getOffset(end.getTimeInMillis());
        long startL = start.getTimeInMillis() + start.getTimeZone().getOffset(start.getTimeInMillis());
        return (endL - startL) / MILLISECS_PER_DAY;
    }

    /**
     * Check if a year is a leap year.
     *
     * @param year the calendar year to check
     * @return true is the given year is a leap year.
     */
    public static boolean isLeapYear(int year) {
        return year >= 1582 ? ((year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0))) : // Gregorian
                (year % 4 == 0); // Julian
    }

    /**
     * Get the Day of the year in WATER, SOLAR, or CALENDAR year.
     * 
     * @param cal the src calendar
     * @param type the type of conversion
     * @return the day of the year in water, solar or calendar year
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
     * Convert a calendar to a serial date value. a serial date is the number of
     * days since Jan 01 1900 plus a fractional.
     *
     * @param cal the calendar object
     * @return the serial date
     */
    public static double toSerialDate(Calendar cal) {
        long calTime = cal.getTimeInMillis();
        return (((double) (calTime - SERIAL_BASE_1900) / 86400000l) + 1.0);
    }

    /**
     * Set a calendar object to a serial date value.
     *
     * @param cal the calendar to set the value
     * @param serialDate the serial date to apply
     */
    public static void setSerialDate(Calendar cal, double serialDate) {
        cal.setTimeInMillis(SERIAL_BASE_1900 + (long) ((serialDate - 1.0) * 86400000l));
    }

    /**
     * This used to be 'deltim' in MMS.
     * 
     * @param calUnit the calendar unit
     * @param increments the increments to compute the delta
     * @return the delta in MMS
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
    private static SimpleDateFormat YYYYMMdd = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat MMdd = new SimpleDateFormat("MM-dd");
    private static SimpleDateFormat dd = new SimpleDateFormat("dd");

    private static int checkYear(String year) throws Exception {
        if (year == null || year.isEmpty()) {
            throw new Exception();
        }
        int y = Integer.parseInt(year);
        if (y < 1900 || y > 2100) {
            throw new Exception();
        }
        return y;
    }

    private static void checkMonth(String month) throws Exception {
        if (month == null || month.isEmpty()) {
            throw new Exception();
        }
        int m = Integer.parseInt(month);
        if (m < 1 || m > 12) {
            throw new Exception();
        }
    }

    private static void checkDay(String day) throws Exception {
        if (day == null || day.isEmpty()) {
            throw new Exception();
        }
        int d = Integer.parseInt(day);
        if (d < 1 || d > 31) {
            throw new Exception();
        }
    }

    static private Date parse(Calendar base, String date) throws Exception {
        Date parse = null;
        try {
            parse = YYYYMMdd.parse(date);
            String[] p = date.split("-");
            checkYear(p[0]);
            checkMonth(p[1]);
            checkDay(p[2]);
        } catch (ParseException ex) {
            try {
                MMdd.parse(date);
                String[] p = date.split("-");
                checkMonth(p[0]);
                checkDay(p[1]);
                parse = YYYYMMdd.parse(base.get(Calendar.YEAR) + "-" + date);
            } catch (ParseException ex1) {
                try {
                    dd.parse(date);
                    checkDay(date);
                    parse = YYYYMMdd.parse(base.get(Calendar.YEAR) + "-" + base.get(Calendar.MONTH) + 1 + "-" + date);
                } catch (ParseException ex2) {
                    throw new Exception();
                }
            }
        }
        return parse;
    }

    /**
     * Parses and checks an espdate range
     *
     * @param espdate the date in espdate format.
     * @return the array of espdate range
     */
    public static synchronized Date[] parseESPDates(String espdate) {
        try {
            String parts[] = espdate.split("\\s*/\\s*");
            if (parts.length != 3) {
                throw new Exception();
            }
            Date[] date = new Date[3];
            String[] p = parts[0].split("-");
            date[0] = YYYYMMdd.parse(parts[0]);
            checkYear(p[0]);
            checkMonth(p[1]);
            checkDay(p[2]);

            Calendar cal = new GregorianCalendar();
            cal.setTime(date[0]);

            date[1] = parse(cal, parts[1]);
            cal.setTime(date[1]);

            date[2] = parse(cal, parts[2]);

            if (!(date[1].after(date[0]) && date[2].after(date[1]))) {
                throw new Exception();
            }
            return date;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Not a valid range '" + espdate + "'");
        }
    }

    /**
     * Parses and checks a duration
     *
     * @param duration the date in iso format.
     * @return the array of start and end date
     */
    public static synchronized Date[] parseDuration(String duration) {
        try {
            String parts[] = duration.split("\\s*/\\s*");
            if (parts.length != 2) {
                throw new Exception();
            }
            Date[] date = new Date[2];
            String[] p = parts[0].split("-");
            date[0] = YYYYMMdd.parse(parts[0]);
            checkYear(p[0]);
            checkMonth(p[1]);
            checkDay(p[2]);

            Calendar cal = new GregorianCalendar();
            cal.setTime(date[0]);

            date[1] = parse(cal, parts[1]);

            if (!(date[1].after(date[0]))) {
                throw new Exception();
            }

            return date;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Not a valid range '" + duration + "'");
        }
    } 

    public static String[] splitDuration(String duration) {
        String parts[] = duration.split("\\s*/\\s*");
        if (parts == null || parts.length != 2) {
            throw new IllegalArgumentException("duration " + duration);
        }
        return new String[]{parts[0], parts[1]};
    }

    public static String durationStart(String duration) {
        String[] d = splitDuration(duration);
        return d[0];
    }

    public static String durationEnd(String duration) {
        String[] d = splitDuration(duration);
        return d[1];
    }

    public static String durationStartMonth(String duration) {
        String[] d = splitDuration(duration);
        String[] p = d[0].split("-");
        if (p.length == 3) {
            return p[1];
        } else if (p.length == 2) {
            return p[0];
        }
        return null;
    }
    
    
    
    public static String durationEndMonth(String duration) {
        String[] d = splitDuration(duration);
        String[] p = d[1].split("-");
        if (p.length == 3) {
            return p[1];
        } else if (p.length == 2) {
            return p[0];
        }
        return null;
    }

    /**
     *
     * @param y historical years, e.g. "1972/2000"
     * @return the years as integers
     */
    public static int[] parseHistoricalYears(String y) {
        try {
            String parts[] = y.split("\\s*/\\s*");
            if (parts.length != 2) {
                throw new Exception();
            }
            int y1 = checkYear(parts[0]);
            int y2 = checkYear(parts[1]);
            if (!(y1 < y2)) {
                throw new Exception();
            }
            return new int[]{y1, y2};
        } catch (Exception E) {
            throw new IllegalArgumentException("Not a valid year range '" + y + "'");
        }
    }

    /**
     * Parses and checks dates.
     *
     * @param date "yyyy-MM-dd" formatted date.
     * @return the checked date
     */
    public static synchronized Date parseDate(String date) {
        try {
            Date d = YYYYMMdd.parse(date);
            String[] p = date.split("-");
            checkYear(p[0]);
            checkMonth(p[1]);
            checkDay(p[2]);
            return d;
        } catch (Exception ex) {
            throw new IllegalArgumentException(date);
        }
    }

    public static synchronized String today() {
        return YYYYMMdd.format(new Date());
    }

    public synchronized static String yesterday() {
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -1);
        return YYYYMMdd.format(cal.getTime());
    }

    public synchronized static String tomorrow() {
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, 1);
        return YYYYMMdd.format(cal.getTime());
    }

    public static Date dayBefore(Date date) {
        Calendar c = new GregorianCalendar();
        c.setTime(date);
        c.add(Calendar.DATE, -1);
        return c.getTime();  //
    }

    public static void main(String[] args) throws ParseException {
        String test = "1981-01-01/04-31/04-30";

        Date date[] = parseESPDates(test);
        System.out.println(Arrays.toString(date));
    }
//    public static void main(String[] args) {
//        long start = System.currentTimeMillis();
//        System.out.println(calculateDays("1980/03/31", "2012/06/17"));
//        long end = System.currentTimeMillis();
//        System.out.println("time " + (end - start));
//    }
}
