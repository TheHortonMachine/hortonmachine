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

/**
 * A simple enumeration of the days of the week.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum EGpsWeekDays {
    SUNDAY(0, 0, 86400), //
    MONDAY(1, 86400, 172800), //
    TUESDAY(2, 172800, 259200), //
    WEDNESDAY(3, 259200, 345600), //
    THURSDAY(4, 345600, 432000), //
    FRIDAY(5, 432000, 518400), //
    SATURDAY(6, 518400, 604800);

    private long fromSeconds;
    private long toSeconds;
    private int index;

    EGpsWeekDays( int index, long fromSeconds, long toSeconds ) {
        this.index = index;
        this.fromSeconds = fromSeconds;
        this.toSeconds = toSeconds;
    }

    public int getIndex() {
        return index;
    }

    public long getFromSeconds() {
        return fromSeconds;
    }

    public long getToSeconds() {
        return toSeconds;
    }

    public static EGpsWeekDays getDay4Seconds( double gpsSecondsofWeek ) {
        int seconds = (int) gpsSecondsofWeek;
        if (seconds > SATURDAY.toSeconds) {
            throw new IllegalArgumentException(
                    "GPS seconds of week has to be smaller than: " + SATURDAY.toSeconds + ". Got: " + gpsSecondsofWeek);
        }
        if (seconds < SUNDAY.fromSeconds) {
            throw new IllegalArgumentException(
                    "GPS seconds of week has to be larger than: " + SUNDAY.fromSeconds + ". Got: " + gpsSecondsofWeek);
        }
        if (seconds > SATURDAY.fromSeconds) {
            return SATURDAY;
        } else if (seconds > FRIDAY.fromSeconds) {
            return FRIDAY;
        } else if (seconds > THURSDAY.fromSeconds) {
            return THURSDAY;
        } else if (seconds > WEDNESDAY.fromSeconds) {
            return WEDNESDAY;
        } else if (seconds > TUESDAY.fromSeconds) {
            return TUESDAY;
        } else if (seconds > MONDAY.fromSeconds) {
            return MONDAY;
        } else {
            return SUNDAY;
        }

    }
}
