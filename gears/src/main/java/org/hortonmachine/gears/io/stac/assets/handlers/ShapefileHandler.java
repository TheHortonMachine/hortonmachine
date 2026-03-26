package org.hortonmachine.gears.io.stac.assets.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CompressionUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

import com.fasterxml.jackson.databind.JsonNode;

public class ShapefileHandler implements IHMStacAssetHandler {
	public final static String[] ACCEPTED_TYPES = { "application/x.shapefile+zip" };

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
			if (asset.getType()!=null && asset.getType().toLowerCase().contains(acceptedType)) {
				supported = true;
				break;
			}
		}
	}

	private void checkSupported() {
		if (!supported) {
			throw new UnsupportedOperationException(
					"Asset type " + asset.getType() + " is not supported by Shapefile Handler.");
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

			File tempDir = Files.createTempDirectory("stac_asset_").toFile();

			// download the zipped shapefile file
			File tempFile = new File(tempDir, "stac_asset.zip");
			// code to download the file from assetUrl to tempFile
			downloadAsset(tempFile.getAbsolutePath(), monitor);

			if (targetType.isAssignableFrom(File.class)) {
				// return the file itself
				return targetType.cast(tempFile);
			}

			// unzip the file
			CompressionUtilities.unzipFolder(tempFile.getAbsolutePath(), tempDir.getAbsolutePath(), false);

			// find the shapefile inside the folder
			List<File> shpFiles = FileUtilities.findFilesByPattern(tempDir.getAbsolutePath(), ".*\\.shp$");
			if (shpFiles.size() > 1) {
				monitor.errorMessage("WARNING: Returning only first resource of " + shpFiles.size() + ". Use readAll.");
			}
			if (shpFiles.size() > 0) {
				File shpFile = shpFiles.get(0);

				OmsVectorReader reader = new OmsVectorReader();
				reader.file = shpFile.getAbsolutePath();
				reader.pm = monitor;
				reader.process();
				SimpleFeatureCollection featureCollection = reader.outVector;
				return targetType.cast(featureCollection);
			}
		}
		return null;
	}

	@Override
	public <T> Map<String, T> readAll(Class<T> targetType, IHMProgressMonitor monitor) throws Exception {
		Map<String, T> objectsMap = new HashMap<>();
		checkSupported();
		if (targetType.isAssignableFrom(SimpleFeatureCollection.class) || targetType.isAssignableFrom(File.class)) {

			File tempDir = Files.createTempDirectory("stac_asset_").toFile();

			// download the zipped shapefile file
			File tempFile = new File(tempDir, "stac_asset.zip");
			// code to download the file from assetUrl to tempFile
			downloadAsset(tempFile.getAbsolutePath(), monitor);

			if (targetType.isAssignableFrom(File.class)) {
				// return the file itself
				objectsMap.put(asset.getId(), targetType.cast(tempFile));
			} else {
				// unzip the file
				CompressionUtilities.unzipFolder(tempFile.getAbsolutePath(), tempDir.getAbsolutePath(), false);

				// find the shapefile inside the folder
				List<File> shpFiles = FileUtilities.findFilesByPattern(tempDir.getAbsolutePath(), ".*\\.shp$");
				for (File shpFile : shpFiles) {
					OmsVectorReader reader = new OmsVectorReader();
					reader.file = shpFile.getAbsolutePath();
					reader.pm = monitor;
					reader.process();
					SimpleFeatureCollection featureCollection = reader.outVector;
					
					// get name from file
					String nameWithoutExtention = FileUtilities.getNameWithoutExtention(shpFile);
					objectsMap.put(nameWithoutExtention, targetType.cast(featureCollection));
				}
			}
		}
		return objectsMap;
	}
}
