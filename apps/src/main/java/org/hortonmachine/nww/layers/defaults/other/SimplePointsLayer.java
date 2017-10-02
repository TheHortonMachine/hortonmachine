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
package org.hortonmachine.nww.layers.defaults.other;

import java.awt.Color;
import java.util.ArrayList;

import org.hortonmachine.nww.layers.objects.BasicMarkerWithInfo;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;

/**
 * A layer of generic points.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SimplePointsLayer extends MarkerLayer {

    private BasicMarkerAttributes basicMarkerAttributes;

    private Material mFillMaterial = Material.BLACK;
    private double mFillOpacity = 1d;
    private double mMarkerSize = 5d;
    private String mShapeType = BasicMarkerShape.SPHERE;

    private String title;

    public SimplePointsLayer(String title) {
        this.title = title;
        basicMarkerAttributes = new BasicMarkerAttributes(mFillMaterial, mShapeType, mFillOpacity);
        basicMarkerAttributes.setMarkerPixels(mMarkerSize);
        basicMarkerAttributes.setMinMarkerSize(0.1);

        setOverrideMarkerElevation(true);
        setElevation(0);

        setMarkers(new ArrayList<Marker>());
    }

    public void setProperties(Color fillColor, Double fillOpacity, Double markerSize, String shapeType) {
        if (fillColor != null) {
            mFillMaterial = new Material(fillColor);
        }
        if (fillOpacity != null) {
            mFillOpacity = fillOpacity;
        }
        if (markerSize != null) {
            mMarkerSize = markerSize;
        }
        if (shapeType != null) {
            mShapeType = shapeType;
        }

        basicMarkerAttributes.setMaterial(mFillMaterial);
        basicMarkerAttributes.setOpacity(mFillOpacity);
        basicMarkerAttributes.setMarkerPixels(mMarkerSize);
        basicMarkerAttributes.setShapeType(mShapeType);
    }

    public void addNewPoint(double lat, double lon) {
        BasicMarker marker = new BasicMarker(Position.fromDegrees(lat, lon, 0), basicMarkerAttributes);
        addMarker(marker);
    }

    public void addNewPoint(double lat, double lon, String info) {
        BasicMarkerWithInfo marker = new BasicMarkerWithInfo(Position.fromDegrees(lat, lon, 0), basicMarkerAttributes,
                info);
        addMarker(marker);
    }

    @Override
    public String toString() {
        if (title != null) {
            return title;
        }
        return "Simple Points";
    }

}
