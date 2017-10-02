package org.hortonmachine.nww.shapes;

import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Path;

public class FeatureLine extends Path implements IFeatureShape {
    private SimpleFeature feature;
    private FeatureStoreInfo featureStoreInfo;

    public FeatureLine(List<Position> verticesList, FeatureStoreInfo featureStoreInfo) {
        super(verticesList);
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
