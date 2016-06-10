package org.jgrasstools.gvsig;

import java.io.File;

import org.gvsig.fmap.dal.DALLocator;
import org.gvsig.fmap.dal.DataManager;
import org.gvsig.fmap.dal.DataStoreParameters;
import org.gvsig.fmap.dal.feature.FeatureSet;
import org.gvsig.fmap.dal.feature.FeatureStore;
import org.gvsig.fmap.dal.store.shp.SHPStoreProvider;
import org.gvsig.tools.library.impl.DefaultLibrariesInitializer;

public class TestVectorReadWrite {

    public TestVectorReadWrite() throws Exception {
        long t1 = System.currentTimeMillis();
        new DefaultLibrariesInitializer().fullInitialize(true);
        long t2 = System.currentTimeMillis();
        System.out.println("Time to load libs: " + (t2-t1));
        
        String vectorPath = "/home/hydrologis/data/natural_earth/ne_10m_populated_places.shp";
        String epsgCode = "EPSG:4326";

        String providerName = SHPStoreProvider.NAME;

        DataManager manager = DALLocator.getDataManager();
        DataStoreParameters parameters = manager.createStoreParameters(providerName);
        parameters.setDynValue("shpfile", new File(vectorPath));
        parameters.setDynValue("crs", epsgCode);
        FeatureStore openStore = (FeatureStore) manager.openStore(providerName, parameters);

        FeatureSet featureSet = openStore.getFeatureSet();
        long size = featureSet.getSize();
        System.out.println("features count: " + size);

        long t3 = System.currentTimeMillis();
        System.out.println("Time to count: " + (t3-t2));
        
    }

    public static void main(String[] args) throws Exception {
        new TestVectorReadWrite();
    }

}
