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
package org.hortonmachine.nww.layers.defaults.vector;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.nww.layers.defaults.NwwEditableVectorLayer;
import org.hortonmachine.nww.shapes.FeatureExtrudedPolygon;
import org.hortonmachine.nww.shapes.FeaturePolygon;
import org.hortonmachine.nww.shapes.FeatureStoreInfo;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.style.SimpleStyle;
import org.opengis.feature.simple.SimpleFeature;

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
public class FeatureCollectionPolygonLayer extends RenderableLayer implements NwwEditableVectorLayer {

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
    private SimpleFeatureCollection featureCollectionLL;

    private int mElevationMode = WorldWind.CLAMP_TO_GROUND;
    private String title;
    private FeatureStoreInfo featureStoreInfo;
    private SimpleFeatureStore featureStore;

    /**
     * Build the layer.
     * 
     * @param title
     *            layer name.
     * @param featureCollectionLL
     *            the featurecollection in latlong.
     * @param featureStore
     *            the feature store. If not null, then the feature attributes
     *            will be editable.
     */
    public FeatureCollectionPolygonLayer( String title, SimpleFeatureCollection featureCollectionLL,
            SimpleFeatureStore featureStore, HashMap<String, String[]> field2ValuesMap ) {
        this.title = title;
        this.featureCollectionLL = featureCollectionLL;
        this.featureStore = featureStore;
        this.featureStoreInfo = new FeatureStoreInfo(featureStore, field2ValuesMap);

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
            removeAllRenderables();
            SimpleFeatureIterator featureIterator = featureCollectionLL.features();
            while( featureIterator.hasNext() ) {
                SimpleFeature polygonAreaFeature = featureIterator.next();
                if (mApplyExtrusion && (mHeightFieldName != null || mHasConstantHeight)) {
                    addExtrudedPolygon(polygonAreaFeature);
                } else {
                    addPolygon(polygonAreaFeature);
                }
            }
            featureIterator.close();
        }

    }

    private void addExtrudedPolygon( SimpleFeature polygonAreaFeature ) {
        try {
            Geometry geometry = (Geometry) polygonAreaFeature.getDefaultGeometry();
            if (geometry == null) {
                return;
            }
            Coordinate[] coordinates = geometry.getCoordinates();
            int numVertices = coordinates.length;
            if (numVertices < 4)
                return;

            boolean hasZ = !Double.isNaN(geometry.getCoordinate().z);

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
                    double tmpH = ((Number) polygonAreaFeature.getAttribute(mHeightFieldName)).doubleValue();
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

                    FeatureExtrudedPolygon extrudedPolygon = new FeatureExtrudedPolygon(featureStoreInfo);
                    extrudedPolygon.setFeature(polygonAreaFeature);

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

    private void addPolygon( SimpleFeature polygonAreaFeature ) {
        Geometry geometry = (Geometry) polygonAreaFeature.getDefaultGeometry();
        if (geometry == null) {
            return;
        }
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
                double tmpH = ((Number) polygonAreaFeature.getAttribute(mHeightFieldName)).doubleValue();
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

                FeaturePolygon polygon = new FeaturePolygon(featureStoreInfo);
                polygon.setFeature(polygonAreaFeature);

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

    private SimpleFeatureCollection getfeatureCollection() throws Exception {
        if (featureStore != null) {
            return NwwUtilities.readAndReproject(featureStore);
        }
        return featureCollectionLL;
    }

    @Override
    public Coordinate getCenter() {
        try {
            ReferencedEnvelope bounds = getfeatureCollection().getBounds();
            return bounds.centre();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Coordinate(0, 0);
    }

    @Override
    public String toString() {
        return title != null ? title : "Polygons";
    }

    @Override
    public GEOMTYPE getType() {
        return GEOMTYPE.POLYGON;
    }

    @Override
    public boolean isEditable() {
        return featureStoreInfo.getFeatureStore() != null;
    }

    @Override
    public FeatureStoreInfo getStoreInfo() {
        return featureStoreInfo;
    }

    @Override
    public void add( SimpleFeature feature ) {
        addPolygon(feature);
    }

    @Override
    public void reload() {
        loadData();
    }
}
