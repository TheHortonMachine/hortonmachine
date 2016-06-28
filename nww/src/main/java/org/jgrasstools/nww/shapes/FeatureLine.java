package org.jgrasstools.nww.shapes;

import java.util.List;

import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Path;

public class FeatureLine extends Path implements IFeatureShape {
    private SimpleFeature feature;
    private SimpleFeatureStore featureStore;

    public FeatureLine( List<Position> verticesList, SimpleFeatureStore featureStore ) {
        super(verticesList);
        this.featureStore = featureStore;
    }

    public SimpleFeature getFeature() {
        return feature;
    }

    public void setFeature( SimpleFeature feature ) {
        this.feature = feature;
    }

    public SimpleFeatureStore getFeatureStore() {
        return featureStore;
    }


}
