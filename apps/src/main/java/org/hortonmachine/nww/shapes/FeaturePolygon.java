package org.hortonmachine.nww.shapes;

import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.render.Polygon;

public class FeaturePolygon extends Polygon implements IFeatureShape {
    private SimpleFeature feature;
    private FeatureStoreInfo featureStoreInfo;
    
    public FeaturePolygon(FeatureStoreInfo featureStoreInfo) {
        this.featureStoreInfo = featureStoreInfo;
    }

    public SimpleFeature getFeature() {
        return feature;
    }

    public void setFeature(SimpleFeature feature) {
        this.feature = feature;
    }

    @Override
    public FeatureStoreInfo getFeatureStoreInfo() {
        return featureStoreInfo;
    }
}
