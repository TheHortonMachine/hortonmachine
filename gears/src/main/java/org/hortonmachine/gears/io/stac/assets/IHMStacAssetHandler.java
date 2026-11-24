package org.hortonmachine.gears.io.stac.assets;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.hortonmachine.gears.io.stac.auth.HMStacResponse;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.NetworkUtilities;

public interface IHMStacAssetHandler {

	void initialize(HMStacAsset asset) throws IOException;

	/**
	 * Does this handler support reading the asset?
	 */
	boolean supports();

	/**
	 * Read the asset as the requested target type.
	 *
	 * @param targetType the target type
	 * @param monitor    the progress monitor
	 * @return the read object or <code>null</code>, if unable to convert into the
	 *         requested type.
	 */
	<T> T read(Class<T> targetType, IHMProgressMonitor monitor) throws Exception;
	
	/**
	 * Read the asset as the requested target type, allowing also for multiple objects.
	 *
	 * @param targetType the target type
	 * @param monitor    the progress monitor
	 * @return a hashmap containing the name and read object for each object. 
	 */	
	<T> Map<String, T> readAll(Class<T> targetType, IHMProgressMonitor monitor) throws Exception;

	String getAssetUrl();

	/**
	 * @return the asset handled or null if not available.
	 */
	default HMStacAsset getAsset() {
		return null;
	}

	default void downloadAsset(String destinationPath, IHMProgressMonitor monitor) throws Exception {
		Path targetFile = Paths.get(destinationPath);
		String href = getAssetUrl();
		if (href == null || href.trim().isEmpty()) {
			throw new IllegalArgumentException("Asset href is null or empty");
		}

		// protected assets are read through the access context of the asset, the s3 ones without
		// authentication as objects of public buckets
		HMStacAsset asset = getAsset();
		HMStacAccess access = asset != null ? asset.getAccess() : null;
		if ((access != null && access.isAuthenticated(href)) || HMStacAccess.isS3Scheme(href)) {
			if (access == null) {
				access = new HMStacAccess();
			}
			if (monitor != null) {
				monitor.beginTask("Downloading asset: " + href, IHMProgressMonitor.UNKNOWN);
			}
			String storageEndpoint = asset != null ? asset.getStorageEndpoint() : null;
			String storageRegion = asset != null ? asset.getStorageRegion() : null;
			try (HMStacResponse response = access.get(href, storageEndpoint, storageRegion);
					InputStream is = response.getInputStream()) {
				NetworkUtilities.copyToFile(is, response.getContentLength(), targetFile.toFile(), monitor);
			} finally {
				if (monitor != null) {
					monitor.done();
				}
			}
			return;
		}

		// Handle file:// or plain path
		if (href.startsWith("file:")) {
			Path src = Paths.get(URI.create(href));
			Files.createDirectories(targetFile.getParent());
			Files.copy(src, targetFile, StandardCopyOption.REPLACE_EXISTING);
			return ;
		}
		if (!href.startsWith("http://") && !href.startsWith("https://")) {
			// assume plain local path
			Path src = Paths.get(href);
			Files.createDirectories(targetFile.getParent());
			Files.copy(src, targetFile, StandardCopyOption.REPLACE_EXISTING);
			return;
		}

		// else treat as HTTP(S)
		NetworkUtilities.downloadFile(href, targetFile.toFile(), monitor);
	}

}
