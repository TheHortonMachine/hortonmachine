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
package org.hortonmachine.nww.layers.defaults.other;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.utils.LasConstraints;
import org.hortonmachine.gears.io.las.utils.LasPointColumns;
import org.hortonmachine.gears.ui.progress.IProgressPrinter;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;

/**
 * A {@link PointCloudLayer} of las points, colored and sliced by elevation, intensity,
 * classification, return number, or drawn with their own colors.
 *
 * <p>Elevation and intensity are colored by default between the {@link #AUTO_CUT_PERCENT} and
 * 100 - {@link #AUTO_CUT_PERCENT} percentiles, so that a few outliers do not leave all the other
 * points of the same color. Any other range can be given.</p>
 */
public class LasPointCloudLayer extends PointCloudLayer {

    /** The percent of points at each end left out of the default range of the colors. */
    public static final double AUTO_CUT_PERCENT = 0.5;
    /** The values sampled to compute the percentiles. */
    private static final int PERCENTILE_SAMPLES = 1_000_000;
    /** The colors of the table of colors used for the points. */
    private static final int COLOR_STEPS = 4096;
    /** The points transformed at once, to keep the temporary memory low. */
    private static final int TRANSFORM_CHUNK = 1 << 20;

    /** The value used to color and slice the points. */
    public enum ColorMode {
        ELEVATION("elevation", "%.2f", true), //
        INTENSITY("intensity", "%.0f", true), //
        CLASSIFICATION("classification", "%.0f", false), //
        IMPULSE("return number", "%.0f", false), //
        /** The own colors of the points, sliced by elevation. */
        RGB("own colors", "%.2f", true);

        private final String description;
        /** The format of the values. */
        public final String format;
        /** If the default range leaves out the extreme values, else it goes from minimum to maximum. */
        private final boolean percentileRange;

        ColorMode( String description, String format, boolean percentileRange ) {
            this.description = description;
            this.format = format;
            this.percentileRange = percentileRange;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * A transform of interleaved x/y coordinates to longitude/latitude, in place.
     */
    public interface XyTransform {
        void transform( double[] xy ) throws Exception;
    }

    private LasPointColumns points;
    private Boolean hasGroundHeights;
    private ColorMode colorMode = ColorMode.ELEVATION;
    private double[] colorRange;
    private boolean drawnNormalized;
    private double drawnMinZ = Double.NaN;
    private double drawnMaxZ = Double.NaN;

    /**
     * @param title the name of the layer.
     */
    public LasPointCloudLayer( String title ) {
        super(title);
    }

    /**
     * Read the points of a las file into a new layer, colored with the default range.
     *
     * @param title the name of the layer.
     * @param reader the reader of the file, rewound at the end.
     * @param crs the CRS of the file.
     * @param sampling read one point every this, 1 to read all.
     * @param mode the coloring.
     * @param progress the monitor, as for {@link LasConstraints#applyConstraints(ALasReader, boolean, IProgressPrinter)}, can be <code>null</code>.
     * @return the layer, empty if the file has no points.
     */
    public static LasPointCloudLayer fromReader( String title, ALasReader reader, CoordinateReferenceSystem crs, int sampling,
            ColorMode mode, IProgressPrinter progress ) throws Exception {
        LasConstraints constraints = new LasConstraints();
        constraints.setSampling(sampling > 1 ? sampling : null);
        constraints.applyConstraints(reader, true, progress);
        LasPointColumns columns = constraints.getFilteredColumns();

        MathTransform toLonLat = new HMCrsTransformer(crs, HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true))
                .getMathTransform();
        LasPointCloudLayer layer = new LasPointCloudLayer(title);
        if (columns != null && columns.size() > 0) {
            layer.setLasPoints(columns, xy -> toLonLat.transform(xy, 0, xy, 0, xy.length / 2), mode, false, false, null);
        }
        return layer;
    }

