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
package org.jgrasstools.nww.layers.defaults.spatialite;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.spatialite.QueryResult;
import org.jgrasstools.gears.spatialite.GTSpatialiteDb;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.style.SimpleStyle;
import org.jgrasstools.nww.layers.defaults.NwwVectorLayer;
import org.jgrasstools.nww.shapes.InfoLine;
import org.jgrasstools.nww.utils.NwwUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;

/**
 * A simple lines layer.
 * 
 * @author Andrea Antonello andrea.antonello@gmail.com
 */
public class SpatialiteLinesLayer extends RenderableLayer implements NwwVectorLayer {

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

    private String tableName;
    private GTSpatialiteDb db;
    private ReferencedEnvelope tableBounds;
    private int featureLimit;

    public SpatialiteLinesLayer( ASpatialDb db, String tableName, int featureLimit ) {
        this.db = (GTSpatialiteDb) db;
        this.tableName = tableName;
        this.featureLimit = featureLimit;

        try {
            tableBounds = this.db.getTableBounds(tableName);
        } catch (Exception e) {
            e.printStackTrace();
            tableBounds = CrsUtilities.WORLD;
        }

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

            try {
                QueryResult tableRecords = db.getTableRecordsMapIn(tableName, null, false, featureLimit,
                        NwwUtilities.GPS_CRS_SRID);
                int count = tableRecords.data.size();
                List<String> names = tableRecords.names;
                for( int i = 0; i < count; i++ ) {
                    Object[] objects = tableRecords.data.get(i);
                    StringBuilder sb = new StringBuilder();
                    double height = -1;
                    for( int j = 1; j < objects.length; j++ ) {
                        String varName = names.get(j);
                        sb.append(varName).append(": ").append(objects[j]).append("\n");

                        if (mHeightFieldName != null && varName == mHeightFieldName && objects[j] instanceof Number) {
                            height = ((Number) objects[j]).doubleValue();
                        }
                    }
                    String info = sb.toString();
                    Geometry geometry = (Geometry) objects[0];
                    if (geometry == null) {
                        continue;
                    }
                    boolean doExtrude = false;
                    if (mApplyExtrusion && (mHeightFieldName != null || mHasConstantHeight)) {
                        doExtrude = true;
                    }
                    addLine(geometry, info, height, doExtrude);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void addLine( Geometry geometry, String info, double height, boolean doExtrude ) {
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
                    double tmpH = height;
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
                    InfoLine path = new InfoLine(verticesList);
                    path.setInfo(info);
                    path.setAltitudeMode(mElevationMode);
                    path.setAttributes(mNormalShapeAttributes);
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
        return tableBounds.centre();
    }

    @Override
    public GEOMTYPE getType() {
        return GEOMTYPE.LINE;
    }

}
