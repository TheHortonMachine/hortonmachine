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
package org.hortonmachine.gears.io.vrt;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.hortonmachine.gears.io.stac.assets.handlers.GeotiffHandler;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

/**
 * Makes a GeoTools image mosaic out of a GDAL virtual raster (VRT) file, so that it is read
 * through the {@link org.geotools.gce.imagemosaic.ImageMosaicReader} as any other mosaic, without
 * GDAL.
 *
 * <p>The mosaic is the usual shapefile index with its properties file and sample image, in a
 * folder next to the VRT. It has a granule for each source file of the VRT, with the footprint
 * the VRT gives it and its absolute path, so the files stay where they are. The index is made
 * once and made again only when the VRT is newer.</p>
 *
 * <p>The sources can be local files or remote GeoTIFFs given as
 * <code>/vsicurl/https://...</code>: these are read as cloud optimized GeoTIFFs, with http range
 * requests, and making the index reads only the header of the first one. Local and remote
 * sources can't be mixed.</p>
 *
 * <p>Only the VRTs that are plain mosaics can be served, as the ones of
 * <code>gdalbuildvrt</code>: every band reads the same band of the same files, with the values
 * as they are. Warped, pansharpened and derived datasets, value transforms (scale, offset,
 * lookup tables), bands taken from different files (<code>gdalbuildvrt -separate</code>) or in
 * another order and the other GDAL virtual paths (as /vsis3/ or /vsizip/) are refused with the
 * list of what is not supported.</p>
 *
 * <p>The mosaic places each file by its own georeferencing, the VRT by its window in the
 * dataset: for the VRTs made by <code>gdalbuildvrt</code> they are the same thing.</p>
 */
public class VrtImageMosaic {

    /** The suffix of the folder with the mosaic, next to the VRT. */
    public static final String INDEX_FOLDER_SUFFIX = "_vrtmosaic";

    private static final String LOCATION_FIELD = "location";
    /** The maximum length of a dbf text field. */
    private static final int MAX_LOCATION_LENGTH = 254;

    /** The GDAL prefix of the files read over http(s). */
    private static final String VSICURL = "/vsicurl/";

    private static final String TIFF_SPI = "it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi";
    private static final String COG_SPI = "it.geosolutions.imageioimpl.plugins.cog.CogImageReaderSpi";
    private static final String COG_RANGE_READER = "it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader";
    private static final String GEOTIFF_FORMAT = "org.geotools.gce.geotiff.GeoTiffFormat";

    private VrtImageMosaic() {
    }

    /**
     * Get the index of the mosaic of a VRT, made in the folder next to it if missing or older
     * than the VRT.
     *
     * @param vrtFile the VRT file.
     * @return the shapefile of the mosaic index, to open as any image mosaic.
     * @throws Exception if the VRT can't be read or is not supported.
     */
    public static File getIndex( File vrtFile ) throws Exception {
        String name = FileUtilities.getNameWithoutExtention(vrtFile);
        File indexFolder = new File(vrtFile.getAbsoluteFile().getParentFile(), name + INDEX_FOLDER_SUFFIX);
        return getIndex(vrtFile, indexFolder);
    }

    /**
     * Get the index of the mosaic of a VRT, made in the given folder if missing or older than the
     * VRT.
     *
     * @param vrtFile     the VRT file.
     * @param indexFolder the folder of the index, created if missing.
     * @return the shapefile of the mosaic index, to open as any image mosaic.
     * @throws Exception if the VRT can't be read or is not supported.
     */
    public static File getIndex( File vrtFile, File indexFolder ) throws Exception {
        String name = FileUtilities.getNameWithoutExtention(vrtFile);
        File shpFile = new File(indexFolder, name + ".shp");
        File propertiesFile = new File(indexFolder, name + ".properties");
        File sampleImageFile = new File(indexFolder, name + Utils.SAMPLE_IMAGE_NAME);
        if (shpFile.exists() && propertiesFile.exists() && sampleImageFile.exists()
                && shpFile.lastModified() >= vrtFile.lastModified()) {
            return shpFile;
        }
        return createIndex(VrtDataset.read(vrtFile), indexFolder);
    }

