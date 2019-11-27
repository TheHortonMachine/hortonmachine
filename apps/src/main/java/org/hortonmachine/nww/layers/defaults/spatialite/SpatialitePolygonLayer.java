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
package org.hortonmachine.nww.layers.defaults.spatialite;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.spatialite.hm.SpatialiteDb;
import org.hortonmachine.gears.spatialite.GTSpatialiteThreadsafeDb;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.nww.layers.defaults.NwwVectorLayer;
import org.hortonmachine.nww.shapes.InfoExtrudedPolygon;
import org.hortonmachine.nww.shapes.InfoPolygon;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.style.SimpleStyle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;

/**
 * A simple polygons layer.
 * 
 * @author Andrea Antonello andrea.antonello@gmail.com
 */
public class SpatialitePolygonLayer extends RenderableLayer implements NwwVectorLayer {

    private String mHeightFieldName;
    private double mVerticalExageration = 1.0;
    private double mConstantHeight = 1.0;
    private boolean mHasConstantHeight = false;
    private boolean mApplyExtrusion = false;

    private BasicShapeAttributes mNormalShapeAttributes;
    private BasicShapeAttributes mSideShapeAttributes;

    private Material mFillMaterial = Material.BLUE;
    private Material mSideFillMaterial = new Material(NwwUtilities.darkenColor(Color.BLUE));
    private Material mStrokeMaterial = Material.RED;
    private double mFillOpacity = 0.8;
    private double mStrokeWidth = 2;

    private int mElevationMode = WorldWind.CLAMP_TO_GROUND;
    private String tableName;
    private SpatialiteDb db;
    private ReferencedEnvelope tableBounds;
    private int featureLimit;

    public SpatialitePolygonLayer( ASpatialDb db, String tableName, int featureLimit ) {
        this.db = (SpatialiteDb) db;
        this.tableName = tableName;
        this.featureLimit = featureLimit;

        try {
            if (db instanceof GTSpatialiteThreadsafeDb) {
                GTSpatialiteThreadsafeDb gtDb = (GTSpatialiteThreadsafeDb) db;
                tableBounds = gtDb.getTableBounds(tableName);
            } else {
                tableBounds = CrsUtilities.WORLD;
            }
        } catch (Exception e) {
            Logger.INSTANCE.insertError("", "ERROR", e);
            tableBounds = CrsUtilities.WORLD;
        }

        setStyle(null);
        loadData();
    }

    public void setStyle( SimpleStyle style ) {
        if (style != null) {
            mFillMaterial = new Material(style.fillColor);
            mSideFillMaterial = new Material(NwwUtilities.darkenColor(style.fillColor));
            mFillOpacity = style.fillOpacity;
            mStrokeMaterial = new Material(style.strokeColor);
            mStrokeWidth = style.strokeWidth;
        }

        if (mNormalShapeAttributes == null)
            mNormalShapeAttributes = new BasicShapeAttributes();
        mNormalShapeAttributes.setInteriorMaterial(mFillMaterial);
        mNormalShapeAttributes.setInteriorOpacity(mFillOpacity);
        mNormalShapeAttributes.setOutlineMaterial(mStrokeMaterial);
        mNormalShapeAttributes.setOutlineWidth(mStrokeWidth);

        if (mSideShapeAttributes == null)
            mSideShapeAttributes = new BasicShapeAttributes();
        mSideShapeAttributes.setInteriorMaterial(mSideFillMaterial);
        mSideShapeAttributes.setInteriorOpacity(mFillOpacity);
    }

    @Override
    public SimpleStyle getStyle() {
        SimpleStyle simpleStyle = new SimpleStyle();
        simpleStyle.fillColor = mNormalShapeAttributes.getInteriorMaterial().getDiffuse();
        simpleStyle.fillOpacity = mNormalShapeAttributes.getInteriorOpacity();
        simpleStyle.strokeColor = mNormalShapeAttributes.getOutlineMaterial().getDiffuse();
        simpleStyle.strokeWidth = mNormalShapeAttributes.getOutlineWidth();
        return simpleStyle;
    }

