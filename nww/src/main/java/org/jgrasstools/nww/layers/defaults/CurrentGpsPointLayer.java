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

import org.jgrasstools.nww.layers.MarkerLayer;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;

/**
 * An updatable GPS position layer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CurrentGpsPointLayer extends MarkerLayer {

    private BasicMarkerAttributes basicMarkerAttributes;
    private BasicMarker gpsMarker;
    private String title;

    public CurrentGpsPointLayer(String title, Color color, String shape, Double _fillOpacity, Double _markerSize) {
        this.title = title;
        Material fillMaterial = Material.BLUE;
        if (color != null) {
            fillMaterial = new Material(color);
        }
        String shapeType = BasicMarkerShape.SPHERE;
        if (shape != null) {
            shapeType = shape;
        }
        double fillOpacity = 1d;
        if (_fillOpacity != null) {
            fillOpacity = _fillOpacity;
        }
        double markerSize = 10d;
        if (_markerSize != null) {
            markerSize = _markerSize;
        }
        basicMarkerAttributes = new BasicMarkerAttributes(fillMaterial, shapeType, fillOpacity);
        basicMarkerAttributes.setMarkerPixels(markerSize);
        basicMarkerAttributes.setMinMarkerSize(0.1);

        setOverrideMarkerElevation(true);
        setElevation(1);

        setMarkers(new ArrayList<Marker>());
    }

    public void updatePosition(double lat, double lon) {
        if (gpsMarker == null) {
            gpsMarker = new BasicMarker(Position.fromDegrees(lat, lon, 0), basicMarkerAttributes);
            addMarker(gpsMarker);
        } else {
            gpsMarker.setPosition(Position.fromDegrees(lat, lon, 0));
        }
    }

    @Override
    public String toString() {
        if (title != null) {
            return title;
        }
        return "Current GPS Position";
    }

}
