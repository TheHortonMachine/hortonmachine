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
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.utils.MercatorUtils;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.shapes.FeatureLine;
import org.hortonmachine.nww.shapes.FeaturePolygon;
import org.hortonmachine.style.SimpleStyle;
import org.hortonmachine.style.SimpleStyleUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
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
public class GeopackageVectorLayer extends RenderableLayer implements NwwLayer {

    private int mElevationMode = WorldWind.CLAMP_TO_GROUND;
    private String title;
    private AirspaceAttributes highlightAttrs;
    private ReferencedEnvelope bounds;
    private GeopackageCommonDb db;
    private String tableName;

    public GeopackageVectorLayer( String gpkgPath, String tableName ) throws Exception {
        this.tableName = tableName;
        this.title = tableName;
        db = new GeopackageDb();
        db.open(gpkgPath);

        loadData();
    }

    public void loadData() {
        Thread t = new WorkerThread();
        t.start();
    }

    public class WorkerThread extends Thread {

        public void run() {
            try {
                FeatureEntry feature = db.feature(tableName);
                int srid = feature.getSrid();
                boolean convert = srid != GeopackageCommonDb.WGS84LL_SRID;
                GeometryColumn geometryColumns = db.getGeometryColumnsForTable(tableName);
                EGeometryType geometryType = geometryColumns.geometryType;
                List<Geometry> geometries = db.getGeometriesIn(tableName, (Envelope) null, null);
                bounds = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
                if (geometryType.isPolygon()) {
                    Material fillMaterial = Material.BLACK;
                    Material strokeMaterial = Material.BLACK;
                    double fillOpacity = 0.7;
                    double strokeWidth = 2;
                    BasicShapeAttributes shapeAttributes = new BasicShapeAttributes();

                    String sldString = db.getSldString(tableName);
                    if (sldString != null) {
                        Style st = SldUtilities.getStyleFromSldString(sldString);
                        SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(st, EGeometryType.POLYGON.name());
                        if (style != null) {
                            fillMaterial = new Material(style.fillColor);
                            fillOpacity = style.fillOpacity;
                            strokeMaterial = new Material(style.strokeColor);
                            strokeWidth = style.strokeWidth;
                        }
                    }
                    shapeAttributes.setInteriorMaterial(fillMaterial);
                    shapeAttributes.setInteriorOpacity(fillOpacity);
                    shapeAttributes.setOutlineMaterial(strokeMaterial);
                    shapeAttributes.setOutlineWidth(strokeWidth);

                    for( Geometry geometry : geometries ) {
                        addPolygon(geometry, shapeAttributes, convert);
                    }

                } else if (geometryType.isLine()) {
                    Material strokeMaterial = Material.BLACK;
                    double strokeWidth = 2;
                    BasicShapeAttributes shapeAttributes = new BasicShapeAttributes();
                    String sldString = db.getSldString(tableName);
                    if (sldString != null) {
                        Style st = SldUtilities.getStyleFromSldString(sldString);
                        SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(st, EGeometryType.LINESTRING.name());
                        strokeMaterial = new Material(style.strokeColor);
                        strokeWidth = style.strokeWidth;
                    }
                    shapeAttributes.setOutlineMaterial(strokeMaterial);
                    shapeAttributes.setOutlineWidth(strokeWidth);

                    for( Geometry geometry : geometries ) {
                        addLine(geometry, shapeAttributes, convert);
                    }
                } else if (geometryType.isPoint()) {
                    Material fillMaterial = Material.GREEN;
                    double markerSize = 5d;

                    String sldString = db.getSldString(tableName);
                    if (sldString != null) {
                        Style st = SldUtilities.getStyleFromSldString(sldString);
                        SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(st, EGeometryType.POINT.name());
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

                    for( Geometry geometry : geometries ) {
                        if (geometry == null) {
                            continue;
                        }
                        int numGeometries = geometry.getNumGeometries();
                        for( int i = 0; i < numGeometries; i++ ) {
                            Geometry geometryN = geometry.getGeometryN(i);
                            if (geometryN instanceof Point) {
                                Point point = (Point) geometryN;
                                Coordinate c = point.getCoordinate();
                                if (convert) {
                                    c = MercatorUtils.convert3857To4326(c);
                                }
                                bounds.expandToInclude(c);
                                Position position = Position.fromDegrees(c.y, c.x);
                                PointPlacemark marker = new PointPlacemark(position);
                                marker.setAltitudeMode(mElevationMode);
                                // marker.setLineEnabled(applyExtrusion);
                                marker.setAttributes(basicMarkerAttributes);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private void addLine( Geometry geometry, BasicShapeAttributes shapeAttributes, boolean convert ) {
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
                    if (convert) {
                        c = MercatorUtils.convert3857To4326(c);
                    }
                    bounds.expandToInclude(c);
                    verticesList.add(Position.fromDegrees(c.y, c.x));
                }
                FeatureLine path = new FeatureLine(verticesList, null);
//                path.setFeature(lineFeature);
                path.setAltitudeMode(mElevationMode);
                path.setAttributes(shapeAttributes);
                path.setHighlightAttributes(highlightAttrs);

                addRenderable(path);
            }
        }
    }

    private void addPolygon( Geometry geometry, BasicShapeAttributes shapeAttributes, boolean convert ) {
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
//                polygon.setFeature(polygonAreaFeature);

                Coordinate[] extCoords = poly.getExteriorRing().getCoordinates();
                int extSize = extCoords.length;
                List<Position> verticesList = new ArrayList<>(extSize);
                for( int n = 0; n < extSize; n++ ) {
                    Coordinate c = extCoords[n];
                    if (convert) {
                        c = MercatorUtils.convert3857To4326(c);
                    }
                    bounds.expandToInclude(c);
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
                        if (convert) {
                            c = MercatorUtils.convert3857To4326(c);
                        }
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
