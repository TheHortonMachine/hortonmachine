package org.hortonmachine.gears.io.stac.assets.handlers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.utils.TableName;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CompressionUtilities;

import com.fasterxml.jackson.databind.JsonNode;

public class GeopackageVectorHandler implements IHMStacAssetHandler {
	public final static String[] ACCEPTED_TYPES = { "application/geopackage+sqlite3", "application/x-sqlite3",
			"application/geopackage", "application/x.geopackage+zip" };

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
					"Asset type " + asset.getType() + " is not supported by Geopackage Handler.");
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
			boolean isZipped = asset.getType().toLowerCase().contains("zip");
			// download the geopackage file
			String suffix = ".gpkg";
			if (isZipped) {
				suffix = ".gpkg.zip";
			}
			File tempFile = File.createTempFile("stac_asset_", suffix);
			// code to download the file from assetUrl to tempFile
			downloadAsset(tempFile.getAbsolutePath(), monitor);

			if (targetType.isAssignableFrom(File.class)) {
				// return the file itself
				return targetType.cast(tempFile);
			}
			
			// if geopackage is zipped, unzip it first
			File geopackageFile;
			if(isZipped) {
				geopackageFile = CompressionUtilities.unzipSingleFile(tempFile.getAbsolutePath(), tempFile.getParentFile().getAbsolutePath(), true);
			} else {
				geopackageFile = tempFile;
			}
			geopackageFile.deleteOnExit();

			// else we need to read the vector data from the geopackage
			String firstSpatialTable = null;
			try (ASpatialDb spatialDb = EDb.GEOPACKAGE.getSpatialDb()) {
				spatialDb.open(geopackageFile.getAbsolutePath());
				List<TableName> tables = spatialDb.getTables();
				// get the first spatial table
				for (TableName table : tables) {
					if (spatialDb.getGeometryColumnsForTable(table.toSqlName()) != null) {
						firstSpatialTable = table.getName();
						break;
					}
				}
			}
			if (firstSpatialTable != null) {
				SimpleFeatureCollection featureCollection = OmsVectorReader
						.readVector(geopackageFile.getAbsolutePath() + "#" + firstSpatialTable);
				return targetType.cast(featureCollection);
			}

			return null;
		}
		return null;
	}

	@Override
	public <T> Map<String, T> readAll(Class<T> targetType, IHMProgressMonitor monitor) throws Exception {
		Map<String, T> objectsMap = new HashMap<>();
		checkSupported();
		if (targetType.isAssignableFrom(SimpleFeatureCollection.class) || targetType.isAssignableFrom(File.class)) {
			boolean isZipped = asset.getType().toLowerCase().contains("zip");
			// download the geopackage file
			String suffix = ".gpkg";
			if (isZipped) {
				suffix = ".gpkg.zip";
			}
			File tempFile = File.createTempFile("stac_asset_", suffix);
			// code to download the file from assetUrl to tempFile
			downloadAsset(tempFile.getAbsolutePath(), monitor);

			if (targetType.isAssignableFrom(File.class)) {
				// return the file itself
				objectsMap.put(asset.getId(), targetType.cast(tempFile));
			}
			
			// if geopackage is zipped, unzip it first
			File geopackageFile;
			if(isZipped) {
				geopackageFile = CompressionUtilities.unzipSingleFile(tempFile.getAbsolutePath(), tempFile.getParentFile().getAbsolutePath(), true);
			} else {
				geopackageFile = tempFile;
			}
			geopackageFile.deleteOnExit();

			// else we need to read the vector data from the geopackage
			try (ASpatialDb spatialDb = EDb.GEOPACKAGE.getSpatialDb()) {
				spatialDb.open(geopackageFile.getAbsolutePath());
				List<TableName> tables = spatialDb.getTables();
				// get the first spatial table
				for (TableName table : tables) {
					if (spatialDb.getGeometryColumnsForTable(table.toSqlName()) != null) {
						SimpleFeatureCollection featureCollection = OmsVectorReader
								.readVector(geopackageFile.getAbsolutePath() + "#" + table.getName());
						objectsMap.put(table.getName(), targetType.cast(featureCollection));
					}
				}
			}
		}
		
		return objectsMap;
	}
}
