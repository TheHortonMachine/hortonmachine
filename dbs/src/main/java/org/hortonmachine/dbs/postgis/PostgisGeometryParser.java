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
package org.hortonmachine.dbs.postgis;

import java.sql.SQLException;

import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.postgis.GeometryBuilder;
import org.postgis.PGgeometry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class PostgisGeometryParser implements IGeometryParser {
    private WKTReader wktReader = new WKTReader();

    @Override
    public Geometry fromResultSet( IHMResultSet rs, int index ) throws Exception {
        Object object = rs.getObject(index);
        if (object instanceof PGgeometry) {
            PGgeometry pgGeometry = (PGgeometry) object;
            Geometry geometry = getGeom(pgGeometry);
            return geometry;
        }
        return null;
    }

    private synchronized Geometry getGeom( PGgeometry pgGeometry ) throws SQLException, ParseException {
        String wkt = pgGeometry.toString();
        String[] splitSRID = GeometryBuilder.splitSRID(wkt);
        Geometry geometry = wktReader.read(splitSRID[1]);
        try {
            int srid = Integer.parseInt(splitSRID[0].substring(5));
            geometry.setSRID(srid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return geometry;
    }

    @Override
    public Geometry fromSqlObject( Object geomObject ) throws Exception {
        if (geomObject instanceof Geometry) {
            return (Geometry) geomObject;
        } else if (geomObject instanceof PGgeometry) {
            return getGeom((PGgeometry) geomObject);
        }
        throw new IllegalArgumentException("Geom object needs to be a JTS/PGGeometry geometry.");
    }

    @Override
    public Object toSqlObject( Geometry geometry ) throws Exception {
        org.postgis.Geometry pgGeometry = GeometryBuilder.geomFromString(geometry.toText());
        pgGeometry.srid = geometry.getSRID();
        return pgGeometry;
    }

}
