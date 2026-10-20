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
package org.hortonmachine.gears.utils.display;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.images.ImageGenerator;

/**
 * Renders {@link GridCoverage2D}s and {@link HMRaster}s as an info line and a quicklook.
 *
 * <p>Single band rasters are colored with the {@link ImageGenerator#DEFAULT_COLORTABLE}
 * stretched on their min and max.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class RasterDisplayRenderer implements IDisplayRenderer {

    @Override
    public boolean canRender( Object value ) {
        return value instanceof GridCoverage2D || value instanceof HMRaster;
    }

    @Override
    public List<DisplayPart> render( Object value, DisplayOptions options ) throws Exception {
        return render(value, null, options);
    }

    /**
     * Render a raster with a given style.
     *
     * @param value the {@link GridCoverage2D} or {@link HMRaster}.
     * @param style the style to use, if <code>null</code> the default one is used.
     * @param options the sizes and limits to use.
     * @return the parts.
     * @throws Exception
     */
    public List<DisplayPart> render( Object value, Style style, DisplayOptions options ) throws Exception {
        GridCoverage2D coverage;
        if (value instanceof HMRaster) {
            coverage = ((HMRaster) value).buildCoverage();
        } else {
            coverage = (GridCoverage2D) value;
        }

        RegionMap region = RegionMap.fromGridGeometry(coverage.getGridGeometry());
        int bands = coverage.getNumSampleDimensions();
        StringBuilder sb = new StringBuilder();
        sb.append("Raster ").append(region.getCols()).append(" x ").append(region.getRows()).append(" cells");
        if (bands > 1) {
            sb.append(", ").append(bands).append(" bands");
        }
        sb.append(", res ").append(format(region.getXres())).append(" x ").append(format(region.getYres()));
        sb.append(", ").append(crsLabel(coverage.getCoordinateReferenceSystem()));

        if (bands == 1) {
            double[] minMax = OmsRasterSummary.getMinMax(coverage);
            if (!Double.isNaN(minMax[0]) && !Double.isNaN(minMax[1])) {
                sb.append(", min ").append(format(minMax[0])).append(", max ").append(format(minMax[1]));
                if (style == null) {
                    style = RasterStyleUtilities.createStyleForColortable(ImageGenerator.DEFAULT_COLORTABLE, minMax[0],
                            minMax[1], 1.0);
                }
            } else {
                sb.append(", no valid values");
            }
        }
        Double novalue = CoverageUtilities.getNovalue(coverage);
        if (novalue != null) {
            sb.append(", novalue ").append(format(novalue));
        }

        ImageGenerator imgGen = new ImageGenerator(null, null);
        try {
            imgGen.addCoverage(coverage, style);
            BufferedImage image = imgGen.drawFullExtent(options.imageWidth(), options.imageHeight(), 0);
            return List.of(new DisplayText(sb.toString()), new DisplayImage(image));
        } finally {
            imgGen.dispose();
        }
    }

    /**
     * @param value the value to format.
     * @return the value with at most 4 decimals, or in scientific notation if very small or big.
     */
    static String format( double value ) {
        double abs = Math.abs(value);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        if (abs != 0 && (abs < 1E-4 || abs >= 1E10)) {
            return String.format(Locale.ROOT, "%.4g", value);
        }
        return new BigDecimal(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /**
     * @param crs the crs, can be <code>null</code>.
     * @return the code of the crs, its name if it has no code, or "no crs".
     */
    static String crsLabel( CoordinateReferenceSystem crs ) {
        if (crs == null) {
            return "no crs";
        }
        try {
            String code = HMCrsRegistry.getCodeFromCrs(crs);
            if (code != null) {
                return code;
            }
        } catch (Exception e) {
            // use the name
        }
        return crs.getName().getCode();
    }
}
