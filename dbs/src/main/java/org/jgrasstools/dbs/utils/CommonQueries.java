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
package org.jgrasstools.dbs.utils;

import org.jgrasstools.dbs.compat.IJGTConnection;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTStatement;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
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
import com.vividsolutions.jts.geom.LineString;

/**
 * A simple utils class to collect common db queries as functions.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CommonQueries {
    private static GeometryFactory gf = new GeometryFactory();
    /**
     * Calculates the GeodesicLength between to points.
     * 
     * @param connection the database connection.
     * @param p1 the first point.
     * @param p2 the second point.
     * @param srid the srid. If <0, 4326 will be used. This needs to be a geographic prj.
     * @return the distance.
     * @throws Exception
     */
    public static double getDistanceBetween( IJGTConnection connection, Coordinate p1, Coordinate p2, int srid )
            throws Exception {
        if (srid < 0) {
            srid = 4326;
        }
        LineString lineString = gf.createLineString(new Coordinate[]{p1, p2});
        String sql = "select GeodesicLength(LineFromText(\"" + lineString.toText() + "\"," + srid + "));";
        try (IJGTStatement stmt = connection.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql);) {
            if (rs.next()) {
                double length = rs.getDouble(1);
                return length;
            }
            throw new RuntimeException("Could not calculate distance.");
        }

    }
}
