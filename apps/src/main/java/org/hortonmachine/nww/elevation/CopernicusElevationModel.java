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
package org.hortonmachine.nww.elevation;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.nww.utils.cache.CacheUtils;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.terrain.BasicElevationModel;
import gov.nasa.worldwind.util.Tile;

/**
 * A global elevation model for NASA World Wind, based on the Copernicus DEM 30m.
 *
 * <p>The elevation tiles are not downloaded from a tile service: each tile is built from the Copernicus Cloud Optimized
 * GeoTIFFs through {@link CopernicusDemSource}, then stored in the World Wind file cache, from which it is read the next
 * times. The data downloaded to build the tiles are cached too, so each piece of the dataset is downloaded only once.</p>
 *
 * <p>To not download huge amounts of data, elevations are provided only for views up to a few hundred kilometers wide:
 * above that, the terrain is flat.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class CopernicusElevationModel extends BasicElevationModel {
    public static final String NAME = "Copernicus DEM 30m";
    private static final String CACHE_NAME = "Earth/CopernicusDEM30";
    private static final int TILE_SIZE = 150;
    /** Level zero tiles of 1.25 degrees: 6 levels reach the 1 arc second of the dataset. */
    private static final double LEVEL_ZERO_DELTA = 1.25;
    private static final int NUM_LEVELS = 6;
    /** Coarser requests than this factor times the resolution of the first level get a flat terrain. */
    private static final double COARSE_FACTOR = 4.0;

    private static ExecutorService executor;
    private final CopernicusDemSource source;
    private final Set<String> pendingTiles = ConcurrentHashMap.newKeySet();
    private final double coarsestResolution;
    /** True while serving an unmapped request, see {@link #getExtremeElevations(Sector)}. */
    private final ThreadLocal<Boolean> unmappedRequest = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public CopernicusElevationModel() {
        super(makeParams());
        File blocksFolder = new File(CacheUtils.getCacheRoot(), CACHE_NAME + "-source");
        source = new CopernicusDemSource(blocksFolder);
        coarsestResolution = getLevels().getFirstLevel().getTexelSize() * COARSE_FACTOR;
        setName(NAME);
    }

    private static AVList makeParams() {
        AVList params = new AVListImpl();
        params.setValue(AVKey.DISPLAY_NAME, NAME);
        params.setValue(AVKey.DATA_CACHE_NAME, CACHE_NAME);
        params.setValue(AVKey.SERVICE, "*");
        params.setValue(AVKey.DATASET_NAME, "copernicus-dem-30m");
        params.setValue(AVKey.FORMAT_SUFFIX, ".bil");
        params.setValue(AVKey.IMAGE_FORMAT, "application/bil32");
        params.setValue(AVKey.DATA_TYPE, AVKey.FLOAT32);
        params.setValue(AVKey.BYTE_ORDER, AVKey.LITTLE_ENDIAN);
        params.setValue(AVKey.TILE_WIDTH, TILE_SIZE);
        params.setValue(AVKey.TILE_HEIGHT, TILE_SIZE);
        params.setValue(AVKey.NUM_LEVELS, NUM_LEVELS);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA,
                new LatLon(Angle.fromDegrees(LEVEL_ZERO_DELTA), Angle.fromDegrees(LEVEL_ZERO_DELTA)));
        params.setValue(AVKey.SECTOR, Sector.FULL_SPHERE);
        params.setValue(AVKey.ELEVATION_MIN, -500d);
        params.setValue(AVKey.ELEVATION_MAX, 9000d);
        params.setValue(AVKey.MISSING_DATA_SIGNAL, -32767d);
        params.setValue(AVKey.NETWORK_RETRIEVAL_ENABLED, true);
        return params;
    }

    private static synchronized ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "Copernicus DEM tiles builder");
                t.setDaemon(true);
                return t;
            });
        }
        return executor;
    }

    @Override
    protected double getElevations( Sector sector, List< ? extends LatLon> latlons, double targetResolution, double[] buffer,
            boolean mapMissingData ) {
        if (targetResolution > coarsestResolution) {
            // views too wide: no data instead of downloading data for half a continent
            if (!mapMissingData) {
                // unmapped requests come from compound models: leave the values of the other models
                return Double.MAX_VALUE;
            }
            Arrays.fill(buffer, 0, Math.min(buffer.length, latlons.size()), 0.0);
            return targetResolution;
        }
        if (!mapMissingData) {
            // the basic model writes its minimum elevation where tiles are not loaded yet, also in
            // unmapped requests: make it write NaN and put back the values of the other models
            int count = Math.min(buffer.length, latlons.size());
            double[] previous = Arrays.copyOf(buffer, count);
            double resolution;
            unmappedRequest.set(Boolean.TRUE);
            try {
                resolution = super.getElevations(sector, latlons, targetResolution, buffer, false);
            } finally {
                unmappedRequest.set(Boolean.FALSE);
            }
            for( int i = 0; i < count; i++ ) {
                if (Double.isNaN(buffer[i])) {
                    buffer[i] = previous[i];
                }
            }
            return resolution;
        }
        return super.getElevations(sector, latlons, targetResolution, buffer, mapMissingData);
    }

    @Override
    public double[] getExtremeElevations( Sector sector ) {
        if (unmappedRequest.get()) {
            return new double[]{Double.NaN, Double.NaN};
        }
        return super.getExtremeElevations(sector);
    }

    @Override
    protected void downloadElevations( Tile tile ) {
        buildTile(tile);
    }

    @Override
    protected void downloadElevations( Tile tile, DownloadPostProcessor postProcessor ) {
        buildTile(tile);
    }

    /**
     * Build the elevation tile from the Copernicus data and store it in the file cache, in the background.
     */
    private void buildTile( Tile tile ) {
        String path = tile.getPath();
        if (!pendingTiles.add(path)) {
            return;
        }
        getExecutor().execute(() -> {
            try {
                Sector sector = tile.getSector();
                float[] elevations = source.getElevations(sector.getMaxLatitude().degrees, sector.getMinLatitude().degrees,
                        sector.getMinLongitude().degrees, sector.getMaxLongitude().degrees, tile.getWidth(),
                        tile.getHeight());

                ByteBuffer buffer = ByteBuffer.allocate(elevations.length * 4).order(ByteOrder.LITTLE_ENDIAN);
                buffer.asFloatBuffer().put(elevations);

                File file = getDataFileStore().newFile(path);
                if (file == null) {
                    return;
                }
                File tmp = new File(file.getParentFile(), file.getName() + ".part");
                try (FileOutputStream out = new FileOutputStream(tmp)) {
                    out.write(buffer.array());
                }
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // let the globe request the tile again, now from the cache
                firePropertyChange(AVKey.ELEVATION_MODEL, null, this);
            } catch (Exception e) {
                Logger.INSTANCE.insertError("CopernicusElevationModel", "Unable to build the elevation tile " + path, e);
                getLevels().markResourceAbsent(tile);
            } finally {
                pendingTiles.remove(path);
            }
        });
    }
}
