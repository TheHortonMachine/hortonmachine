package org.hortonmachine.gears.io.stac.assets;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.RegionMap;

import com.github.davidmoten.aws.lw.client.Client;

public interface IHMStacAssetRasterHandler extends IHMStacAssetHandler {
	double getResolution();
	double getNoValue();
	
	GridCoverage2D readRaster(RegionMap region) throws Exception;
	GridCoverage2D readRaster(RegionMap region, Client client) throws Exception;

	GridCoverage2D readRaster(RegionMap region, String user, String password) throws Exception;
	GridCoverage2D readRaster(RegionMap region, String user, String password, Client client) throws Exception;
}
