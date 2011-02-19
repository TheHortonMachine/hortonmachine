package org.omscentral.modules.analysis.esp;

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.Locale;
import java.sql.Date;

public class ModelDateTime extends GregorianCalendar {

    private static final long serialVersionUID = 1L;
    private double juldate = -1;
    private String[] month_names = {"jan", "feb", "mar", "apr", "may", "jun",
        "jul", "aug", "sep", "oct", "nov", "dec"};

    /*
     ** Overridden constructors
     */
    public ModelDateTime() {
        super();
    }

    public ModelDateTime(TimeZone zone) {
        super(zone);
    }

    public ModelDateTime(Locale aLocale) {
        super(aLocale);
    }

    public ModelDateTime(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }

    public ModelDateTime(int y, int m, int d) {
        set(y, m, d, 0, 0, 0, 0);
    }

    public ModelDateTime(int y, int m, int d, int h, int mi) {
        set(y, m, d, h, mi, 0, 0);
    }

    public ModelDateTime(int y, int m, int d, int h, int mi, int s) {
        set(y, m, d, h, mi, s, 0);
    }

    /*
     ** New contstructors
     */
    public ModelDateTime(int y, int m, int d, int h, int mi, double s) {
        set(y, m, d, h, mi, (int) s, (int) ((s - (int) s) * 1000.0));
    }

    public ModelDateTime(double jd) {
        juldate = jd;
        setJul2Greg(jd);
    }

    public ModelDateTime(double jd, TimeZone zone) {
        juldate = jd;
        setTimeZone(zone);
        setJul2Greg(jd);
    }

    public ModelDateTime(String date_time) throws SetModelDateTimeException {
        set(date_time);
    }

    public ModelDateTime(String date_time, TimeZone zone)
            throws SetModelDateTimeException {
        setTimeZone(zone);
        set(date_time);
    }

    /*
     ** New methods that overload Calendar methods
     */
    public void set(int y, int m, int d, int h, int mi, int s, int ms) {
        set(Calendar.YEAR, y);
        set(Calendar.MONTH, (m - 1));
        set(Calendar.DAY_OF_MONTH, d);
        set(Calendar.HOUR_OF_DAY, h);
        set(Calendar.MINUTE, mi);
        set(Calendar.SECOND, s);
        set(Calendar.MILLISECOND, ms);

        juldate = -1;
        getJulian();
    }

    public void set(String date_time)
            throws SetModelDateTimeException {

        try {
            int year, month, day, hour, minute, second;
            year = (Integer.valueOf(date_time.substring(0, 4)).intValue());
            if (date_time.charAt(5) == ' ') {
                month = (Integer.valueOf(date_time.substring(6, 7)).intValue());
            } else {
                month = (Integer.valueOf(date_time.substring(5, 7)).intValue());
            }

            if (date_time.charAt(8) == ' ') {
                day = (Integer.valueOf(date_time.substring(9, 10)).intValue());
            } else {
                day = (Integer.valueOf(date_time.substring(8, 10)).intValue());
            }

            hour = 0;
            minute = 0;
            second = 0;

            if (date_time.length() > 15) {
                if (date_time.charAt(11) == ' ') {
                    hour = (Integer.valueOf(date_time.substring(12, 13)).intValue());
                } else {
                    hour = (Integer.valueOf(date_time.substring(11, 13)).intValue());
                }

                if (date_time.charAt(14) == ' ') {
                    minute = (Integer.valueOf(date_time.substring(15, 16)).intValue());
                } else {
                    minute = (Integer.valueOf(date_time.substring(14, 16)).intValue());
                }
            }

            if (date_time.length() == 19) {
                if (date_time.charAt(17) == ' ') {
                    second = (Integer.valueOf(date_time.substring(18, 19)).intValue());
                } else {
                    second = (Integer.valueOf(date_time.substring(17, 19)).intValue());
                }
            }

            set(year, month, day, hour, minute, second, 0);
        } catch (Exception e) {
        }
    }

