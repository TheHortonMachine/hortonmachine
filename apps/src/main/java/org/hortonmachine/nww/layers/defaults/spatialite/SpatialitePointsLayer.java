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
package org.hortonmachine.nww.layers.defaults.spatialite;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.gears.spatialite.GTSpatialiteThreadsafeDb;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.nww.layers.defaults.NwwVectorLayer;
import org.hortonmachine.nww.layers.defaults.other.MarkerLayer;
import org.hortonmachine.nww.shapes.InfoPoint;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.style.SimpleStyle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;

/**
 * A layer of points.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialitePointsLayer extends MarkerLayer implements NwwVectorLayer {

    private BasicMarkerAttributes basicMarkerAttributes;

    private Material mFillMaterial = Material.GREEN;
    private double mFillOpacity = 1d;
    private double mMarkerSize = 5d;
    private String mShapeType = BasicMarkerShape.SPHERE;

    private String tableName;
    private ReferencedEnvelope tableBounds;

    public SpatialitePointsLayer( ASpatialDb db, String tableName, int featureLimit ) {
        this.tableName = tableName;

        try {
            if (db instanceof GTSpatialiteThreadsafeDb) {
                GTSpatialiteThreadsafeDb gtDb = (GTSpatialiteThreadsafeDb) db;
                tableBounds = gtDb.getTableBounds(tableName);
            } else {
                tableBounds = CrsUtilities.WORLD;
            }
        } catch (Exception e) {
            e.printStackTrace();
            tableBounds = CrsUtilities.WORLD;
        }

        basicMarkerAttributes = new BasicMarkerAttributes(mFillMaterial, mShapeType, mFillOpacity);
        basicMarkerAttributes.setMarkerPixels(mMarkerSize);
        basicMarkerAttributes.setMinMarkerSize(0.1);

        setOverrideMarkerElevation(true);
        setElevation(0);

        setMarkers(new ArrayList<Marker>());

        try {
            QueryResult tableRecords = db.getTableRecordsMapIn(tableName, null, featureLimit, NwwUtilities.GPS_CRS_SRID, null);
            int count = tableRecords.data.size();
            List<String> names = tableRecords.names;
            for( int i = 0; i < count; i++ ) {
                Object[] objects = tableRecords.data.get(i);
                StringBuilder sb = new StringBuilder();
                for( int j = 1; j < objects.length; j++ ) {
                    String varName = names.get(j);
                    sb.append(varName).append(": ").append(objects[j]).append("\n");

                }
                String info = sb.toString();
                Geometry geometry = (Geometry) objects[0];
                if (geometry == null) {
                    continue;
                }
                int numGeometries = geometry.getNumGeometries();
                for( int j = 0; j < numGeometries; j++ ) {
                    Geometry geometryN = geometry.getGeometryN(j);
                    if (geometryN instanceof Point) {
                        Point point = (Point) geometryN;
                        InfoPoint marker = new InfoPoint(Position.fromDegrees(point.getY(), point.getX(), 0),
                                basicMarkerAttributes);
                        marker.setInfo(info);
                        addMarker(marker);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setStyle( SimpleStyle style ) {
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
        return tableName != null ? tableName : "Points";
    }

    @Override
    public Coordinate getCenter() {
        return tableBounds.centre();
    }

    @Override
    public GEOMTYPE getType() {
        return GEOMTYPE.POINT;
    }
}
