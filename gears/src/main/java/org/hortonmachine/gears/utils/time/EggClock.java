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

import java.io.PrintStream;
import java.util.Date;

/**
 * A simple time print utility.
 * 
 * <pre>
 * EggClock timer = new EggClock("Time check: ", " min\n");
 * timer.start();
 * 
 * -> do some stuff here
 *
 * timer.printTimePassedInMinutes(System.err);
 * </pre>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.6
 */
public class EggClock {
    private long startTimeMillis;
    private long startSubTimeMillis;
    private final String preFix;
    private final String postFix;

    private boolean started = false;

    public EggClock( String preFix, String postFix ) {
        this.preFix = preFix;
        this.postFix = postFix;
    }

    public void start() {
        startTimeMillis = System.currentTimeMillis();
        startSubTimeMillis = startTimeMillis;
        started = true;
    }

    public void startAndPrint( PrintStream pm ) {
        if (!started) {
            start();
            pm.println(new Date(startTimeMillis));
        } else {
            pm.println("Clock already started before, ignoring call.");
        }

    }

    /**
     * Prints the passed interval in minutes (rounded) to the out stream.
     *
     * @param pm the stream to which to print to.
     * @param newLine if <code>true</code>, also a newline is added.
     */
    public void printTimePassedInMinutes( PrintStream pm ) {
        printTimePassedInMinutes(pm, startTimeMillis);
    }

    /**
     * Prints the passed interval in seconds (rounded) to the out stream.
     *
     * @param pm the stream to which to print to.
     * @param newLine if <code>true</code>, also a newline is added.
     */
    public void printTimePassedInSeconds( PrintStream pm ) {
        printTimePassedInSeconds(pm, startTimeMillis);
    }

    /**
     * Starts a sub counter.
     */
    public void startSub() {
        startSubTimeMillis = System.currentTimeMillis();
    }

    /**
     * Prints the passed interval in minutes (rounded) from the sub counter start to the out stream.
     *
     * @param pm the stream to which to print to.
     * @param newLine if <code>true</code>, also a newline is added.
     */
    public void printSubTimePassedInMinutes( PrintStream pm ) {
        printTimePassedInMinutes(pm, startSubTimeMillis);
    }

    /**
     * Prints the passed interval in seconds (rounded) from the sub counter start to the out stream.
     *
     * @param pm the stream to which to print to.
     * @param newLine if <code>true</code>, also a newline is added.
     */
    public void printSubTimePassedInSeconds( PrintStream pm ) {
        printTimePassedInSeconds(pm, startSubTimeMillis);
    }

    private void printTimePassedInMinutes( PrintStream pm, long startTimeMillis ) {
        long currentTimeMillis = System.currentTimeMillis();
        int mins = (int) Math.round((currentTimeMillis - startTimeMillis) / 1000.0 / 60.0);
        if (preFix != null) {
            pm.print(preFix);
        }
        pm.print(mins);
        if (postFix != null) {
            pm.print(postFix);
        }
    }

    private void printTimePassedInSeconds( PrintStream pm, long startTimeMillis ) {
        long currentTimeMillis = System.currentTimeMillis();
        int mins = (int) Math.round((currentTimeMillis - startTimeMillis) / 1000.0);
        if (preFix != null) {
            pm.print(preFix);
        }
        pm.print(mins);
        if (postFix != null) {
            pm.print(postFix);
        }
    }

}
