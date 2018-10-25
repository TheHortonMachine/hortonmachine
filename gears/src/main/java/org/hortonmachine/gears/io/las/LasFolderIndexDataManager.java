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
package org.hortonmachine.gears.io.las;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.util.WeakValueHashMap;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.index.LasIndexer;
import org.hortonmachine.gears.io.las.index.OmsLasIndexReader;
import org.hortonmachine.gears.io.las.index.strtree.STRtreeJGT;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.AbstractSTRtree;
import org.locationtech.jts.index.strtree.ItemBoundable;

/**
 * A class that manages las folder data.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
class LasFolderIndexDataManager extends ALasDataManager implements AutoCloseable {
    private WeakValueHashMap<String, Pair> fileName2LasReaderMap;
    private WeakValueHashMap<String, STRtreeJGT> fileName2IndexMap;
    private List<String> fileName4LasReaderMapSupport;
    // private int READERCACHE = 5;
    private File lasFolderIndexFile;
    private File lasFolder;
    private STRtreeJGT mainLasFolderIndex;
    private GridCoverage2D inDem;
    private double elevThreshold;

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
    LasFolderIndexDataManager( File lasFolderIndexFile, GridCoverage2D inDem, double elevThreshold,
            CoordinateReferenceSystem inCrs ) {
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

    @Override
    public File getFile() {
        return lasFolderIndexFile;
    }

    /**
     * Open the main folder file and read the main index.
     *
     * @throws Exception
     */
    @Override
    public void open() throws Exception {
        mainLasFolderIndex = OmsLasIndexReader.readIndex(lasFolderIndexFile.getAbsolutePath());
    }

    /**
     * Get points inside a given geometry boundary.
     *
     * @param checkGeom the {@link org.locationtech.jts.geom.Geometry} to use to check.
     * @param doOnlyEnvelope check for the geom envelope instead of a intersection with it.
     * @return the list of points contained in the supplied geometry.
     * @throws Exception
     */
    @Override
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
                        pair = getIndexPair(lasFile);
                        if (pair != null) {
                            fileName2LasReaderMap.put(name, pair);
                            fileName4LasReaderMapSupport.add(name);
                        }
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
                                    if (HMConstants.isNovalue(value)) {
                                        continue;
                                    }
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
     * @param checkGeom the {@link org.locationtech.jts.geom.Geometry} to use to check.
     * @param doOnlyEnvelope check for the geom envelope instead of a intersection with it.
     * @param minMaxZ an array to be filled with the min and max z to be used as style.
     * @return the list of envelopes contained in the supplied geometry.
     * @throws Exception
     */
    @Override
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
        List< ? > filesList = mainLasFolderIndex.query(env);
        for( Object fileName : filesList ) {
            if (fileName instanceof String) {
                String name = (String) fileName;
                File lasFile = new File(lasFolder, name);
                File lasIndexFile = FileUtilities.substituteExtention(lasFile, "lasfix");

                String absolutePath = lasIndexFile.getAbsolutePath();
                STRtreeJGT lasIndex = fileName2IndexMap.get(absolutePath);
                if (lasIndex == null) {
                    lasIndex = OmsLasIndexReader.readIndex(absolutePath);
                    fileName2IndexMap.put(absolutePath, lasIndex);
                }
                List< ? > queryBoundables = lasIndex.queryBoundables(env);
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

    @Override
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

    @Override
    public List<ReferencedEnvelope> getEnvelopeList() throws Exception {
        getOverallEnvelope();
        return referencedEnvelope2DList;
    }

    @Override
    public synchronized ReferencedEnvelope3D getEnvelope3D() throws Exception {
        if (referencedEnvelope3D == null) {
            checkReadersMap();

            for( String key : fileName4LasReaderMapSupport ) {
                Pair pair = fileName2LasReaderMap.get(key);
                if (pair == null) {
                    File lasFile = new File(lasFolder, key);
                    pair = getIndexPair(lasFile);
                    if (pair == null) {
                        System.err.println("Null reader pair: " + lasFile);
                        continue;
                    }
                }
                ILasHeader header = pair.reader.getHeader();
                ReferencedEnvelope3D envelope = header.getDataEnvelope();
                if (referencedEnvelope3D == null) {
                    referencedEnvelope3D = envelope;
                } else {
                    referencedEnvelope3D.expandToInclude(envelope.getMinX(), envelope.getMinY(), envelope.getMinZ());
                    referencedEnvelope3D.expandToInclude(envelope.getMaxX(), envelope.getMaxY(), envelope.getMaxZ());
                }
            }
        }
        return referencedEnvelope3D;
    }

    @Override
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
                Polygon polygon = OmsLasIndexReader.envelopeToPolygon(envelope);
                Object[] objs = new Object[]{polygon, name};
                builder.addAll(objs);
                SimpleFeature feature = builder.buildFeature(null);
                ((DefaultFeatureCollection) overviewFeatures).add(feature);
            }
        }
        return overviewFeatures;
    }

    @SuppressWarnings("rawtypes")
    private void checkReadersMap() throws Exception {
        checkOpen();
        if (fileName2LasReaderMap.size() == 0) {
            List filesList = mainLasFolderIndex.itemsTree();
            for( Object fileName : filesList ) {
                if (fileName instanceof String) {
                    String name = (String) fileName;
                    getReader(name);
                } else if (fileName instanceof List) {
                    List filesList2 = (List) fileName;
                    for( Object fileName2 : filesList2 ) {
                        if (fileName2 instanceof String) {
                            String name2 = (String) fileName2;
                            getReader(name2);
                        }
                    }
                } else {
                    throw new RuntimeException();
                }
            }
        }
    }

    private void getReader( String name ) throws Exception {
        Pair pair = fileName2LasReaderMap.get(name);
        if (pair == null) {
            File lasFile = new File(lasFolder, name);
            pair = getIndexPair(lasFile);
            if (pair != null) {
                fileName2LasReaderMap.put(name, pair);
                fileName4LasReaderMapSupport.add(name);
            }
            // System.out.println("Added: " + lasIndexFile);
            // } else {

        }
    }

    private Pair getIndexPair( File lasFile ) throws Exception {
        File lasIndexFile = FileUtilities.substituteExtention(lasFile, "lasfix");
        if (lasIndexFile.exists()) {
            ALasReader reader = ALasReader.getReader(lasFile, crs);
            reader.open();
            reader.getHeader();
            STRtreeJGT lasIndex = OmsLasIndexReader.readIndex(lasIndexFile.getAbsolutePath());
            Pair pair = new Pair();
            pair.reader = reader;
            pair.strTree = lasIndex;
            return pair;
        } else {
            System.err.println("Doesn't exist: " + lasIndexFile);
        }
        return null;
    }

    private void checkOpen() throws Exception {
        if (mainLasFolderIndex == null) {
            open();
        }
    }

    @Override
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

}
