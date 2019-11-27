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
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.shapes.FeatureLine;
import org.hortonmachine.nww.shapes.FeaturePolygon;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.style.SimpleStyle;
import org.hortonmachine.style.SimpleStyleUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;

/**
 * A simple lines layer.
 * 
 * @author Andrea Antonello andrea.antonello@gmail.com
 */
public class ShapefilesFolderLayer extends RenderableLayer implements NwwLayer {

    private int mElevationMode = WorldWind.CLAMP_TO_GROUND;
    private String title;
    private AirspaceAttributes highlightAttrs;
    private File[] shpFiles;
    private ReferencedEnvelope bounds;

    public ShapefilesFolderLayer( String folderPath ) throws IOException {
        File folderFile = new File(folderPath);
        shpFiles = folderFile.listFiles(new FilenameFilter(){
            @Override
            public boolean accept( File dir, String name ) {
                return name.toLowerCase().endsWith(".shp");
            }
        });
        this.title = folderFile.getName();

        if (shpFiles.length == 0) {
            throw new IOException("No data found in folder.");
        }

        loadData();
    }

    public void loadData() {
        Thread t = new WorkerThread();
        t.start();
    }

    public class WorkerThread extends Thread {

        public void run() {

            for( File shpFile : shpFiles ) {
                try {
                    SimpleFeatureCollection readFC = NwwUtilities.readAndReproject(shpFile.getAbsolutePath());
                    ReferencedEnvelope tmpBounds = readFC.getBounds();
                    if (tmpBounds.getWidth() == 0 || tmpBounds.getHeight() == 0) {
                        continue;
                    }
                    if (bounds == null) {
                        bounds = new ReferencedEnvelope(tmpBounds);
                    } else {
                        bounds.expandToInclude(tmpBounds);
                    }
                    GeometryDescriptor geometryDescriptor = readFC.getSchema().getGeometryDescriptor();
                    if (EGeometryType.isPolygon(geometryDescriptor)) {
                        Material fillMaterial = Material.BLACK;
                        Material strokeMaterial = Material.BLACK;
                        double fillOpacity = 0.7;
                        double strokeWidth = 2;
                        BasicShapeAttributes shapeAttributes = new BasicShapeAttributes();
                        SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(shpFile.getAbsolutePath(), EGeometryType.POLYGON.name());
                        if (style != null) {
                            fillMaterial = new Material(style.fillColor);
                            fillOpacity = style.fillOpacity;
                            strokeMaterial = new Material(style.strokeColor);
                            strokeWidth = style.strokeWidth;
                        }
                        shapeAttributes.setInteriorMaterial(fillMaterial);
                        shapeAttributes.setInteriorOpacity(fillOpacity);
                        shapeAttributes.setOutlineMaterial(strokeMaterial);
                        shapeAttributes.setOutlineWidth(strokeWidth);

                        SimpleFeatureIterator featureIterator = readFC.features();
                        try {
                            while( featureIterator.hasNext() ) {
                                SimpleFeature polygonAreaFeature = featureIterator.next();
                                addPolygon(polygonAreaFeature, shapeAttributes);
                            }
                        } finally {
                            featureIterator.close();
                        }
                    } else if (EGeometryType.isLine(geometryDescriptor)) {
                        Material strokeMaterial = Material.BLACK;
                        double strokeWidth = 2;
                        BasicShapeAttributes shapeAttributes = new BasicShapeAttributes();
                        SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(shpFile.getAbsolutePath(), EGeometryType.LINESTRING.name());
                        if (style != null) {
                            strokeMaterial = new Material(style.strokeColor);
                            strokeWidth = style.strokeWidth;
                        }
                        shapeAttributes.setOutlineMaterial(strokeMaterial);
                        shapeAttributes.setOutlineWidth(strokeWidth);

                        SimpleFeatureIterator featureIterator = readFC.features();
                        try {
                            while( featureIterator.hasNext() ) {
                                SimpleFeature lineFeature = featureIterator.next();
                                addLine(lineFeature, shapeAttributes);
                            }
                        } finally {
                            featureIterator.close();
                        }
                    } else if (EGeometryType.isPoint(geometryDescriptor)) {
                        Material fillMaterial = Material.GREEN;
                        double markerSize = 5d;
                        SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(shpFile.getAbsolutePath(), EGeometryType.POINT.name());
                        if (style != null) {
                            fillMaterial = new Material(style.fillColor);
                            markerSize = style.shapeSize;
                        }
                        PointPlacemarkAttributes basicMarkerAttributes = new PointPlacemarkAttributes();
                        Color color = fillMaterial.getDiffuse();
                        basicMarkerAttributes.setImageColor(color);
                        // basicMarkerAttributes.setLineMaterial(new Material(darkenColor));
                        // basicMarkerAttributes.setLineWidth(1d);
                        basicMarkerAttributes.setUsePointAsDefaultImage(true);
                        basicMarkerAttributes.setScale(markerSize);

                        SimpleFeatureIterator featureIterator = readFC.features();
                        try {
                            while( featureIterator.hasNext() ) {
                                SimpleFeature pointFeature = featureIterator.next();
                                Geometry geometry = (Geometry) pointFeature.getDefaultGeometry();
                                if (geometry == null) {
                                    continue;
                                }
                                int numGeometries = geometry.getNumGeometries();
                                for( int i = 0; i < numGeometries; i++ ) {
                                    Geometry geometryN = geometry.getGeometryN(i);
                                    if (geometryN instanceof Point) {
                                        Point point = (Point) geometryN;
                                        double x = point.getX();
                                        double y = point.getY();
                                        Position position = Position.fromDegrees(y, x);
                                        PointPlacemark marker = new PointPlacemark(position);
                                        marker.setAltitudeMode(mElevationMode);
                                        // marker.setLineEnabled(applyExtrusion);
                                        marker.setAttributes(basicMarkerAttributes);
                                    }
                                }
                            }
                        } finally {
                            featureIterator.close();
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    }

    private void addLine( SimpleFeature lineFeature, BasicShapeAttributes shapeAttributes ) {
        Geometry geometry = (Geometry) lineFeature.getDefaultGeometry();
        if (geometry == null) {
            return;
        }
        Coordinate[] coordinates = geometry.getCoordinates();
        if (coordinates.length < 2)
            return;

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
                    verticesList.add(Position.fromDegrees(c.y, c.x));
                }
                FeatureLine path = new FeatureLine(verticesList, null);
                path.setFeature(lineFeature);
                path.setAltitudeMode(mElevationMode);
                path.setAttributes(shapeAttributes);
                path.setHighlightAttributes(highlightAttrs);

                addRenderable(path);
            }
        }
    }

    private void addPolygon( SimpleFeature polygonAreaFeature, BasicShapeAttributes shapeAttributes ) {
        Geometry geometry = (Geometry) polygonAreaFeature.getDefaultGeometry();
        if (geometry == null) {
            return;
        }
        Coordinate[] coordinates = geometry.getCoordinates();
        int numVertices = coordinates.length;
        if (numVertices < 4)
            return;

        int numGeometries = geometry.getNumGeometries();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometryN = geometry.getGeometryN(i);
            if (geometryN instanceof org.locationtech.jts.geom.Polygon) {
                org.locationtech.jts.geom.Polygon poly = (org.locationtech.jts.geom.Polygon) geometryN;

                FeaturePolygon polygon = new FeaturePolygon(null);
                polygon.setFeature(polygonAreaFeature);

                Coordinate[] extCoords = poly.getExteriorRing().getCoordinates();
                int extSize = extCoords.length;
                List<Position> verticesList = new ArrayList<>(extSize);
                for( int n = 0; n < extSize; n++ ) {
                    Coordinate c = extCoords[n];
                    verticesList.add(Position.fromDegrees(c.y, c.x));
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
                        internalVerticesList.add(Position.fromDegrees(c.y, c.x));
                    }
                    polygon.addInnerBoundary(internalVerticesList);
                }

                polygon.setAltitudeMode(mElevationMode);
                polygon.setAttributes(shapeAttributes);

                addRenderable(polygon);
            }
        }
    }

    @Override
    public String toString() {
        return title != null ? title : "Lines";
    }

    @Override
    public Coordinate getCenter() {
        if (bounds != null)
            return bounds.centre();
        else
            return new Coordinate(0, 0);
    }

}
