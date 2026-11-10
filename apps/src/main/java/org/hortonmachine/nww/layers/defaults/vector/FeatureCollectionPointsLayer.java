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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.utils.style.FeatureStyler;
import org.hortonmachine.nww.layers.defaults.NwwEditableVectorLayer;
import org.hortonmachine.nww.layers.defaults.NwwFeatureLayer;
import org.hortonmachine.nww.shapes.FeaturePoint;
import org.hortonmachine.nww.shapes.FeatureStoreInfo;
import org.hortonmachine.style.SimpleStyle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;

/**
 * A layer of points.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class FeatureCollectionPointsLayer extends RenderableLayer implements NwwEditableVectorLayer, NwwFeatureLayer {

    private PointPlacemarkAttributes basicMarkerAttributes;

    private Material mFillMaterial = Material.GREEN;
    private double mFillOpacity = 1d;
    private double mMarkerSize = 15d;
    private String mShapeType = BasicMarkerShape.SPHERE;

    private String title;

    private SimpleFeatureCollection featureCollectionLL;

    private FeatureStoreInfo featureStoreInfo;

    private SimpleFeatureStore featureStore;

    /** The SLD style, <code>null</code> to draw all features with the simple style. */
    private FeatureStyler styler;
    /** The attributes of the looks given by the SLD style, shared by the features. */
    private final Map<FeatureStyler.Symbol, PointPlacemarkAttributes> stylerAttributes = new ConcurrentHashMap<>();

    public FeatureCollectionPointsLayer( String title, SimpleFeatureCollection featureCollectionLL,
            SimpleFeatureStore featureStore, HashMap<String, String[]> field2ValuesMap, Object imageObject ) {
        this.title = title;
        this.featureCollectionLL = featureCollectionLL;
        this.featureStore = featureStore;
        featureStoreInfo = new FeatureStoreInfo(featureStore, field2ValuesMap);

        basicMarkerAttributes = new PointPlacemarkAttributes();
        basicMarkerAttributes.setLabelMaterial(mFillMaterial);
        basicMarkerAttributes.setLineMaterial(mFillMaterial);
        basicMarkerAttributes.setUsePointAsDefaultImage(true);
        if (imageObject != null) {
            if (imageObject instanceof BufferedImage) {
                BufferedImage image = (BufferedImage) imageObject;
                basicMarkerAttributes.setImage(image);
            } else if (imageObject instanceof String) {
                basicMarkerAttributes.setImageAddress((String) imageObject);
            }
        } else {
            basicMarkerAttributes.setScale(mMarkerSize);
        }
        try {
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadData() throws Exception {
        List<Renderable> renderables = new ArrayList<>();
        FeatureLabels labels = new FeatureLabels();
        SimpleFeatureIterator featureIterator = getfeatureCollection().features();
        try {
            while( featureIterator.hasNext() ) {
                SimpleFeature pointFeature = featureIterator.next();
                PointPlacemarkAttributes attributes = getAttributes(pointFeature, labels);
                if (attributes != null) {
                    addPoint(pointFeature, attributes, renderables);
                }
            }
        } finally {
            featureIterator.close();
        }
        removeAllRenderables();
        addRenderables(renderables);
        if (!labels.isEmpty()) {
            addRenderable(labels);
        }
    }

    @Override
    public void setSldStyle( Style style ) {
        styler = style != null ? new FeatureStyler(style) : null;
        stylerAttributes.clear();
        reload();
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
    private PointPlacemarkAttributes getAttributes( SimpleFeature feature, FeatureLabels labels ) {
        if (styler == null) {
            return basicMarkerAttributes;
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
            Color color = symbolColor(symbol);
            PointPlacemarkAttributes attributes = new PointPlacemarkAttributes();
            attributes.setLabelMaterial(new Material(color));
            applyOpacity(attributes, color, getOpacity());
            attributes.setUsePointAsDefaultImage(true);
            attributes.setScale(symbol.size());
            return attributes;
        });
    }

    /**
     * @return the color of the points of a look of the SLD style: the fill color, or the border
     *          one if not filled.
     */
    private static Color symbolColor( FeatureStyler.Symbol symbol ) {
        Color color = symbol.fill() != null ? symbol.fill() : symbol.stroke();
        return color != null ? color : Color.GRAY;
    }

    /**
     * Draw the points, and the images of the markers, with the opacity of the layer.
     *
     * @param attributes the attributes to change.
     * @param color the color of the points, without opacity.
     * @param opacity the opacity of the layer.
     */
    private static void applyOpacity( PointPlacemarkAttributes attributes, Color color, double opacity ) {
        int alpha = (int) Math.round(255 * Math.max(0, Math.min(1, opacity)));
        attributes.setLineMaterial(new Material(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha)));
        attributes.setImageColor(new Color(255, 255, 255, alpha));
    }

    /**
     * The opacity of the whole layer, which doesn't change the style.
     */
    @Override
    public void setOpacity( double opacity ) {
        super.setOpacity(opacity);
        if (basicMarkerAttributes != null) {
            applyOpacity(basicMarkerAttributes, mFillMaterial.getDiffuse(), opacity);
        }
        stylerAttributes.forEach(( symbol, attributes ) -> applyOpacity(attributes, symbolColor(symbol), opacity));
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
    public SimpleFeatureCollection getFeatureCollection() {
        return featureCollectionLL;
    }

    @Override
    public SimpleFeatureStore getFeatureStore() {
        return featureStore;
    }

    private void addPoint( SimpleFeature pointFeature, PointPlacemarkAttributes attributes, List<Renderable> renderables ) {
        Geometry geometry = (Geometry) pointFeature.getDefaultGeometry();
        if (geometry == null) {
            return;
        }
        int numGeometries = geometry.getNumGeometries();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometryN = geometry.getGeometryN(i);
            if (geometryN instanceof Point) {
                Point point = (Point) geometryN;
                FeaturePoint marker = new FeaturePoint(Position.fromDegrees(point.getY(), point.getX(), 0), featureStoreInfo);
                marker.setFeature(pointFeature);
                marker.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                marker.setAttributes(attributes);
                renderables.add(marker);
            }
        }
    }

    public void setStyle( SimpleStyle style ) {
        if (style != null) {
            mFillMaterial = new Material(style.fillColor);
            mFillOpacity = style.fillOpacity;
            mMarkerSize = style.shapeSize;
            mShapeType = style.shapeType;
        }

        basicMarkerAttributes.setLabelMaterial(mFillMaterial);
        applyOpacity(basicMarkerAttributes, mFillMaterial.getDiffuse(), getOpacity());
        basicMarkerAttributes.setUsePointAsDefaultImage(true);
        basicMarkerAttributes.setScale(mMarkerSize);
    }

    @Override
    public SimpleStyle getStyle() {
        SimpleStyle simpleStyle = new SimpleStyle();
        simpleStyle.fillColor = basicMarkerAttributes.getLabelMaterial().getDiffuse();
        simpleStyle.fillOpacity = mFillOpacity;
        simpleStyle.shapeSize = basicMarkerAttributes.getScale();
        simpleStyle.shapeType = mShapeType;
        return simpleStyle;
    }

    // public void addNewPoint(double lat, double lon) {
    // BasicMarker marker = new BasicMarker(Position.fromDegrees(lat, lon, 0),
    // basicMarkerAttributes);
    // addMarker(marker);
    // }
    //
    // public void addNewPoint(double lat, double lon, String info) {
    // if (info == null) {
    // addNewPoint(lat, lon);
    // return;
    // }
    // BasicMarkerWithInfo marker = new
    // BasicMarkerWithInfo(Position.fromDegrees(lat, lon, 0),
    // basicMarkerAttributes,
    // info);
    // addMarker(marker);
    // }

    @Override
    public String toString() {
        return title != null ? title : "Points";
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
        return GEOMTYPE.POINT;
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
        PointPlacemarkAttributes attributes = getAttributes(feature, null);
        if (attributes != null) {
            List<Renderable> renderables = new ArrayList<>();
            addPoint(feature, attributes, renderables);
            addRenderables(renderables);
        }
    }

    @Override
    public void reload() {
        try {
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
