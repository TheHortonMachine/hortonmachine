/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.io.grasslegacy.utils;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * <p>
 * The window object represents a particular geographic regione, containing also the information of
 * the grid's resolution and therefore also the number of rows and cols of the region.
 * </p>
 * <p>
 * <b>Warning</b>: since the rows and cols are integers, the resolution is recalculated to fulfill
 * this. Therefore the asked resolution could not be exactly the one in the end is supplied.
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class Window extends Object {

    public static final String BLACKBOARD_KEY = "eu.hydrologis.jgrass.libs.region"; //$NON-NLS-1$

    public final static String BLANK_WINDOW = "proj:           0\nzone:           0\nnorth:          1\nsouth:          0\nwest:           0\neast:           1\nrows:           1\ncols:           1\ne-w resol:      1\nn-s resol:      1";

    private int proj = 0;

    private int zone = 0;

    private double n = -9999.0;

    private double s = -9999.0;

    private double w = -9999.0;

    private double e = -9999.0;

    private double ns_res = -9999.0;

    private double we_res = -9999.0;

    private int rows = 0;

    private int cols = 0;

    private LinkedHashMap<String, String> additionalGrassEntries;

    /**
     * Creates a new instance of Window by supplying a JGrass workspace WIND file
     * 
     * @param windowFile a JGrass workspace WIND file path
     */
    public Window( String windowFile ) {
        readWindowFile(windowFile, this);
    }

    /**
     * Creates a new instance of Window from the boundaries and the number of rows and cols
     * 
     * @param west west coord
     * @param east east coord
     * @param south south coord
     * @param north north coord
     * @param _rows required number of rows
     * @param _cols required number of cols
     */
    public Window( double west, double east, double south, double north, int _rows, int _cols ) {
        w = west;
        e = east;
        s = south;
        n = north;
        rows = _rows;
        cols = _cols;
        fixResolution();
    }

    /**
     * Creates a new instance of Window
     */
    public Window( double west, double east, double south, double north, double weres, double nsres ) {
        w = west;
        e = east;
        s = south;
        n = north;
        we_res = weres;
        ns_res = nsres;
        // System.out.println("WINDOW 2 - west=" + w + ", east=" + e + ",
        // south=" + s + ", north=" + n
        // + ", we_res=" + we_res + ", ns_res=" + ns_res);

        fixRowsAndCols();
        fixResolution();
    }

    /**
     * Creates a new instance of Window from another window object
     * 
     * @param win a window object
     */
    public Window( Window win ) {
        w = win.getWest();
        e = win.getEast();
        s = win.getSouth();
        n = win.getNorth();
        rows = win.getRows();
        cols = win.getCols();
        fixResolution();
    }

    public Window( String west, String east, String south, String north, String ewres, String nsres ) {
        double no = -1.0;
        double so = -1.0;
        double ea = -1.0;
        double we = -1.0;
        double xres = -1.0;
        double yres = -1.0;
        if (north.indexOf("N") != -1 || north.indexOf("n") != -1) {
            north = north.substring(0, north.length() - 1);
            no = degreeToNumber(north);
        } else if (north.indexOf("S") != -1 || north.indexOf("s") != -1) {
            north = north.substring(0, north.length() - 1);
            no = -degreeToNumber(north);
        } else {
            no = Double.parseDouble(north);
        }
        if (south.indexOf("N") != -1 || south.indexOf("n") != -1) {
            south = south.substring(0, south.length() - 1);
            so = degreeToNumber(south);
        } else if (south.indexOf("S") != -1 || south.indexOf("s") != -1) {
            south = south.substring(0, south.length() - 1);
            so = -degreeToNumber(south);
        } else {
            so = Double.parseDouble(south);
        }
        if (west.indexOf("E") != -1 || west.indexOf("e") != -1) {
            west = west.substring(0, west.length() - 1);
            we = degreeToNumber(west);
        } else if (west.indexOf("W") != -1 || west.indexOf("w") != -1) {
            west = west.substring(0, west.length() - 1);
            we = -degreeToNumber(west);
        } else {
            we = Double.parseDouble(west);
        }
        if (east.indexOf("E") != -1 || east.indexOf("e") != -1) {
            east = east.substring(0, east.length() - 1);
            ea = degreeToNumber(east);
        } else if (east.indexOf("W") != -1 || east.indexOf("w") != -1) {
            east = east.substring(0, east.length() - 1);
            ea = -degreeToNumber(east);
        } else {
            ea = Double.parseDouble(east);
        }

        if (ewres.indexOf(':') != -1) {
            xres = degreeToNumber(ewres);
        } else {
            xres = Double.parseDouble(ewres);
        }
        if (nsres.indexOf(':') != -1) {
            yres = degreeToNumber(nsres);
        } else {
            yres = Double.parseDouble(nsres);
        }

        Window tmp = new Window(we, ea, so, no, xres, yres);
        setExtent(tmp);

    }

    private double degreeToNumber( String value ) {
        double number = -1;

        String[] valueSplit = value.trim().split(":");
        if (valueSplit.length == 3) {
            // deg:min:sec.ss
            double deg = Double.parseDouble(valueSplit[0]);
            double min = Double.parseDouble(valueSplit[1]);
            double sec = Double.parseDouble(valueSplit[2]);
            number = deg + min / 60.0 + sec / 60.0 / 60.0;
        } else if (valueSplit.length == 2) {
            // deg:min
            double deg = Double.parseDouble(valueSplit[0]);
            double min = Double.parseDouble(valueSplit[1]);
            number = deg + min / 60.0;
        } else if (valueSplit.length == 1) {
            // deg
            number = Double.parseDouble(valueSplit[0]);
        }
        return number;
    }

    public Window( String west, String east, String south, String north, int rows, int cols ) {
        double no = -1.0;
        double so = -1.0;
        double ea = -1.0;
        double we = -1.0;
        if (north.indexOf("N") != -1 || north.indexOf("n") != -1) {
            north = north.substring(0, north.length() - 1);
            no = degreeToNumber(north);
        } else if (north.indexOf("S") != -1 || north.indexOf("s") != -1) {
            north = north.substring(0, north.length() - 1);
            no = -degreeToNumber(north);
        } else {
            no = Double.parseDouble(north);
        }
        if (south.indexOf("N") != -1 || south.indexOf("n") != -1) {
            south = south.substring(0, south.length() - 1);
            so = degreeToNumber(south);
        } else if (south.indexOf("S") != -1 || south.indexOf("s") != -1) {
            south = south.substring(0, south.length() - 1);
            so = -degreeToNumber(south);
        } else {
            so = Double.parseDouble(south);
        }
        if (west.indexOf("E") != -1 || west.indexOf("e") != -1) {
            west = west.substring(0, west.length() - 1);
            we = degreeToNumber(west);
        } else if (west.indexOf("W") != -1 || west.indexOf("w") != -1) {
            west = west.substring(0, west.length() - 1);
            we = -degreeToNumber(west);
        } else {
            we = Double.parseDouble(west);
        }
        if (east.indexOf("E") != -1 || east.indexOf("e") != -1) {
            east = east.substring(0, east.length() - 1);
            ea = degreeToNumber(east);
        } else if (east.indexOf("W") != -1 || east.indexOf("w") != -1) {
            east = east.substring(0, east.length() - 1);
            ea = -degreeToNumber(east);
        } else {
            ea = Double.parseDouble(east);
        }

        Window tmp = new Window(we, ea, so, no, rows, cols);
        setExtent(tmp);

    }

    /**
     * @return the JTS envelope from the window object
     */
    public Envelope getEnvelope() {
        return new Envelope(new Coordinate(w, n), new Coordinate(e, s));
    }

    /**
     * 
     */
    public void limitRowsCols( int _rows, int _cols ) {
        /* If current rows > _rows then clamp the value. */
        if (rows > _rows) {
            rows = _rows;
        }
        if (cols > _cols) {
            cols = _cols;
        }
        fixResolution();
    }

    /**
     * Sets the extent of this window using another window.
     * 
     * @param win another window object
     */
    public void setExtent( Window win ) {
        w = win.getWest();
        e = win.getEast();
        n = win.getNorth();
        s = win.getSouth();
        rows = win.getRows();
        cols = win.getCols();
        fixResolution();
        fixRowsAndCols();
    }

    /**
     * @return the rectangle wrapping the window object
     */
    public java.awt.geom.Rectangle2D.Double getRectangle() {
        return new java.awt.geom.Rectangle2D.Double(w, s, getXExtent(), getYExtent());
    }

    @SuppressWarnings("nls")
    public String toString() {
        return ("window:\nwest=" + w + "\neast=" + e + "\nsouth=" + s + "\nnorth=" + n + "\nwe_res=" + we_res + "\nns_res="
                + ns_res + "\nrows=" + rows + "\ncols=" + cols);
    }

    /**
     * fix the resolution with the integer rounded rows and cols
     */
    public void fixResolution() {
        we_res = (e - w) / cols;
        ns_res = (n - s) / rows;
    }

    /**
     * calculate rows and cols from the region and its resolution. Round if required.
     */
    public void fixRowsAndCols() {
        rows = (int) Math.round((n - s) / ns_res);
        if (rows < 1)
            rows = 1;
        cols = (int) Math.round((e - w) / we_res);
        if (cols < 1)
            cols = 1;
    }

    /*
     * some getters and setters
     */

    public int getProj() {
        return proj;
    }

    public void setProj( int _proj ) {
        proj = _proj;
    }

    public int getZone() {
        return zone;
    }

    public void setZone( int _zone ) {
        zone = _zone;
    }

    public double getWest() {
        return w;
    }

    public double getEast() {
        return e;
    }

    public double getSouth() {
        return s;
    }

    public double getNorth() {
        return n;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public double getWEResolution() {
        return we_res;
    }

    public double getNSResolution() {
        return ns_res;
    }

    public double getXExtent() {
        return e - w;
    }

    public double getYExtent() {
        return n - s;
    }

    public void setCols( int cols ) {
        this.cols = cols;
    }

    public void setEast( double e ) {
        this.e = e;
    }

    public void setNorth( double n ) {
        this.n = n;
    }

    public void setNSResolution( double ns_res ) {
        this.ns_res = ns_res;
    }

    public void setRows( int rows ) {
        this.rows = rows;
    }

    public void setSouth( double s ) {
        this.s = s;
    }

    public void setWest( double w ) {
        this.w = w;
    }

    public void setWEResolution( double we_res ) {
        this.we_res = we_res;
    }

    public void setAdditionalGrassEntries( LinkedHashMap<String, String> adds ) {
        this.additionalGrassEntries = adds;
    }

    public LinkedHashMap<String, String> getAdditionalGrassEntries() {
        return additionalGrassEntries;
    }

    /**
     * Moves the point given by X and Y to be on the grid of the active region.
     * 
     * @param x the easting of the arbitrary point
     * @param y the northing of the arbitrary point
     * @param activeWindow the active window from which to take the grid
     * @return the snapped point
     */
    public static Point2D.Double snapToNextHigherInActiveRegionResolution( double x, double y, Window activeWindow ) {

        double minx = activeWindow.getRectangle().getBounds2D().getMinX();
        double ewres = activeWindow.getWEResolution();
        double xsnap = minx + (Math.ceil((x - minx) / ewres) * ewres);

        double miny = activeWindow.getRectangle().getBounds2D().getMinY();
        double nsres = activeWindow.getNSResolution();
        double ysnap = miny + (Math.ceil((y - miny) / nsres) * nsres);

        return new Point2D.Double(xsnap, ysnap);

    }

    /**
     * Get the active region window from the mapset
     * 
     * @param mapsetPath the path to the mapset folder
     * @return the active Window object of that mapset
     */
    public static Window getActiveWindowFromMapset( String mapsetPath ) {
        File windFile = new File(mapsetPath + File.separator + GrassLegacyConstans.WIND);
        if (!windFile.exists()) {
            return null;
        }

        return new Window(windFile.getAbsolutePath());
    }

    /**
     * Write active region window to the mapset
     * 
     * @param mapsetPath the path to the mapset folder
     * @param the active Window object
     * @throws IOException 
     */
    public static void writeActiveWindowToMapset( String mapsetPath, Window window ) throws IOException {
        writeWindowFile(mapsetPath + File.separator + GrassLegacyConstans.WIND, window);
    }

    /**
     * Write default region window to the PERMANENT mapset
     * 
     * @param locationPath the path to the location folder
     * @param the active Window object
     * @throws IOException 
     */
    public static void writeDefaultWindowToLocation( String locationPath, Window window ) throws IOException {
        writeWindowFile(locationPath + File.separator + GrassLegacyConstans.PERMANENT_MAPSET + File.separator
                + GrassLegacyConstans.DEFAULT_WIND, window);
    }

    /**
     * Takes an envelope and an active region and creates a new region to match the bounds of the
     * envelope, but the resolutions of the active region. This is important if the region has to
     * match some feature layer. The bounds are assured to contain completely the envelope.
     * 
     * @param bounds
     * @param activeRegion
     */
    public static Window adaptActiveRegionToEnvelope( Envelope bounds, Window activeRegion ) {
        Point2D eastNorth = Window.snapToNextHigherInActiveRegionResolution(bounds.getMaxX(), bounds.getMaxY(), activeRegion);
        Point2D westsouth = Window.snapToNextHigherInActiveRegionResolution(bounds.getMinX() - activeRegion.getWEResolution(),
                bounds.getMinY() - activeRegion.getNSResolution(), activeRegion);
        Window tmp = new Window(westsouth.getX(), eastNorth.getX(), westsouth.getY(), eastNorth.getY(),
                activeRegion.getWEResolution(), activeRegion.getNSResolution());
        // activeRegion.setExtent(tmp);
        return tmp;
    }

    /**
     * @param filePath the path to the window file
     * @param w the resulting window object
     */
    @SuppressWarnings("nls")
    private static Throwable readWindowFile( String filePath, Window w ) {
        String line;

        try {
            BufferedReader windReader;
            windReader = new BufferedReader(new FileReader(filePath));
            LinkedHashMap<String, String> store = new LinkedHashMap<String, String>();
            while( (line = windReader.readLine()) != null ) {
                line = line.replaceFirst(":", "@@@@");
                StringTokenizer tok = new StringTokenizer(line, "@@@@"); //$NON-NLS-1$
                if (tok.countTokens() == 2) {
                    String key = tok.nextToken().trim();
                    String value = tok.nextToken().trim();
                    /*
                     * If key is 'e-w res' or 'n-s resol' or 'res3' then store 'xxx resol'
                     */
                    // this is to keep compatibility with GRASS, which seems to
                    // have changed
                    if ((key.indexOf("res") != -1 && key.indexOf("resol") == -1) //$NON-NLS-1$ //$NON-NLS-2$
                            || key.indexOf("res3") != -1) { //$NON-NLS-1$
                        if (!key.startsWith("compressed")) //$NON-NLS-1$
                            store.put(key.replaceAll("res", "resol"), value); //$NON-NLS-1$ //$NON-NLS-2$
                    } else {
                        store.put(key, value);
                    }
                }
            }

            try {
                w.setProj(Integer.parseInt(store.get("proj"))); //$NON-NLS-1$
                w.setZone(Integer.parseInt(store.get("zone"))); //$NON-NLS-1$
                store.remove("proj");
                store.remove("zone");
            } catch (Exception e) {
                e.printStackTrace();
            }

            Window tmpWindow = null;
            if (store.containsKey("n-s res")) { //$NON-NLS-1$
                tmpWindow = new Window(store.get("west"), //$NON-NLS-1$
                        store.get("east"), //$NON-NLS-1$
                        store.get("south"), //$NON-NLS-1$
                        store.get("north"), //$NON-NLS-1$
                        store.get("e-w res"), store.get("n-s res")); //$NON-NLS-1$
                store.remove("e-w resol");
                store.remove("n-s resol");
            } else if (store.containsKey("cols")) { //$NON-NLS-1$
                tmpWindow = new Window(store.get("west"), //$NON-NLS-1$
                        store.get("east"), //$NON-NLS-1$
                        store.get("south"), //$NON-NLS-1$
                        store.get("north"), //$NON-NLS-1$
                        Integer.parseInt(store.get("rows")), //$NON-NLS-1$
                        Integer.parseInt(store.get("cols"))); //$NON-NLS-1$
                store.remove("cols");
                store.remove("rows");
            } else {
                throw new RuntimeException();
            }
            store.remove("north");
            store.remove("south");
            store.remove("east");
            store.remove("west");

            // what is noty needed in JGrass is needed in GRASS, so keep it
            tmpWindow.setAdditionalGrassEntries(store);

            w.setExtent(tmpWindow);

            windReader.close();
            windReader = null;
            store = null;
        } catch (FileNotFoundException e) {
            return new RuntimeException().initCause(e);
        } catch (IOException e) {
            return new RuntimeException().initCause(e);
        }
        return null;
    }

    /**
     * Reads a text file and changes only the window region values using the window object supplied.
     * 
     * @param filePath the path to the WIND file
     * @param w the window to set
     * @return a throwable object to trace if everything went well
     * @throws IOException 
     */
    private static void writeWindowFile( String filepath, Window w ) throws IOException {

        String line;
        File file = new File(filepath);
        if (!file.exists()) {
            /*
             * if on vfat filesystem it could be a problem of case, often
             * happens with WIND file. So at least try that check.
             */
            String nameLower = file.getName().toLowerCase();
            String nameUpper = file.getName().toUpperCase();
            String baseDir = file.getParent();
            File tmpFile = null;
            if ((tmpFile = new File(baseDir + File.separator + nameLower)).exists()) {
                file = tmpFile;
            } else if ((tmpFile = new File(baseDir + File.separator + nameUpper)).exists()) {
                file = tmpFile;
            } else {
                // ok, file doesn't really exist, just create a blank window first
                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                out.write(BLANK_WINDOW);
                out.close();
            }
        }
        BufferedReader windReader = new BufferedReader(new FileReader(file));
        LinkedHashMap<String, String> store = new LinkedHashMap<String, String>();
        while( (line = windReader.readLine()) != null ) {
            StringTokenizer tok = new StringTokenizer(line, ":"); //$NON-NLS-1$
            if (tok.countTokens() == 2) {
                String key = tok.nextToken().trim();
                String value = tok.nextToken().trim();
                /*
                 * this is now corrected, since GRASS seems to support only resol from 6.2 on
                 */
                if ((key.indexOf("res") != -1 && key.indexOf("resol") == -1) //$NON-NLS-1$ //$NON-NLS-2$
                        || key.indexOf("res3") != -1) { //$NON-NLS-1$
                    store.put(key.replaceAll("res", "resol"), value); //$NON-NLS-1$ //$NON-NLS-2$
                } else
                    store.put(key, value);
            }
        }

        /*
         * Now overwrite the window region entries using the values in the supplied window
         * object.
         */
        store.put("north", new java.lang.Double(w.getNorth()).toString()); //$NON-NLS-1$
        store.put("south", new java.lang.Double(w.getSouth()).toString()); //$NON-NLS-1$
        store.put("east", new java.lang.Double(w.getEast()).toString()); //$NON-NLS-1$
        store.put("west", new java.lang.Double(w.getWest()).toString()); //$NON-NLS-1$
        store.put("n-s resol", new java.lang.Double(w.getNSResolution()).toString()); //$NON-NLS-1$
        store.put("e-w resol", new java.lang.Double(w.getWEResolution()).toString()); //$NON-NLS-1$
        store.put("cols", new java.lang.Integer(w.getCols()).toString()); //$NON-NLS-1$
        store.put("rows", new java.lang.Integer(w.getRows()).toString()); //$NON-NLS-1$
        windReader.close();
        windReader = null;

        /* Now write the data back to the file */
        StringBuffer data = new StringBuffer(512);
        Iterator<String> it = store.keySet().iterator();
        while( it.hasNext() ) {
            String key = it.next();
            data.append(key + ":   " + store.get(key) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        BufferedWriter windWriter = new BufferedWriter(new FileWriter(file));
        windWriter.write(data.toString());
        windWriter.flush();
        windWriter.close();
    }

}