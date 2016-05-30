package org.jgrasstools.nww.shapes;

import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.MultiResolutionPath;
import gov.nasa.worldwind.render.Path;

public class FeatureLineMultires extends MultiResolutionPath implements IFeatureShape{
    private SimpleFeature feature;

    public FeatureLineMultires(List<Position> verticesList) {
        super(verticesList);
    }

    public SimpleFeature getFeature() {
        return feature;
    }

    public void setFeature(SimpleFeature feature) {
        this.feature = feature;
    }
}