    /**
     * Set the points, colored with the given mode.
     *
     * @param points the points, kept by the layer and not to be modified.
     * @param toLonLat the transform of their coordinates to longitude/latitude.
     * @param mode the coloring.
     * @param normalized if <code>true</code>, the heights over the ground of the points are used
     *            for slicing and for the colors by elevation, where they are known.
     * @param drawNormalized if <code>true</code>, the points are drawn at their height over the
     *            ground, where it is known.
     * @param colorRange the lowest and highest value of the colors, <code>null</code> for the default.
     */
    public void setLasPoints( LasPointColumns points, XyTransform toLonLat, ColorMode mode, boolean normalized,
            boolean drawNormalized, double[] colorRange ) throws Exception {
        int n = points.size();
        double[] lons = new double[n];
        double[] lats = new double[n];
        double[] xy = new double[Math.min(n, TRANSFORM_CHUNK) * 2];
        for( int start = 0; start < n; start += TRANSFORM_CHUNK ) {
            int count = Math.min(TRANSFORM_CHUNK, n - start);
            double[] chunk = count * 2 == xy.length ? xy : new double[count * 2];
            for( int i = 0; i < count; i++ ) {
                chunk[i * 2] = points.x[start + i];
                chunk[i * 2 + 1] = points.y[start + i];
            }
            toLonLat.transform(chunk);
            for( int i = 0; i < count; i++ ) {
                lons[start + i] = chunk[i * 2];
                lats[start + i] = chunk[i * 2 + 1];
            }
        }
        this.points = points;
        hasGroundHeights = null;
        Colors colors = computeColors(mode, normalized, colorRange);
        double[] elevs = computeElevations(drawNormalized);
        setPoints(lons, lats, elevs, colors.values, colors.rgbs);
        colorMode = mode;
        this.colorRange = colors.range;
        drawnNormalized = drawNormalized;
    }

    /**
     * Color the points again.
     *
     * @param mode the coloring.
     * @param normalized if <code>true</code>, the heights over the ground of the points are used
     *            for slicing and for the colors by elevation, where they are known.
     * @param drawNormalized if <code>true</code>, the points are drawn at their height over the
     *            ground, where it is known.
     * @param colorRange the lowest and highest value of the colors, <code>null</code> for the default.
     */
    public void setColoring( ColorMode mode, boolean normalized, boolean drawNormalized, double[] colorRange ) {
        if (points == null || getPointsCount() == 0) {
            return;
        }
        Colors colors = computeColors(mode, normalized, colorRange);
        if (drawNormalized != drawnNormalized) {
            setElevations(computeElevations(drawNormalized));
            drawnNormalized = drawNormalized;
        }
        setValuesAndColors(colors.values, colors.rgbs);
        colorMode = mode;
        this.colorRange = colors.range;
    }

    @Override
    public void clear() {
        super.clear();
        points = null;
        hasGroundHeights = null;
        colorRange = null;
        drawnMinZ = Double.NaN;
        drawnMaxZ = Double.NaN;
    }

    @Override
    public void freeResources() {
        super.freeResources();
        points = null;
    }

    /**
     * @return the points, or <code>null</code>.
     */
    public LasPointColumns getLasPoints() {
        return points;
    }

    public ColorMode getColorMode() {
        return colorMode;
    }

    /**
     * @return the lowest and highest value of the colors of the last coloring, or <code>null</code>.
     */
    public double[] getColorRange() {
        return colorRange == null ? null : colorRange.clone();
    }

    /**
     * @return the lowest and highest elevation the points are drawn at.
     */
    public double[] getDrawnElevationRange() {
        return new double[]{drawnMinZ, drawnMaxZ};
    }

    /**
     * @return <code>true</code> if some point has a height over the ground.
     */
    public boolean hasGroundHeights() {
        LasPointColumns p = points;
        if (p == null) {
            return false;
        }
        if (hasGroundHeights == null) {
            boolean found = false;
            for( int i = 0; i < p.size() && !found; i++ ) {
                found = !Float.isNaN(p.groundHeight[i]);
            }
            hasGroundHeights = found;
        }
        return hasGroundHeights;
    }

    private static class Colors {
        float[] values;
        int[] rgbs;
        double[] range;
    }

