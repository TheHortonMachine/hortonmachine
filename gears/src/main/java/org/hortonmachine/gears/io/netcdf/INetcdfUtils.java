package org.hortonmachine.gears.io.netcdf;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;

public interface INetcdfUtils {

    /**
     * Convert a given coordinate from the passed projection to lat long.
     * 
     * @param netcdfProj
     * @param coordinate
     * @return
     */
    default Coordinate toLatLong( ProjectionImpl netcdfProj, Coordinate coordinate ) {
        LatLonPoint latLonPoint = netcdfProj.projToLatLon(coordinate.x, coordinate.y);
        coordinate = new Coordinate(latLonPoint.getLongitude(), latLonPoint.getLatitude());
        return coordinate;
    }

    /**
     * Utility method for getting NoData from an input {@link Variable}
     *
     * @param var Variable instance
     * @return a Number representing NoData
     */
    default Number getNodata( Variable var ) {
        if (var != null) {
            // Getting all the Variable attributes
            List<Attribute> attributes = var.getAttributes();
            String fullName;
            // Searching for FILL_VALUE or MISSING_VALUE attributes
            for( Attribute attribute : attributes ) {
                fullName = attribute.getFullName();
                if (fullName.equalsIgnoreCase("_FillValue") || fullName.equalsIgnoreCase("missing_value")) {
                    return attribute.getNumericValue();
                }
            }
        }
        return null;
    }
}