    /**
     * Make the index of the mosaic of a VRT, replacing an existing one.
     *
     * @param vrt         the VRT.
     * @param indexFolder the folder of the index, created if missing.
     * @return the shapefile of the mosaic index.
     * @throws Exception if the VRT is not supported or the index can't be written.
     */
    public static File createIndex( VrtDataset vrt, File indexFolder ) throws Exception {
        List<String> problems = checkSupported(vrt);
        if (!problems.isEmpty()) {
            throw new UnsupportedOperationException(
                    "The VRT can't be read as an image mosaic: " + String.join("; ", problems) + " (" + vrt.file + ")");
        }
        CoordinateReferenceSystem crs = parseCrs(vrt.srs);
        VrtDataset.Band firstBand = vrt.bands.get(0);

        // the granules, once per file even if the VRT draws a file more times
        List<String> locations = new ArrayList<>();
        List<Envelope> footprints = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        boolean heterogeneous = false;
        for( VrtDataset.Source source : firstBand.sources() ) {
            String location = location(source);
            if (!seen.add(location)) {
                continue;
            }
            locations.add(location);
            footprints.add(footprint(vrt, source));
            heterogeneous = heterogeneous || isResampled(source);
        }
        boolean remote = isRemote(locations.get(0));

        // sample model and color model of the first file stand for all
        SampleImage sample = readSampleImage(locations.get(0));
        int sampleBands = sample.sampleModel().getNumBands();
        if (sampleBands != vrt.bands.size()) {
            throw new UnsupportedOperationException("The VRT has " + vrt.bands.size() + " bands, its source files "
                    + sampleBands + ": a subset of the bands can't be read as an image mosaic (" + vrt.file + ")");
        }

        if (!indexFolder.exists() && !indexFolder.mkdirs()) {
            throw new IllegalArgumentException("Can't create the folder of the mosaic index: " + indexFolder);
        }
        String name = FileUtilities.getNameWithoutExtention(vrt.file);
        deleteIndex(indexFolder, name);

        File shpFile = new File(indexFolder, name + ".shp");
        writeShapefile(shpFile, name, crs, locations, footprints);
        Utils.storeSampleImage(new File(indexFolder, name + Utils.SAMPLE_IMAGE_NAME), sample.sampleModel(),
                sample.colorModel());
        writeProperties(new File(indexFolder, name + ".properties"), name, vrt, locations, remote, heterogeneous);
        return shpFile;
    }

    /**
     * @param file a file.
     * @return <code>true</code> if it has the .vrt extension.
     */
    public static boolean isVrt( File file ) {
        return file != null && file.getName().toLowerCase(Locale.ROOT).endsWith(".vrt");
    }

    /**
     * Get the reader of the mosaic of a VRT, making its index if needed, with the longitude first
     * axis order as the other raster readers of HortonMachine.
     *
     * @param vrtFile the VRT file.
     * @return the reader, to dispose when done.
     * @throws Exception if the VRT can't be read or is not supported.
     */
    public static ImageMosaicReader getReader( File vrtFile ) throws Exception {
        File shpFile = getIndex(vrtFile);
        ImageMosaicReader reader = new ImageMosaicFormat().getReader(shpFile, HMCrsRegistry.longitudeFirstHints());
        if (reader == null) {
            throw new IllegalArgumentException("Can't open the mosaic of the VRT: " + vrtFile);
        }
        return reader;
    }

