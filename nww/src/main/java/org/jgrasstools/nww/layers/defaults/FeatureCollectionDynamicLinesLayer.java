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
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.nww.gui.NwwPanel;
import org.jgrasstools.nww.gui.style.SimpleStyle;
import org.jgrasstools.nww.shapes.FeatureLine;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.util.Logging;

/**
 * A simple lines layer. THIS IS EXPERIMENTAL FOR TESTING ONLY
 * 
 * @author Andrea Antonello andrea.antonello@gmail.com
 */
public class FeatureCollectionDynamicLinesLayer extends RenderableLayer implements NwwVectorLayer {

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
    private Path renderPath;
    private NwwPanel wwdPanel;

    public FeatureCollectionDynamicLinesLayer(NwwPanel panel, String title,
        SimpleFeatureCollection featureCollectionLL) {
        this.wwdPanel = panel;
        this.title = title;
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
        //        loadData();
    }

    @Override
    public void setStyle(SimpleStyle style) {
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

    public void setExtrusionProperties(Double constantExtrusionHeight, String heightFieldName,
        Double verticalExageration, boolean withoutExtrusion) {
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

    public void setElevationMode(int elevationMode) {
        mElevationMode = elevationMode;
    }

    @Override
    protected void doRender(DrawContext dc) {

        ReferencedEnvelope viewportBounds = wwdPanel.getViewportBounds();
        if (viewportBounds == null) {
            return;
        }
        ReferencedEnvelope bounds = featureCollectionLL.getBounds();
        if (!bounds.intersects((Envelope) viewportBounds)) {
            return;
        }

        System.out.println("Render called");
        SimpleFeatureIterator featureIterator = featureCollectionLL.features();
        while (featureIterator.hasNext()) {
            SimpleFeature lineFeature = featureIterator.next();
            boolean doExtrude = false;
            if (mApplyExtrusion && (mHeightFieldName != null || mHasConstantHeight)) {
                doExtrude = true;
            }
            Geometry geometry = (Geometry) lineFeature.getDefaultGeometry();
            if (!viewportBounds.intersects(geometry.getEnvelopeInternal())) {
                continue;
            }
            int numGeometries = geometry.getNumGeometries();
            for (int i = 0; i < numGeometries; i++) {
                Geometry geometryN = geometry.getGeometryN(i);
                if (geometryN instanceof LineString) {
                    LineString line = (LineString) geometryN;
                    Coordinate[] lineCoords = line.getCoordinates();
                    int numVertices = lineCoords.length;
                    List<Position> verticesList = new ArrayList<>(numVertices);
                    for (int j = 0; j < numVertices; j++) {
                        Coordinate c = lineCoords[j];
                        verticesList.add(Position.fromDegrees(c.y, c.x));
                    }
                    Path renderPath = new Path();
                    renderPath.setAltitudeMode(mElevationMode);
                    renderPath.setAttributes(mNormalShapeAttributes);
                    renderPath.setHighlightAttributes(highlightAttrs);
                    renderPath.setExtrude(doExtrude);
                    renderPath.setPositions(verticesList);
                    renderPath.render(dc);
                }
            }

        }
        featureIterator.close();
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
