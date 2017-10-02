package org.hortonmachine.nww.shapes;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureStore;

public class FeatureStoreInfo {

    private HashMap<String, String[]> field2ValuesMap;
    private SimpleFeatureStore featureStore;

    public FeatureStoreInfo(SimpleFeatureStore featureStore, HashMap<String, String[]> field2ValuesMap) {
        this.featureStore = featureStore;
        this.field2ValuesMap = field2ValuesMap;
    }

    public SimpleFeatureStore getFeatureStore() {
        return featureStore;
    }

    public HashMap<String, String[]> getField2ValuesMap() {
        return field2ValuesMap;
    }
}
