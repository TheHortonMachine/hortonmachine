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
package org.hortonmachine.gears.utils.features;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;

import org.locationtech.jts.geom.Geometry;

/**
 * Helper class for simple filter constructions.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class FilterUtilities {

    /**
     * Create a bounding box filter from a bounding box.
     * 
     * @param attribute the geometry attribute or null in the case of default "the_geom".
     * @param bbox the {@link BoundingBox}.
     * @return the filter.
     * @throws CQLException
     */
    public static Filter getBboxFilter( String attribute, BoundingBox bbox ) throws CQLException {
        double w = bbox.getMinX();
        double e = bbox.getMaxX();
        double s = bbox.getMinY();
        double n = bbox.getMaxY();

        return getBboxFilter(attribute, w, e, s, n);
    }

    /**
     * Create a bounding box filter from the bounds coordinates.
     * 
     * @param attribute the geometry attribute or null in the case of default "the_geom".
     * @param west western bound coordinate.
     * @param east eastern bound coordinate.
     * @param south southern bound coordinate.
     * @param north northern bound coordinate.
     * @return the filter.
     * @throws CQLException
     */
    public static Filter getBboxFilter( String attribute, double west, double east, double south, double north )
            throws CQLException {

        if (attribute == null) {
            attribute = "the_geom";
        }

        StringBuilder sB = new StringBuilder();
        sB.append("BBOX(");
        sB.append(attribute);
        sB.append(",");
        sB.append(west);
        sB.append(",");
        sB.append(south);
        sB.append(",");
        sB.append(east);
        sB.append(",");
        sB.append(north);
        sB.append(")");

        Filter bboxFilter = CQL.toFilter(sB.toString());

        return bboxFilter;
    }

    public static Filter getCQLFilter( String expression ) throws CQLException {
        Filter cqlFilter = ECQL.toFilter(expression);
        return cqlFilter;
    }

    /**
     * Creates an intersect filter.
     * 
     * @param geomName the name of the geom field to filter.
     * @param geometry the geometry to use as filtering geom.
     * @return the filter.
     * @throws CQLException
     */
    public static Filter getIntersectsGeometryFilter( String geomName, Geometry geometry ) throws CQLException {
        Filter result = CQL.toFilter("INTERSECTS(" + geomName + ", " + geometry.toText() + " )");
        return result;
    }

}
