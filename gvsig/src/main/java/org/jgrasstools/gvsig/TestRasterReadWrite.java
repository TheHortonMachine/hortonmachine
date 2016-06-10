package org.jgrasstools.gvsig;

import java.io.File;

import org.cresques.cts.IProjection;

import org.gvsig.fmap.dal.DALLocator;
import org.gvsig.fmap.dal.DataManager;
import org.gvsig.fmap.dal.coverage.RasterLocator;
import org.gvsig.fmap.dal.coverage.dataset.Buffer;
import org.gvsig.fmap.dal.coverage.store.RasterDataStore;
import org.gvsig.fmap.dal.coverage.store.RasterQuery;
import org.gvsig.fmap.dal.coverage.store.parameter.RasterDataParameters;
import org.gvsig.fmap.dal.coverage.store.parameter.RasterFileStoreParameters;
import org.gvsig.tools.library.impl.DefaultLibrariesInitializer;

/**
 * 
 * This is just a test.
 * 
 * It needs the plugin containing DefaultGdalIOLibrary and all its dependencies.
 * 
 *  Also need native libs and set GDAL_DATA:
 *  
 *  export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/home/hydrologis/SOFTWARE/GVSIG/gvSIG-desktop-2.3.0-2426-hydrologis/gvSIG/extensiones/org.gvsig.gdal.app.mainplugin/gdal:"
 *  export GDAL_DATA="/home/hydrologis/SOFTWARE/GVSIG/gvSIG-desktop-2.3.0-2426-hydrologis/gvSIG/extensiones/org.gvsig.gdal.app.mainplugin/gdal/data"
 *  
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class TestRasterReadWrite {

    public TestRasterReadWrite() throws Exception {
        new DefaultLibrariesInitializer().fullInitialize(true);

//        String rasterPath = "/home/hydrologis/Dropbox/hydrologis/TMP/dati_esempi/dtm/dtm_flanginec.asc";
        String rasterPath = "/home/hydrologis/data/SPEARFISH/spearfish_topo24_EPSG_4326.tif";

        DataManager manager = DALLocator.getDataManager();
        RasterFileStoreParameters params = (RasterFileStoreParameters) manager.createStoreParameters("Gdal Store");
        params.setFile(new File(rasterPath));
        // params.setSRS(getProjection());
        RasterDataStore rasterDataStore = (RasterDataStore) manager.createStore(params);

        // get the prj
        RasterDataParameters rdParams = ((RasterDataParameters) rasterDataStore.getParameters());
        IProjection crsObject = (IProjection) rdParams.getSRS();

        RasterQuery query = RasterLocator.getManager().createQuery();
        query.setDrawableBands(new int[] { 0 });
        query.setReadOnly(true);
        query.setAreaOfInterest();
        boolean supersamplingLoadingBuffer = false;
        query.setSupersamplingOption(supersamplingLoadingBuffer);
        Buffer buffer = rasterDataStore.query(query);

        int width = buffer.getWidth();
        int height = buffer.getHeight();

        System.out.println(width + "/" + height);
    }

    public static void main(String[] args) throws Exception {
        new TestRasterReadWrite();
    }

}
