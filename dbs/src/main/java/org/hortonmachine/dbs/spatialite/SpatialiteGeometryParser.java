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
package org.hortonmachine.dbs.spatialite;

import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

public class SpatialiteGeometryParser implements IGeometryParser {
    SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
    SpatialiteWKBWriter wkbWriter = new SpatialiteWKBWriter();
    WKBReader jtsWkbReader = new WKBReader();

    @Override
    public Geometry fromResultSet( IHMResultSet rs, int index ) throws Exception {
        byte[] geomBytes = rs.getBytes(index);
        if (geomBytes != null) {
            Geometry geometry;
            try {
                // in case of spatialite to use ST_AsBinary is suggested, since there are so many fast changing specs
                geometry = jtsWkbReader.read(geomBytes);
            } catch (Exception e) {
                // in case it doesn't work, try the latest spatialite WKB parser
                geometry = wkbReader.read(geomBytes);
            }
            return geometry;
        }
        return null;
    }

    @Override
    public Geometry fromSqlObject( Object geomObject ) throws Exception {
        if (geomObject instanceof byte[]) {
            byte[] geomBytes = (byte[]) geomObject;
            Geometry geometry = wkbReader.read(geomBytes);
            return geometry;
        }
        throw new IllegalArgumentException("Geom object needs to be a byte array.");
    }

    @Override
    public Object toSqlObject( Geometry geometry ) throws Exception {
        return wkbWriter.write(geometry);
    }

}
