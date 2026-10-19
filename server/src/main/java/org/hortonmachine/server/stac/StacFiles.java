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
package org.hortonmachine.server.stac;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.server.web.MediaTypes;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The description of the files of the workspace as assets of STAC items, with their footprint.
 *
 * <p>Nothing reads a whole raster: the GeoTIFFs are read by windows, the other rasters only if they are small,
 * the vectors only through their headers or, for GeoJSON, streamed if they are small.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class StacFiles {
    private static final Logger LOGGER = Logger.getLogger(StacFiles.class.getName());

    public static final String COG = MediaTypes.GEOTIFF + "; profile=cloud-optimized";
    /** The rasters but the GeoTIFFs larger than this are not read, so they have no statistics. */
    public static final long MAX_IN_MEMORY_CELLS = 25_000_000L;
    /** The GeoJSON files larger than this are not read, so they have no footprint. */
    public static final long MAX_GEOJSON_BYTES = 50L * 1024 * 1024;
    /** The files that come with a shapefile, in the same asset. */
    public static final List<String> SHAPEFILE_SIDECARS = List.of("shx", "dbf", "prj", "cpg", "qix", "sbn", "sbx", "fix", "qmd");
    /** The formats whose files can have sidecars. */
    public static final List<String> MAIN_EXTENSIONS = List.of("tif", "tiff", "asc", "nc", "grd", "img", "shp", "gpkg", "geojson",
            "las", "laz");
    /**
     * The files next to a file of a {@link #MAIN_EXTENSIONS main format}, with its name, that are part of it: the parts of
     * shapefiles, georeferencing, statistics, overviews, styles, metadata and the journals of geopackages.
     */
    public static final List<String> SIDECAR_EXTENSIONS = List.of("aux.xml", "shp.xml", "ovr", "tfw", "tifw", "wld", "prj", "shx",
            "dbf", "cpg", "qix", "sbn", "sbx", "fix", "qmd", "qml", "sld", "xml", "gpkg-wal", "gpkg-shm", "gpkg-journal");

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final int DENSIFY_SEGMENTS = 16;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A file as asset, with its footprint.
     */
    public static class Description {
        /** The fields of the asset, without href. */
        public final ObjectNode asset;
        /** The footprint in WGS84, longitude first, or <code>null</code> if the file is not geographic. */
        public final Geometry footprint;

        Description( ObjectNode asset, Geometry footprint ) {
            this.asset = asset;
            this.footprint = footprint;
        }
    }

    private StacFiles() {
    }

    private static String extension( File file ) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    /**
     * @return <code>true</code> if the file is of a format that can have sidecars, as rasters and vectors.
     */
    public static boolean isMain( File file ) {
        return MAIN_EXTENSIONS.contains(extension(file));
    }

    /**
     * Find the file a sidecar belongs to: <code>roads.shp</code> for <code>roads.dbf</code>, <code>dtm.tif</code>
     * for <code>dtm.tif.aux.xml</code> or <code>dtm.tfw</code>.
     *
     * @return the main file, or <code>null</code> if the file is not a sidecar.
     */
    public static File mainOf( File file ) {
        String name = file.getName();
        String lower = name.toLowerCase(Locale.ROOT);
        File folder = file.getParentFile();
        for( String sidecar : SIDECAR_EXTENSIONS ) {
            if (!lower.endsWith("." + sidecar)) {
                continue;
            }
            String base = name.substring(0, name.length() - sidecar.length() - 1);
            File whole = new File(folder, base);
            if (isMain(whole) && whole.isFile()) {
                return whole;
            }
            for( String main : MAIN_EXTENSIONS ) {
                for( String candidate : new String[]{main, main.toUpperCase(Locale.ROOT)} ) {
                    File mainFile = new File(folder, base + "." + candidate);
                    if (mainFile.isFile()) {
                        return mainFile;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return the sidecars of a file among the files of its folder, by name.
     */
    public static List<File> sidecarsOf( File main, File[] folderFiles ) {
        List<File> sidecars = new ArrayList<>();
        String base = main.getName().contains(".") ? main.getName().substring(0, main.getName().lastIndexOf('.')) : main.getName();
        for( File file : folderFiles ) {
            String name = file.getName();
            if (!file.equals(main) && name.startsWith(base) && file.isFile() && main.equals(mainOf(file))) {
                sidecars.add(file);
            }
        }
        sidecars.sort(null);
        return sidecars;
    }

    /**
     * @return <code>true</code> if the file is a sidecar of a shapefile next to it, described with it.
     */
    public static boolean isShapefileSidecar( File file ) {
        String extension = extension(file);
        if (!SHAPEFILE_SIDECARS.contains(extension)) {
            return false;
        }
        String name = file.getName();
        String base = name.substring(0, name.length() - extension.length() - 1);
        return new File(file.getParentFile(), base + ".shp").isFile() || new File(file.getParentFile(), base + ".SHP").isFile();
    }

    /**
     * @return the sidecars of a shapefile that exist.
     */
    public static List<File> shapefileSidecars( File shapefile ) {
        String name = shapefile.getName();
        String base = name.substring(0, name.length() - 4);
        List<File> sidecars = new ArrayList<>();
        for( String extension : SHAPEFILE_SIDECARS ) {
            for( String candidate : new String[]{extension, extension.toUpperCase(Locale.ROOT)} ) {
                File sidecar = new File(shapefile.getParentFile(), base + "." + candidate);
                if (sidecar.isFile()) {
                    sidecars.add(sidecar);
                    break;
                }
            }
        }
        return sidecars;
    }

    /**
     * @return the size of a file, with its sidecars for a shapefile.
     */
    public static long size( File file ) {
        long size = file.length();
        if (extension(file).equals("shp")) {
            for( File sidecar : shapefileSidecars(file) ) {
                size += sidecar.length();
            }
        }
        return size;
    }

    /**
     * @return <code>true</code> if the file is a GeoTIFF written as COG, by GDAL or by the HortonMachine.
     */
    public static boolean isCog( File file ) {
        String extension = extension(file);
        if (!extension.equals("tif") && !extension.equals("tiff")) {
            return false;
        }
        try (InputStream input = new FileInputStream(file)) {
            byte[] start = input.readNBytes(256);
            return new String(start, StandardCharsets.ISO_8859_1).contains("LAYOUT=IFDS_BEFORE_DATA");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Describe a file as asset of an item.
     *
     * @param file the file; for a shapefile the sidecars are added as <code>hm:sidecars</code>.
     * @param title the title of the asset.
     * @return the description, also for files that can't be read, without footprint.
     */
    public static Description describe( File file, String title ) {
        ObjectNode asset = MAPPER.createObjectNode();
        String extension = extension(file);
        boolean cog = isCog(file);
        asset.put("type", cog ? COG : MediaTypes.forFile(file.getName()));
        asset.put("title", title);
        asset.putArray("roles").add("data");
        asset.put("file:size", size(file));
        Geometry footprint = null;
        try {
            if (extension.equals("shp")) {
                ArrayNode sidecars = asset.putArray("hm:sidecars");
                shapefileSidecars(file).forEach(sidecar -> sidecars.add(sidecar.getName()));
                footprint = describeVector(file, asset);
            } else if (extension.equals("gpkg")) {
                footprint = describeVector(file, asset);
            } else if (extension.equals("geojson") && file.length() <= MAX_GEOJSON_BYTES) {
                footprint = describeGeoJson(file, asset);
            } else if (extension.equals("tif") || extension.equals("tiff")) {
                try (HMRaster raster = HMRaster.fromFileWindowed(file.getAbsolutePath())) {
                    footprint = describeRaster(raster, file, asset, true);
                }
            } else if (isOtherRaster(file)) {
                footprint = describeOtherRaster(file, asset);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "The file can't be read as geographic: " + file, e);
        }
        return new Description(asset, footprint);
    }

    private static boolean isOtherRaster( File file ) {
        String extension = extension(file);
        return extension.equals("asc") || extension.equals("nc") || extension.equals("grd") || extension.equals("img");
    }

    private static Geometry describeOtherRaster( File file, ObjectNode asset ) throws Exception {
        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        if (format == null || format instanceof UnknownFormat) {
            return null;
        }
        AbstractGridCoverage2DReader reader = format.getReader(file);
        if (reader == null) {
            return null;
        }
        try {
            long cells = (long) reader.getOriginalGridRange().getSpan(0) * reader.getOriginalGridRange().getSpan(1);
            if (cells <= MAX_IN_MEMORY_CELLS) {
                try (HMRaster raster = HMRaster.fromFile(file)) {
                    return describeRaster(raster, file, asset, true);
                }
            }
            // only the georeferencing of the header
            ReferencedEnvelope envelope = ReferencedEnvelope.reference(reader.getOriginalEnvelope());
            int cols = reader.getOriginalGridRange().getSpan(0);
            int rows = reader.getOriginalGridRange().getSpan(1);
            putProjection(asset, envelope.getCoordinateReferenceSystem(), envelope, rows, cols);
            return footprint(envelope, envelope.getCoordinateReferenceSystem());
        } finally {
            reader.dispose();
        }
    }

    /**
     * Describe a raster: projection, bands with statistics.
     *
     * @param withStatistics <code>true</code> to compute the statistics if the file has none.
     */
    static Geometry describeRaster( HMRaster raster, File file, ObjectNode asset, boolean withStatistics ) throws Exception {
        RegionMap region = raster.getRegionMap();
        Envelope envelope = new Envelope(region.getWest(), region.getEast(), region.getSouth(), region.getNorth());
        CoordinateReferenceSystem crs = raster.getCrs();
        putProjection(asset, crs, envelope, region.getRows(), region.getCols());

        ObjectNode band = asset.putArray("raster:bands").addObject();
        band.put("data_type", dataType(raster.getDataType()));
        double novalue = raster.getNovalue();
        if (Double.isNaN(novalue)) {
            band.put("nodata", "nan");
        } else {
            band.put("nodata", novalue);
        }
        band.put("spatial_resolution", region.getXres());
        RasterStats stats = RasterStats.read(file);
        if (stats == null && withStatistics) {
            stats = RasterStats.compute(raster);
        }
        if (stats != null) {
            ObjectNode statistics = band.putObject("statistics");
            statistics.put("minimum", stats.minimum).put("maximum", stats.maximum).put("mean", stats.mean).put("stddev",
                    stats.stddev);
            if (stats.validPercent != null) {
                statistics.put("valid_percent", stats.validPercent);
            }
        }
        return crs != null ? footprint(envelope, crs) : null;
    }

    private static Geometry describeVector( File file, ObjectNode asset ) throws Exception {
        Map<String, Object> params = new HashMap<>();
        if (extension(file).equals("gpkg")) {
            params.put("dbtype", "geopkg");
            params.put("database", file.getAbsolutePath());
            params.put("read_only", true);
        } else {
            params.put("url", file.toURI().toURL());
        }
        DataStore store = DataStoreFinder.getDataStore(params);
        if (store == null) {
            return null;
        }
        try {
            Envelope union = null;
            CoordinateReferenceSystem crs = null;
            Geometry footprint = null;
            ArrayNode layers = MAPPER.createArrayNode();
            for( String typeName : store.getTypeNames() ) {
                SimpleFeatureSource source = store.getFeatureSource(typeName);
                if (source.getSchema().getGeometryDescriptor() == null) {
                    continue;
                }
                ReferencedEnvelope bounds = source.getBounds();
                if (bounds == null) {
                    bounds = source.getFeatures().getBounds();
                }
                CoordinateReferenceSystem layerCrs = source.getSchema().getCoordinateReferenceSystem();
                ObjectNode layer = layers.addObject().put("name", typeName).put("geometry_type",
                        source.getSchema().getGeometryDescriptor().getType().getBinding().getSimpleName());
                int count = source.getCount(Query.ALL);
                if (count >= 0) {
                    layer.put("count", count);
                }
                if (bounds == null || bounds.isEmpty() || layerCrs == null) {
                    continue;
                }
                Geometry layerFootprint = footprint(bounds, layerCrs);
                footprint = footprint == null ? layerFootprint : footprint.union(layerFootprint);
                if (crs == null) {
                    crs = layerCrs;
                    union = new Envelope(bounds);
                } else if (HMCrsRegistry.crsEquals(crs, layerCrs)) {
                    union.expandToInclude(bounds);
                }
            }
            asset.set("hm:layers", layers);
            if (crs != null) {
                putProjection(asset, crs, union, null, null);
            }
            return footprint;
        } finally {
            store.dispose();
        }
    }

    /**
     * Describe a GeoJSON file, streaming it: the bounds of all the coordinates, in WGS84 as RFC 7946 wants.
     *
     * @return the footprint, <code>null</code> if there are no coordinates or they are not longitudes and latitudes.
     */
    private static Geometry describeGeoJson( File file, ObjectNode asset ) throws Exception {
        Envelope envelope = new Envelope();
        int features = 0;
        try (JsonParser parser = MAPPER.getFactory().createParser(file)) {
            JsonToken token;
            while( (token = parser.nextToken()) != null ) {
                if (token != JsonToken.FIELD_NAME) {
                    continue;
                }
                String field = parser.currentName();
                if (field.equals("coordinates")) {
                    parser.nextToken();
                    readCoordinates(parser, envelope);
                } else if (field.equals("type")) {
                    parser.nextToken();
                    if ("Feature".equals(parser.getValueAsString())) {
                        features++;
                    }
                }
            }
        }
        asset.putArray("hm:layers").addObject().put("name", file.getName()).put("count", features);
        if (envelope.isNull() || envelope.getMinX() < -180 || envelope.getMaxX() > 180 || envelope.getMinY() < -90
                || envelope.getMaxY() > 90) {
            return null;
        }
        asset.put("proj:code", "EPSG:4326");
        asset.putArray("proj:bbox").add(envelope.getMinX()).add(envelope.getMinY()).add(envelope.getMaxX()).add(envelope.getMaxY());
        return GEOMETRY_FACTORY.toGeometry(envelope);
    }

    /**
     * Read the value of a coordinates member, nested arrays ending in positions, into the envelope.
     */
    private static void readCoordinates( JsonParser parser, Envelope envelope ) throws IOException {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            parser.skipChildren();
            return;
        }
        JsonToken token = parser.nextToken();
        if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
            // a position: x, y and maybe more
            double x = parser.getDoubleValue();
            token = parser.nextToken();
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                envelope.expandToInclude(x, parser.getDoubleValue());
            }
            while( token != JsonToken.END_ARRAY && token != null ) {
                token = parser.nextToken();
            }
            return;
        }
        while( token != JsonToken.END_ARRAY && token != null ) {
            readCoordinates(parser, envelope);
            token = parser.nextToken();
        }
    }

    /**
     * Add the fields of the projection extension.
     */
    private static void putProjection( ObjectNode asset, CoordinateReferenceSystem crs, Envelope envelope, Integer rows,
            Integer cols ) throws Exception {
        if (crs != null) {
            String code = HMCrsRegistry.getCodeFromCrs(crs);
            // a code declared by a .prj is not always a real one, as EPSG:30031000: the clients couldn't use it
            if (code != null && !isKnownCode(code)) {
                code = null;
            }
            if (code != null) {
                asset.put("proj:code", code);
            } else {
                // as WKT 1, the projection extension wants WKT 2
                asset.putNull("proj:code");
                asset.put("hm:wkt", crs.toWKT());
            }
        }
        asset.putArray("proj:bbox").add(envelope.getMinX()).add(envelope.getMinY()).add(envelope.getMaxX()).add(envelope.getMaxY());
        if (rows != null && cols != null) {
            asset.putArray("proj:shape").add(rows).add(cols);
            double xres = envelope.getWidth() / cols;
            double yres = envelope.getHeight() / rows;
            asset.putArray("proj:transform").add(xres).add(0.0).add(envelope.getMinX()).add(0.0).add(-yres).add(envelope.getMaxY());
        }
    }

    /**
     * @return <code>true</code> if the code of a projection can be decoded.
     */
    private static boolean isKnownCode( String code ) {
        try {
            return HMCrsRegistry.INSTANCE.getCrs(code) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return the outline of an envelope in WGS84, longitude first, densified to follow the projection.
     */
    static Geometry footprint( Envelope envelope, CoordinateReferenceSystem crs ) throws Exception {
        Geometry polygon = GEOMETRY_FACTORY.toGeometry(envelope);
        CoordinateReferenceSystem wgs84 = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
        if (HMCrsRegistry.crsEquals(crs, wgs84)) {
            return polygon;
        }
        double step = Math.max(envelope.getWidth(), envelope.getHeight()) / DENSIFY_SEGMENTS;
        if (step > 0) {
            polygon = Densifier.densify(polygon, step);
        }
        return new HMCrsTransformer(crs, wgs84).transform(polygon);
    }

    private static String dataType( int dataBufferType ) {
        switch( dataBufferType ) {
        case DataBuffer.TYPE_BYTE:
            return "uint8";
        case DataBuffer.TYPE_USHORT:
            return "uint16";
        case DataBuffer.TYPE_SHORT:
            return "int16";
        case DataBuffer.TYPE_INT:
            return "int32";
        case DataBuffer.TYPE_FLOAT:
            return "float32";
        default:
            return "float64";
        }
    }
}
