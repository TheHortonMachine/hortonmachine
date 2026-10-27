package org.hortonmachine.gears.io.stac.assets;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.RegionMap;

import com.github.davidmoten.aws.lw.client.Client;

public interface IHMStacAssetRasterHandler extends IHMStacAssetHandler {
	double getResolution();
	double getNoValue();

	/**
	 * Read the CRS from the raster file itself, for assets that don't declare it in the catalog.
	 *
	 * @return the crs or null if the format doesn't support reading it without the whole file.
	 * @throws Exception
	 */
	default CoordinateReferenceSystem readCrs() throws Exception {
		return null;
	}

	GridCoverage2D readRaster(RegionMap region) throws Exception;
	GridCoverage2D readRaster(RegionMap region, Client client) throws Exception;

	GridCoverage2D readRaster(RegionMap region, String user, String password) throws Exception;
	GridCoverage2D readRaster(RegionMap region, String user, String password, Client client) throws Exception;
}
