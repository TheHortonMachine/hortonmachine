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
import org.hortonmachine.nww.shapes.FeatureLine;
import org.hortonmachine.nww.shapes.FeatureStoreInfo;
import org.hortonmachine.nww.shapes.FeatureSurfacePolyline;
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
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;

/**
 * A simple lines layer.
 * 
 * @author Andrea Antonello andrea.antonello@gmail.com
 */
public class FeatureCollectionLinesLayer extends RenderableLayer implements NwwEditableVectorLayer {

    private String mHeightFieldName;
    private double mVerticalExageration = 1.0;
    private double mConstantHeight = 1.0;
    private boolean mHasConstantHeight = false;
    private boolean mApplyExtrusion = false;

    private BasicShapeAttributes mNormalShapeAttributes;

    private Material mStrokeMaterial = Material.BLACK;
    private double mStrokeWidth = 2;

    private int mElevationMode = WorldWind.CLAMP_TO_GROUND;
    private String title;
    private AirspaceAttributes highlightAttrs;
    private FeatureStoreInfo featureStoreInfo;
    private SimpleFeatureStore featureStore;
    private SimpleFeatureCollection featureCollectionLL;

    /** The SLD style, <code>null</code> to draw all features with the simple style. */
    private volatile FeatureStyler styler;
    /** The attributes of the looks given by the SLD style, shared by the features. */
    private final Map<FeatureStyler.Symbol, BasicShapeAttributes> stylerAttributes = new ConcurrentHashMap<>();
    /** Incremented at each load, so that an older load stops and doesn't replace a newer one. */
    private final AtomicInteger loadGeneration = new AtomicInteger();

