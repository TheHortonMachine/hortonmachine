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

import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.nww.layers.defaults.NwwEditableVectorLayer;
import org.hortonmachine.nww.shapes.FeaturePoint;
import org.hortonmachine.nww.shapes.FeatureStoreInfo;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.style.SimpleStyle;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;

/**
 * A layer of points.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureCollectionPointsLayer extends RenderableLayer implements NwwEditableVectorLayer {

    private PointPlacemarkAttributes basicMarkerAttributes;

    private Material mFillMaterial = Material.GREEN;
    private double mFillOpacity = 1d;
    private double mMarkerSize = 15d;
    private String mShapeType = BasicMarkerShape.SPHERE;

    private String title;

    private SimpleFeatureCollection featureCollectionLL;

    private FeatureStoreInfo featureStoreInfo;

    private SimpleFeatureStore featureStore;

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

    private void loadData() throws Exception {
        SimpleFeatureIterator featureIterator = getfeatureCollection().features();
        try {
            removeAllRenderables();
            while( featureIterator.hasNext() ) {
                SimpleFeature pointFeature = featureIterator.next();
                addPoint(pointFeature);
            }
        } finally {
            featureIterator.close();
        }
    }

    private SimpleFeatureCollection getfeatureCollection() throws Exception {
        if (featureStore != null) {
            return NwwUtilities.readAndReproject(featureStore);
        }
        return featureCollectionLL;
    }

    private void addPoint( SimpleFeature pointFeature ) {
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
                marker.setAttributes(basicMarkerAttributes);
                addRenderable(marker);
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
        basicMarkerAttributes.setLineMaterial(mFillMaterial);
        basicMarkerAttributes.setUsePointAsDefaultImage(true);
        basicMarkerAttributes.setScale(mMarkerSize);
    }

    @Override
    public SimpleStyle getStyle() {
        SimpleStyle simpleStyle = new SimpleStyle();
        simpleStyle.fillColor = basicMarkerAttributes.getLabelMaterial().getDiffuse();
        simpleStyle.shapeSize = basicMarkerAttributes.getScale();
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
        addPoint(feature);
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
