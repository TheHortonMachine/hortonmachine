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
package org.jgrasstools.nww.layers.defaults.vector;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.nww.gui.style.SimpleStyle;
import org.jgrasstools.nww.layers.defaults.NwwVectorLayer;
import org.jgrasstools.nww.layers.defaults.NwwVectorLayer.GEOMTYPE;
import org.jgrasstools.nww.shapes.FeatureLine;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;

/**
 * A simple lines layer.
 * 
 * @author Andrea Antonello andrea.antonello@gmail.com
 */
public class FeatureCollectionLinesLayer extends RenderableLayer implements NwwVectorLayer {

    private String mHeightFieldName;
    private double mVerticalExageration = 1.0;
    private double mConstantHeight = 1.0;
    private boolean mHasConstantHeight = false;
    private boolean mApplyExtrusion = false;

    private BasicShapeAttributes mNormalShapeAttributes;

    private Material mStrokeMaterial = Material.BLACK;
    private double mStrokeWidth = 2;
    private SimpleFeatureCollection featureCollectionLL;

    private int mElevationMode = WorldWind.CLAMP_TO_GROUND;
    private String title;
    private AirspaceAttributes highlightAttrs;
    private SimpleFeatureStore featureStore;

    /**
     * Build the layer.
     * 
     * @param title layer name.
     * @param featureCollectionLL the featurecollection in latlong.
     * @param featureStore the feature store. If not null, then the feature attributes will be editable.
     */
    public FeatureCollectionLinesLayer( String title, SimpleFeatureCollection featureCollectionLL,
            SimpleFeatureStore featureStore ) {
        this.title = title;
        this.featureCollectionLL = featureCollectionLL;
        this.featureStore = featureStore;

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

    public class WorkerThread extends Thread {

        public void run() {
            SimpleFeatureIterator featureIterator = featureCollectionLL.features();
            while( featureIterator.hasNext() ) {
                SimpleFeature lineFeature = featureIterator.next();
                boolean doExtrude = false;
                if (mApplyExtrusion && (mHeightFieldName != null || mHasConstantHeight)) {
                    doExtrude = true;
                }
                addLine(lineFeature, doExtrude);
            }
            featureIterator.close();
        }

        private void addLine( SimpleFeature lineFeature, boolean doExtrude ) {
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
                    FeatureLine path = new FeatureLine(verticesList, featureStore);
                    path.setFeature(lineFeature);
                    path.setAltitudeMode(mElevationMode);
                    path.setAttributes(mNormalShapeAttributes);
                    path.setHighlightAttributes(highlightAttrs);
                    path.setExtrude(doExtrude);

                    addRenderable(path);
                }
            }
        }
    }

    @Override
    public String toString() {
        return title != null ? title : "Lines";
    }

    @Override
    public Coordinate getCenter() {
        ReferencedEnvelope bounds = featureCollectionLL.getBounds();
        return bounds.centre();
    }

    @Override
    public GEOMTYPE getType() {
        return GEOMTYPE.LINE;
    }

}