    /*
     ** Output methods
     */
    public String getMmsDateTime() {

        Format f = new Format("%02d");
//      Format sf = new Format ("%02.0f");

        float sec = (float) (get(Calendar.SECOND))
                + (float) (get(Calendar.MILLISECOND) * 0.001);

        return (get(Calendar.YEAR) + " "
                + f.form(get(Calendar.MONTH) + 1) + " "
                + f.form(get(Calendar.DAY_OF_MONTH)) + " "
                + f.form(get(Calendar.HOUR_OF_DAY)) + " "
                + f.form(get(Calendar.MINUTE)) + " "
                + f.form((int) sec));
    }

    public String getGISDateTime() {

        Format f = new Format("%02d");
//      Format sf = new Format ("%02.0f");

        float sec = (float) (get(Calendar.SECOND))
                + (float) (get(Calendar.MILLISECOND) * 0.001);

        return (get(Calendar.YEAR) + "-"
                + f.form(get(Calendar.MONTH) + 1) + "-"
                + f.form(get(Calendar.DAY_OF_MONTH)) + ":"
                + f.form(get(Calendar.HOUR_OF_DAY)) + ":"
                + f.form(get(Calendar.MINUTE)) + ":"
                + f.form((int) sec));
    }

    public String getRuntimeGraphDateTime() {
        // I'm not sure what the last zero is -- markstro
        // "2001,10,1,0,0,0,0"

        Format f = new Format("%02d");

        float sec = (float) (get(Calendar.SECOND))
                + (float) (get(Calendar.MILLISECOND) * 0.001);

        return (get(Calendar.YEAR) + ","
                + f.form(get(Calendar.MONTH) + 1) + ","
                + f.form(get(Calendar.DAY_OF_MONTH)) + ","
                + f.form(get(Calendar.HOUR_OF_DAY)) + ","
                + f.form(get(Calendar.MINUTE)) + ","
                + f.form((int) sec)) + ",0";
    }

    public String getMmsEspInitDate() {
        Format f = new Format("%02d");
        return (get(Calendar.YEAR)
                + f.form(get(Calendar.MONTH) + 1)
                + f.form(get(Calendar.DAY_OF_MONTH)));
    }

    public String getTabDateTime() {
        Format f = new Format("%02d");
        return (get(Calendar.YEAR) + "\t"
                + f.form(get(Calendar.MONTH) + 1) + "\t"
                + f.form(get(Calendar.DAY_OF_MONTH)) + "\t"
                + f.form(get(Calendar.HOUR_OF_DAY)) + "\t"
                + f.form(get(Calendar.MINUTE)) + "\t"
                + f.form(get(Calendar.SECOND)));
    }

    public String getControlFileDateTime() {
        Format f = new Format("%02d");
//      Format sf = new Format ("%02f");
        float sec = (float) (get(Calendar.SECOND))
                + (float) (get(Calendar.MILLISECOND) * 0.001);
        return (get(Calendar.YEAR) + ","
                + f.form(get(Calendar.MONTH) + 1) + ","
                + f.form(get(Calendar.DAY_OF_MONTH)) + ","
                + f.form(get(Calendar.HOUR_OF_DAY)) + ","
                + f.form(get(Calendar.MINUTE)) + ","
                + f.form(get(Calendar.SECOND)));
    }

    public String getSQLDate() {
        Format f = new Format("%02d");
        return (get(Calendar.YEAR) + "-"
                + f.form(get(Calendar.MONTH) + 1) + "-"
                + f.form(get(Calendar.DAY_OF_MONTH)));
    }

    public String toString() {
        return (getSQLDate());
    }

    public String getMonthDay() {
        Format f = new Format("%02d");

        return (month_names[get(Calendar.MONTH)] + "-"
                + f.form(get(Calendar.DAY_OF_MONTH)));
    }

    public int getYear() {
        return (get(Calendar.YEAR));
    }