    /**
     * Build the layer.
     * 
     * @param title layer name.
     * @param featureCollectionLL the featurecollection in latlong.
     * @param featureStore the feature store. If not null, then the feature attributes will be editable.
     * @param field2ValuesMap an optional map of fields and possible values.
     */
    public FeatureCollectionLinesLayer( String title, SimpleFeatureCollection featureCollectionLL,
            SimpleFeatureStore featureStore, HashMap<String, String[]> field2ValuesMap ) {
        this.title = title;
        this.featureStore = featureStore;
        this.featureStoreInfo = new FeatureStoreInfo(featureStore, field2ValuesMap);
        this.featureCollectionLL = featureCollectionLL;

        AirspaceAttributes attrs = new BasicAirspaceAttributes();
        attrs.setDrawInterior(true);
        attrs.setDrawOutline(true);
        attrs.setInteriorMaterial(new Material(Color.WHITE));
        attrs.setOutlineMaterial(new Material(Color.BLACK));
        attrs.setOutlineWidth(2);
        attrs.setEnableAntialiasing(true);
        highlightAttrs = new BasicAirspaceAttributes(attrs);
        highlightAttrs.setOutlineMaterial(new Material(Color.RED));

        setStyle(null);
        loadData();
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
    public void setStyle( SimpleStyle style ) {
        if (style != null) {
            mStrokeMaterial = new Material(style.strokeColor);
            mStrokeWidth = style.strokeWidth;
        }
        if (mNormalShapeAttributes == null)
            mNormalShapeAttributes = new BasicShapeAttributes();
        mNormalShapeAttributes.setOutlineMaterial(mStrokeMaterial);
        mNormalShapeAttributes.setOutlineWidth(mStrokeWidth);
        mNormalShapeAttributes.setDrawOutline(mStrokeWidth > 0);
    }

    @Override
    public SimpleStyle getStyle() {
        SimpleStyle simpleStyle = new SimpleStyle();
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
     * @return the attributes, <code>null</code> if the feature is not drawn.
     */
    private BasicShapeAttributes getAttributes( SimpleFeature feature, FeatureLabels labels ) {
        if (styler == null) {
            return mNormalShapeAttributes;
        }
        FeatureStyler.Styled styled = styler.style(feature);
        if (styled == null) {
            return null;
        }
        if (styled.label() != null && labels != null) {
            labels.add((Geometry) feature.getDefaultGeometry(), styled.label());
        }
        if (styled.symbol() == null || styled.symbol().stroke() == null) {
            return null;
        }
        return stylerAttributes.computeIfAbsent(styled.symbol(), symbol -> {
            BasicShapeAttributes attributes = new BasicShapeAttributes();
            attributes.setOutlineMaterial(new Material(symbol.stroke()));
            attributes.setOutlineOpacity(symbol.strokeOpacity());
            attributes.setOutlineWidth(symbol.strokeWidth());
            attributes.setDrawOutline(symbol.strokeWidth() > 0 && symbol.strokeOpacity() > 0);
            return attributes;
        });
    }

    public class WorkerThread extends Thread {

        public void run() {
            int generation = loadGeneration.incrementAndGet();
            try {
                List<Renderable> renderables = new ArrayList<>();
                FeatureLabels labels = new FeatureLabels();
                SimpleFeatureIterator featureIterator = getfeatureCollection().features();
                try {
                    while( featureIterator.hasNext() ) {
                        if (generation != loadGeneration.get()) {
                            // a newer load started, as for a new style
                            return;
                        }
                        SimpleFeature lineFeature = featureIterator.next();
                        BasicShapeAttributes attributes = getAttributes(lineFeature, labels);
                        if (attributes == null) {
                            continue;
                        }
                        boolean doExtrude = false;
                        if (mApplyExtrusion && (mHeightFieldName != null || mHasConstantHeight)) {
                            doExtrude = true;
                        }
                        addLine(lineFeature, doExtrude, attributes, renderables);
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
                firePropertyChange(AVKey.LAYER, null, FeatureCollectionLinesLayer.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void addLine( SimpleFeature lineFeature, boolean doExtrude, BasicShapeAttributes attributes,
            List<Renderable> renderables ) {
        Geometry geometry = (Geometry) lineFeature.getDefaultGeometry();
        if (geometry == null) {
            return;
        }
        Coordinate[] coordinates = geometry.getCoordinates();
        if (coordinates.length < 2)
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
                double tmpH = ((Number) lineFeature.getAttribute(mHeightFieldName)).doubleValue();
                tmpH = tmpH * mVerticalExageration;
                h += tmpH;
            }
            break;
        }
        int numGeometries = geometry.getNumGeometries();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometryN = geometry.getGeometryN(i);
            if (geometryN instanceof LineString) {
                LineString line = (LineString) geometryN;
                Coordinate[] lineCoords = line.getCoordinates();
                int numVertices = lineCoords.length;
                List<Position> verticesList = new ArrayList<>(numVertices);
                for( int j = 0; j < numVertices; j++ ) {
                    Coordinate c = lineCoords[j];
                    if (hasZ) {
                        double z = c.z;
                        verticesList.add(Position.fromDegrees(c.y, c.x, z + h));
                    } else {
                        verticesList.add(Position.fromDegrees(c.y, c.x, h));
                    }
                }
                if (!doExtrude && isDrapedOnTerrain()) {
                    FeatureSurfacePolyline surfaceLine = new FeatureSurfacePolyline(verticesList, featureStoreInfo);
                    surfaceLine.setFeature(lineFeature);
                    surfaceLine.setAttributes(attributes);
                    surfaceLine.setHighlightAttributes(highlightAttrs);
                    renderables.add(surfaceLine);
                } else {
                    FeatureLine path = new FeatureLine(verticesList, featureStoreInfo);
                    path.setFeature(lineFeature);
                    path.setAltitudeMode(mElevationMode);
                    path.setAttributes(attributes);
                    path.setHighlightAttributes(highlightAttrs);
                    path.setExtrude(doExtrude);
                    renderables.add(path);
                }
            }
        }
    }

    /**
     * Non extruded lines that have no own heights are draped on the terrain as surface shapes.
     *
     * <p>3D lines are straight between their vertices, so the terrain cuts through them.</p>
     */
    private boolean isDrapedOnTerrain() {
        return mElevationMode != WorldWind.ABSOLUTE && mHeightFieldName == null;
    }

    @Override
    public String toString() {
        return title != null ? title : "Lines";
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
    public GEOMTYPE getType() {
        return GEOMTYPE.LINE;
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
        BasicShapeAttributes attributes = getAttributes(feature, null);
        if (attributes != null) {
            List<Renderable> renderables = new ArrayList<>();
            addLine(feature, mApplyExtrusion, attributes, renderables);
            addRenderables(renderables);
        }
    }

    @Override
    public void reload() {
        loadData();
    }

}
