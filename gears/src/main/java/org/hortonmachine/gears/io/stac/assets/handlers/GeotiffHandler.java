package org.hortonmachine.gears.io.stac.assets.handlers;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.stream.ImageInputStream;

import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
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
import it.geosolutions.imageioimpl.plugins.cog.CogImageReader;
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
	
	/**
	 * Static helper method to read a cog on a specific geographic region, without needing to read the whole coverage.
	 * 
	 * This method is counterintuitive but necessary, as the geotools reader alone does nto seem to work properly 
	 * with the COG reader when reading out ranges. 
	 * TODO check what we are doing wrong with geotools.
	 * 
	 * @param assetUrl the url of the geotiff asset to read.
	 * @param north the north coordinate of the region to read.
	 * @param south the south coordinate of the region to read.
	 * @param west the west coordinate of the region to read.
	 * @param east the east coordinate of the region to read.
	 * @return the read raster.
	 * @throws Exception
	 */	
	public static GridCoverage2D readCogOnRegion(
	        String url,
	        double north,
	        double south,
	        double west,
	        double east) throws Exception {

	    BasicAuthURI cogUri = new BasicAuthURI(url, false);

	    HttpRangeReader rangeReader =
	            new HttpRangeReader(cogUri.getUri(), CogImageReadParam.DEFAULT_HEADER_LENGTH);

	    CogSourceSPIProvider inputProvider = new CogSourceSPIProvider(
	            cogUri,
	            new CogImageReaderSpi(),
	            new CogImageInputStreamSpi(),
	            rangeReader.getClass().getName()
	    );

	    // Use GeoTiffReader only through the COG-aware provider
	    GeoTiffReader metaReader = new GeoTiffReader(inputProvider);

	    CoordinateReferenceSystem crs = metaReader.getCoordinateReferenceSystem();
	    ReferencedEnvelope fullEnv = new ReferencedEnvelope(metaReader.getOriginalEnvelope());

	    int fullWidth = metaReader.getOriginalGridRange().getSpan(0);
	    int fullHeight = metaReader.getOriginalGridRange().getSpan(1);

	    double west0 = fullEnv.getMinX();
	    double east0 = fullEnv.getMaxX();
	    double south0 = fullEnv.getMinY();
	    double north0 = fullEnv.getMaxY();

	    double cellWidth = (east0 - west0) / fullWidth;
	    double cellHeight = (north0 - south0) / fullHeight;

	    // 2) Convert requested geographic region to pixel rectangle
	    int x = (int) Math.floor((west - west0) / cellWidth);
	    int y = (int) Math.floor((north0 - north) / cellHeight);

	    int maxX = (int) Math.ceil((east - west0) / cellWidth);
	    int maxY = (int) Math.ceil((north0 - south) / cellHeight);

	    int w = maxX - x;
	    int h = maxY - y;

	    // clip to raster bounds
	    if (x < 0) x = 0;
	    if (y < 0) y = 0;
	    if (x + w > fullWidth) w = fullWidth - x;
	    if (y + h > fullHeight) h = fullHeight - y;

	    Rectangle sourceRegion = new Rectangle(x, y, w, h);

	    ImageInputStream cogStream =
	            new CogImageInputStreamSpi().createInputStreamInstance(cogUri);

	    CogImageReader reader = new CogImageReader(new CogImageReaderSpi());
	    reader.setInput(cogStream);

	    CogImageReadParam param = new CogImageReadParam();
	    param.setSourceRegion(sourceRegion);
	    param.setRangeReaderClass(HttpRangeReader.class);

	    BufferedImage image = reader.read(0, param);

	    // 4) Build the exact geographic envelope of the cropped rectangle
	    double croppedWest = west0 + x * cellWidth;
	    double croppedEast = west0 + (x + w) * cellWidth;
	    double croppedNorth = north0 - y * cellHeight;
	    double croppedSouth = north0 - (y + h) * cellHeight;

	    ReferencedEnvelope croppedEnv = new ReferencedEnvelope(
	            croppedWest, croppedEast, croppedSouth, croppedNorth, crs);

	    // 5) Convert BufferedImage -> GridCoverage2D
	    GridCoverageFactory factory = new GridCoverageFactory();
	    return factory.create("cog_crop", image, croppedEnv);
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
