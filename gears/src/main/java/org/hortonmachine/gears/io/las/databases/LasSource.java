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
package org.hortonmachine.gears.io.las.databases;

import org.locationtech.jts.geom.Polygon;

/**
 * {@link LasSourcesTable} object.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class LasSource {
    public long id;
    
    /**
     * Number of levels produced for this source.
     * 
     * <p>tables are named like <b>laslevelN</b> with N starting with 1
     * being the level that summarizes the {@link LasCell}s data.
     *  
     */
    public int levels;

    /**
     * The cell resolution in the sources srid units.
     */
    public double resolution;

    /**
     * The level multiplication factor.
     */
    public double levelFactor;
    
    public Polygon polygon;
    public String name;
    public double minElev;
    public double maxElev;
    public double minIntens;
    public double maxIntens;
}
