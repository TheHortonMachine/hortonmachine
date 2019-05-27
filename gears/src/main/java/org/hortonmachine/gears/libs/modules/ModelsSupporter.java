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
package org.hortonmachine.gears.libs.modules;

import java.lang.reflect.Field;
import java.util.Collection;

import oms3.Access;
import oms3.ComponentAccess;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

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
     * The 9 directions around a pixel as <b>ROW/COL of the flowdirections grid</b>.
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
     * The 8 flowdirections, defined as entering towards the center cell.
     * 
     * <p>Each array contains [<b>row, col</b>, flowvalue].
     * 
     * <p><b>NOTE that these work in ROW/COL order, not COL/ROW!</b>
     * 
     * <pre>
     *        -1     0     1 
     *      +-----+-----+-----+
     * -1   |  8  |  7  |  6  |
     *      +-----+-----+-----+
     *  0   |  1  |  0  |  5  |
     *      +-----+-----+-----+
     *  1   |  2  |  3  |  4  |
     *      +-----+-----+-----+
     *      
     * </pre>     
     */
    public final static int[][] DIR_WITHFLOW_ENTERING = {//
    {0, 0, 0}, //
            {0, 1, 5}, //
            {-1, 1, 6},//
            {-1, 0, 7},//
            {-1, -1, 8}, //
            {0, -1, 1},//
            {1, -1, 2}, //
            {1, 0, 3}, //
            {1, 1, 4}//
    };

    /**
     * The 8 flowdirections, defined as exiting from the center cell.
     * 
     * <p>Each array contains [<b>col, row</b>, flowvalue].
     * 
     * <pre>
     *        -1     0     1 
     *      +-----+-----+-----+
     * -1   |  4  |  3  |  2  |
     *      +-----+-----+-----+
     *  0   |  5  |  0  |  1  |
     *      +-----+-----+-----+
     *  1   |  6  |  7  |  8  |
     *      +-----+-----+-----+
     *      
     * </pre>     
     */
    public static final int[][] DIR_WITHFLOW_EXITING = {//
    {0, 0, 0},//
            {1, 0, 1}, //
            {1, -1, 2},//
            {0, -1, 3}, //
            {-1, -1, 4}, //
            {-1, 0, 5},//
            {-1, 1, 6},//
            {0, 1, 7},//
            {1, 1, 8}, //
            {0, 0, 9}, //
            {0, 0, 10}//
    };

    /*
     * This is similar to exiting inverted, but is in cols and rows and have a particular order to work (in tca3d) with triangle.
     */
    public static final int[][] DIR_WITHFLOW_EXITING_INVERTED = {{0, 0, 0}, {0, 1, 1}, {-1, 1, 2}, {-1, 0, 3}, {-1, -1, 4},
            {0, -1, 5}, {1, -1, 6}, {1, 0, 7}, {1, 1, 8}, {0, 0, 9}, {0, 0, 10}};

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

    public static String getStatusString( int statusValue ) {
        switch( statusValue ) {
        case Status.CERTIFIED:
            return "CERTIFIED";
        case Status.DRAFT:
            return "DRAFT";
        case Status.EXPERIMENTAL:
            return "EXPERIMENTAL";
        case Status.TESTED:
            return "TESTED";
        case Status.VALIDATED:
            return "VALIDATED";
        }
        return "DRAFT";
    }

    public static void collectParameters( StringBuilder sbTmp, Collection<Access> accessList, String pre ) throws Exception {
        for( Access access : accessList ) {
            Field field = access.getField();
            String fieldName = field.getName();
            Description descriptionAnnot = field.getAnnotation(Description.class);

            if (fieldName.equals("pm") || fieldName.equals("gf") || fieldName.equals("doProcess") || fieldName.equals("doReset")) {
                // ignore progress monitor
                continue;
            }
            String fieldDescription = " - ";
            if (descriptionAnnot != null) {
                fieldDescription = descriptionAnnot.value();
                if (fieldDescription == null) {
                    fieldDescription = " - ";
                }

                Unit unitAnn = field.getAnnotation(Unit.class);
                if (unitAnn != null) {
                    fieldDescription = fieldDescription + " [" + unitAnn.value() + "]";
                }
            }

            sbTmp.append(pre).append(fieldName).append(": ");
            sbTmp.append(fieldDescription).append("\n");
        }
    }

    public static String generateHelp( Object parent ) throws Exception {

        Class< ? > moduleClass = parent.getClass();

        StringBuilder sb = new StringBuilder();

        // try with module description
        Description description = moduleClass.getAnnotation(Description.class);
        String descriptionStr = description.value();
        String NEWLINE = "\n";
        if (description != null) {
            sb.append("Description").append(NEWLINE);
            sb.append("-----------").append(NEWLINE);
            sb.append(NEWLINE);
            sb.append(descriptionStr);
            sb.append(NEWLINE);
            sb.append(NEWLINE);
        }
        // general info
        sb.append("General Information").append(NEWLINE);
        sb.append("-------------------").append(NEWLINE);
        sb.append(NEWLINE);
        // general info: status
        Status status = moduleClass.getAnnotation(Status.class);
        if (status != null) {
            sb.append("Module status: " + ModelsSupporter.getStatusString(status.value())).append(NEWLINE);
        }

        // general info: authors
        Author author = moduleClass.getAnnotation(Author.class);
        if (author != null) {
            String authorNameStr = author.name();
            String[] authorNameSplit = authorNameStr.split(",");

            String authorContactStr = author.contact();
            String[] authorContactSplit = authorContactStr.split(",");

            sb.append("Authors").append(NEWLINE);
            for( String authorName : authorNameSplit ) {
                sb.append("* ").append(authorName.trim()).append(NEWLINE);
            }
            sb.append(NEWLINE);
            sb.append("Contacts: ").append(NEWLINE);
            for( String authorContact : authorContactSplit ) {
                sb.append("* ").append(authorContact.trim()).append(NEWLINE);
            }
            sb.append(NEWLINE);
        }
        // general info: license
        License license = moduleClass.getAnnotation(License.class);
        if (license != null) {
            String licenseStr = license.value();
            sb.append("License: " + licenseStr).append(NEWLINE);
        }
        // general info: keywords
        Keywords keywords = moduleClass.getAnnotation(Keywords.class);
        if (keywords != null) {
            String keywordsStr = keywords.value();
            sb.append("Keywords: " + keywordsStr).append(NEWLINE);
        }
        sb.append(NEWLINE);

        // gather input fields
        Object annotatedObject = moduleClass.newInstance();
        ComponentAccess cA = new ComponentAccess(annotatedObject);

        // parameters
        sb.append("Parameters").append(NEWLINE);
        sb.append("----------").append(NEWLINE);
        sb.append(NEWLINE);
        // parameters: fields
        Collection<Access> inputs = cA.inputs();
        StringBuilder sbTmp = new StringBuilder();
        ModelsSupporter.collectParameters(sbTmp, inputs, "\t");
        String params = sbTmp.toString();
        if (params.trim().length() > 0) {
            sb.append("\tInput Parameters").append(NEWLINE);
            sb.append("\t----------------").append(NEWLINE);
            sb.append(params);
            sb.append(NEWLINE);
        }
        Collection<Access> outputs = cA.outputs();
        sbTmp = new StringBuilder();
        ModelsSupporter.collectParameters(sbTmp, outputs, "\t");
        params = sbTmp.toString();
        if (params.trim().length() > 0) {
            sb.append("\tOutput Parameters").append(NEWLINE);
            sb.append("\t-----------------").append(NEWLINE);
            sb.append(params);
            sb.append(NEWLINE);
        }
        sb.append(NEWLINE);

        return sb.toString();
    }

    public static String generateTemplate( Object parent ) {
        Class< ? > class1 = parent.getClass();
        String name = class1.getSimpleName();
        StringBuilder sb = new StringBuilder();
        String newName = name;
        String varName = name.toLowerCase();
        sb.append("def ").append(varName).append(" = new ");
        sb.append(newName).append("()\n");
        java.lang.reflect.Field[] fields = class1.getFields();
        for( java.lang.reflect.Field field : fields ) {
            String fname = field.getName();
            if (fname.equals("pm") || fname.equals("gf") || fname.equals("doProcess") || fname.equals("doReset")) {
                continue;
            }
            Class< ? > cl = field.getType();
            Out out = field.getAnnotation(Out.class);
            if (out != null) {
                continue;
            }
            if (cl.isAssignableFrom(String.class)) {
                sb.append(varName).append(".").append(fname).append(" = \"\"\n");
            } else {
                sb.append(varName).append(".").append(fname).append(" = ?\n");
            }
        }
        sb.append(varName).append(".process()\n");

        // outputs
        // for( java.lang.reflect.Field field : fields ) {
        // String fname = field.getName();
        // Class< ? > cl = field.getType();
        // Out out = field.getAnnotation(Out.class);
        // if (out == null) {
        // continue;
        // }
        // if (cl.isAssignableFrom(org.geotools.coverage.grid.GridCoverage2D.class)) {
        // sb.append("dumpRaster(").append(varName).append(".").append(fname).append(", ").append(fname).append(")\n");
        // } else if (cl.isAssignableFrom(org.geotools.data.simple.SimpleFeatureCollection.class)) {
        // sb.append("dumpVector(").append(varName).append(".").append(fname).append(", ").append(fname).append(")\n");
        // } else {
        // System.err.println("Unrecognised: " + fname);
        // }
        // }
        return sb.toString();
    }
}
