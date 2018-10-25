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
package org.hortonmachine.dbs.h2gis;

import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMResultSet;

import org.locationtech.jts.geom.Geometry;

public class H2GisGeometryParser implements IGeometryParser {

    @Override
    public Geometry fromResultSet( IHMResultSet rs, int index ) throws Exception {
        Geometry geometry = (Geometry) rs.getObject(index);
        return geometry;
    }

    @Override
    public Geometry fromSqlObject( Object geomObject ) throws Exception {
        if (geomObject instanceof Geometry) {
            return (Geometry) geomObject;
        }
        throw new IllegalArgumentException("Geom object needs to be a JTS geometry.");
    }

    @Override
    public Object toSqlObject( Geometry geometry ) throws Exception {
        return geometry;
    }

}
