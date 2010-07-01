/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.libs.modules;


/**
 * <p>
 * Facility methods and constants used by the console engine
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class ModelsSupporter {
    /** name for the JGrass database constant */
    public final static String GRASSDB = "grassdb"; //$NON-NLS-1$

    /** name for the JGrass location constant */
    public final static String LOCATION = "location"; //$NON-NLS-1$

    /** name for the JGrass mapset constant */
    public final static String MAPSET = "mapset"; //$NON-NLS-1$

    /** name for the startdate constant */
    public final static String STARTDATE = "time_start_up"; //$NON-NLS-1$

    /** name for the enddate constant */
    public final static String ENDDATE = "time_ending_up"; //$NON-NLS-1$

    /** name for the deltat constant */
    public final static String DELTAT = "time_delta"; //$NON-NLS-1$

    /** name for the remotedb constant */
    public final static String REMOTEDBURL = "remotedburl"; //$NON-NLS-1$

    /** name for the JGrass active region constant */
    public final static String ACTIVEREGIONWINDOW = "active region window"; //$NON-NLS-1$

    /** name for the JGrass featurecollection */
    public final static String FEATURECOLLECTION = "featurecollection"; //$NON-NLS-1$

    /** name for the JGrass unknown elements */
    public final static String UNKNOWN = "unknown"; //$NON-NLS-1$

    /** name for the raster unit id */
    public final static String UNITID_RASTER = "raster unit id"; //$NON-NLS-1$

    /** name for the color map unit id */
    public final static String UNITID_COLORMAP = "colormap unit id"; //$NON-NLS-1$

    /** name for the text unit id */
    public final static String UNITID_TEXTFILE = "text file unit id"; //$NON-NLS-1$

    /** name for the categories unit id */
    public final static String UNITID_CATS = "categories unit id"; //$NON-NLS-1$

    /** name for the scalar unit id */
    public final static String UNITID_SCALAR = "scalar unit id"; //$NON-NLS-1$

    /** name for the vector unit id */
    public final static String UNITID_FEATURE = "feature unit id"; //$NON-NLS-1$

    /** name for the generic unit id */
    public final static String UNITID_UNKNOWN = "unknown unit id"; //$NON-NLS-1$

    /** variable telling that the output should be redirected to console */
    public final static String CONSOLE = "CONSOLE"; //$NON-NLS-1$

    /** variable telling that the output should be redirected to gui table */
    public final static String UITABLE = "UITABLE"; //$NON-NLS-1$

    /**
     * The 9 directions around a pixel.
     * <p>
     * Also containing the central position 0,0
     * </p>
     * <p>
     * FIXME Erica used to add several {0,0} at the end, in order to catch certain values. Those
     * have to be tracked down.
     * </p>
     */
    public final static int[][] DIR = {{0, 0}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}};

    /**
     * The 9 directions and their flow values around a pixel.
     * <p>
     * Also containing the central position 0,0
     * </p>
     * <p>
     * FIXME Erica used to add several {0,0,0} at the end, in order to catch certain values. Those
     * have to be tracked down.
     * </p>
     */
    public final static int[][] DIR_WITHFLOW_ENTERING = {{0, 0, 0}, {0, 1, 5}, {-1, 1, 6}, {-1, 0, 7}, {-1, -1, 8}, {0, -1, 1}, {1, -1, 2}, {1, 0, 3}, {1, 1, 4}};
    public static final int[][] DIR_WITHFLOW_EXITING = {{0, 0, 0}, {-1, 0, 5}, {-1, 1, 6}, {-1, 0, 7}, {1, 1, 8}, {0, -1, 3}, {1, -1, 2}, {1, 0, 1}, {-1, -1, 4}, {0, 0, 9}, {0, 0, 10}};
    /*
     * This is similar to exiting inverted, but is in cols and dows and have a particular order to work (in tca3d) with triangle.
     */
    public static final int[][] DIR_WITHFLOW_EXITING_INVERTED = {{0, 0, 0}, {0, 1, 1}, {-1, 1, 2}, {-1, 0, 3}, {-1, -1, 4}, {0, -1, 5}, {1, -1, 6}, {1, 0, 7}, {1, 1, 8}, {0, 0, 9}, {0, 0, 10}};

    /**
     * Calculate the drainage direction factor (is used in some horton machine like pitfiller,
     * flow,...)
     * <p>
     * Is the distance betwen the central pixel, in a 3x3 kernel, and the neighboured pixels.
     * 
     * @param dx is the resolution of a raster map in the x direction.
     * @param dy is the resolution of the raster map in the y direction.
     * @return <b>fact</b> the direction factor or 1/lenght where lenght is the distance of the
     *         pixel from the central poxel.
     */
    public static double[] calculateDirectionFactor( double dx, double dy ) {
        // direction factor, where the components are 1/length
        double[] fact = new double[9];
        for( int k = 1; k <= 8; k++ ) {
            fact[k] = 1.0 / (Math.sqrt(DIR[k][0] * dy * DIR[k][0] * dy + DIR[k][1] * DIR[k][1] * dx * dx));
        }
        return fact;
    }
    
    // ///////////////////////////////////////////////////
    // MAP TYPES
    // ///////////////////////////////////////////////////

    /**
     * color map type identificator
     */
    final public static String COLORMAP = "colormap"; //$NON-NLS-1$

    /**
     * text file type identificator
     */
    final public static String TEXTFILE = "textfile"; //$NON-NLS-1$

    /**
     * color map type identificator
     */
    final public static String CATSMAP = "catsmap"; //$NON-NLS-1$

    /**
     * raster map type identificator
     */
    final public static String GRASSRASTERMAP = "grassrastermap"; //$NON-NLS-1$

    final public static String DEFAULTKEY = "defaultkey"; //$NON-NLS-1$

    public static final String RESOLUTION = "resolution";

}
