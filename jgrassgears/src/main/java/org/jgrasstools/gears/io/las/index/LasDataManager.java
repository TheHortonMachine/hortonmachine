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
package org.jgrasstools.gears.io.las.index;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.strtree.AbstractSTRtree;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.util.WeakValueHashMap;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.index.strtree.STRtreeJGT;
import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that manages las folder data.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasDataManager implements AutoCloseable {
    private WeakValueHashMap<String, Pair> fileName2LasReaderMap;
    private WeakValueHashMap<String, STRtreeJGT> fileName2IndexMap;
    private List<String> fileName4LasReaderMapSupport;
    // private int READERCACHE = 5;
    private File lasFolderIndexFile;
    private File lasFolder;
    private STRtreeJGT mainLasFolderIndex;
    private GridCoverage2D inDem;
    private double elevThreshold;
    private CoordinateReferenceSystem crs;

    private GeometryFactory gf = GeometryUtilities.gf();
    private double[] intensityRange;
    private double[] impulses;
    private int impulsesNum = -1;
    private double[] classes;
    private boolean hasConstraint = false;
    private SimpleFeatureCollection overviewFeatures;
    private ReferencedEnvelope referencedEnvelope2D;
    private List<ReferencedEnvelope> referencedEnvelope2DList = new ArrayList<ReferencedEnvelope>();
    private List<String> fileNamesList = new ArrayList<String>();
    private ReferencedEnvelope3D referencedEnvelope3D;

    /**
     * Constructor.
     * 
     * @param lasFolderIndexFile the las folder index file.
     * @param inDem a dem to normalize the elevation. If <code>null</code>, the original las elevation is used.
     * @param elevThreshold a threshold to use for the elevation normalization.
     * @param inCrs the data {@link org.opengis.referencing.crs.CoordinateReferenceSystem}. if null, the one of the dem is read, if available.
     */
    public LasDataManager( File lasFolderIndexFile, GridCoverage2D inDem, double elevThreshold, CoordinateReferenceSystem inCrs ) {
        this.lasFolderIndexFile = lasFolderIndexFile;
        this.inDem = inDem;
        this.elevThreshold = elevThreshold;
        lasFolder = lasFolderIndexFile.getParentFile();

        try {
            // prj file rules if available
            inCrs = CrsUtilities.readProjectionFile(lasFolderIndexFile.getAbsolutePath(), "lasfolder");
        } catch (Exception e) {
            // ignore and try to read
        }
        if (inCrs != null) {
            crs = inCrs;
        } else if (inDem != null) {
            crs = inDem.getCoordinateReferenceSystem();
        } else {
            throw new IllegalArgumentException("The Crs can't be null.");
        }
        // indexMap = new LinkedHashMap<String, Pair>() {
        // @Override
        // protected boolean removeEldestEntry(Entry<String, Pair> eldest) {
        // return size() > READERCACHE;
        // }
        // };
        fileName2LasReaderMap = new WeakValueHashMap<String, Pair>();
        fileName2IndexMap = new WeakValueHashMap<String, STRtreeJGT>();
        fileName4LasReaderMapSupport = new ArrayList<String>();
    }

    public File getFolderIndexFile() {
        return lasFolderIndexFile;
    }

    /**
     * Open the main folder file and read the main index.
     *
     * @throws Exception
     */
    public void open() throws Exception {
        mainLasFolderIndex = LasIndexReader.readIndex(lasFolderIndexFile.getAbsolutePath());
    }

    public void setIntensityConstraint( double[] minMax ) {
        intensityRange = minMax;
        hasConstraint = true;
    }

    public void setImpulsesConstraint( double[] impulsesToKeep ) {
        impulses = impulsesToKeep;
        hasConstraint = true;
    }

    public void setImpulsesNumConstraint( int impulsesNumToKeep ) {
        impulsesNum = impulsesNumToKeep;
        hasConstraint = true;
    }

    public void setClassesConstraint( double[] classesToKeep ) {
        classes = classesToKeep;
        hasConstraint = true;
    }

    /**
     * Get points inside a given geometry boundary.
     *
     * @param checkGeom the {@link com.vividsolutions.jts.geom.Geometry} to use to check.
     * @param doOnlyEnvelope check for the geom envelope instead of a intersection with it.
     * @return the list of points contained in the supplied geometry.
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public synchronized List<LasRecord> getPointsInGeometry( Geometry checkGeom, boolean doOnlyEnvelope ) throws Exception {
        checkOpen();
        ArrayList<LasRecord> pointsListForTile = new ArrayList<LasRecord>();

        Envelope env = checkGeom.getEnvelopeInternal();
        PreparedGeometry preparedGeometry = null;
        if (!doOnlyEnvelope) {
            preparedGeometry = PreparedGeometryFactory.prepare(checkGeom);
        }

        List filesList = mainLasFolderIndex.query(env);
        for( Object fileName : filesList ) {
            if (fileName instanceof String) {
                String name = (String) fileName;

                Pair pair = fileName2LasReaderMap.get(name);
                if (pair == null) {
                    File lasFile = new File(lasFolder, name);
                    File lasIndexFile = FileUtilities.substituteExtention(lasFile, "lasfix");

                    if (lasIndexFile.exists()) {
                        ALasReader reader = ALasReader.getReader(lasFile, crs);
                        reader.open();
                        reader.getHeader();
                        STRtreeJGT lasIndex = LasIndexReader.readIndex(lasIndexFile.getAbsolutePath());
                        pair = new Pair();
                        pair.reader = reader;
                        pair.strTree = lasIndex;
                        fileName2LasReaderMap.put(name, pair);
                        fileName4LasReaderMapSupport.add(name);
                    } else {
                        continue;
                    }
                }

                List addressesList = pair.strTree.query(env);
                for( Object obj : addressesList ) {
                    if (obj instanceof double[]) {
                        double[] addresses = (double[]) obj;
                        long from = (long) addresses[0];
                        long to = (long) addresses[1];
                        for( long pointNum = from; pointNum < to; pointNum++ ) {
                            LasRecord lasDot = pair.reader.getPointAt(pointNum);
                            if (!doAccept(lasDot)) {
                                continue;
                            }
                            if (inDem != null) {
                                Coordinate c = new Coordinate(lasDot.x, lasDot.y);
                                if (env.contains(c)) {
                                    // check geom instead of only envelope?
                                    if (!doOnlyEnvelope && !preparedGeometry.contains(gf.createPoint(c))) {
                                        continue;
                                    }
                                    double value = CoverageUtilities.getValue(inDem, lasDot.x, lasDot.y);
                                    double height = lasDot.z - value;
                                    if (height > elevThreshold) {
                                        // lasDot.z = height;
                                        lasDot.groundElevation = height;
                                        pointsListForTile.add(lasDot);
                                    }
                                }
                            } else {
                                Coordinate c = new Coordinate(lasDot.x, lasDot.y);
                                if (env.contains(c)) {
                                    // check geom instead of only envelope?
                                    if (!doOnlyEnvelope && !preparedGeometry.contains(gf.createPoint(c))) {
                                        continue;
                                    }
                                    pointsListForTile.add(lasDot);
                                }
                            }

                        }
                    }
                }
            }
        }
        return pointsListForTile;
    }

    /**
     * Retrieve all the trees envelopes that intersect the geometry.
     *
     * @param checkGeom the {@link com.vividsolutions.jts.geom.Geometry} to use to check.
     * @param doOnlyEnvelope check for the geom envelope instead of a intersection with it.
     * @param minMaxZ an array to be filled with the min and max z to be used as style.
     * @return the list of envelopes contained in the supplied geometry.
     * @throws Exception
     */
    public synchronized List<Geometry> getEnvelopesInGeometry( Geometry checkGeom, boolean doOnlyEnvelope, double[] minMaxZ )
            throws Exception {
        checkOpen();
        ArrayList<Geometry> envelopeListForTile = new ArrayList<Geometry>();

        Envelope env = checkGeom.getEnvelopeInternal();
        PreparedGeometry preparedGeometry = null;
        if (!doOnlyEnvelope) {
            preparedGeometry = PreparedGeometryFactory.prepare(checkGeom);
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        List filesList = mainLasFolderIndex.query(env);
        for( Object fileName : filesList ) {
            if (fileName instanceof String) {
                String name = (String) fileName;
                File lasFile = new File(lasFolder, name);
                File lasIndexFile = FileUtilities.substituteExtention(lasFile, "lasfix");

                String absolutePath = lasIndexFile.getAbsolutePath();
                STRtreeJGT lasIndex = fileName2IndexMap.get(absolutePath);
                if (lasIndex == null) {
                    lasIndex = LasIndexReader.readIndex(absolutePath);
                    fileName2IndexMap.put(absolutePath, lasIndex);
                }
                List queryBoundables = lasIndex.queryBoundables(env);
                for( Object object : queryBoundables ) {
                    if (object instanceof ItemBoundable) {
                        ItemBoundable itemBoundable = (ItemBoundable) object;
                        double[] item = (double[]) itemBoundable.getItem();
                        if (item.length > 0) {
                            Envelope bounds = (Envelope) itemBoundable.getBounds();
                            Polygon envelopePolygon = LasIndexer.envelopeToPolygon(bounds);
                            envelopePolygon.setUserData(new double[]{item[2], item[3]});
                            if (minMaxZ != null) {
                                min = Math.min(min, item[2]);
                                max = Math.max(max, item[2]);
                            }
                            if (doOnlyEnvelope) {
                                envelopeListForTile.add(envelopePolygon);
                            } else {
                                if (preparedGeometry.intersects(envelopePolygon)) {
                                    envelopeListForTile.add(envelopePolygon);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (minMaxZ != null) {
            minMaxZ[0] = min;
            minMaxZ[1] = max;
        }
        return envelopeListForTile;
    }

    /**
     * Retrieve all the envelope features that intersect the geometry.
     *
     * <p>an elev attribute is added with the max elev contained in the envelope.
     *
     * @param checkGeom the {@link com.vividsolutions.jts.geom.Geometry} to use to check.
     * @param doOnlyEnvelope check for the geom envelope instead of a intersection with it.
     * @param minMaxZI an array to be filled with the [minz,maxz, minintensity, maxintensity] to be used as style.
     * @param doPoints if <code>true</code>, create points instead of polygons.
     * @return the features of the envelopes contained in the supplied geometry.
     * @throws Exception
     */
    public synchronized SimpleFeatureCollection getEnvelopeFeaturesInGeometry( Geometry checkGeom, boolean doOnlyEnvelope,
            double[] minMaxZI, boolean doPoints ) throws Exception {
        List<Geometry> envelopesInGeometry = getEnvelopesInGeometry(checkGeom, doOnlyEnvelope, null);

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("overview");
        b.setCRS(crs);
        if (!doPoints) {
            b.add("the_geom", Polygon.class);
        } else {
            b.add("the_geom", Point.class);
        }
        b.add("elev", Double.class);
        b.add("intensity", Double.class);

        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double minI = Double.POSITIVE_INFINITY;
        double maxI = Double.NEGATIVE_INFINITY;

        DefaultFeatureCollection newFeatures = new DefaultFeatureCollection();
        for( int i = 0; i < envelopesInGeometry.size(); i++ ) {
            Geometry geom = envelopesInGeometry.get(i);
            if (doPoints) {
                Envelope envelope = geom.getEnvelopeInternal();
                Coordinate centre = envelope.centre();
                geom = gf.createPoint(centre);
            }

            double elev = -9999.0;
            double intens = -9999.0;
            Object userData = geom.getUserData();
            if (userData instanceof double[]) {
                double[] data = (double[]) userData;
                elev = data[0];
                intens = data[1];
            }

            if (minMaxZI != null) {
                minZ = Math.min(minZ, elev);
                maxZ = Math.max(maxZ, elev);
                minI = Math.min(minI, intens);
                maxI = Math.max(maxI, intens);
            }

            Object[] objs = new Object[]{geom, elev, intens};
            builder.addAll(objs);
            SimpleFeature feature = builder.buildFeature(null);
            newFeatures.add(feature);
        }

        if (minMaxZI != null) {
            minMaxZI[0] = minZ;
            minMaxZI[1] = maxZ;
            minMaxZI[2] = minI;
            minMaxZI[3] = maxI;
        }
        return newFeatures;
    }

    /**
     * Check the point for constraints.
     *
     * @param lasDot the point to check.
     * @return <code>true</code> if the point is accepted.
     */
    private boolean doAccept( LasRecord lasDot ) {
        if (!hasConstraint) {
            return true;
        }
        boolean takeIt = true;
        if (intensityRange != null) {
            short intensity = lasDot.intensity;
            if (intensity >= intensityRange[0] && intensity <= intensityRange[1]) {
                takeIt = true;
            } else {
                return false;
            }
        }
        if (impulses != null) {
            int impulse = lasDot.returnNumber;
            takeIt = false;
            for( final double imp : impulses ) {
                if (impulse == (int) imp) {
                    takeIt = true;
                    break;
                }
            }
            if (!takeIt)
                return false;
        }
        if (impulsesNum != -1) {
            int numOfReturns = lasDot.numberOfReturns;
            if (numOfReturns != (int) impulsesNum) {
                return false;
            }
        }
        if (classes != null) {
            int classification = lasDot.classification;
            takeIt = false;
            for( final double classs : classes ) {
                if (classification == (int) classs) {
                    // since it is the last checked, if it is true, accept it
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Get the overall envelope of the las folder.
     *
     * <p>This reads the data from the index.</p>
     *
     * @return the {@link org.geotools.geometry.jts.ReferencedEnvelope} of the data.
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public synchronized ReferencedEnvelope getOverallEnvelope() throws Exception {
        if (referencedEnvelope2D == null) {
            checkOpen();
            Class<AbstractSTRtree> class1 = AbstractSTRtree.class;
            Field f1 = class1.getDeclaredField("itemBoundables");
            f1.setAccessible(true);
            ArrayList boundablesList = (ArrayList) f1.get(mainLasFolderIndex);
            Envelope env = null;
            for( Object item : boundablesList ) {
                if (item instanceof ItemBoundable) {
                    ItemBoundable boundable = (ItemBoundable) item;
                    Envelope envelope = (Envelope) boundable.getBounds();
                    ReferencedEnvelope tmp = new ReferencedEnvelope(envelope, crs);
                    referencedEnvelope2DList.add(tmp);
                    String name = (String) boundable.getItem();
                    fileNamesList.add(name);

                    if (env == null) {
                        env = envelope;
                    } else {
                        env.expandToInclude(envelope.getMinX(), envelope.getMinY());
                        env.expandToInclude(envelope.getMaxX(), envelope.getMaxY());
                    }
                }
            }
            referencedEnvelope2D = new ReferencedEnvelope(env, crs);
        }
        return referencedEnvelope2D;
    }

    /**
     * Getter for the list of envelopes of all las files.
     *
     * @return the list of {@link org.geotools.geometry.jts.ReferencedEnvelope}s.
     * @throws Exception
     */
    public List<ReferencedEnvelope> getEnvelopeList() throws Exception {
        getOverallEnvelope();
        return referencedEnvelope2DList;
    }

    /**
     * Get the overall envelope 3d of the las folder.
     *
     * <p>Warning: this needs to open all involved readers.
     *
     * @return the {@link org.geotools.geometry.jts.ReferencedEnvelope3D} of the data.
     * @throws Exception
     */
    public synchronized ReferencedEnvelope3D getEnvelope3D() throws Exception {
        if (referencedEnvelope3D == null) {
            checkReadersMap();

            for( String key : fileName4LasReaderMapSupport ) {
                Pair pair = fileName2LasReaderMap.get(key);
                ReferencedEnvelope3D envelope = pair.reader.getHeader().getDataEnvelope();
                if (referencedEnvelope3D == null) {
                    referencedEnvelope3D = envelope;
                } else {
                    referencedEnvelope3D.expandToInclude(envelope);
                }
            }
        }
        return referencedEnvelope3D;
    }

    /**
     * Creates a polygon {@link org.geotools.data.simple.SimpleFeatureCollection} for all las files.
     *
     * @return the features of teh las file bounds.
     * @throws Exception
     */
    public synchronized SimpleFeatureCollection getOverviewFeatures() throws Exception {
        if (overviewFeatures == null) {
            List<ReferencedEnvelope> envelopeList = getEnvelopeList();

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("overview");
            b.setCRS(crs);
            b.add("the_geom", Polygon.class);
            b.add("name", String.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

            overviewFeatures = new DefaultFeatureCollection();

            for( int i = 0; i < envelopeList.size(); i++ ) {
                String name = fileNamesList.get(i);
                ReferencedEnvelope envelope = envelopeList.get(i);
                Polygon polygon = LasIndexReader.envelopeToPolygon(envelope);
                Object[] objs = new Object[]{polygon, name};
                builder.addAll(objs);
                SimpleFeature feature = builder.buildFeature(null);
                ((DefaultFeatureCollection) overviewFeatures).add(feature);
            }
        }
        return overviewFeatures;
    }

    private void checkReadersMap() throws Exception {
        checkOpen();
        if (fileName2LasReaderMap.size() == 0) {
            @SuppressWarnings("rawtypes")
            List filesList = mainLasFolderIndex.itemsTree();
            for( Object fileName : filesList ) {
                if (fileName instanceof String) {
                    String name = (String) fileName;

                    Pair pair = fileName2LasReaderMap.get(name);
                    if (pair == null) {
                        File lasFile = new File(lasFolder, name);
                        File lasIndexFile = FileUtilities.substituteExtention(lasFile, "lasfix");

                        if (lasIndexFile.exists()) {
                            ALasReader reader = ALasReader.getReader(lasFile, crs);
                            reader.open();
                            reader.getHeader();
                            STRtreeJGT lasIndex = LasIndexReader.readIndex(lasIndexFile.getAbsolutePath());
                            pair = new Pair();
                            pair.reader = reader;
                            pair.strTree = lasIndex;
                            fileName2LasReaderMap.put(name, pair);
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
    }

    private void checkOpen() throws Exception {
        if (mainLasFolderIndex == null) {
            open();
        }
    }

    public void close() throws Exception {
        for( String key : fileName4LasReaderMapSupport ) {
            Pair pair = fileName2LasReaderMap.get(key);
            if (pair != null)
                pair.close();
        }
        fileName4LasReaderMapSupport.clear();
        fileName2LasReaderMap.clear();
        fileName2LasReaderMap = null;
    }

    private class Pair {
        ALasReader reader;
        STRtreeJGT strTree;
        public void close() {
            if (reader != null)
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            reader = null;
            strTree = null;
        }
    }

    // ///////////////////////////////////////////////
    // UTILITY METHODS
    // ///////////////////////////////////////////////

    /**
     * Extracts the points contained inside a vertical range from the supplied list of points.
     *
     * @param pointsList the list os {@link org.jgrasstools.gears.io.las.core.LasRecord points}.
     * @param min the min value of the range.
     * @param max the max value of the range.
     * @return the points contained in the range.
     */
    public static List<LasRecord> getPointsInVerticalRange( List<LasRecord> pointsList, double min, double max ) {
        ArrayList<LasRecord> pointsListInVertical = new ArrayList<LasRecord>();
        for( LasRecord lasRecord : pointsList ) {
            if (NumericsUtilities.isBetween(lasRecord.z, min, max)) {
                pointsListInVertical.add(lasRecord);
            }
        }
        return pointsListInVertical;
    }

    /**
     * Extracts the points contained inside a height from ground range from the supplied list of points.
     *
     * <p>No check is done on the existence of the ground height value.
     *
     * @param pointsList the list os {@link org.jgrasstools.gears.io.las.core.LasRecord points}.
     * @param min the min value of the range.
     * @param max the max value of the range.
     * @return the points contained in the range.
     */
    public static List<LasRecord> getPointsInHeightRange( List<LasRecord> pointsList, double min, double max ) {
        ArrayList<LasRecord> pointsListInVertical = new ArrayList<LasRecord>();
        for( LasRecord lasRecord : pointsList ) {
            if (NumericsUtilities.isBetween(lasRecord.groundElevation, min, max)) {
                pointsListInVertical.add(lasRecord);
            }
        }
        return pointsListInVertical;
    }

    public static void main( String[] args ) throws Exception {
        // LasDataManagerNew l = new LasDataManagerNew(new
        // File("/media/OCEANDTM/testindex/index.lasfolder"), null, 0, null);
        // l.open();
        // SimpleFeatureCollection readVector =
        // OmsVectorReader.readVector("/media/OCEANDTM/testindex/testbounds.shp");
        // List<Geometry> geoms = FeatureUtilities.featureCollectionToGeometriesList(readVector,
        // true, null);
        // SimpleFeatureCollection fc = l.getEnvelopeFeaturesInGeometry(geoms.get(0), false, null,
        // false);
        // OmsVectorWriter.writeVector("/media/OCEANDTM/testindex/testenvelopes.shp", fc);
        LasDataManager l = new LasDataManager(new File("/media/OCEANDTM/testindex/index.lasfolder"), null, 0, null);
        l.open();
        SimpleFeatureCollection readVector = OmsVectorReader.readVector("/media/OCEANDTM/testindex/testbounds.shp");
        CoordinateReferenceSystem crs = readVector.getSchema().getCoordinateReferenceSystem();
        List<Geometry> geoms = FeatureUtilities.featureCollectionToGeometriesList(readVector, true, null);
        List<LasRecord> p = l.getPointsInGeometry(geoms.get(0), false);
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        for( LasRecord lasRecord : p ) {
            fc.add(LasUtils.tofeature(lasRecord, crs));
        }
        OmsVectorWriter.writeVector("/media/OCEANDTM/testindex/testpoints2.shp", fc);
    }
}
