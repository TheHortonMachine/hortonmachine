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
package org.jgrasstools.lesto.modules.utilities.cluster;
import org.jgrasstools.gears.utils.clustering.GvmSimpleKeyer;

import com.vividsolutions.jts.geom.Coordinate;
/**
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ShpClusterElevationKeyer extends GvmSimpleKeyer<Coordinate> {

    @Override
    protected Coordinate combineKeys( Coordinate r1, Coordinate r2 ) {
        return r1.z < r2.z ? r2 : r1;
    }

}