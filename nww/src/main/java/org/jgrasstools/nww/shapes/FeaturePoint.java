package org.jgrasstools.nww.shapes;

import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;

public class FeaturePoint extends BasicMarker implements IFeatureShape{
    public FeaturePoint(Position position, MarkerAttributes attrs) {
        super(position, attrs);
    }

    private SimpleFeature feature;

    public SimpleFeature getFeature() {
        return feature;
    }

    public void setFeature(SimpleFeature feature) {
        this.feature = feature;
    }
}
