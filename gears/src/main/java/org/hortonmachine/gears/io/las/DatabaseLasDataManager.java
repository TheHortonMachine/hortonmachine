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
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.databases.LasCell;
import org.hortonmachine.gears.io.las.databases.LasCellsTable;
import org.hortonmachine.gears.io.las.databases.LasSource;
import org.hortonmachine.gears.io.las.databases.LasSourcesTable;
import org.hortonmachine.gears.io.las.index.LasIndexer;
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

/**
 * A class that manages las databases.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
class DatabaseLasDataManager extends ALasDataManager {
    private File databaseFile;
    private GridCoverage2D inDem;
    private double elevThreshold;

    private SimpleFeatureCollection overviewFeatures;
    private ReferencedEnvelope referencedEnvelope2D;
    private List<ReferencedEnvelope> referencedEnvelope2DList = new ArrayList<ReferencedEnvelope>();
    private ReferencedEnvelope3D referencedEnvelope3D;
    private boolean isOpen;

    private ASpatialDb spatialDb;

    /**
     * Constructor.
     * 
     * @param databaseFile the indexed database file.
     * @param inDem a dem to normalize the elevation. If <code>null</code>, the original las elevation is used.
     * @param elevThreshold a threshold to use for the elevation normalization.
     * @param inCrs the data {@link org.opengis.referencing.crs.CoordinateReferenceSystem}. if null, the one of the dem is read, if available.
     */
    DatabaseLasDataManager( File databaseFile, GridCoverage2D inDem, double elevThreshold, CoordinateReferenceSystem inCrs ) {
        this.databaseFile = databaseFile;
        this.inDem = inDem;
        this.elevThreshold = elevThreshold;
    }

    @Override
    public File getFile() {
        return databaseFile;
    }

    /**
     * Open the main folder file and read the main index.
     *
     * @throws Exception
     */
    @Override
    public void open() throws Exception {
        EDb edb = EDb.fromFileDesktop(databaseFile);
        spatialDb = edb.getSpatialDb();
        spatialDb.open(databaseFile.getAbsolutePath());
        GeometryColumn geometryColumnsForTable = spatialDb.getGeometryColumnsForTable(LasCellsTable.TABLENAME);
        crs = CrsUtilities.getCrsFromSrid(geometryColumnsForTable.srid);
        isOpen = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized List<LasRecord> getPointsInGeometry( Geometry checkGeom, boolean doOnlyEnvelope ) throws Exception {
        checkOpen();

        ArrayList<LasRecord> pointsListForTile = new ArrayList<LasRecord>();
        Envelope checkEnvelope = checkGeom.getEnvelopeInternal();
        PreparedGeometry preparedGeometry = null;
        if (!doOnlyEnvelope) {
            preparedGeometry = PreparedGeometryFactory.prepare(checkGeom);
        }
        final PreparedGeometry _preparedGeometry = preparedGeometry;
        List<LasCell> lasCells = LasCellsTable.getLasCells(spatialDb, checkGeom, true, true, false, false, false);
        lasCells.stream().forEach(cell -> {
            double[][] positions = LasCellsTable.getCellPositions(cell);
            short[][] cellIntensityClass = LasCellsTable.getCellIntensityClass(cell);

            for( int i = 0; i < positions.length; i++ ) {
                LasRecord dot = new LasRecord();
                dot.x = positions[i][0];
                dot.y = positions[i][1];
                dot.z = positions[i][2];

                Coordinate c = new Coordinate(dot.x, dot.y);
                if (doOnlyEnvelope && !checkEnvelope.contains(c)) {
                    continue;
                } else if (!doOnlyEnvelope && !_preparedGeometry.contains(gf.createPoint(c))) {
                    continue;
                }

                if (inDem != null) {
                    double value = CoverageUtilities.getValue(inDem, c);
                    if (HMConstants.isNovalue(value)) {
                        continue;
                    }
                    double height = dot.z - value;
                    if (height > elevThreshold) {
                        dot.groundElevation = height;
                    }
                }

                dot.intensity = cellIntensityClass[i][0];
                dot.classification = (byte) cellIntensityClass[i][1];

                pointsListForTile.add(dot);
            }
        });
        return pointsListForTile;
    }

    @Override
    public synchronized List<Geometry> getEnvelopesInGeometry( Geometry checkGeom, boolean doOnlyEnvelope, double[] minMaxZ )
            throws Exception {
        checkOpen();

        List<LasCell> lasCells = LasCellsTable.getLasCells(spatialDb, checkGeom, true, false, false, false, false);

        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        Envelope env = new Envelope();
        for( LasCell lasCell : lasCells ) {
            minZ = Math.min(minZ, lasCell.minElev);
            maxZ = Math.max(maxZ, lasCell.maxElev);
            env.expandToInclude(lasCell.polygon.getEnvelopeInternal());
        }

        ReferencedEnvelope3D dataEnvelope = new ReferencedEnvelope3D(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(),
                minZ, maxZ, crs);
        Polygon envelopePolygon = LasIndexer.envelopeToPolygon(dataEnvelope);
        ArrayList<Geometry> envelopeList = new ArrayList<Geometry>();
        envelopeList.add(envelopePolygon);
        return envelopeList;
    }

    @Override
    public synchronized ReferencedEnvelope getOverallEnvelope() throws Exception {
        if (referencedEnvelope2D == null) {
            checkOpen();

            List<LasSource> lasSources = LasSourcesTable.getLasSources(spatialDb);
            ReferencedEnvelope total = new ReferencedEnvelope(crs);
            for( LasSource lasSource : lasSources ) {
                Envelope envelopeInternal = lasSource.polygon.getEnvelopeInternal();
                total.expandToInclude(envelopeInternal);
            }
            referencedEnvelope2D = total;
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

            List<LasSource> lasSources = LasSourcesTable.getLasSources(spatialDb);
            ReferencedEnvelope total = new ReferencedEnvelope(crs);
            double minZ = Double.POSITIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for( LasSource lasSource : lasSources ) {
                Envelope envelopeInternal = lasSource.polygon.getEnvelopeInternal();
                total.expandToInclude(envelopeInternal);
                minZ = Math.min(minZ, lasSource.minElev);
                maxZ = Math.max(maxZ, lasSource.maxElev);
            }
            referencedEnvelope3D = new ReferencedEnvelope3D(total.getMinX(), total.getMaxX(), total.getMinY(), total.getMaxY(),
                    minZ, maxZ, crs);
        }
        return referencedEnvelope3D;
    }

    @Override
    public synchronized SimpleFeatureCollection getOverviewFeatures() throws Exception {
        if (overviewFeatures == null) {
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("overview");
            b.setCRS(crs);
            b.add("the_geom", Polygon.class);
            b.add("name", String.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

            overviewFeatures = new DefaultFeatureCollection();
            List<LasSource> lasSources = LasSourcesTable.getLasSources(spatialDb);
            for( int i = 0; i < lasSources.size(); i++ ) {
                LasSource lasSource = lasSources.get(i);
                String name = lasSource.name;
                Object[] objs = new Object[]{lasSource.polygon, name};
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
        if (spatialDb != null)
            spatialDb.close();
    }

}
