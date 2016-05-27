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
package org.jgrasstools.nww.layers.defaults;

import java.awt.Color;
import java.util.ArrayList;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.nww.gui.style.SimpleStyle;
import org.jgrasstools.nww.layers.BasicMarkerWithInfo;
import org.jgrasstools.nww.layers.MarkerLayer;
import org.jgrasstools.nww.layers.defaults.NwwVectorLayer.GEOMTYPE;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;

/**
 * A layer of points.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureCollectionPointsLayer extends MarkerLayer implements NwwVectorLayer {

    private BasicMarkerAttributes basicMarkerAttributes;

    private Material mFillMaterial = Material.GREEN;
    private double mFillOpacity = 1d;
    private double mMarkerSize = 5d;
    private String mShapeType = BasicMarkerShape.SPHERE;

    private String title;

    private SimpleFeatureCollection featureCollectionLL;

    public FeatureCollectionPointsLayer(String title, SimpleFeatureCollection featureCollectionLL, String infoField) {
        this.title = title;
        this.featureCollectionLL = featureCollectionLL;
        basicMarkerAttributes = new BasicMarkerAttributes(mFillMaterial, mShapeType, mFillOpacity);
        basicMarkerAttributes.setMarkerPixels(mMarkerSize);
        basicMarkerAttributes.setMinMarkerSize(0.1);

        setOverrideMarkerElevation(true);
        setElevation(0);

        setMarkers(new ArrayList<Marker>());

        SimpleFeatureIterator featureIterator = featureCollectionLL.features();
        try {
            while (featureIterator.hasNext()) {
                SimpleFeature pointFeature = featureIterator.next();
                Geometry geometry = (Geometry) pointFeature.getDefaultGeometry();
                String info = null;
                if (infoField != null) {
                    Object attribute = pointFeature.getAttribute(infoField);
                    if (attribute != null)
                        info = attribute.toString();
                }
                int numGeometries = geometry.getNumGeometries();
                for (int i = 0; i < numGeometries; i++) {
                    Geometry geometryN = geometry.getGeometryN(i);
                    if (geometryN instanceof Point) {
                        Point point = (Point) geometryN;
                        addNewPoint(point.getY(), point.getX(), info);
                    }
                }
            }
        } finally {
            featureIterator.close();
        }

    }

    public void setStyle(SimpleStyle style) {
        if (style != null) {
            mFillMaterial = new Material(style.fillColor);
            mFillOpacity = style.fillOpacity;
            mMarkerSize = style.shapeSize;
            mShapeType = style.shapeType;
        }

        basicMarkerAttributes.setMaterial(mFillMaterial);
        basicMarkerAttributes.setOpacity(mFillOpacity);
        basicMarkerAttributes.setMarkerPixels(mMarkerSize);
        basicMarkerAttributes.setShapeType(mShapeType);
    }

    @Override
    public SimpleStyle getStyle() {
        SimpleStyle simpleStyle = new SimpleStyle();
        simpleStyle.fillColor = basicMarkerAttributes.getMaterial().getDiffuse();
        simpleStyle.fillOpacity = basicMarkerAttributes.getOpacity();
        simpleStyle.shapeSize = basicMarkerAttributes.getMarkerPixels();
        simpleStyle.shapeType = basicMarkerAttributes.getShapeType();
        return simpleStyle;
    }

    public void addNewPoint(double lat, double lon) {
        BasicMarker marker = new BasicMarker(Position.fromDegrees(lat, lon, 0), basicMarkerAttributes);
        addMarker(marker);
    }

    public void addNewPoint(double lat, double lon, String info) {
        if (info == null) {
            addNewPoint(lat, lon);
            return;
        }
        BasicMarkerWithInfo marker = new BasicMarkerWithInfo(Position.fromDegrees(lat, lon, 0), basicMarkerAttributes,
                info);
        addMarker(marker);
    }

    @Override
    public String toString() {
        return title != null ? title : "Points";
    }

    @Override
    public Coordinate getCenter() {
        ReferencedEnvelope bounds = featureCollectionLL.getBounds();
        return bounds.centre();
    }

    @Override
    public GEOMTYPE getType() {
        return GEOMTYPE.POINT;
    }
}
