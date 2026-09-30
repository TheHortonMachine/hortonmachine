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

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.hortonmachine.dbs.log.Logger;

import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReader;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.cog.DefaultCogImageInputStream;
import it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader;

/**
 * Access to the global Copernicus DEM 30m (GLO-30), published as Cloud Optimized GeoTIFFs of 1x1 degree on the AWS
 * open data registry.
 *
 * <p>The data are read in blocks of {@value #BLOCK_SIZE} pixels, at the overview level that fits the requested
 * resolution. Each block is downloaded once, with a single range request, and cached on disk, compressed, in the given
 * cache folder. The most recently used blocks are also kept in memory.</p>
 *
 * <p>Tiles missing from the dataset (the oceans) have elevation 0.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class CopernicusDemSource {
    private static final String BASE_URL = "https://copernicus-dem-30m.s3.amazonaws.com/";
    private static final String TILE_LIST = "tileList.txt";
    private static final int BLOCK_SIZE = 1024;
    private static final int MAX_BLOCKS_IN_MEMORY = 48;
    private static final int MAX_OPEN_READERS = 32;
    /**
     * An overview is used as long as its pixels are at most this factor larger than the requested spacing: the full
     * resolution blocks are 4 times the data of the first overview.
     */
    private static final double OVERVIEW_TOLERANCE = 1.5;
    /** The cached elevations are stored as integers of this unit, in meters. */
    private static final float QUANTUM = 0.1f;

    /** A block of elevations of a tile at a given overview level. */
    private static class Block {
        final int x0, y0, width, height;
        final float[] data;

        Block( int x0, int y0, int width, int height, float[] data ) {
            this.x0 = x0;
            this.y0 = y0;
            this.width = width;
            this.height = height;
            this.data = data;
        }
    }

    /** The structure of a tile: the size of the full resolution image and of its overviews. */
    private static class TileInfo {
        int[] widths;
        int[] heights;
    }

    private final File cacheFolder;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private volatile Set<String> availableTiles;
    private final Map<String, TileInfo> tileInfos = new ConcurrentHashMap<>();
    private final Map<String, Block> blocksInMemory = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true){
        protected boolean removeEldestEntry( Map.Entry<String, Block> eldest ) {
            return size() > MAX_BLOCKS_IN_MEMORY;
        }
    });
    private final Map<String, CogImageReader> readers = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true){
        protected boolean removeEldestEntry( Map.Entry<String, CogImageReader> eldest ) {
            if (size() > MAX_OPEN_READERS) {
                eldest.getValue().dispose();
                return true;
            }
            return false;
        }
    });
    /** Locks to download each block only once, also with concurrent requests. */
    private final Map<String, Object> blockLocks = new ConcurrentHashMap<>();

    /**
     * @param cacheFolder the folder in which to cache the tiles list and the downloaded blocks.
     */
    public CopernicusDemSource( File cacheFolder ) {
        this.cacheFolder = cacheFolder;
        cacheFolder.mkdirs();
    }

    /**
     * Fill a regular grid of elevations, sampled with nearest neighbour.
     *
     * <p>The samples are placed at the edges of the given region: the first row lies on the north edge, the last on
     * the south edge, the first column on the west edge, the last on the east edge.</p>
     *
     * @param north the north edge, in degrees.
     * @param south the south edge, in degrees.
     * @param west the west edge, in degrees.
     * @param east the east edge, in degrees.
     * @param cols the number of columns of the grid.
     * @param rows the number of rows of the grid.
     * @return the elevations, row by row from north to south.
     * @throws Exception
     */
    public float[] getElevations( double north, double south, double west, double east, int cols, int rows )
            throws Exception {
        double dLat = (north - south) / (rows - 1);
        double dLon = (east - west) / (cols - 1);
        // the coarsest overview that is still at least as detailed as the grid spacing
        double spacing = Math.min(dLat, dLon);

        float[] elevations = new float[cols * rows];
        for( int r = 0; r < rows; r++ ) {
            double lat = north - r * dLat;
            for( int c = 0; c < cols; c++ ) {
                double lon = west + c * dLon;
                elevations[r * cols + c] = getElevation(lat, lon, spacing);
            }
        }
        return elevations;
    }

    private float getElevation( double lat, double lon, double spacing ) throws Exception {
        // keep the samples on the edges inside the dataset
        lat = Math.max(-89.9999, Math.min(89.9999, lat));
        if (lon >= 180)
            lon -= 360;
        if (lon < -180)
            lon += 360;

        int tileLat = (int) Math.floor(lat);
        int tileLon = (int) Math.floor(lon);
        String tileName = getTileName(tileLat, tileLon);
        if (!getAvailableTiles().contains(tileName)) {
            return 0f;
        }
        TileInfo info = getTileInfo(tileName);
        int level = 0;
        for( int i = info.heights.length - 1; i >= 0; i-- ) {
            if (1.0 / info.heights[i] <= spacing * OVERVIEW_TOLERANCE) {
                level = i;
                break;
            }
        }
        int width = info.widths[level];
        int height = info.heights[level];

        // the grids are pixel-is-point, shifted by half a pixel of the full resolution image
        double west0 = tileLon - 0.5 / info.widths[0];
        double north0 = tileLat + 1 + 0.5 / info.heights[0];
        int col = (int) Math.floor((lon - west0) * width);
        int row = (int) Math.floor((north0 - lat) * height);
        col = Math.max(0, Math.min(width - 1, col));
        row = Math.max(0, Math.min(height - 1, row));

        Block block = getBlock(tileName, level, col / BLOCK_SIZE, row / BLOCK_SIZE);
        return block.data[(row - block.y0) * block.width + (col - block.x0)];
    }

    private Block getBlock( String tileName, int level, int bx, int by ) throws Exception {
        String key = tileName + "_" + level + "_" + bx + "_" + by;
        Block block = blocksInMemory.get(key);
        if (block != null) {
            return block;
        }
        Object lock = blockLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            block = blocksInMemory.get(key);
            if (block != null) {
                return block;
            }
            File blockFile = new File(cacheFolder, "blocks" + File.separator + tileName + File.separator + key + ".dem");
            if (blockFile.exists()) {
                block = readBlock(blockFile);
            } else {
                block = downloadBlock(tileName, level, bx, by);
                writeBlock(block, blockFile);
            }
            blocksInMemory.put(key, block);
        }
        blockLocks.remove(key);
        return block;
    }

    private Block downloadBlock( String tileName, int level, int bx, int by ) throws Exception {
        TileInfo info = getTileInfo(tileName);
        int x0 = bx * BLOCK_SIZE;
        int y0 = by * BLOCK_SIZE;
        int w = Math.min(BLOCK_SIZE, info.widths[level] - x0);
        int h = Math.min(BLOCK_SIZE, info.heights[level] - y0);

        CogImageReadParam param = new CogImageReadParam();
        param.setSourceRegion(new Rectangle(x0, y0, w, h));
        param.setRangeReaderClass(HttpRangeReader.class);
        Raster raster;
        CogImageReader reader = getReader(tileName);
        synchronized (reader) {
            raster = reader.read(level, param).getRaster();
        }
        float[] data = raster.getSamples(0, 0, w, h, 0, new float[w * h]);
        return new Block(x0, y0, w, h, data);
    }

    private TileInfo getTileInfo( String tileName ) throws Exception {
        TileInfo info = tileInfos.get(tileName);
        if (info != null) {
            return info;
        }
        // the structure is cached on disk too, to not read the headers again in the next sessions
        File infoFile = new File(cacheFolder, "blocks" + File.separator + tileName + File.separator + "info.txt");
        info = new TileInfo();
        if (infoFile.exists()) {
            String[] lines = Files.readString(infoFile.toPath()).trim().split("\n");
            info.widths = new int[lines.length];
            info.heights = new int[lines.length];
            for( int i = 0; i < lines.length; i++ ) {
                String[] split = lines[i].trim().split("x");
                info.widths[i] = Integer.parseInt(split[0]);
                info.heights[i] = Integer.parseInt(split[1]);
            }
        } else {
            CogImageReader reader = getReader(tileName);
            StringBuilder sb = new StringBuilder();
            synchronized (reader) {
                int count = reader.getNumImages(true);
                info.widths = new int[count];
                info.heights = new int[count];
                for( int i = 0; i < count; i++ ) {
                    info.widths[i] = reader.getWidth(i);
                    info.heights[i] = reader.getHeight(i);
                    sb.append(info.widths[i]).append("x").append(info.heights[i]).append("\n");
                }
            }
            infoFile.getParentFile().mkdirs();
            Files.writeString(infoFile.toPath(), sb.toString());
        }
        tileInfos.put(tileName, info);
        return info;
    }

    private CogImageReader getReader( String tileName ) {
        synchronized (readers) {
            CogImageReader reader = readers.get(tileName);
            if (reader == null) {
                URI uri = URI.create(BASE_URL + tileName + "/" + tileName + ".tif");
                DefaultCogImageInputStream stream = new DefaultCogImageInputStream(uri,
                        new HttpRangeReader(uri, CogImageReadParam.DEFAULT_HEADER_LENGTH));
                reader = new CogImageReader(new CogImageReaderSpi());
                reader.setInput(stream);
                readers.put(tileName, reader);
            }
            return reader;
        }
    }

    /**
     * @return the names of the tiles of the dataset, downloaded once and cached.
     */
    private Set<String> getAvailableTiles() throws Exception {
        if (availableTiles == null) {
            synchronized (this) {
                if (availableTiles == null) {
                    File listFile = new File(cacheFolder, TILE_LIST);
                    if (!listFile.exists()) {
                        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + TILE_LIST))
                                .timeout(Duration.ofSeconds(60)).build();
                        File tmp = new File(cacheFolder, TILE_LIST + ".part");
                        HttpResponse<InputStream> response = httpClient.send(request,
                                HttpResponse.BodyHandlers.ofInputStream());
                        if (response.statusCode() != 200) {
                            throw new IOException("Unable to read the Copernicus DEM tiles list: HTTP " + response.statusCode());
                        }
                        try (InputStream in = response.body()) {
                            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                        Files.move(tmp.toPath(), listFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    Set<String> names = new HashSet<>();
                    for( String line : Files.readAllLines(listFile.toPath()) ) {
                        line = line.trim();
                        if (!line.isEmpty())
                            names.add(line);
                    }
                    availableTiles = names;
                    Logger.INSTANCE.insertInfo("CopernicusDemSource", "Copernicus DEM tiles available: " + names.size());
                }
            }
        }
        return availableTiles;
    }

    /**
     * @return the name of the tile having the given south west corner, e.g. Copernicus_DSM_COG_10_N46_00_E011_00_DEM.
     */
    static String getTileName( int lat, int lon ) {
        String ns = lat < 0 ? "S" : "N";
        String ew = lon < 0 ? "W" : "E";
        return String.format("Copernicus_DSM_COG_10_%s%02d_00_%s%03d_00_DEM", ns, Math.abs(lat), ew, Math.abs(lon));
    }

    /**
     * Write a block compressed: the elevations are rounded to {@link #QUANTUM} and stored as differences from the previous
     * value of the row, which are small numbers on real terrain and compress much better than the raw floats.
     */
    private static void writeBlock( Block block, File file ) throws IOException {
        file.getParentFile().mkdirs();
        File tmp = new File(file.getParentFile(), file.getName() + ".part");
        try (DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(
                new BufferedOutputStream(new FileOutputStream(tmp)), new Deflater(Deflater.BEST_COMPRESSION), 65536))) {
            out.writeInt(block.x0);
            out.writeInt(block.y0);
            out.writeInt(block.width);
            out.writeInt(block.height);
            for( int r = 0; r < block.height; r++ ) {
                int previous = 0;
                for( int c = 0; c < block.width; c++ ) {
                    int value = Math.round(block.data[r * block.width + c] / QUANTUM);
                    int delta = value - previous;
                    // zigzag varint: small deltas take a single byte
                    int zigzag = (delta << 1) ^ (delta >> 31);
                    while( (zigzag & ~0x7F) != 0 ) {
                        out.writeByte((zigzag & 0x7F) | 0x80);
                        zigzag >>>= 7;
                    }
                    out.writeByte(zigzag);
                    previous = value;
                }
            }
        }
        // written to a temporary file first, to never leave half written blocks in the cache
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static Block readBlock( File file ) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new InflaterInputStream(new FileInputStream(file)), 65536))) {
            int x0 = in.readInt();
            int y0 = in.readInt();
            int w = in.readInt();
            int h = in.readInt();
            float[] data = new float[w * h];
            for( int r = 0; r < h; r++ ) {
                int previous = 0;
                for( int c = 0; c < w; c++ ) {
                    int zigzag = 0;
                    int shift = 0;
                    int b;
                    do {
                        b = in.readUnsignedByte();
                        zigzag |= (b & 0x7F) << shift;
                        shift += 7;
                    } while( (b & 0x80) != 0 );
                    int delta = (zigzag >>> 1) ^ -(zigzag & 1);
                    previous += delta;
                    data[r * w + c] = previous * QUANTUM;
                }
            }
            return new Block(x0, y0, w, h, data);
        }
    }
}
