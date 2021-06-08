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
package org.hortonmachine.dbs.geopackage;

import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.geopackage.geom.GeoPkgGeomReader;
import org.hortonmachine.dbs.geopackage.geom.GeoPkgGeomWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class GeopackageGeometryParser implements IGeometryParser {

    @Override
    public Geometry fromResultSet( IHMResultSet rs, int index ) throws Exception {
        byte[] geomBytes = rs.getBytes(index);
        if (geomBytes != null) {
            Geometry geometry = new GeoPkgGeomReader(geomBytes).get();
            return geometry;
        }
        return null;
    }

    @Override
    public Geometry fromSqlObject( Object geomObject ) throws Exception {
        if (geomObject instanceof byte[]) {
            byte[] geomBytes = (byte[]) geomObject;
            Geometry geometry = new GeoPkgGeomReader(geomBytes).get();
            return geometry;
        }
        throw new IllegalArgumentException("Geom object needs to be a byte array.");
    }

    @Override
    public Object toSqlObject( Geometry geometry ) throws Exception {
        Coordinate coordinate = geometry.getCoordinate();
        int dim = 2;
        if(!Double.isNaN(coordinate.z)) {
            dim = 3; // wkbwriter only supports 2 and 3
        }
        byte[] bytes = new GeoPkgGeomWriter(dim).write(geometry);
        return bytes;
    }

}
