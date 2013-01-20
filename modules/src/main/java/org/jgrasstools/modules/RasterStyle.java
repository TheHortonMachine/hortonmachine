/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.modules;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.modules.r.summary.OmsRasterSummary;
import org.jgrasstools.gears.utils.colors.ColorTables;
import org.jgrasstools.gears.utils.colors.RasterStyleUtilities;

/**
 * A simple raster styling utility for scripting environment.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RasterStyle {

    private double min;
    private double max;
    private double alpha = 1.0;

    public RasterStyle( GridCoverage2D raster ) throws Exception {
        OmsRasterSummary summary = new OmsRasterSummary();
        summary.inRaster = raster;
        summary.process();

        min = summary.outMin;
        max = summary.outMax;
    }

    public void setAlpha( double alpha ) {
        this.alpha = alpha;
    }

    public String style( String colorTableName ) throws Exception {
        ColorTables[] colorTables = ColorTables.values();
        for( ColorTables colorTable : colorTables ) {
            if (colorTable.name().equals(colorTableName.toLowerCase().trim())) {
                String createStyleForColortable = RasterStyleUtilities.createStyleForColortable(colorTableName, min, max, null,
                        alpha);
                return createStyleForColortable;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("The colortable ");
        sb.append(colorTableName);
        sb.append(" could not be found in the default colortables.\n");
        sb.append("Available colortables are:\n");
        for( ColorTables colorTable : colorTables ) {
            sb.append("\t");
            sb.append(colorTable.name());
            sb.append("\n");
        }
        throw new ModelsIllegalargumentException(sb.toString(), this);
    }

}