    public int getMonth() {
        return (get(Calendar.MONTH) + 1);
    }

    public int getDay() {
        return (get(Calendar.DAY_OF_MONTH));
    }

    public String getRiverWareDateTime() {
        Format f = new Format("%02d");

        return (get(Calendar.YEAR) + "-"
                + f.form(get(Calendar.MONTH) + 1) + "-"
                + f.form(get(Calendar.DAY_OF_MONTH)) + " "
                + f.form(get(Calendar.HOUR_OF_DAY)) + ":"
                + f.form(get(Calendar.MINUTE)));
    }

    public Date getJDBCDate() {
        return new Date(getTimeInMillis());
    }

    public long getMillis() {
        return (getTimeInMillis());
    }


    /*
     * convert Julian date to Gregorian
     *
     * The Julian day starts at noon of the Gregorian day and extends
     * to noon the next Gregorian day. The Gregorian day is assumed
     * to begin at midnight.
     *
     * Julian date converter. Takes a julian date (the number of days since
     ** some distant epoch or other), and returns an int pointer to static space.
     ** Copied from Algorithm 199 in Collected algorithms of the CACM
     ** Author: Robert G. Tantzen, Translator: Nat Howard
     */
    public void setJul2Greg(double julian_day) {

        int month, day, year, hour, minute;
        double second;

        long j = (long) (julian_day);
        double tmp, frac = julian_day - j;


        if (frac >= 0.5) {
            frac = frac - 0.5;
            j++;
        } else {
            frac = frac + 0.5;
        }

        j -= 1721119L;

        year = (int) ((4L * j - 1L) / 146097L);
        j = 4L * j - 1L - 146097L * year;
        day = (int) (j / 4L);
        j = (4L * day + 3L) / 1461L;
        day = (int) (4L * day + 3L - 1461L * j);
        day = (int) ((day + 4L) / 4L);
        month = (int) ((5L * day - 3L) / 153L);
        day = (int) (5L * day - 3 - 153L * month);
        day = (int) ((day + 5L) / 5L);
        year = (int) (100L * year + j);
        if (month < 10) {
            month += 3;
        } else {
            month -= 9;
            year += 1;
        }
        tmp = 3600.0 * (frac * 24.0);
        hour = (int) (tmp / 3600.0);
        tmp = tmp - hour * 3600.0;
        minute = (int) (tmp / 60.0);
        second = tmp - minute * 60.0;

        juldate = julian_day;
        set(year, month, day, hour, minute, (int) second,
                (int) ((second - (int) second) * 1000.0));
    }

    /**
     *
     *  Takes a date, and returns a Julian day. A Julian day is the number of
     *  days since some base date  (in the very distant past).
     *  Handy for getting date of x number of days after a given Julian date
     *  (use jdate to get that from the Gregorian date).
     *  Author: Robert G. Tantzen, translator: Nat Howard
     *  Translated from the algol original in Collected Algorithms of CACM
     *  (This and jdate are algorithm 199).
     */
    public double getJulian() {

//      if (juldate > 0.0) return (juldate);

        long m = (long) (get(Calendar.MONTH) + 1);
        long d = (long) (get(Calendar.DAY_OF_MONTH));
        long y = (long) (get(Calendar.YEAR));
        long c, ya, j;

        double seconds = (double) (get(Calendar.HOUR_OF_DAY) * 3600.0)
                + (double) (get(Calendar.MINUTE) * 60)
                + (double) (get(Calendar.SECOND))
                + (double) (get(Calendar.MILLISECOND) * 0.001);

        if (m > 2) {
            m -= 3;
        } else {
            m += 9;
            --y;
        }

        c = y / 100L;
        ya = y - (100L * c);
        j = (146097L * c) / 4L + (1461L * ya) / 4L + (153L * m + 2L)
                / 5L + d + 1721119L;
        if (seconds < 12 * 3600.0) {
            j--;
            seconds += 12.0 * 3600.0;
        } else {
            seconds = seconds - 12.0 * 3600.0;
        }

        juldate = j + (seconds / 3600.0) / 24.0;

        return (juldate);
    }

