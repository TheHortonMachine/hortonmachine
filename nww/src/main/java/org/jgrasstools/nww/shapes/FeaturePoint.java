package org.jgrasstools.nww.shapes;

import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.PointPlacemark;

public class FeaturePoint extends PointPlacemark implements IFeatureShape {
    private SimpleFeature feature;
    private SimpleFeatureStore featureStore;

    public FeaturePoint( Position position, SimpleFeatureStore featureStore ) {
        super(position);
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
