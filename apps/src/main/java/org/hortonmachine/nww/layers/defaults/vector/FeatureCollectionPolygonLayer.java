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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.utils.style.FeatureStyler;
import org.hortonmachine.nww.layers.defaults.NwwEditableVectorLayer;
import org.hortonmachine.nww.shapes.FeatureExtrudedPolygon;
import org.hortonmachine.nww.shapes.FeaturePolygon;
import org.hortonmachine.nww.shapes.FeatureStoreInfo;
import org.hortonmachine.nww.shapes.FeatureSurfacePolygon;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.style.SimpleStyle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Renderable;

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

    /** The SLD style, <code>null</code> to draw all features with the simple style. */
    private volatile FeatureStyler styler;
    /** The attributes of the looks given by the SLD style, shared by the features. */
    private final Map<FeatureStyler.Symbol, BasicShapeAttributes[]> stylerAttributes = new ConcurrentHashMap<>();
    /** Incremented at each load, so that an older load stops and doesn't replace a newer one. */
    private final AtomicInteger loadGeneration = new AtomicInteger();

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
        // a zero opacity fill and a zero width border are not drawn at all
        mNormalShapeAttributes.setDrawInterior(mFillOpacity > 0);
        mNormalShapeAttributes.setDrawOutline(mStrokeWidth > 0);

        if (mSideShapeAttributes == null)
            mSideShapeAttributes = new BasicShapeAttributes();
        mSideShapeAttributes.setInteriorMaterial(mSideFillMaterial);
        mSideShapeAttributes.setInteriorOpacity(mFillOpacity);
        mSideShapeAttributes.setDrawInterior(mFillOpacity > 0);
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

    @Override
    public void setSldStyle( Style style ) {
        styler = style != null ? new FeatureStyler(style) : null;
        stylerAttributes.clear();
        loadData();
    }

    @Override
    public Style getSldStyle() {
        return styler != null ? styler.getStyle() : null;
    }

    /**
     * Get the attributes of a feature, adding its label if the style has one.
     *
     * @param feature the feature.
     * @param labels the labels to add to, <code>null</code> to add none.
     * @return the normal and the side attributes, <code>null</code> if the feature is not drawn.
     */
    private BasicShapeAttributes[] getAttributes( SimpleFeature feature, FeatureLabels labels ) {
        if (styler == null) {
            return new BasicShapeAttributes[]{mNormalShapeAttributes, mSideShapeAttributes};
        }
        FeatureStyler.Styled styled = styler.style(feature);
        if (styled == null) {
            return null;
        }
        if (styled.label() != null && labels != null) {
            labels.add((Geometry) feature.getDefaultGeometry(), styled.label());
        }
        if (styled.symbol() == null) {
            return null;
        }
        return stylerAttributes.computeIfAbsent(styled.symbol(), symbol -> {
            BasicShapeAttributes normal = new BasicShapeAttributes();
            BasicShapeAttributes side = new BasicShapeAttributes();
            Color fill = symbol.fill();
            normal.setDrawInterior(fill != null && symbol.fillOpacity() > 0);
            side.setDrawInterior(fill != null && symbol.fillOpacity() > 0);
            if (fill != null) {
                normal.setInteriorMaterial(new Material(fill));
                normal.setInteriorOpacity(symbol.fillOpacity());
                side.setInteriorMaterial(new Material(NwwUtilities.darkenColor(fill)));
                side.setInteriorOpacity(symbol.fillOpacity());
            }
            Color stroke = symbol.stroke();
            normal.setDrawOutline(stroke != null && symbol.strokeWidth() > 0 && symbol.strokeOpacity() > 0);
            if (stroke != null) {
                normal.setOutlineMaterial(new Material(stroke));
                normal.setOutlineOpacity(symbol.strokeOpacity());
                normal.setOutlineWidth(symbol.strokeWidth());
            }
            return new BasicShapeAttributes[]{normal, side};
        });
    }

    public class WorkerThread extends Thread {

        public void run() {
            int generation = loadGeneration.incrementAndGet();
            List<Renderable> renderables = new ArrayList<>();
            FeatureLabels labels = new FeatureLabels();
            SimpleFeatureIterator featureIterator = featureCollectionLL.features();
            try {
                while( featureIterator.hasNext() ) {
                    if (generation != loadGeneration.get()) {
                        // a newer load started, as for a new style
                        return;
                    }
                    SimpleFeature polygonAreaFeature = featureIterator.next();
                    BasicShapeAttributes[] attributes = getAttributes(polygonAreaFeature, labels);
                    if (attributes == null) {
                        continue;
                    }
                    if (mApplyExtrusion && (mHeightFieldName != null || mHasConstantHeight)) {
                        addExtrudedPolygon(polygonAreaFeature, attributes, renderables);
                    } else {
                        addPolygon(polygonAreaFeature, attributes[0], renderables);
                    }
                }
            } finally {
                featureIterator.close();
            }
            synchronized (loadGeneration) {
                if (generation == loadGeneration.get()) {
                    removeAllRenderables();
                    addRenderables(renderables);
                    if (!labels.isEmpty()) {
                        addRenderable(labels);
                    }
                }
            }
            // the map was drawn when the load started: draw it again with the new shapes
            firePropertyChange(AVKey.LAYER, null, FeatureCollectionPolygonLayer.this);
        }

    }

    private void addExtrudedPolygon( SimpleFeature polygonAreaFeature, BasicShapeAttributes[] attributes,
            List<Renderable> renderables ) {
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
                    extrudedPolygon.setAttributes(attributes[0]);
                    extrudedPolygon.setSideAttributes(attributes[1]);

                    renderables.add(extrudedPolygon);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addPolygon( SimpleFeature polygonAreaFeature, BasicShapeAttributes attributes,
            List<Renderable> renderables ) {
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

                List<List<Position>> innerVerticesLists = new ArrayList<>();
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
                    innerVerticesLists.add(internalVerticesList);
                }

                if (isDrapedOnTerrain()) {
                    FeatureSurfacePolygon polygon = new FeatureSurfacePolygon(featureStoreInfo);
                    polygon.setFeature(polygonAreaFeature);
                    polygon.setOuterBoundary(verticesList);
                    for( List<Position> innerVerticesList : innerVerticesLists ) {
                        polygon.addInnerBoundary(innerVerticesList);
                    }
                    polygon.setAttributes(attributes);
                    renderables.add(polygon);
                } else {
                    FeaturePolygon polygon = new FeaturePolygon(featureStoreInfo);
                    polygon.setFeature(polygonAreaFeature);
                    polygon.setOuterBoundary(verticesList);
                    for( List<Position> innerVerticesList : innerVerticesLists ) {
                        polygon.addInnerBoundary(innerVerticesList);
                    }
                    polygon.setAltitudeMode(mElevationMode);
                    polygon.setAttributes(attributes);
                    renderables.add(polygon);
                }
            }
        }
    }

    /**
     * Non extruded polygons that have no own heights are draped on the terrain as surface shapes.
     *
     * <p>3D shapes are planar between their vertices, so the terrain cuts through them.</p>
     */
    private boolean isDrapedOnTerrain() {
        return mElevationMode != WorldWind.ABSOLUTE && mHeightFieldName == null;
    }

    /**
     * @return the collection given to the layer, not the whole feature store, since it can be a
     *          filtered view of it. Collections read from a store read it again at each use, so
     *          they show the edited attributes.
     */
    private SimpleFeatureCollection getfeatureCollection() throws Exception {
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
    public Envelope getBounds() {
        try {
            return getfeatureCollection().getBounds();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        // the label of an added feature is shown at the next reload
        BasicShapeAttributes[] attributes = getAttributes(feature, null);
        if (attributes != null) {
            List<Renderable> renderables = new ArrayList<>();
            addPolygon(feature, attributes[0], renderables);
            addRenderables(renderables);
        }
    }

    @Override
    public void reload() {
        loadData();
    }
}
