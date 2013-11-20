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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasReader_1_0;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.modules.utils.fileiterator.OmsFileIterator;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Utilities for Las handling classes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasUtils {
    public static final String THE_GEOM = "the_geom";
    public static final String ELEVATION = "elev";
    public static final String INTENSITY = "intensity";
    public static final String CLASSIFICATION = "classifica";
    public static final String IMPULSE = "impulse";
    public static final String NUM_OF_IMPULSES = "numimpulse";
    private static SimpleFeatureBuilder lasSimpleFeatureBuilder;

    private static DateTime gpsEpoch = new DateTime(1980, 1, 6, 0, 0, 0, 0, DateTimeZone.UTC);
    private static DateTime javaEpoch = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);

    public enum POINTTYPE {
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
        final Point point = GeometryUtilities.gf().createPoint(new Coordinate(r.x, r.y));
        final Object[] values = new Object[]{point, r.z, r.intensity, r.classification, r.returnNumber, r.numberOfReturns};
        SimpleFeatureBuilder lasFeatureBuilder = getLasFeatureBuilder(crs);
        lasFeatureBuilder.addAll(values);
        final SimpleFeature feature = lasFeatureBuilder.buildFeature(null);
        return feature;
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
            int classification = ((Number) lasFeature.getAttribute(CLASSIFICATION)).intValue();
            r.classification = classification;
            int impulse = ((Number) lasFeature.getAttribute(IMPULSE)).intValue();
            r.returnNumber = impulse;
            int numOfImpulses = ((Number) lasFeature.getAttribute(NUM_OF_IMPULSES)).intValue();
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
            int seconds = Integer.parseInt(split[1]);
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

        SimpleFeatureCollection newCollection = FeatureCollections.newCollection();

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
            LasReader_1_0 r = new LasReader_1_0(file, crs);
            try {
                r.open();
                ReferencedEnvelope3D envelope = r.getEnvelope();
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

}
