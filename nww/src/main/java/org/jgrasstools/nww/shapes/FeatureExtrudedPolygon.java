package org.jgrasstools.nww.shapes;

import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.render.ExtrudedPolygon;

public class FeatureExtrudedPolygon extends ExtrudedPolygon implements IFeatureShape {
    private SimpleFeature feature;
    private SimpleFeatureStore featureStore;

    public FeatureExtrudedPolygon( SimpleFeatureStore featureStore ) {
        this.featureStore = featureStore;
    }

    public SimpleFeature getFeature() {
        return feature;
    }

    public void setFeature( SimpleFeature feature ) {
        this.feature = feature;
    }

    @Override
    public SimpleFeatureStore getFeatureStore() {
        return featureStore;
    }
}
