/*
 * This file is part of Hortonmachine (http://www.hortonmachine.org)
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
package org.hortonmachine.modules;

import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;

/**
 * A wrapper class to support scripting.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoscriptHelper {

    public static String printColorTables() {
        StringBuilder sb = new StringBuilder();
        for( EColorTables table : EColorTables.values() ) {
            sb.append(",").append(table.name());
        }
        return sb.substring(1);
    }

    public static String styleForColorTable( String tableName, double min, double max, double opacity ) throws Exception {
        return RasterStyleUtilities.styleToString(RasterStyleUtilities.createStyleForColortable(tableName, min, max, opacity));
    }

}
