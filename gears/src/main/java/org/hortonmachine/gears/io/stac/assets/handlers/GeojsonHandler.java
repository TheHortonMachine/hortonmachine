package org.hortonmachine.gears.io.stac.assets.handlers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

import com.fasterxml.jackson.databind.JsonNode;

public class GeojsonHandler implements IHMStacAssetHandler {
	public final static String[] ACCEPTED_TYPES = { "application/geo+json", "application/vnd.geo+json", "application/json", "application/geojson" };

	private HMStacAsset asset;
	private JsonNode assetNode;
	private boolean supported = false;
	private String assetUrl;

	@Override
	public void initialize(HMStacAsset asset) throws IOException {
		this.asset = asset;
		this.assetNode = asset.getAssetNode();
		this.assetUrl = assetNode.get("href").textValue();
		for (String acceptedType : ACCEPTED_TYPES) {
			if (asset.getType().toLowerCase().contains(acceptedType)) {
				supported = true;
				break;
			}
		}
	}

	private void checkSupported() {
		if (!supported) {
			throw new UnsupportedOperationException(
					"Asset type " + asset.getType() + " is not supported by Geojson Handler.");
		}
	}

	@Override
	public String getAssetUrl() {
		return assetUrl;
	}

	@Override
	public boolean supports() {
		for (String acceptedType : ACCEPTED_TYPES) {
			if (asset.getType().toLowerCase().contains(acceptedType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <T> T read(Class<T> targetType, IHMProgressMonitor monitor) throws Exception {
		checkSupported();
		if (targetType.isAssignableFrom(SimpleFeatureCollection.class) || targetType.isAssignableFrom(File.class)) {
			// download the geopackage file
			File tempFile = File.createTempFile("stac_asset_", ".geojson");
			tempFile.deleteOnExit();
			// code to download the file from assetUrl to tempFile
			downloadAsset(tempFile.getAbsolutePath(), monitor);

			if (!targetType.isAssignableFrom(SimpleFeatureCollection.class)) {
				// return the file itself
				return targetType.cast(tempFile);
			}

			SimpleFeatureCollection featureCollection = OmsVectorReader.readVector(tempFile.getAbsolutePath());
			return targetType.cast(featureCollection);
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
}