    public static void main(String arg[]) {

        ModelDateTime mdt = new ModelDateTime();
        System.out.println("\ncurrent time");
        System.out.println("   julian = " + mdt.getJulian());
        System.out.println("   mms datetime = " + mdt.getMmsDateTime());
        System.out.println("   sql date = " + mdt.getSQLDate());
        System.out.println("   riverware datetime = " + mdt.getRiverWareDateTime());
        System.out.println("   Current TZ = " + (mdt.getTimeZone()).getID());

        mdt = new ModelDateTime(2440000.0);
        System.out.println("\nset JD = 2440000.0");
        System.out.println("   julian = " + mdt.getJulian());
        System.out.println("   mms datetime = " + mdt.getMmsDateTime());
        System.out.println("   sql date = " + mdt.getSQLDate());
        System.out.println("   riverware datetime = " + mdt.getRiverWareDateTime());

        mdt = new ModelDateTime(1968, 5, 23, 12, 0, 0);
        System.out.println("\nset to 1968, 5, 23, 12, 0, 0");
        System.out.println("   julian = " + mdt.getJulian());
        System.out.println("   mms datetime = " + mdt.getMmsDateTime());
        System.out.println("   sql date = " + mdt.getSQLDate());
        System.out.println("   riverware datetime = " + mdt.getRiverWareDateTime());

        try {
            mdt = new ModelDateTime("1968-05-23");
            System.out.println("\nset RiverWare date 1968-05-23");
            System.out.println("   julian = " + mdt.getJulian());
            System.out.println("   mms datetime = " + mdt.getMmsDateTime());
            System.out.println("   sql date = " + mdt.getSQLDate());
            System.out.println("   riverware datetime = " + mdt.getRiverWareDateTime());
        } catch (SetModelDateTimeException e) {
            e.printStackTrace();
        }


        try {
            mdt = new ModelDateTime("1968-05-23 12:01");
            System.out.println("\nset RiverWare datetime 1968-05-23 12:01");
            System.out.println("   julian = " + mdt.getJulian());
            System.out.println("   mms datetime = " + mdt.getMmsDateTime());
            System.out.println("   sql date = " + mdt.getSQLDate());
            System.out.println("   riverware datetime = " + mdt.getRiverWareDateTime());
        } catch (SetModelDateTimeException e) {
            e.printStackTrace();
        }

        try {
            mdt = new ModelDateTime("1968-05-23 12:01:05");
            System.out.println("\nset RiverWare datetime 1968-05-23 12:01:05");
            System.out.println("   julian = " + mdt.getJulian());
            System.out.println("   mms datetime = " + mdt.getMmsDateTime());
            System.out.println("   sql date = " + mdt.getSQLDate());
            System.out.println("   riverware datetime = " + mdt.getRiverWareDateTime());
        } catch (SetModelDateTimeException e) {
            e.printStackTrace();
        }

        mdt = new ModelDateTime(1968, 5, 23, 12, 0, 0);
        ModelDateTime mdt1 = new ModelDateTime(1968, 5, 24, 12, 0, 0);

        System.out.println();
        if (mdt.before(mdt1)) {
            System.out.println(mdt.getRiverWareDateTime() + " is before "
                    + mdt1.getRiverWareDateTime());
        }

        if (mdt.after(mdt1)) {
            System.out.println(mdt.getRiverWareDateTime() + " is after "
                    + mdt1.getRiverWareDateTime());
        }

        if (mdt.equals(mdt1)) {
            System.out.println(mdt.getRiverWareDateTime() + " is after "
                    + mdt1.getRiverWareDateTime());
        }

        /*
        mdt = new ModelDateTime ("1970-01-01 00:01:00", new SimpleTimeZone (0, "GMT"));
         */
        System.out.println("\ncomputeTime = " + mdt.getJDBCDate().toString());
    }
}

