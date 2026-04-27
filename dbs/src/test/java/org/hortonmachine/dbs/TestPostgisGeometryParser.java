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
package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hortonmachine.dbs.postgis.PostgisGeometryParser;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.postgis.PGgeometry;

public class TestPostgisGeometryParser {

    @Test
    public void testLineStringZmConversion() throws Exception {
        PGgeometry pgGeometry = new PGgeometry("SRID=4326;LINESTRING(1 2 3 4,5 6 7 8)");
        
        // jts wktreader does not support M values
        Geometry geometry = new PostgisGeometryParser().fromSqlObject(pgGeometry);

        assertTrue(geometry instanceof LineString);
        assertEquals(4326, geometry.getSRID());
        assertEquals(2, geometry.getNumPoints());

        Coordinate firstCoordinate = geometry.getCoordinates()[0];
        assertEquals(1.0, firstCoordinate.getX(), 0.0);
        assertEquals(2.0, firstCoordinate.getY(), 0.0);
        assertEquals(3.0, firstCoordinate.getZ(), 0.0);
        assertEquals(4.0, firstCoordinate.getM(), 0.0);
    }
}