    /**
     * Check if a VRT can be served as an image mosaic.
     *
     * @param vrt the VRT.
     * @return what is not supported, empty if all is.
     */
    public static List<String> checkSupported( VrtDataset vrt ) {
        List<String> problems = new ArrayList<>();
        if (vrt.subClass != null) {
            problems.add("the dataset is a " + vrt.subClass);
        }
        if (vrt.geoTransform == null) {
            problems.add("the GeoTransform is missing");
        } else if (!vrt.isNorthUp()) {
            problems.add("the GeoTransform is rotated");
        }
        if (vrt.srs == null) {
            problems.add("the SRS is missing");
        }
        if (vrt.bands.isEmpty()) {
            problems.add("there are no bands");
            return problems;
        }

        List<VrtDataset.Source> firstSources = vrt.bands.get(0).sources();
        if (firstSources.isEmpty()) {
            problems.add("the first band has no sources");
        }
        for( int b = 0; b < vrt.bands.size(); b++ ) {
            VrtDataset.Band band = vrt.bands.get(b);
            int bandNumber = b + 1;
            if (band.subClass() != null) {
                problems.add("band " + band.band() + " is a " + band.subClass());
            }
            List<VrtDataset.Source> sources = band.sources();
            if (sources.size() != firstSources.size()) {
                problems.add("band " + band.band() + " has other sources than band 1");
                continue;
            }
            for( int s = 0; s < sources.size(); s++ ) {
                VrtDataset.Source source = sources.get(s);
                if (!source.path().equals(firstSources.get(s).path())) {
                    problems.add("band " + band.band() + " has other sources than band 1");
                    break;
                }
                if (source.sourceBand() != bandNumber) {
                    problems.add("band " + band.band() + " reads band " + source.sourceBand() + " of " + source.path());
                    break;
                }
            }
            for( VrtDataset.Source source : sources ) {
                if (b == 0) {
                    checkSource(source, problems);
                } else if (source.transformsValues()) {
                    problems.add("the values of " + source.path() + " are transformed in band " + band.band());
                }
            }
        }

        // the mosaic reads all the granules in the same way, as files or as remote GeoTIFFs
        long remoteCount = firstSources.stream().map(VrtImageMosaic::location).filter(l -> l != null && isRemote(l))
                .count();
        if (remoteCount > 0 && remoteCount < firstSources.size()) {
            problems.add("local files and /vsicurl/ sources are mixed");
        }
        return problems;
    }

    private static void checkSource( VrtDataset.Source source, List<String> problems ) {
        if (!source.kind().equals("SimpleSource") && !source.kind().equals("ComplexSource")
                && !source.kind().equals("AveragedSource")) {
            problems.add("the " + source.kind() + " of " + source.path() + " is not supported");
        }
        if (source.transformsValues()) {
            problems.add("the values of " + source.path() + " are transformed");
        }
        String location = location(source);
        if (location == null) {
            problems.add("the GDAL virtual path " + source.path() + " is not supported, only /vsicurl/ to http(s) GeoTIFFs");
            return;
        }
        if (location.length() > MAX_LOCATION_LENGTH) {
            problems.add("the location of " + source.path() + " is longer than " + MAX_LOCATION_LENGTH + " characters");
        }
        if (!isRemote(location) && !new File(location).exists()) {
            problems.add("the source file " + source.path() + " doesn't exist");
        }
    }

