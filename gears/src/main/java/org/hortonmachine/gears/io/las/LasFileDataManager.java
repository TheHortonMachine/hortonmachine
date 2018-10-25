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
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.index.LasIndexer;
import org.hortonmachine.gears.io.las.index.OmsLasIndexReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;

/**
 * A class that manages single las files.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
class LasFileDataManager extends ALasDataManager {
    private File lasFile;
    private GridCoverage2D inDem;
    private double elevThreshold;

    private SimpleFeatureCollection overviewFeatures;
    private ReferencedEnvelope referencedEnvelope2D;
    private List<ReferencedEnvelope> referencedEnvelope2DList = new ArrayList<ReferencedEnvelope>();
    private List<String> fileNamesList = new ArrayList<String>();
    private ReferencedEnvelope3D referencedEnvelope3D;
    private ALasReader lasReader;
    private ILasHeader lasHeader;
    private boolean isOpen;
    private STRtree pointsTree;

    /**
     * Constructor.
     * 
     * @param lasFile the las folder index file.
     * @param inDem a dem to normalize the elevation. If <code>null</code>, the original las elevation is used.
     * @param elevThreshold a threshold to use for the elevation normalization.
     * @param inCrs the data {@link org.opengis.referencing.crs.CoordinateReferenceSystem}. if null, the one of the dem is read, if available.
     */
    LasFileDataManager( File lasFile, GridCoverage2D inDem, double elevThreshold, CoordinateReferenceSystem inCrs ) {
        this.lasFile = lasFile;
        this.inDem = inDem;
        this.elevThreshold = elevThreshold;

        fileNamesList.add(lasFile.getName());

        try {
            // prj file rules if available
            inCrs = CrsUtilities.readProjectionFile(lasFile.getAbsolutePath(), "las");
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
    }

    @Override
    public File getFile() {
        return lasFile;
    }

    /**
     * Open the main folder file and read the main index.
     *
     * @throws Exception
     */
    @Override
    public void open() throws Exception {
        lasReader = ALasReader.getReader(lasFile, crs);
        lasReader.open();
        lasHeader = lasReader.getHeader();
        isOpen = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized List<LasRecord> getPointsInGeometry( Geometry checkGeom, boolean doOnlyEnvelope ) throws Exception {
        checkOpen();

        ArrayList<LasRecord> pointsListForTile = new ArrayList<LasRecord>();
        Envelope checkEnvelope = checkGeom.getEnvelopeInternal();
        if (pointsTree != null) {
            List<LasRecord> pointsList = pointsTree.query(checkEnvelope);
            PreparedGeometry preparedGeometry = null;
            if (!doOnlyEnvelope) {
                preparedGeometry = PreparedGeometryFactory.prepare(checkGeom);
            }
            for( LasRecord lasDot : pointsList ) {
                Coordinate c = new Coordinate(lasDot.x, lasDot.y);
                if (!checkEnvelope.contains(c)) {
                    continue;
                }
                if (!doOnlyEnvelope && !preparedGeometry.contains(gf.createPoint(c))) {
                    continue;
                }
                pointsListForTile.add(lasDot);
            }
        } else {
            pointsTree = new STRtree();
            ReferencedEnvelope overallEnvelope = getOverallEnvelope();
            if (doOnlyEnvelope && checkEnvelope.covers(overallEnvelope)) {
                // read it straight
                while( lasReader.hasNextPoint() ) {
                    LasRecord lasDot = lasReader.getNextPoint();
                    if (!doAccept(lasDot)) {
                        continue;
                    }
                    pointsTree.insert(new Envelope(new Coordinate(lasDot.x, lasDot.y)), lasDot);
                    pointsListForTile.add(lasDot);
                }
            } else {

                Envelope env = checkGeom.getEnvelopeInternal();
                PreparedGeometry preparedGeometry = null;
                if (!doOnlyEnvelope) {
                    preparedGeometry = PreparedGeometryFactory.prepare(checkGeom);
                }

                while( lasReader.hasNextPoint() ) {
                    LasRecord lasDot = lasReader.getNextPoint();
                    if (!doAccept(lasDot)) {
                        continue;
                    }
                    Coordinate c = new Coordinate(lasDot.x, lasDot.y);
                    pointsTree.insert(new Envelope(c), lasDot);
                    if (!env.contains(c)) {
                        continue;
                    }

                    if (inDem != null) {
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
                            lasDot.groundElevation = height;
                            pointsListForTile.add(lasDot);
                        }
                    } else {
                        if (!doOnlyEnvelope && !preparedGeometry.contains(gf.createPoint(c))) {
                            continue;
                        }
                        pointsListForTile.add(lasDot);
                    }
                }

            }
            close();
        }
        return pointsListForTile;
    }

    @Override
    public synchronized List<Geometry> getEnvelopesInGeometry( Geometry checkGeom, boolean doOnlyEnvelope, double[] minMaxZ )
            throws Exception {
        checkOpen();

        ReferencedEnvelope3D dataEnvelope = lasHeader.getDataEnvelope();
        Polygon envelopePolygon = LasIndexer.envelopeToPolygon(dataEnvelope);
        ArrayList<Geometry> envelopeList = new ArrayList<Geometry>();
        envelopeList.add(envelopePolygon);
        return envelopeList;
    }

    @Override
    public synchronized ReferencedEnvelope getOverallEnvelope() throws Exception {
        if (referencedEnvelope2D == null) {
            checkOpen();
            ReferencedEnvelope3D dataEnvelope = lasHeader.getDataEnvelope();
            referencedEnvelope2D = new ReferencedEnvelope(dataEnvelope, crs);
            referencedEnvelope2DList.add(referencedEnvelope2D);
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
            checkOpen();
            ReferencedEnvelope3D dataEnvelope = lasHeader.getDataEnvelope();
            referencedEnvelope3D = new ReferencedEnvelope3D(dataEnvelope, crs);
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

    private void checkOpen() throws Exception {
        if (!isOpen) {
            open();
        }
    }

    @Override
    public void close() throws Exception {
        isOpen = false;
        if (lasReader != null)
            lasReader.close();
    }

}
