package org.hortonmachine.gears.io.stac.assets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

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

	String getAssetUrl();

	default void downloadAsset(String destinationPath, IHMProgressMonitor monitor) throws Exception {
		Path targetFile = Paths.get(destinationPath);
		String href = getAssetUrl();
		if (href == null || href.trim().isEmpty()) {
			throw new IllegalArgumentException("Asset href is null or empty");
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

		// else treat as HTTP(S) – same code as above…
		URL url = new URL(href);
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(30_000);
		connection.setReadTimeout(60_000);

		long contentLength = connection.getContentLengthLong();

		if (monitor != null) {
			monitor.beginTask("Downloading asset: " + href, IHMProgressMonitor.UNKNOWN);
		}

		Files.createDirectories(targetFile.getParent());

		try (InputStream is = new BufferedInputStream(connection.getInputStream());
				OutputStream os = new BufferedOutputStream(Files.newOutputStream(targetFile))) {

			byte[] buffer = new byte[8192];
			long totalRead = 0;
			int read;

			while ((read = is.read(buffer)) != -1) {
				os.write(buffer, 0, read);
				totalRead += read;

				if (monitor != null) {
					if (contentLength > 0) {
						int percent = (int) (100L * totalRead / contentLength);
						monitor.message("Downloaded: " + percent + "%...");
					}
					if (monitor.isCanceled()) {
						throw new IOException("Download canceled by user");
					}
				}
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

}