    /**
     * Get where the mosaic reads a source from.
     *
     * @param source the source.
     * @return the absolute path of a file, or the http(s) address of a
     *          <code>/vsicurl/</code> GeoTIFF, or <code>null</code> for the other GDAL virtual
     *          paths, which can't be read.
     */
    static String location( VrtDataset.Source source ) {
        String path = source.path();
        if (!source.isVirtualPath()) {
            return new File(path).getAbsolutePath();
        }
        if (!path.startsWith(VSICURL)) {
            return null;
        }
        String url = path.substring(VSICURL.length());
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            // also the /vsicurl?url=...&options form
            return null;
        }
        int queryIndex = lowerUrl.indexOf('?');
        String lowerPath = queryIndex < 0 ? lowerUrl : lowerUrl.substring(0, queryIndex);
        if (!lowerPath.endsWith(".tif") && !lowerPath.endsWith(".tiff")) {
            // the remote granules are read by the reader of the cloud optimized GeoTIFFs
            return null;
        }
        return url;
    }

    private static boolean isRemote( String location ) {
        return location.startsWith("http://") || location.startsWith("https://");
    }

    /**
     * The extent of the whole source file in the VRT, also when only a window of it is drawn,
     * since the mosaic reads the whole file.
     */
    static Envelope footprint( VrtDataset vrt, VrtDataset.Source source ) {
        double[] dst = source.dstRect() != null ? source.dstRect() : new double[]{0, 0, vrt.width, vrt.height};
        double[] src = source.srcRect();
        double colStart = dst[0];
        double rowStart = dst[1];
        double colEnd = dst[0] + dst[2];
        double rowEnd = dst[1] + dst[3];
        if (src != null && src[2] > 0 && src[3] > 0) {
            double scaleX = dst[2] / src[2];
            double scaleY = dst[3] / src[3];
            colStart = dst[0] - src[0] * scaleX;
            rowStart = dst[1] - src[1] * scaleY;
            if (source.sourceWidth() > 0 && source.sourceHeight() > 0) {
                colEnd = colStart + source.sourceWidth() * scaleX;
                rowEnd = rowStart + source.sourceHeight() * scaleY;
            }
        }
        double[] gt = vrt.geoTransform;
        double x1 = gt[0] + colStart * gt[1];
        double x2 = gt[0] + colEnd * gt[1];
        double y1 = gt[3] + rowStart * gt[5];
        double y2 = gt[3] + rowEnd * gt[5];
        return new Envelope(x1, x2, y1, y2);
    }

    private static boolean isResampled( VrtDataset.Source source ) {
        double[] src = source.srcRect();
        double[] dst = source.dstRect();
        if (src == null || dst == null) {
            return false;
        }
        return Math.abs(dst[2] - src[2]) > 1E-6 || Math.abs(dst[3] - src[3]) > 1E-6;
    }

    /** The EPSG code at the end of a WKT, the one of the whole CRS: AUTHORITY["EPSG","32632"]] or ID["EPSG",32632]]. */
    private static final Pattern TOP_EPSG = Pattern
            .compile("(?:AUTHORITY|ID)\\[\\s*\"EPSG\"\\s*,\\s*\"?(\\d+)\"?\\s*\\]\\s*\\]\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EPSG_CODE = Pattern.compile("^EPSG:(\\d+)$", Pattern.CASE_INSENSITIVE);

    /**
     * Get the CRS of the SRS of a VRT: from its EPSG code if it has one, so that it is decoded
     * with the longitude first as all the CRSs of HortonMachine, else from its WKT.
     *
     * @param srs the SRS text.
     * @return the CRS.
     * @throws Exception if it can't be decoded.
     */
    static CoordinateReferenceSystem parseCrs( String srs ) throws Exception {
        String text = srs.trim();
        Matcher matcher = EPSG_CODE.matcher(text);
        if (!matcher.matches()) {
            matcher = TOP_EPSG.matcher(text);
        }
        if (matcher.find(0)) {
            try {
                return HMCrsRegistry.INSTANCE.getCrs("EPSG:" + matcher.group(1));
            } catch (Exception e) {
                // not a known code, try the WKT
            }
        }
        return CRS.parseWKT(text);
    }

    /** The image layout the mosaic needs to know before reading any granule. */
    private record SampleImage( SampleModel sampleModel, ColorModel colorModel ) {
    }

    /**
     * The layout of the image of a source: the reader decodes no pixels for it, of a remote
     * GeoTIFF it reads only the header.
     */
    private static SampleImage readSampleImage( String location ) throws Exception {
        AbstractGridCoverage2DReader reader;
        if (isRemote(location)) {
            reader = GeotiffHandler.createCogReader(location, null, null);
        } else {
            File file = new File(location);
            Hints hints = HMCrsRegistry.longitudeFirstHints();
            AbstractGridFormat format = GridFormatFinder.findFormat(file, hints);
            if (format == null || format.getClass().getSimpleName().equals("UnknownFormat")) {
                throw new IllegalArgumentException("No reader for the source file: " + file);
            }
            reader = format.getReader(file, hints);
        }
        if (reader == null) {
            throw new IllegalArgumentException("No reader for the source: " + location);
        }
        try {
            GridCoverage2D coverage = reader.read((GeneralParameterValue[]) null);
            RenderedImage image = coverage.getRenderedImage();
            SampleImage sampleImage = new SampleImage(image.getSampleModel(), image.getColorModel());
            coverage.dispose(true);
            return sampleImage;
        } finally {
            reader.dispose();
        }
    }

    private static void writeShapefile( File shpFile, String name, CoordinateReferenceSystem crs, List<String> locations,
            List<Envelope> footprints ) throws Exception {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(name);
        typeBuilder.setCRS(crs);
        typeBuilder.add("the_geom", Polygon.class);
        typeBuilder.length(MAX_LOCATION_LENGTH).add(LOCATION_FIELD, String.class);
        SimpleFeatureType type = typeBuilder.buildFeatureType();

        // the mosaic draws the first granule on top, the VRT the last source
        ListFeatureCollection collection = new ListFeatureCollection(type);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for( int i = locations.size() - 1; i >= 0; i-- ) {
            builder.add(FeatureUtilities.envelopeToPolygon(footprints.get(i)));
            builder.add(locations.get(i));
            collection.add(builder.buildFeature(null));
        }

        ShapefileDataStore store = new ShapefileDataStore(shpFile.toURI().toURL());
        try {
            store.createSchema(type);
            store.forceSchemaCRS(crs);
            try (Transaction transaction = new DefaultTransaction()) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) store.getFeatureSource();
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception e) {
                    transaction.rollback();
                    throw e;
                }
            }
        } finally {
            store.dispose();
        }
    }

    private static void writeProperties( File propertiesFile, String name, VrtDataset vrt, List<String> locations,
            boolean remote, boolean heterogeneous ) throws Exception {
        double xRes = Math.abs(vrt.geoTransform[1]);
        double yRes = Math.abs(vrt.geoTransform[5]);
        List<String> lines = new ArrayList<>();
        lines.add("#Image mosaic of the VRT " + vrt.file.getAbsolutePath().replace('\\', '/'));
        lines.add("Name=" + name);
        lines.add("TypeName=" + name);
        lines.add("LocationAttribute=" + LOCATION_FIELD);
        if (remote) {
            // remote cloud optimized GeoTIFFs, read with http range requests
            lines.add("PathType=URL");
            lines.add("Cog=true");
            lines.add("CogRangeReader=" + COG_RANGE_READER);
        } else {
            lines.add("PathType=ABSOLUTE");
            lines.add("AbsolutePath=true");
        }
        lines.add("LevelsNum=1");
        lines.add("Levels=" + xRes + "," + yRes);
        lines.add("Heterogeneous=" + heterogeneous);
        lines.add("HeterogeneousCRS=false");
        lines.add("ExpandToRGB=false");
        lines.add("Caching=false");
        lines.add("CheckAuxiliaryMetadata=false");
        boolean allTiff = locations.stream().map(p -> p.toLowerCase(Locale.ROOT))
                .allMatch(p -> p.endsWith(".tif") || p.endsWith(".tiff"));
        if (remote) {
            lines.add("SuggestedSPI=" + COG_SPI);
            lines.add("SuggestedFormat=" + GEOTIFF_FORMAT);
        } else if (allTiff) {
            lines.add("SuggestedSPI=" + TIFF_SPI);
            lines.add("SuggestedFormat=" + GEOTIFF_FORMAT);
        }
        Double noData = noData(vrt);
        if (noData != null) {
            lines.add("NoData=" + noData);
        }
        FileUtilities.writeFile(lines, propertiesFile);
    }

    /**
     * The nodata of the first band, or of its first source that has one.
     */
    private static Double noData( VrtDataset vrt ) {
        VrtDataset.Band band = vrt.bands.get(0);
        if (band.noData() != null) {
            return band.noData();
        }
        return band.sources().stream().map(VrtDataset.Source::noData).filter(n -> n != null).findFirst().orElse(null);
    }

    /**
     * Delete the files of an index made before, as the reader adds its own next to the shapefile.
     */
    private static void deleteIndex( File indexFolder, String name ) throws Exception {
        File[] files = indexFolder.listFiles();
        if (files == null) {
            return;
        }
        String sampleImageName = name + Utils.SAMPLE_IMAGE_NAME;
        for( File file : files ) {
            String fileName = file.getName();
            if (file.isFile() && (fileName.startsWith(name + ".") || fileName.equals(sampleImageName))) {
                FileUtilities.deleteFileOrDir(file);
            }
        }
    }
}
