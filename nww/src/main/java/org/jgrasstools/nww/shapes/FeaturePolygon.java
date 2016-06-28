package org.jgrasstools.nww.shapes;

import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.render.Polygon;

public class FeaturePolygon extends Polygon implements IFeatureShape {
    private SimpleFeature feature;
    private SimpleFeatureStore featureStore;
    
    public FeaturePolygon(SimpleFeatureStore featureStore) {
        this.featureStore = featureStore;
    }

    public SimpleFeature getFeature() {
        return feature;
    }

    public void setFeature(SimpleFeature feature) {
        this.feature = feature;
    }

    public SimpleFeatureStore getFeatureStore() {
        return featureStore;
    }
}
