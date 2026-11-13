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

import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.GeneralBounds;
import org.geotools.util.factory.Hints;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * Writes a GDAL virtual raster (VRT) that mosaics a set of georeferenced images, as
 * <code>gdalbuildvrt</code> does, without GDAL.
 *
 * <p>No pixel is read: of each image only the georeferencing and the layout (size, bands, data
 * type, nodata) are read from its header. The images need to share the coordinate reference
 * system, the number of bands and the data type, while their resolutions can differ: the VRT
 * takes the finest one and the coarser images are drawn resampled.</p>
 *
 * <p>The images are drawn in the given order, the later ones over the earlier ones where they
 * overlap. If an image has a nodata value, its nodata cells let the images below show
 * through. The images in the folder of the VRT or below it are written relative to it, so the
 * folder can be moved as a whole, the others with their absolute path.</p>
 *
 * <p>The VRT can be read by GDAL and QGIS and by HortonMachine, through {@link VrtImageMosaic}.</p>
 */
public class VrtWriter {

    /** The tolerance under which a pixel position is taken as a whole number. */
    private static final double PIXEL_EPSILON = 1E-6;

    private VrtWriter() {
    }

    /** What the VRT needs of an image, read from its header. */
    private record ImageInfo( File file, GeneralBounds envelope, int cols, int rows, CoordinateReferenceSystem crs,
            int bands, int dataType, Double noData ) {
    }

    /**
     * Write the VRT mosaic of a set of images.
     *
     * @param pm      optional progress monitor.
     * @param images  the images, in drawing order (the last on top).
     * @param vrtFile the VRT file to write.
     * @throws Exception if an image can't be read or doesn't fit with the others.
     */
    public static void writeMosaic( IHMProgressMonitor pm, List<File> images, File vrtFile ) throws Exception {
        if (pm == null) {
            pm = new DummyProgressMonitor();
        }
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("No images to write in the VRT.");
        }

        List<ImageInfo> infos = new ArrayList<>();
        pm.beginTask("Reading the image headers...", images.size());
        try {
            for( File image : images ) {
                infos.add(readInfo(image));
                pm.worked(1);
            }
        } finally {
            pm.done();
        }

        ImageInfo first = infos.get(0);
        double xRes = Double.POSITIVE_INFINITY;
        double yRes = Double.POSITIVE_INFINITY;
        double west = Double.POSITIVE_INFINITY;
        double east = Double.NEGATIVE_INFINITY;
        double south = Double.POSITIVE_INFINITY;
        double north = Double.NEGATIVE_INFINITY;
        for( ImageInfo info : infos ) {
            if (!HMCrsRegistry.crsEquals(first.crs(), info.crs())) {
                throw new IllegalArgumentException(
                        "The images need the same CRS: " + info.file() + " has another one than " + first.file());
            }
            if (info.bands() != first.bands()) {
                throw new IllegalArgumentException("The images need the same number of bands: " + info.file() + " has "
                        + info.bands() + ", " + first.file() + " " + first.bands());
            }
            if (info.dataType() != first.dataType()) {
                throw new IllegalArgumentException("The images need the same data type: " + info.file() + " has "
                        + gdalDataType(info.dataType()) + ", " + first.file() + " " + gdalDataType(first.dataType()));
            }
            GeneralBounds env = info.envelope();
            xRes = Math.min(xRes, env.getSpan(0) / info.cols());
            yRes = Math.min(yRes, env.getSpan(1) / info.rows());
            west = Math.min(west, env.getMinimum(0));
            east = Math.max(east, env.getMaximum(0));
            south = Math.min(south, env.getMinimum(1));
            north = Math.max(north, env.getMaximum(1));
        }
        int width = (int) Math.ceil(snap((east - west) / xRes));
        int height = (int) Math.ceil(snap((north - south) / yRes));
        String dataType = gdalDataType(first.dataType());
        Double noData = first.noData();