    private Colors computeColors( ColorMode mode, boolean normalized, double[] colorRange ) {
        LasPointColumns p = points;
        int n = p.size();
        boolean useHeights = normalized && hasGroundHeights();
        float[] values = new float[n];
        IntStream.range(0, n).parallel().forEach(i -> {
            switch( mode ) {
            case INTENSITY:
                values[i] = p.intensity[i];
                break;
            case CLASSIFICATION:
                values[i] = p.classification[i];
                break;
            case IMPULSE:
                values[i] = p.returnNumber[i];
                break;
            default:
                float height = useHeights ? p.groundHeight[i] : Float.NaN;
                values[i] = Float.isNaN(height) ? (float) p.z[i] : height;
                break;
            }
        });

        double[] range = colorRange != null ? colorRange.clone() : defaultRange(values, mode.percentileRange);
        if (!(range[1] > range[0])) {
            range = new double[]{range[0] - 0.5, range[0] + 0.5};
        }

        int[] rgbs = new int[n];
        if (mode == ColorMode.RGB) {
            // las colors can be 8 or 16 bits
            boolean sixteenBits = IntStream.range(0, n).parallel()
                    .anyMatch(i -> (p.red[i] & 0xffff) > 255 || (p.green[i] & 0xffff) > 255 || (p.blue[i] & 0xffff) > 255);
            int shift = sixteenBits ? 8 : 0;
            IntStream.range(0, n).parallel().forEach(i -> {
                int r = Math.min(255, (p.red[i] & 0xffff) >> shift);
                int g = Math.min(255, (p.green[i] & 0xffff) >> shift);
                int b = Math.min(255, (p.blue[i] & 0xffff) >> shift);
                rgbs[i] = (r << 16) | (g << 8) | b;
            });
        } else {
            String table = mode == ColorMode.ELEVATION ? EColorTables.elev.name() : EColorTables.rainbow.name();
            ColorInterpolator interpolator = new ColorInterpolator(table, range[0], range[1], null);
            // a table of colors, instead of a color object for every point
            int[] lut = new int[COLOR_STEPS];
            double low = range[0];
            double step = (range[1] - range[0]) / (COLOR_STEPS - 1);
            for( int i = 0; i < COLOR_STEPS; i++ ) {
                lut[i] = interpolator.getColorFor(low + i * step).getRGB();
            }
            IntStream.range(0, n).parallel().forEach(i -> {
                int index = (int) Math.round((values[i] - low) / step);
                rgbs[i] = lut[Math.max(0, Math.min(COLOR_STEPS - 1, index))];
            });
        }

        Colors colors = new Colors();
        colors.values = values;
        colors.rgbs = rgbs;
        colors.range = range;
        return colors;
    }

    /**
     * @return the range of the values: between the percentiles or from minimum to maximum.
     */
    private static double[] defaultRange( float[] values, boolean percentiles ) {
        int n = values.length;
        if (percentiles) {
            // the percentiles of an even sample of the values, precise enough for the colors
            int step = Math.max(1, n / PERCENTILE_SAMPLES);
            int m = (n + step - 1) / step;
            float[] sample = new float[m];
            for( int i = 0; i < m; i++ ) {
                sample[i] = values[i * step];
            }
            Arrays.sort(sample);
            int lowIndex = (int) Math.floor((m - 1) * AUTO_CUT_PERCENT / 100.0);
            int highIndex = (int) Math.ceil((m - 1) * (100.0 - AUTO_CUT_PERCENT) / 100.0);
            return new double[]{sample[lowIndex], sample[highIndex]};
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for( float value : values ) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return new double[]{min, max};
    }

    private double[] computeElevations( boolean drawNormalized ) {
        LasPointColumns p = points;
        int n = p.size();
        boolean useHeights = drawNormalized && hasGroundHeights();
        double[] elevs = new double[n];
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for( int i = 0; i < n; i++ ) {
            float height = useHeights ? p.groundHeight[i] : Float.NaN;
            elevs[i] = Float.isNaN(height) ? p.z[i] : height;
            min = Math.min(min, elevs[i]);
            max = Math.max(max, elevs[i]);
        }
        drawnMinZ = min;
        drawnMaxZ = max;
        return elevs;
    }
}
