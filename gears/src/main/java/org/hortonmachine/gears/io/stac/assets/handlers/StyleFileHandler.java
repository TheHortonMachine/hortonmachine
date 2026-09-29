/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.io.stac.assets.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.geotools.api.style.Style;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.SldUtilities;

/**
 * Handler for style assets: OGC SLD and QGIS QML files.
 *
 * <p>Supported target types:</p>
 * <ul>
 * <li>{@link String}: the xml content of the style</li>
 * <li>{@link File}: the downloaded style file</li>
 * <li>{@link Style}: the geotools style (SLD only, null for QML)</li>
 * </ul>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class StyleFileHandler implements IHMStacAssetHandler {
    public static final String SLD_TYPE = "application/vnd.ogc.sld+xml";
    public static final String QML_TYPE = "application/vnd.qgis.qml+xml";

    /** The supported style formats, also used as file extensions. */
    public enum StyleFormat {
        SLD, QML;

        public String getExtension() {
            return name().toLowerCase();
        }
    }

    private HMStacAsset asset;
    private String assetUrl;
    private StyleFormat format;

    @Override
    public void initialize( HMStacAsset asset ) throws IOException {
        this.asset = asset;
        this.assetUrl = asset.getAssetNode().has("href") ? asset.getAssetNode().get("href").textValue() : null;

        String type = asset.getType();
        if (type != null) {
            String assetType = type.replace(" ", "").toLowerCase();
            if (assetType.contains(SLD_TYPE)) {
                format = StyleFormat.SLD;
            } else if (assetType.contains(QML_TYPE)) {
                format = StyleFormat.QML;
            }
        } else if (assetUrl != null) {
            // no media type declared, fall back to the file extension
            String path = assetUrl.toLowerCase();
            int queryIndex = path.indexOf('?');
            if (queryIndex > 0)
                path = path.substring(0, queryIndex);
            if (path.endsWith(".sld")) {
                format = StyleFormat.SLD;
            } else if (path.endsWith(".qml")) {
                format = StyleFormat.QML;
            }
        }
    }

    @Override
    public boolean supports() {
        return format != null;
    }

    /**
     * @return the style format of the asset or null if not supported.
     */
    public StyleFormat getFormat() {
        return format;
    }

    @Override
    public String getAssetUrl() {
        return assetUrl;
    }

    @Override
    public <T> T read( Class<T> targetType, IHMProgressMonitor monitor ) throws Exception {
        checkSupported();
        if (targetType.isAssignableFrom(File.class)) {
            return targetType.cast(downloadToTempFile(monitor));
        } else if (targetType.isAssignableFrom(String.class)) {
            return targetType.cast(readString(monitor));
        } else if (targetType.isAssignableFrom(Style.class)) {
            if (format == StyleFormat.SLD) {
                return targetType.cast(SldUtilities.getStyleFromSldString(readString(monitor)));
            }
        }
        return null;
    }

    @Override
    public <T> Map<String, T> readAll( Class<T> targetType, IHMProgressMonitor monitor ) throws Exception {
        // a style asset is always a single object
        Map<String, T> objectsMap = new HashMap<>();
        T result = read(targetType, monitor);
        if (result != null) {
            objectsMap.put(asset.getId(), result);
        }
        return objectsMap;
    }

    private String readString( IHMProgressMonitor monitor ) throws Exception {
        File file = downloadToTempFile(monitor);
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } finally {
            file.delete();
        }
    }

    private File downloadToTempFile( IHMProgressMonitor monitor ) throws Exception {
        File tempFile = File.createTempFile("stac_style_", "." + format.getExtension());
        downloadAsset(tempFile.getAbsolutePath(), monitor);
        return tempFile;
    }

    private void checkSupported() {
        if (format == null) {
            throw new UnsupportedOperationException("Asset type " + asset.getType() + " is not supported by the style handler.");
        }
    }
}
