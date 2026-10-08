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
package org.hortonmachine.webmaps;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.Map;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.RandomIterFactory;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.locationtech.jts.geom.Envelope;

/**
 * Renders a coverage into an image to look at: rgb and paletted images as they are, single band
 * data stretched on a color ramp between its min and max, novalues transparent.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class RasterPreview {
    /** Larger rasters are subsampled. */
    private static final long MAX_PIXELS = 2_000_000L;
    /** Larger rasters are not reprojected to be drawn on the map. */
    private static final long MAX_REPROJECT_PIXELS = 16_000_000L;

    private static final int[][] RAMP = {//
            {44, 123, 182}, //
            {171, 217, 233}, //
            {255, 255, 191}, //
            {253, 174, 97}, //
            {215, 25, 28}//
    };

    /** The image of the raster as it is. */
    BufferedImage image;
    /** The image reprojected to WGS84, null if not available. */
    public BufferedImage wgs84Image;
    /** The lon/lat area of {@link #wgs84Image}, or the footprint of the raster if the image is not available. */
    public Envelope wgs84Envelope;
    int cols;
    int rows;
    int bands;
    String dataType;
    String crs;
    ReferencedEnvelope envelope;
    Double novalue;
    /** The range of the first band, null for rgb and paletted images. */
    public double[] range;
    /** Why the image could not be placed on the map, null if all went well. */
    public String note;
    /** The argb colors of the classes of categorical data, null for continuous data. */
    Map<Integer, Integer> classColors;

    public static RasterPreview create( GridCoverage2D coverage ) throws Exception {
        return create(coverage, null);
    }

    /**
     * @param coverage the raster.
     * @param classColors for categorical data, the argb color of each class value: values without a
     *          class are transparent. If null, the data are stretched on a color ramp.
     */
    public static RasterPreview create( GridCoverage2D coverage, Map<Integer, Integer> classColors ) throws Exception {
        RasterPreview preview = new RasterPreview();
        preview.classColors = classColors;
        RenderedImage ri = coverage.getRenderedImage();
        preview.cols = ri.getWidth();
        preview.rows = ri.getHeight();
        preview.bands = ri.getSampleModel().getNumBands();
        preview.dataType = dataTypeName(ri.getSampleModel().getDataType());
        preview.envelope = new ReferencedEnvelope(coverage.getEnvelope2D());
        preview.novalue = CoverageUtilities.getNovalue(coverage);
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        preview.crs = crsName(crs);
        preview.image = render(ri, preview.novalue, preview);

        if (crs == null) {
            preview.note = "The raster has no CRS, it can't be placed on the map.";
            return preview;
        }
        ReferencedEnvelope wgs84 = preview.envelope.transform(DefaultGeographicCRS.WGS84, true);
        preview.wgs84Envelope = new Envelope(wgs84.getMinX(), wgs84.getMaxX(), wgs84.getMinY(), wgs84.getMaxY());
        if (HMCrsRegistry.crsEquals(crs, DefaultGeographicCRS.WGS84)) {
            preview.wgs84Image = preview.image;
        } else if ((long) preview.cols * preview.rows <= MAX_REPROJECT_PIXELS) {
            GridCoverage2D geographic = (GridCoverage2D) Operations.DEFAULT.resample(coverage, DefaultGeographicCRS.WGS84);
            ReferencedEnvelope geoEnv = new ReferencedEnvelope(geographic.getEnvelope2D());
            preview.wgs84Envelope = new Envelope(geoEnv.getMinX(), geoEnv.getMaxX(), geoEnv.getMinY(), geoEnv.getMaxY());
            // same stretch as the original, so the colors match
            preview.wgs84Image = render(geographic.getRenderedImage(), preview.novalue, preview.range, classColors);
        } else {
            preview.note = "The raster is too large to be reprojected for the map, only its footprint is shown.";
        }
        return preview;
    }

    private static BufferedImage render( RenderedImage ri, Double novalue, RasterPreview stats ) {
        double[] range = null;
        if (stats.classColors == null && !isRgb(ri) && !isPaletted(ri)) {
            range = computeRange(ri, novalue);
            stats.range = range;
        }
        return render(ri, novalue, range, stats.classColors);
    }

    private static boolean isRgb( RenderedImage ri ) {
        return ri.getSampleModel().getNumBands() >= 3 && ri.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE;
    }

    private static boolean isPaletted( RenderedImage ri ) {
        return ri.getSampleModel().getNumBands() == 1 && ri.getColorModel() instanceof IndexColorModel;
    }

    private static int step( RenderedImage ri ) {
        long pixels = (long) ri.getWidth() * ri.getHeight();
        return Math.max(1, (int) Math.ceil(Math.sqrt((double) pixels / MAX_PIXELS)));
    }

    private static boolean isNovalue( double value, Double novalue ) {
        return Double.isNaN(value) || Double.isInfinite(value) || (novalue != null && value == novalue);
    }

    private static double[] computeRange( RenderedImage ri, Double novalue ) {
        int step = step(ri);
        RandomIter iter = RandomIterFactory.create(ri, null);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        try {
            for( int y = ri.getMinY(); y < ri.getMinY() + ri.getHeight(); y += step ) {
                for( int x = ri.getMinX(); x < ri.getMinX() + ri.getWidth(); x += step ) {
                    double v = iter.getSampleDouble(x, y, 0);
                    if (isNovalue(v, novalue))
                        continue;
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
        } finally {
            iter.done();
        }
        return min <= max ? new double[]{min, max} : null;
    }

    private static BufferedImage render( RenderedImage ri, Double novalue, double[] range, Map<Integer, Integer> classColors ) {
        int step = step(ri);
        int width = (ri.getWidth() + step - 1) / step;
        int height = (ri.getHeight() + step - 1) / step;
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int bands = ri.getSampleModel().getNumBands();
        boolean rgb = isRgb(ri);
        ColorModel cm = ri.getColorModel();
        IndexColorModel palette = isPaletted(ri) ? (IndexColorModel) cm : null;
        RandomIter iter = RandomIterFactory.create(ri, null);
        try {
            for( int oy = 0; oy < height; oy++ ) {
                int y = ri.getMinY() + oy * step;
                for( int ox = 0; ox < width; ox++ ) {
                    int x = ri.getMinX() + ox * step;
                    int argb;
                    if (classColors != null) {
                        double v = iter.getSampleDouble(x, y, 0);
                        Integer color = isNovalue(v, novalue) ? null : classColors.get((int) Math.round(v));
                        argb = color != null ? color : 0;
                    } else if (palette != null) {
                        argb = palette.getRGB(iter.getSample(x, y, 0));
                    } else if (rgb) {
                        int a = bands >= 4 ? iter.getSample(x, y, 3) : 255;
                        argb = (a << 24) | (iter.getSample(x, y, 0) << 16) | (iter.getSample(x, y, 1) << 8) | iter.getSample(x, y, 2);
                    } else {
                        double v = iter.getSampleDouble(x, y, 0);
                        argb = isNovalue(v, novalue) || range == null ? 0 : rampColor(v, range);
                    }
                    out.setRGB(ox, oy, argb);
                }
            }
        } finally {
            iter.done();
        }
        return out;
    }

    private static int rampColor( double value, double[] range ) {
        double t = range[1] > range[0] ? (value - range[0]) / (range[1] - range[0]) : 0.5;
        t = Math.max(0, Math.min(1, t)) * (RAMP.length - 1);
        int i = Math.min((int) t, RAMP.length - 2);
        double f = t - i;
        int r = (int) Math.round(RAMP[i][0] + f * (RAMP[i + 1][0] - RAMP[i][0]));
        int g = (int) Math.round(RAMP[i][1] + f * (RAMP[i + 1][1] - RAMP[i][1]));
        int b = (int) Math.round(RAMP[i][2] + f * (RAMP[i + 1][2] - RAMP[i][2]));
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private static String crsName( CoordinateReferenceSystem crs ) {
        if (crs == null)
            return "none";
        try {
            String code = HMCrsRegistry.getCodeFromCrs(crs);
            if (code != null)
                return code;
        } catch (Exception e) {
            // use the name
        }
        return crs.getName().toString();
    }

    private static String dataTypeName( int dataType ) {
        switch( dataType ) {
        case DataBuffer.TYPE_BYTE:
            return "byte";
        case DataBuffer.TYPE_SHORT:
            return "short";
        case DataBuffer.TYPE_USHORT:
            return "ushort";
        case DataBuffer.TYPE_INT:
            return "int";
        case DataBuffer.TYPE_FLOAT:
            return "float";
        case DataBuffer.TYPE_DOUBLE:
            return "double";
        default:
            return "unknown";
        }
    }
}