    public void setExtrusionProperties( Double constantExtrusionHeight, String heightFieldName, Double verticalExageration,
            boolean withoutExtrusion ) {
        if (constantExtrusionHeight != null) {
            mHasConstantHeight = true;
            mConstantHeight = constantExtrusionHeight;
            mApplyExtrusion = !withoutExtrusion;
        }
        if (heightFieldName != null) {
            mHeightFieldName = heightFieldName;
            mVerticalExageration = verticalExageration;
            mApplyExtrusion = !withoutExtrusion;
        }
    }

    public void setElevationMode( int elevationMode ) {
        mElevationMode = elevationMode;
    }

    public void loadData() {
        Thread t = new WorkerThread();
        t.start();
    }

    public class WorkerThread extends Thread {

        public void run() {

            try {
                QueryResult tableRecords = db.getTableRecordsMapIn(tableName, null, featureLimit, NwwUtilities.GPS_CRS_SRID,
                        null);
                int count = tableRecords.data.size();
                List<String> names = tableRecords.names;
                for( int i = 0; i < count; i++ ) {
                    Object[] objects = tableRecords.data.get(i);
                    StringBuilder sb = new StringBuilder();
                    double height = -1;
                    for( int j = 1; j < objects.length; j++ ) {
                        String varName = names.get(j);
                        sb.append(varName).append(": ").append(objects[j]).append("\n");

                        if (mHeightFieldName != null && varName == mHeightFieldName && objects[j] instanceof Number) {
                            height = ((Number) objects[j]).doubleValue();
                        }
                    }
                    String info = sb.toString();
                    Geometry geometry = (Geometry) objects[tableRecords.geometryIndex];
                    if (geometry == null) {
                        continue;
                    }
                    if (mApplyExtrusion && (mHeightFieldName != null || mHasConstantHeight)) {
                        addExtrudedPolygon(geometry, info, height);
                    } else {
                        addPolygon(geometry, info, height);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void addExtrudedPolygon( Geometry geometry, String info, double height ) {
            try {
                Coordinate[] coordinates = geometry.getCoordinates();
                int numVertices = coordinates.length;
                if (numVertices < 4)
                    return;

                boolean hasZ = false;

                double h = 0.0;
                switch( mElevationMode ) {
                case WorldWind.RELATIVE_TO_GROUND:
                    hasZ = false;
                case WorldWind.ABSOLUTE:
                default:
                    if (mHasConstantHeight) {
                        h = mConstantHeight;
                    }
                    if (mHeightFieldName != null) {
                        double tmpH = height;
                        tmpH = tmpH * mVerticalExageration;
                        h += tmpH;
                    }
                    break;
                }

                int numGeometries = geometry.getNumGeometries();
                for( int i = 0; i < numGeometries; i++ ) {
                    Geometry geometryN = geometry.getGeometryN(i);
                    if (geometryN instanceof org.locationtech.jts.geom.Polygon) {
                        org.locationtech.jts.geom.Polygon poly = (org.locationtech.jts.geom.Polygon) geometryN;

                        InfoExtrudedPolygon extrudedPolygon = new InfoExtrudedPolygon();
                        extrudedPolygon.setInfo(info);

                        Coordinate[] extCoords = poly.getExteriorRing().getCoordinates();
                        int extSize = extCoords.length;
                        List<Position> verticesList = new ArrayList<>(extSize);
                        for( int n = 0; n < extSize; n++ ) {
                            Coordinate c = extCoords[n];
                            if (hasZ) {
                                double z = c.z;
                                verticesList.add(Position.fromDegrees(c.y, c.x, z + h));
                            } else {
                                verticesList.add(Position.fromDegrees(c.y, c.x, h));
                            }
                        }
                        verticesList.add(verticesList.get(0));
                        extrudedPolygon.setOuterBoundary(verticesList);

                        int numInteriorRings = poly.getNumInteriorRing();
                        for( int k = 0; k < numInteriorRings; k++ ) {
                            LineString interiorRing = poly.getInteriorRingN(k);
                            Coordinate[] intCoords = interiorRing.getCoordinates();
                            int internalNumVertices = intCoords.length;
                            List<Position> internalVerticesList = new ArrayList<>(internalNumVertices);
                            for( int j = 0; j < internalNumVertices; j++ ) {
                                Coordinate c = intCoords[j];
                                if (hasZ) {
                                    double z = c.z;
                                    internalVerticesList.add(Position.fromDegrees(c.y, c.x, z + h));
                                } else {
                                    internalVerticesList.add(Position.fromDegrees(c.y, c.x, h));
                                }
                            }
                            extrudedPolygon.addInnerBoundary(internalVerticesList);
                        }

                        extrudedPolygon.setAltitudeMode(mElevationMode);
                        extrudedPolygon.setAttributes(mNormalShapeAttributes);
                        extrudedPolygon.setSideAttributes(mSideShapeAttributes);

                        addRenderable(extrudedPolygon);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addPolygon( Geometry geometry, String info, double height ) {
            Coordinate[] coordinates = geometry.getCoordinates();
            int numVertices = coordinates.length;
            if (numVertices < 4)
                return;

            boolean hasZ = !Double.isNaN(geometry.getCoordinate().z);

            double h = 0.0;
            switch( mElevationMode ) {
            case WorldWind.CLAMP_TO_GROUND:
                hasZ = false;
                break;
            case WorldWind.RELATIVE_TO_GROUND:
                hasZ = false;
            case WorldWind.ABSOLUTE:
            default:
                if (mHasConstantHeight) {
                    h = mConstantHeight;
                }
                if (mHeightFieldName != null) {
                    double tmpH = height;
                    tmpH = tmpH * mVerticalExageration;
                    h += tmpH;
                }
                break;
            }
            int numGeometries = geometry.getNumGeometries();
            for( int i = 0; i < numGeometries; i++ ) {
                Geometry geometryN = geometry.getGeometryN(i);
                if (geometryN instanceof org.locationtech.jts.geom.Polygon) {
                    org.locationtech.jts.geom.Polygon poly = (org.locationtech.jts.geom.Polygon) geometryN;

                    InfoPolygon polygon = new InfoPolygon();
                    polygon.setInfo(info);

                    Coordinate[] extCoords = poly.getExteriorRing().getCoordinates();
                    int extSize = extCoords.length;
                    List<Position> verticesList = new ArrayList<>(extSize);
                    for( int n = 0; n < extSize; n++ ) {
                        Coordinate c = extCoords[n];
                        if (hasZ) {
                            double z = c.z;
                            verticesList.add(Position.fromDegrees(c.y, c.x, z + h));
                        } else {
                            verticesList.add(Position.fromDegrees(c.y, c.x, h));
                        }
                    }
                    verticesList.add(verticesList.get(0));
                    polygon.setOuterBoundary(verticesList);

                    int numInteriorRings = poly.getNumInteriorRing();
                    for( int k = 0; k < numInteriorRings; k++ ) {
                        LineString interiorRing = poly.getInteriorRingN(k);
                        Coordinate[] intCoords = interiorRing.getCoordinates();
                        int internalNumVertices = intCoords.length;
                        List<Position> internalVerticesList = new ArrayList<>(internalNumVertices);
                        for( int j = 0; j < internalNumVertices; j++ ) {
                            Coordinate c = intCoords[j];
                            if (hasZ) {
                                double z = c.z;
                                internalVerticesList.add(Position.fromDegrees(c.y, c.x, z + h));
                            } else {
                                internalVerticesList.add(Position.fromDegrees(c.y, c.x, h));
                            }
                        }
                        polygon.addInnerBoundary(internalVerticesList);
                    }

                    polygon.setAltitudeMode(mElevationMode);
                    polygon.setAttributes(mNormalShapeAttributes);

                    addRenderable(polygon);
                }
            }
        }
    }

    @Override
    public Coordinate getCenter() {
        return tableBounds.centre();
    }

    @Override
    public String toString() {
        return tableName != null ? tableName : "Polygons";
    }

    @Override
    public GEOMTYPE getType() {
        return GEOMTYPE.POLYGON;
    }
}
