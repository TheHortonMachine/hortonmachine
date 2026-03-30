package org.hortonmachine.gears.io.stac.assets.handlers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.PlanetaryComputerMicrosoft;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetRasterHandler;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidmoten.aws.lw.client.Client;

import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.CogImageInputStreamSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogSourceSPIProvider;
import it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader;
import it.geosolutions.imageioimpl.plugins.cog.RangeReader;

public class GeotiffHandler implements IHMStacAssetRasterHandler {
	public final static String[] ACCEPTED_TYPES = { 
			"image/tiff;application=geotiff", //
			"image/vnd.stac.geotiff", //
			"image/tiff;application=geotiff;profile=cloud-optimized", //
			"image/vnd.stac.geotiff;profile=cloud-optimized", //
			"image/vnd.stac.geotiff;cloud-optimized=true" //
			};

	private HMStacAsset asset;
	private double noValue = HMConstants.doubleNovalue;
	private double resolution;
	private JsonNode assetNode;
	private boolean supported = false;
	private String assetUrl;

	@Override
	public void initialize(HMStacAsset asset) throws IOException {
		this.asset = asset;
		this.assetNode = asset.getAssetNode();
		this.assetUrl = assetNode.get("href").textValue();
		String type = asset.getType();
		if(type==null) {
			return;
		}
		String assetType = type.replace(" ", "").toLowerCase();
		for (String acceptedType : ACCEPTED_TYPES) {
			String checkType = acceptedType.replace(" ", "").toLowerCase();
			if (assetType.contains(checkType)) {
				supported = true;
				break;
			}
		}
		if (supported) {
			JsonNode rasterBandNode = assetNode.get("raster:bands");
			if (rasterBandNode != null && !rasterBandNode.isEmpty()) {
				Iterator<JsonNode> rbIterator = rasterBandNode.elements();
				while (rbIterator.hasNext()) {
					JsonNode rbNode = rbIterator.next();
					JsonNode noValueNode = rbNode.get("nodata");
					if (noValueNode != null) {
						noValue = noValueNode.asDouble();
					}
					JsonNode resolNode = rbNode.get("spatial_resolution");
					if (resolNode != null) {
						resolution = resolNode.asDouble();
					}
				}
			}
		}
	}

	@Override
	public boolean supports() {
		return supported;
	}
	
	@Override
	public String getAssetUrl() {
		return assetUrl;
	}

	@Override
	public double getNoValue() {
		return noValue;
	}

	@Override
	public double getResolution() {
		return resolution;
	}

	@Override
	public <T> T read(Class<T> targetType, IHMProgressMonitor monitor) throws Exception {
		checkSupported();
		if (targetType.isAssignableFrom(GridCoverage2D.class)) {
		    return targetType.cast(readRaster(null));
		} else if (targetType.isAssignableFrom(File.class)) {
			// download the asset to a temporary file and return it
			File tempFile = File.createTempFile("geotiff_asset_", ".tif");
			downloadAsset(tempFile.getAbsolutePath(), monitor);
			return targetType.cast(tempFile);
		} else if (targetType.isAssignableFrom(InputStream.class)) {
			BasicAuthURI cogUri = new BasicAuthURI(assetUrl, false);
			return targetType.cast(readS3Raster(cogUri, null));
		}
		return null;
	}
	
	@Override
	public <T> Map<String, T> readAll(Class<T> targetType, IHMProgressMonitor monitor) throws Exception {
		// at the moment we fallback to the single result
		Map<String, T> objectsMap = new HashMap<>();
		T result = read(targetType, monitor);
		if(result!=null) {
			objectsMap.put(asset.getId(), result);
		}
		return objectsMap;
	}

	private void checkSupported() {
		if (!supported) {
			throw new UnsupportedOperationException(
					"Asset type " + asset.getType() + " is not supported by GeotiffHandler.");
		}
	}

	/**
	 * Read the asset's coverage into a local raster.
	 * 
	 * @param region   and optional region to read from.
	 * @param user     an optional user in case of authentication.
	 * @param password an optional password in case of authentication.
	 * @param client   an optional client in case of S3 authentication
	 * @return the read raster from the asset's url
	 * @throws Exception
	 */
	@Override
	public GridCoverage2D readRaster(RegionMap region, String user, String password, Client client) throws Exception {
		checkSupported();
		BasicAuthURI cogUri = new BasicAuthURI(assetUrl, false);
		if (user != null && password != null) {
			cogUri.setUser(user);
			cogUri.setPassword(password);
		}
		GeoTiffReader reader;
		if (assetUrl.startsWith("s3://")) { // TODO manage multiple S3 servers
			InputStream inputProvider = readS3Raster(cogUri, client);
			reader = new GeoTiffReader(inputProvider);
		} else {
			if (PlanetaryComputerMicrosoft.isAzureBlob(assetUrl)) {
				String accessibleHref = PlanetaryComputerMicrosoft.getHrefWithToken(assetUrl);
				cogUri = new BasicAuthURI(accessibleHref, false);
			}

			RangeReader rangeReader = new HttpRangeReader(cogUri.getUri(), CogImageReadParam.DEFAULT_HEADER_LENGTH);
			CogSourceSPIProvider inputProvider = new CogSourceSPIProvider(cogUri, new CogImageReaderSpi(),
					new CogImageInputStreamSpi(), rangeReader.getClass().getName());
			reader = new GeoTiffReader(inputProvider);
		}
		CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();

		GeneralParameterValue[] generalParameter = null;
		if (region != null) {
			generalParameter = CoverageUtilities.createGridGeometryGeneralParameter(region, crs);
		}
        return reader.read(generalParameter);
	}

	public InputStream readS3Raster(BasicAuthURI cogUri, Client client) {
		String[] bucketAndObject = assetUrl.split("://")[1].split("/", 2);

		return client.path(bucketAndObject[0], bucketAndObject[1]).responseInputStream();
	}

	@Override
	public GridCoverage2D readRaster(RegionMap region) throws Exception {
		return readRaster(region, null, null, null);
	}

	@Override
	public GridCoverage2D readRaster(RegionMap region, String user, String password) throws Exception {
		return readRaster(region, user, password, null);
	}

	@Override
	public GridCoverage2D readRaster(RegionMap region, Client client) throws Exception {
		return readRaster(region, null, null, client);
	}

}
