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
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetRasterHandler;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.crs.CrsUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.davidmoten.aws.lw.client.Client;

/**
 * Handler of the ESRI ASCII grids (.asc).
 *
 * <p>There is no registered media type for them: the assets are recognized by the extension of their href, if their
 * type is missing or generic text. An ASCII grid can't be read in parts, so it is always downloaded whole and read
 * locally, with the projection of the asset (<code>proj:code</code>), if any, as its .prj.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class AsciiGridHandler implements IHMStacAssetRasterHandler {
    public static final String EXTENSION = ".asc";
    /** The types an ASCII grid is given, besides none. */
    public static final String[] ACCEPTED_TYPES = {"text/plain", "application/octet-stream"};

    private HMStacAsset asset;
    private String assetUrl;
    private boolean supported = false;
    private double noValue = HMConstants.doubleNovalue;
    private double resolution;

    @Override
    public void initialize( HMStacAsset asset ) throws IOException {
        this.asset = asset;
        JsonNode assetNode = asset.getAssetNode();
        JsonNode hrefNode = assetNode.get("href");
        assetUrl = hrefNode != null ? hrefNode.textValue() : null;
        if (assetUrl == null || !fileName(assetUrl).toLowerCase().endsWith(EXTENSION)) {
            return;
        }
        String type = asset.getType();
        if (type == null) {
            supported = true;
        } else {
            String assetType = type.replace(" ", "").toLowerCase();
            for( String acceptedType : ACCEPTED_TYPES ) {
                if (assetType.startsWith(acceptedType)) {
                    supported = true;
                    break;
                }
            }
        }
        if (supported) {
            Double assetNoValue = GeotiffHandler.readNoValue(assetNode);
            if (assetNoValue != null) {
                noValue = assetNoValue;
            }
            JsonNode bands = assetNode.get("raster:bands");
            if (bands != null) {
                Iterator<JsonNode> iterator = bands.elements();
                while( iterator.hasNext() ) {
                    JsonNode resolutionNode = iterator.next().get("spatial_resolution");
                    if (resolutionNode != null) {
                        resolution = resolutionNode.asDouble();
                    }
                }
            }
        }
    }

    /**
     * @return the name of the file of a url, without query and fragment.
     */
    static String fileName( String url ) {
        String path = url;
        int end = path.length();
        for( char separator : new char[]{'?', '#'} ) {
            int index = path.indexOf(separator);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        path = path.substring(0, end);
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public boolean supports() {
        return supported;
    }

    @Override
    public HMStacAsset getAsset() {
        return asset;
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

    private void checkSupported() {
        if (!supported) {
            throw new UnsupportedOperationException("Asset type " + asset.getType() + " is not supported by AsciiGridHandler.");
        }
    }

    /**
     * Download the grid into a new temporary folder, with its projection as .prj if the asset gives one.
     *
     * @return the downloaded grid.
     */
    private File download( IHMProgressMonitor monitor ) throws Exception {
        File folder = Files.createTempDirectory("hm-stac-asc").toFile();
        folder.deleteOnExit();
        String name = fileName(assetUrl);
        File grid = new File(folder, name.isEmpty() ? "asset" + EXTENSION : name);
        grid.deleteOnExit();
        downloadAsset(grid.getAbsolutePath(), monitor);
        Integer epsg = asset.getEpsg();
        if (epsg != null) {
            CrsUtilities.writeProjectionFile(grid.getAbsolutePath(), "asc", HMCrsRegistry.INSTANCE.getCrs("EPSG:" + epsg, true));
            File prj = new File(folder, grid.getName().substring(0, grid.getName().length() - EXTENSION.length()) + ".prj");
            prj.deleteOnExit();
        }
        return grid;
    }

    @Override
    public <T> T read( Class<T> targetType, IHMProgressMonitor monitor ) throws Exception {
        checkSupported();
        if (targetType.isAssignableFrom(GridCoverage2D.class)) {
            return targetType.cast(readRaster(null));
        } else if (targetType.isAssignableFrom(File.class)) {
            return targetType.cast(download(monitor));
        }
        return null;
    }

    @Override
    public <T> Map<String, T> readAll( Class<T> targetType, IHMProgressMonitor monitor ) throws Exception {
        Map<String, T> objectsMap = new HashMap<>();
        T result = read(targetType, monitor);
        if (result != null) {
            objectsMap.put(asset.getId(), result);
        }
        return objectsMap;
    }

    /**
     * Read the grid, downloaded whole through the access of the asset.
     *
     * @param region an optional region to read, else all of it.
     * @param user ignored: the authentication is the one of the access of the asset.
     * @param password ignored.
     * @param client ignored, as ASCII grids are not read from S3.
     */
    @Override
    public GridCoverage2D readRaster( RegionMap region, String user, String password, Client client ) throws Exception {
        checkSupported();
        File grid = download(null);
        OmsRasterReader reader = new OmsRasterReader();
        reader.file = grid.getAbsolutePath();
        if (region != null) {
            reader.pNorth = region.getNorth();
            reader.pSouth = region.getSouth();
            reader.pWest = region.getWest();
            reader.pEast = region.getEast();
            reader.pXres = region.getXres();
            reader.pYres = region.getYres();
        }
        reader.process();
        return reader.outRaster;
    }

    @Override
    public GridCoverage2D readRaster( RegionMap region ) throws Exception {
        return readRaster(region, null, null, null);
    }

    @Override
    public GridCoverage2D readRaster( RegionMap region, String user, String password ) throws Exception {
        return readRaster(region, user, password, null);
    }

    @Override
    public GridCoverage2D readRaster( RegionMap region, Client client ) throws Exception {
        return readRaster(region, null, null, client);
    }
}
