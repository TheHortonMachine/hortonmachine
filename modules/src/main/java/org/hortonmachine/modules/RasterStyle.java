/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.modules;

import org.geotools.coverage.grid.GridCoverage2D;

/**
 * A wrapper for {@link org.hortonmachine.gears.utils.colors.RasterStyle} to be used in scripting.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RasterStyle {

    private org.hortonmachine.gears.utils.colors.RasterStyle rasterStyle;

    public RasterStyle( GridCoverage2D raster ) throws Exception {
        rasterStyle = new org.hortonmachine.gears.utils.colors.RasterStyle(raster);
    }

    public RasterStyle( int min, int max ) throws Exception {
        rasterStyle = new org.hortonmachine.gears.utils.colors.RasterStyle(min, max);
    }

    public void setAlpha( double alpha ) {
        rasterStyle.setAlpha(alpha);
    }

    public String style( String colorTableName ) throws Exception {
        return rasterStyle.style(colorTableName);
    }

}
