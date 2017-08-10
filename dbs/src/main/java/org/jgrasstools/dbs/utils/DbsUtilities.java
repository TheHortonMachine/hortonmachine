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

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * A simple utils class for the dbs module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DbsUtilities {

    private static GeometryFactory geomFactory;
    private static PrecisionModel precModel;

    public static PrecisionModel basicPrecisionModel() {
        return (pm());
    }

    public static GeometryFactory gf() {
        if (geomFactory == null) {
            geomFactory = new GeometryFactory(pm());
        }
        return (geomFactory);
    }

    public static PrecisionModel pm() {
        if (precModel == null) {
            precModel = new PrecisionModel(PrecisionModel.FLOATING);
        }
        return (precModel);
    }

    /**
     * Join a list of strings by comma.
     * 
     * @param items the list of strings.
     * @return the resulting string.
     */
    public static String joinByComma( List<String> items ) {
        StringBuilder sb = new StringBuilder();
        for( String item : items ) {
            sb.append(",").append(item);
        }
        return sb.substring(1);
    }

    /**
     * Create a polygon using an envelope.
     * 
     * @param env the envelope to use.
     * @return the created geomerty.
     */
    public static Polygon createPolygonFromEnvelope( Envelope env ) {
        double minX = env.getMinX();
        double minY = env.getMinY();
        double maxY = env.getMaxY();
        double maxX = env.getMaxX();
        Coordinate[] c = new Coordinate[]{new Coordinate(minX, minY), new Coordinate(minX, maxY), new Coordinate(maxX, maxY),
                new Coordinate(maxX, minY), new Coordinate(minX, minY)};
        return gf().createPolygon(c);
    }

    /**
     * Create a polygon using boundaries.
     * 
     * @param minX the min x.
     * @param minY the min y.
     * @param maxX the max x.
     * @param maxY the max y.
     * @return the created geomerty.
     */
    public static Polygon createPolygonFromBounds( double minX, double minY, double maxX, double maxY ) {
        Coordinate[] c = new Coordinate[]{new Coordinate(minX, minY), new Coordinate(minX, maxY), new Coordinate(maxX, maxY),
                new Coordinate(maxX, minY), new Coordinate(minX, minY)};
        return gf().createPolygon(c);
    }

    public static byte[] hexStringToByteArray( String s ) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for( int i = 0; i < len; i += 2 ) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
