/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.io.las.utils;

import static java.lang.Math.abs;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasReader;
import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.modules.utils.fileiterator.OmsFileIterator;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

/**
 * Utilities for Las handling classes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasUtils {
    private static final GeometryFactory gf = GeometryUtilities.gf();

    public static final String THE_GEOM = "the_geom";
    public static final String ELEVATION = "elev";
    public static final String INTENSITY = "intensity";
    public static final String CLASSIFICATION = "classifica";
    public static final String IMPULSE = "impulse";
    public static final String NUM_OF_IMPULSES = "numimpulse";
    private static SimpleFeatureBuilder lasSimpleFeatureBuilder;

    public static String dateTimeFormatterYYYYMMDD_string = "yyyy-MM-dd";
    public static DateTimeFormatter dateTimeFormatterYYYYMMDD = DateTimeFormat.forPattern(dateTimeFormatterYYYYMMDD_string);

    private static DateTime gpsEpoch = new DateTime(1980, 1, 6, 0, 0, 0, 0, DateTimeZone.UTC);
    // private static DateTime javaEpoch = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);

    public static enum VALUETYPE {
        ELEVATION, GROUNDELEVATION, CLASSIFICATION, INTENSITY, IMPULSE, NUM_OF_IMPULSES, X, Y
    }

    public static enum POINTTYPE {
        UNCLASSIFIED(1, "UNCLASSIFIED"), //
        GROUND(2, "GROUND"), //
        VEGETATION_MIN(3, "LOW VEGETATION"), //
        VEGETATION_MED(4, "MEDIUM VEGETATION"), //
        VEGETATION_MAX(5, "HIGH VEGETATION"), //
        BUILDING(6, "BUILDING"), //
        LOW_POINT(7, "LOW POINT (NOISE)"), //
        MASS_POINT(8, "MODEL KEY-POINT (MASS)"), //
        WATER(9, "WATER"), //
        OVERLAP(12, "OVERLAP");

        private String label;
        private int value;

        POINTTYPE( int value, String label ) {
            this.value = value;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Read just the version bytes from a las file.
     * 
     * <p>This can be handy is one needs to choose version reader.
     * 
     * @param lasFile the las file to check.
     * @return the version string as "major.minor" .
     * @throws IOException
     */
    public static String getLasFileVersion( File lasFile ) throws IOException {
        FileInputStream fis = null;
        FileChannel fc = null;
        try {
            fis = new FileInputStream(lasFile);
            fc = fis.getChannel();
            // Version Major
            fis.skip(24);
            int versionMajor = fis.read();
            // Version Minor
            int versionMinor = fis.read();
            String version = versionMajor + "." + versionMinor; //$NON-NLS-1$
            return version;
        } finally {
            fc.close();
            fis.close();
        }
    }

    /**
     * Creates a builder for las data.
     * 
     * The attributes are:
     * 
     * <ul>
     *   <li>the_geom:  a point geometry</li>
     *   <li>elev</li>
     *   <li>intensity</li>
     *   <li>classification</li>
     *   <li>impulse</li>
     *   <li>numimpulse</li>
     * </ul>
     * 
     * 
     * @param crs the {@link CoordinateReferenceSystem}.
     * @return the {@link SimpleFeatureBuilder builder}.
     */
    public static SimpleFeatureBuilder getLasFeatureBuilder( CoordinateReferenceSystem crs ) {
        if (lasSimpleFeatureBuilder == null) {
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("lasdata");
            b.setCRS(crs);
            b.add(THE_GEOM, Point.class);
            b.add(ELEVATION, Double.class);
            b.add(INTENSITY, Double.class);
            b.add(CLASSIFICATION, Integer.class);
            b.add(IMPULSE, Double.class);
            b.add(NUM_OF_IMPULSES, Double.class);
            final SimpleFeatureType featureType = b.buildFeatureType();
            lasSimpleFeatureBuilder = new SimpleFeatureBuilder(featureType);
        }
        return lasSimpleFeatureBuilder;
    }

    public static SimpleFeature tofeature( LasRecord r, CoordinateReferenceSystem crs ) {
        final Point point = toGeometry(r);
        double elev = r.z;
        if (!Double.isNaN(r.groundElevation)) {
            elev = r.groundElevation;
        }
        final Object[] values = new Object[]{point, elev, r.intensity, r.classification, r.returnNumber, r.numberOfReturns};
        SimpleFeatureBuilder lasFeatureBuilder = getLasFeatureBuilder(crs);
        lasFeatureBuilder.addAll(values);
        final SimpleFeature feature = lasFeatureBuilder.buildFeature(null);
        return feature;
    }

    public static Point toGeometry( LasRecord r ) {
        return gf.createPoint(new Coordinate(r.x, r.y));
    }

    public static List<LasRecord> getLasRecordsFromFeatureCollection( SimpleFeatureCollection lasCollection ) {
        List<SimpleFeature> featuresList = FeatureUtilities.featureCollectionToList(lasCollection);
        List<LasRecord> lasList = new ArrayList<LasRecord>();
        for( SimpleFeature lasFeature : featuresList ) {
            LasRecord r = new LasRecord();
            Coordinate coordinate = ((Geometry) lasFeature.getDefaultGeometry()).getCoordinate();
            r.x = coordinate.x;
            r.y = coordinate.y;
            double elevation = ((Number) lasFeature.getAttribute(ELEVATION)).doubleValue();
            r.z = elevation;
            short intensity = ((Number) lasFeature.getAttribute(INTENSITY)).shortValue();
            r.intensity = intensity;
            byte classification = ((Number) lasFeature.getAttribute(CLASSIFICATION)).byteValue();
            r.classification = classification;
            short impulse = ((Number) lasFeature.getAttribute(IMPULSE)).shortValue();
            r.returnNumber = impulse;
            short numOfImpulses = ((Number) lasFeature.getAttribute(NUM_OF_IMPULSES)).shortValue();
            r.numberOfReturns = numOfImpulses;
            lasList.add(r);
        }
        return lasList;
    }

    /**
     * Converts las gps time to {@link DateTime}.
     * 
     * <p>
     * Time based on Global Encoding Bit:
     * <pre>
     *     LAS 1.0:
     *     LAS 1.1:
     *       no encoding information available
     *   
     *     LAS 1.2:
     *       0: GPS Time is GPS Week Time
     *       1: GPS Time is POSIX Time or (!!) Standard GPS Time minus 1 x 10**9
     *   
     *     LAS 1.3:
     *     LAS 1.4:
     *       0: GPS Time is GPS Week Time
     *       1: GPS Time is Standard GPS Time minus 1 x 10**9
     * </pre>
     * 
     * <p>
     * Discussions:
     * <ul>
     * <li>https://groups.google.com/d/msg/lastools/ik_knw5njqY/7nAqsJfV4dUJ</li>
     * </ul>
     * 
     * @param gpsTime the time value.
     * @param gpsTimeType the time type (0=week.seconds, 1=adjusted standard gps time)
     * @return the UTC date object.
     */
    public static DateTime gpsTimeToDateTime( double gpsTime, int gpsTimeType ) {
        if (gpsTimeType == 0) {
            String[] split = String.valueOf(gpsTime).split("\\.");
            int week = Integer.parseInt(split[0]);
//            int seconds = Integer.parseInt(split[1]);
            long seconds = Long.parseLong(split[1]);
            double standardGpsTimeSeconds = week * 604800 + seconds;
            double standardGpsTimeMillis = standardGpsTimeSeconds * 1000;
            DateTime dt = gpsEpoch.plus((long) standardGpsTimeMillis);
            return dt;
        } else {
            // gps time is adjusted gps time
            double standardGpsTimeSeconds = gpsTime + 1E9;
            double standardGpsTimeMillis = standardGpsTimeSeconds * 1000;
            DateTime dt1 = gpsEpoch.plus((long) standardGpsTimeMillis);
            return dt1;
        }

    }

    /**
     * Converts an date object to standard gps time.
     * 
     * @param dateTime the object (UTC).
     * @return the standard gps time in seconds.
     */
    public static double dateTimeToStandardGpsTime( DateTime dateTime ) {
        long millis = dateTime.getMillis() - gpsEpoch.getMillis();
        return millis / 1000.0;
    }

    /**
     * Dump an overview shapefile for a las folder.
     * 
     * @param folder the folder.
     * @param crs the crs to use.
     * @throws Exception
     */
    public static void dumpLasFolderOverview( String folder, CoordinateReferenceSystem crs ) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("overview");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("name", String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();

        OmsFileIterator iter = new OmsFileIterator();
        iter.inFolder = folder;
        iter.fileFilter = new FileFilter(){
            public boolean accept( File pathname ) {
                return pathname.getName().endsWith(".las");
            }
        };
        iter.process();

        List<File> filesList = iter.filesList;

        for( File file : filesList ) {
            ALasReader r = new LasReader(file, crs);
            try {
                r.open();
                ReferencedEnvelope3D envelope = r.getHeader().getDataEnvelope();
                Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(envelope);
                Object[] objs = new Object[]{polygon, r.getLasFile().getName()};
                builder.addAll(objs);
                SimpleFeature feature = builder.buildFeature(null);
                newCollection.add(feature);
            } finally {
                r.close();
            }
        }

        File folderFile = new File(folder);
        File outFile = new File(folder, "overview_" + folderFile.getName() + ".shp");
        OmsVectorWriter.writeVector(outFile.getAbsolutePath(), newCollection);
    }

    /**
     * Projected distance between two points.
     * 
     * @param r1 the first point.
     * @param r2 the second point.
     * @return the 2D distance.
     */
    public static double distance( LasRecord r1, LasRecord r2 ) {
        double distance = NumericsUtilities.pythagoras(r1.x - r2.x, r1.y - r2.y);
        return distance;
    }

    /**
     * Distance between two points.
     * 
     * @param r1 the first point.
     * @param r2 the second point.
     * @return the 3D distance.
     */
    public static double distance3D( LasRecord r1, LasRecord r2 ) {
        double deltaElev = Math.abs(r1.z - r2.z);
        double projectedDistance = NumericsUtilities.pythagoras(r1.x - r2.x, r1.y - r2.y);
        double distance = NumericsUtilities.pythagoras(projectedDistance, deltaElev);
        return distance;
    }

    /**
     * String representation for a {@link LasRecord}.
     * 
     * @param dot the record to convert.
     * @return the string.
     */
    public static String lasRecordToString( LasRecord dot ) {
        final String CR = "\n";
        final String TAB = "\t";
        StringBuilder retValue = new StringBuilder();
        retValue.append("Dot ( \n").append(TAB).append("x = ").append(dot.x).append(CR).append(TAB).append("y = ").append(dot.y)
                .append(CR).append(TAB).append("z = ").append(dot.z).append(CR).append(TAB).append("intensity = ")
                .append(dot.intensity).append(CR).append(TAB).append("impulse = ").append(dot.returnNumber).append(CR)
                .append(TAB).append("impulseNum = ").append(dot.numberOfReturns).append(CR).append(TAB)
                .append("classification = ").append(dot.classification).append(CR).append(TAB).append("gpsTime = ")
                .append(dot.gpsTime).append(CR).append(" )");
        return retValue.toString();
    }

    /**
     * Compare two {@link LasRecord}s.
     * 
     * @param dot1 the first record.
     * @param dot2 the second record.
     * @return <code>true</code>, if the records are the same.
     */
    public static boolean lasRecordEqual( LasRecord dot1, LasRecord dot2 ) {
        double delta = 0.000001;
        boolean check = NumericsUtilities.dEq(dot1.x, dot2.x, delta);
        if (!check) {
            return false;
        }
        check = NumericsUtilities.dEq(dot1.y, dot2.y, delta);
        if (!check) {
            return false;
        }
        check = NumericsUtilities.dEq(dot1.z, dot2.z, delta);
        if (!check) {
            return false;
        }
        check = dot1.intensity == dot2.intensity;
        if (!check) {
            return false;
        }
        check = dot1.classification == dot2.classification;
        if (!check) {
            return false;
        }
        check = dot1.returnNumber == dot2.returnNumber;
        if (!check) {
            return false;
        }
        check = dot1.numberOfReturns == dot2.numberOfReturns;
        if (!check) {
            return false;
        }
        return true;
    }

    /**
     * Calculate the avg of a value in a list of {@link LasRecord}s.
     * 
     * @param points the records.
     * @param valueType the value to consider. 
     * @return the avg.
     */
    public static double avg( List<LasRecord> points, VALUETYPE valueType ) {
        double sum = 0;
        int count = 0;
        for( LasRecord lasRecord : points ) {
            sum = sum + getValue(valueType, lasRecord);
            count++;
        }
        double avg = sum / count;
        return avg;
    }

    private static double getValue( VALUETYPE valueType, LasRecord lasRecord ) {
        switch( valueType ) {
        case ELEVATION:
            return lasRecord.z;
        case GROUNDELEVATION:
            return lasRecord.groundElevation;
        case CLASSIFICATION:
            return lasRecord.classification;
        case INTENSITY:
            return lasRecord.intensity;
        case IMPULSE:
            return lasRecord.returnNumber;
        case NUM_OF_IMPULSES:
            return lasRecord.numberOfReturns;
        case X:
            return lasRecord.x;
        case Y:
            return lasRecord.y;
        }
        return Double.NaN;
    }

    /**
     * Calculate the histogram of a list of {@link LasRecord}s.
     * 
     * @param points the list of points.
     * @param valueType the value to consider.
     * @param bins the number of bins.
     * @return the histogram as matrix of rows num like bins and 3 columns for [binCenter, count, cummulated-normalize-count].
     */
    public static double[][] histogram( List<LasRecord> points, VALUETYPE valueType, int bins ) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for( LasRecord lasRecord : points ) {
            double value = getValue(valueType, lasRecord);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        double range = max - min;
        double step = range / bins;
        double[][] histogram = new double[bins][3];
        for( int i = 0; i < histogram.length; i++ ) {
            histogram[i][0] = min + step * (i + 1);
        }

        for( LasRecord lasRecord : points ) {
            double value = getValue(valueType, lasRecord);
            for( int j = 0; j < histogram.length; j++ ) {
                if (value <= histogram[j][0]) {
                    histogram[j][1] = histogram[j][1] + 1;
                    break;
                }
            }
        }

        double cumulatedMax = 0;
        for( int i = 0; i < histogram.length; i++ ) {
            if (i == 0) {
                histogram[i][2] = histogram[i][1];
            } else {
                histogram[i][2] = (histogram[i - 1][2] + histogram[i][1]);
            }
            cumulatedMax = histogram[i][2];
        }

        for( int i = 0; i < histogram.length; i++ ) {
            histogram[i][2] = histogram[i][2] / cumulatedMax;
            // and move the bin markers to their centers
            histogram[i][0] = histogram[i][0] - step / 2.0;
        }

        return histogram;
    }

    /**
     * Triangulates a set of las points.
     * 
     * <p>If a threshold is supplied, a true dsm filtering is also applied.
     * 
     * @param lasPoints the list of points.
     * @param elevThres the optional threshold for true dsm calculation.
     * @param useGround use the ground elevation instead of z.
     * @param pm the monitor.
     * @return the list of triangles.
     */
    public static List<Geometry> triangulate( List<LasRecord> lasPoints, Double elevThres, boolean useGround,
            IJGTProgressMonitor pm ) {
        pm.beginTask("Triangulation...", -1);
        List<Coordinate> lasCoordinates = new ArrayList<Coordinate>();
        for( LasRecord lasRecord : lasPoints ) {
            lasCoordinates.add(new Coordinate(lasRecord.x, lasRecord.y, useGround ? lasRecord.groundElevation : lasRecord.z));
        }
        DelaunayTriangulationBuilder triangulationBuilder = new DelaunayTriangulationBuilder();
        triangulationBuilder.setSites(lasCoordinates);
        Geometry triangles = triangulationBuilder.getTriangles(gf);
        pm.done();

        ArrayList<Geometry> trianglesList = new ArrayList<Geometry>();
        int numTriangles = triangles.getNumGeometries();
        if (elevThres == null) {
            // no true dsm to be calculated
            for( int i = 0; i < numTriangles; i++ ) {
                Geometry geometryN = triangles.getGeometryN(i);
                trianglesList.add(geometryN);
            }
        } else {
            double pElevThres = elevThres;
            numTriangles = triangles.getNumGeometries();
            pm.beginTask("Extracting triangles based on threshold...", numTriangles);
            for( int i = 0; i < numTriangles; i++ ) {
                pm.worked(1);
                Geometry geometryN = triangles.getGeometryN(i);
                Coordinate[] coordinates = geometryN.getCoordinates();
                double z0 = coordinates[0].z;
                double z1 = coordinates[1].z;
                double z2 = coordinates[2].z;
                double diff1 = abs(z0 - z1);
                if (diff1 > pElevThres) {
                    continue;
                }
                double diff2 = abs(z0 - z2);
                if (diff2 > pElevThres) {
                    continue;
                }
                double diff3 = abs(z1 - z2);
                if (diff3 > pElevThres) {
                    continue;
                }
                trianglesList.add(geometryN);
            }
            pm.done();
        }
        return trianglesList;
    }

    /**
     * Smooths a set of las points through the IDW method.
     * 
     * <p>Note that the values in the original data are changed.
     * 
     * @param lasPoints the list of points to smooth.
     * @param useGround if <code>true</code>, the ground elev is smoothed instead of the z. 
     * @param idwBuffer the buffer around the points to consider for smoothing.
     * @param pm the monitor.
     */
    public static void smoothIDW( List<LasRecord> lasPoints, boolean useGround, double idwBuffer, IJGTProgressMonitor pm ) {
        List<Coordinate> coordinatesList = new ArrayList<Coordinate>();
        if (useGround) {
            for( LasRecord dot : lasPoints ) {
                Coordinate c = new Coordinate(dot.x, dot.y, dot.groundElevation);
                coordinatesList.add(c);
            }
        } else {
            for( LasRecord dot : lasPoints ) {
                Coordinate c = new Coordinate(dot.x, dot.y, dot.z);
                coordinatesList.add(c);
            }
        }

        // make triangles tree
        STRtree pointsTree = new STRtree(coordinatesList.size());
        pm.beginTask("Make points tree...", coordinatesList.size());
        for( Coordinate coord : coordinatesList ) {
            pointsTree.insert(new Envelope(coord), coord);
            pm.worked(1);
        }
        pm.done();

        pm.beginTask("Interpolate...", coordinatesList.size());
        for( int i = 0; i < coordinatesList.size(); i++ ) {
            Coordinate coord = coordinatesList.get(i);
            Envelope env = new Envelope(coord);
            env.expandBy(idwBuffer);
            List<Coordinate> nearPoints = pointsTree.query(env);
            double avg = 0;
            for( Coordinate coordinate : nearPoints ) {
                avg += coordinate.z;
            }
            avg = avg / nearPoints.size();

            LasRecord lasRecord = lasPoints.get(i);
            if (useGround) {
                lasRecord.groundElevation = avg;
            } else {
                lasRecord.z = avg;
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * Clone a {@link LasRecord}. 
     * 
     * @param lasRecord the record to clone.
     * @return the duplicate new object.
     */
    public static LasRecord clone( LasRecord lasRecord ) {
        LasRecord clone = new LasRecord();
        clone.x = lasRecord.x;
        clone.y = lasRecord.y;
        clone.z = lasRecord.z;
        clone.intensity = lasRecord.intensity;
        clone.returnNumber = lasRecord.returnNumber;
        clone.numberOfReturns = lasRecord.numberOfReturns;
        clone.classification = lasRecord.classification;
        clone.color = lasRecord.color;
        clone.gpsTime = lasRecord.gpsTime;
        clone.groundElevation = lasRecord.groundElevation;
        clone.pointsDensity = lasRecord.pointsDensity;
        return clone;
    }
}
