package org.hortonmachine.nww.shapes;

import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.PointPlacemark;

public class FeaturePoint extends PointPlacemark implements IFeatureShape {
    private SimpleFeature feature;
    private FeatureStoreInfo featureStoreInfo;

    public FeaturePoint( Position position, FeatureStoreInfo featureStoreInfo ) {
        super(position);
        this.featureStoreInfo = featureStoreInfo;
    }

    public SimpleFeature getFeature() {
        return feature;
    }

    public void setFeature( SimpleFeature feature ) {
        this.feature = feature;
    }

    @Override
    public FeatureStoreInfo getFeatureStoreInfo() {
        return featureStoreInfo;
    }
}
