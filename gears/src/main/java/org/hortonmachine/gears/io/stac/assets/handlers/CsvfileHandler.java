package org.hortonmachine.gears.io.stac.assets.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CompressionUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

import com.fasterxml.jackson.databind.JsonNode;

public class CsvfileHandler implements IHMStacAssetHandler {
	public final static String[] ACCEPTED_TYPES = { "application/x.csv+zip" };

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
					"Asset type " + asset.getType() + " is not supported by CSV Handler.");
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
		if (targetType.isAssignableFrom(File.class)) {

			File tempDir = Files.createTempDirectory("stac_asset_").toFile();

			// download the zipped shapefile file
			File tempFile = new File(tempDir, "stac_asset.zip");
			// code to download the file from assetUrl to tempFile
			downloadAsset(tempFile.getAbsolutePath(), monitor);

			// unzip the file
			CompressionUtilities.unzipFolder(tempFile.getAbsolutePath(), tempDir.getAbsolutePath(), false);

			// find the csv inside the folder
			List<File> csvFiles = FileUtilities.findFilesByPattern(tempDir.getAbsolutePath(), ".*\\.csv$");
			if (csvFiles.size() > 1) {
				monitor.errorMessage("WARNING: Returning only first resource of " + csvFiles.size() + ". Use readAll.");
			}
			if (csvFiles.size() > 0) {
				return targetType.cast(csvFiles.get(0));
			}
		}
		return null;
	}

	@Override
	public <T> Map<String, T> readAll(Class<T> targetType, IHMProgressMonitor monitor) throws Exception {
		Map<String, T> objectsMap = new HashMap<>();
		checkSupported();
		if (targetType.isAssignableFrom(File.class)) {

			File tempDir = Files.createTempDirectory("stac_asset_").toFile();

			// download the zipped csv file
			File tempFile = new File(tempDir, "stac_asset.zip");
			// code to download the file from assetUrl to tempFile
			downloadAsset(tempFile.getAbsolutePath(), monitor);

			// unzip the file
			CompressionUtilities.unzipFolder(tempFile.getAbsolutePath(), tempDir.getAbsolutePath(), false);

			// find the shapefile inside the folder
			List<File> csvFiles = FileUtilities.findFilesByPattern(tempDir.getAbsolutePath(), ".*\\.csv$");
			for (File csvFile : csvFiles) {
				// get name from file
				String nameWithoutExtention = FileUtilities.getNameWithoutExtention(csvFile);
				objectsMap.put(nameWithoutExtention, targetType.cast(csvFile));
			}
		}
		return objectsMap;
	}
}