        File vrtFolder = vrtFile.getAbsoluteFile().getParentFile();
        StringBuilder sb = new StringBuilder();
        sb.append("<VRTDataset rasterXSize=\"").append(width).append("\" rasterYSize=\"").append(height).append("\">\n");
        sb.append("  <SRS dataAxisToSRSAxisMapping=\"1,2\">").append(escape(first.crs().toWKT())).append("</SRS>\n");
        sb.append("  <GeoTransform>").append(number(west)).append(", ").append(number(xRes)).append(", 0.0, ")
                .append(number(north)).append(", 0.0, ").append(number(-yRes)).append("</GeoTransform>\n");
        for( int band = 1; band <= first.bands(); band++ ) {
            sb.append("  <VRTRasterBand dataType=\"").append(dataType).append("\" band=\"").append(band).append("\">\n");
            if (noData != null) {
                sb.append("    <NoDataValue>").append(number(noData)).append("</NoDataValue>\n");
            }
            for( ImageInfo info : infos ) {
                appendSource(sb, info, band, dataType, vrtFolder, west, north, xRes, yRes);
            }
            sb.append("  </VRTRasterBand>\n");
        }
        sb.append("</VRTDataset>\n");
        FileUtilities.writeFile(sb.toString(), vrtFile);
    }

    private static void appendSource( StringBuilder sb, ImageInfo info, int band, String dataType, File vrtFolder,
            double west, double north, double xRes, double yRes ) {
        GeneralBounds env = info.envelope();
        double xOff = snap((env.getMinimum(0) - west) / xRes);
        double yOff = snap((north - env.getMaximum(1)) / yRes);
        double xSize = snap(env.getSpan(0) / xRes);
        double ySize = snap(env.getSpan(1) / yRes);
        // the nodata cells of a complex source are transparent
        String kind = info.noData() != null ? "ComplexSource" : "SimpleSource";

        sb.append("    <").append(kind).append(">\n");
        sb.append("      ").append(sourceFilename(info.file(), vrtFolder)).append("\n");
        sb.append("      <SourceBand>").append(band).append("</SourceBand>\n");
        sb.append("      <SourceProperties RasterXSize=\"").append(info.cols()).append("\" RasterYSize=\"")
                .append(info.rows()).append("\" DataType=\"").append(dataType).append("\" />\n");
        sb.append("      <SrcRect xOff=\"0\" yOff=\"0\" xSize=\"").append(info.cols()).append("\" ySize=\"")
                .append(info.rows()).append("\" />\n");
        sb.append("      <DstRect xOff=\"").append(number(xOff)).append("\" yOff=\"").append(number(yOff))
                .append("\" xSize=\"").append(number(xSize)).append("\" ySize=\"").append(number(ySize)).append("\" />\n");
        if (info.noData() != null) {
            sb.append("      <NODATA>").append(number(info.noData())).append("</NODATA>\n");
        }
        sb.append("    </").append(kind).append(">\n");
    }

    /**
     * The SourceFilename element: relative to the VRT if the image is in its folder or below.
     */
    private static String sourceFilename( File image, File vrtFolder ) {
        Path imagePath = image.getAbsoluteFile().toPath().normalize();
        Path folderPath = vrtFolder.toPath().normalize();
        if (imagePath.startsWith(folderPath)) {
            String relative = folderPath.relativize(imagePath).toString().replace('\\', '/');
            return "<SourceFilename relativeToVRT=\"1\">" + escape(relative) + "</SourceFilename>";
        }
        String absolute = imagePath.toString().replace('\\', '/');
        return "<SourceFilename relativeToVRT=\"0\">" + escape(absolute) + "</SourceFilename>";
    }

    /**
     * Read the header of an image: the reader decodes no pixels for the layout.
     */
    private static ImageInfo readInfo( File image ) throws Exception {
        Hints hints = HMCrsRegistry.longitudeFirstHints();
        AbstractGridFormat format = GridFormatFinder.findFormat(image, hints);
        if (format == null || format.getClass().getSimpleName().equals("UnknownFormat")) {
            throw new IllegalArgumentException("No reader for the image: " + image);
        }
        AbstractGridCoverage2DReader reader = format.getReader(image, hints);
        if (reader == null) {
            throw new IllegalArgumentException("No reader for the image: " + image);
        }
        try {
            GeneralBounds envelope = reader.getOriginalEnvelope();
            GridEnvelope gridRange = reader.getOriginalGridRange();
            GridCoverage2D coverage = reader.read((GeneralParameterValue[]) null);
            try {
                SampleModel sampleModel = coverage.getRenderedImage().getSampleModel();
                Double noData = CoverageUtilities.getNovalue(coverage);
                return new ImageInfo(image, envelope, gridRange.getSpan(0), gridRange.getSpan(1),
                        reader.getCoordinateReferenceSystem(), sampleModel.getNumBands(), sampleModel.getDataType(), noData);
            } finally {
                coverage.dispose(true);
            }
        } finally {
            reader.dispose();
        }
    }

    /**
     * @param dataType a {@link DataBuffer} data type.
     * @return the GDAL name of the data type.
     */
    static String gdalDataType( int dataType ) {
        switch( dataType ) {
        case DataBuffer.TYPE_BYTE:
            return "Byte";
        case DataBuffer.TYPE_USHORT:
            return "UInt16";
        case DataBuffer.TYPE_SHORT:
            return "Int16";
        case DataBuffer.TYPE_INT:
            return "Int32";
        case DataBuffer.TYPE_FLOAT:
            return "Float32";
        case DataBuffer.TYPE_DOUBLE:
            return "Float64";
        default:
            throw new IllegalArgumentException("Data type not supported in a VRT: " + dataType);
        }
    }

    /**
     * @return the value rounded to the closest whole number if it is within the pixel tolerance of it.
     */
    private static double snap( double value ) {
        double rounded = Math.rint(value);
        return Math.abs(value - rounded) < PIXEL_EPSILON ? rounded : value;
    }

    /**
     * @return the number as text, whole numbers without decimals, NaN as GDAL writes it.
     */
    private static String number( double value ) {
        if (Double.isNaN(value)) {
            return "nan";
        }
        if (value == Math.rint(value) && Math.abs(value) < 1E15) {
            return String.valueOf((long) value);
        }
        // the shortest text that reads back as the same double
        return Double.toString(value);
    }

    private static String escape( String text ) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
