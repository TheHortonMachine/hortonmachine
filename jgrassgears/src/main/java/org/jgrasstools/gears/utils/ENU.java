package org.jgrasstools.gears.utils;

import com.vividsolutions.jts.geom.Coordinate;
import static java.lang.Math.*;

/**
 * Conversion of Geodetic coordinates to the Local Tangent Plane.
 * 
 * <p>Class that supports WGS84 to East-North-Up conversion. The conversions
 * reference the base coordinate that is given at construction time.
 * 
 * <p>Math is available in the paper: Conversion of Geodetic coordinates to the 
 * Local Tangent Plane.
 * 
 */
public class ENU {

    private static double semiMajorAxis = 6378137.0;
    private static double smeiMinorAxis = 6356752.3142;
    private static double flatness = (semiMajorAxis - smeiMinorAxis) / semiMajorAxis;
    private static double eccentricityP2 = flatness * (2 - flatness);

    private Coordinate baseCoordinateLL;

    /**
     * Create a new East North Up system.
     * 
     * @param baseCoordinateLL the WGS84 coordinate to use a origin of the ENU. 
     */
    public ENU( Coordinate baseCoordinateLL ) {
        this.baseCoordinateLL = baseCoordinateLL;
    }

    /**
     * Converts WGS84 coordinates to Earth-Centered Earth-Fixed (ECEF) coordinates.
     * 
     * @param cLL the wgs84 coordinate.
     * @return the ecef coordinate.
     */
    public Coordinate wgs84ToEcef( Coordinate cLL ) {
        double lambda = toRadians(cLL.y);
        double phi = toRadians(cLL.x);
        double sinLambda = sin(lambda);
        double cosLambda = cos(lambda);
        double cosPhi = cos(phi);
        double sinPhi = sin(phi);
        double N = semiMajorAxis / sqrt(1 - eccentricityP2 * pow(sinLambda, 2.0));

        double h = cLL.z;
        double x = (h + N) * cosLambda * cosPhi;
        double y = (h + N) * cosLambda * sinPhi;
        double z = (h + (1 - eccentricityP2) * N) * sinLambda;
        return new Coordinate(x, y, z);
    }

    /**
     * Converts an Earth-Centered Earth-Fixed (ECEF) coordinate to ENU. 
     * 
     * @param cEcef the ECEF coordinate.
     * @return the ENU coordinate.
     */
    public Coordinate ecefToEnu( Coordinate cEcef ) {
        double lambda = toRadians(baseCoordinateLL.y);
        double phi = toRadians(baseCoordinateLL.x);
        double sinLambda = sin(lambda);
        double cosLambda = cos(lambda);
        double cosPhi = cos(phi);
        double sinPhi = sin(phi);
        double N = semiMajorAxis / sqrt(1 - eccentricityP2 * pow(sinLambda, 2.0));

        // the origin of the LTP expressed in ECEF-r coordinates
        double h = baseCoordinateLL.z;
        double x0 = (h + N) * cosLambda * cosPhi;
        double y0 = (h + N) * cosLambda * sinPhi;
        double z0 = (h + (1 - eccentricityP2) * N) * sinLambda;

        double deltaX = cEcef.x - x0;
        double deltaY = cEcef.y - y0;
        double deltaZ = cEcef.z - z0;

        double xLTP = -sinPhi * deltaX + cosPhi * deltaY;
        double yLTP = -cosPhi * sinLambda * deltaX - sinLambda * sinPhi * deltaY + cosLambda * deltaZ;
        double zLTP = cosLambda * cosPhi * deltaX + cosLambda * sinPhi * deltaY + sinLambda * deltaZ;

        return new Coordinate(xLTP, yLTP, zLTP);
    }

    /**
     * Converts the wgs84 coordinate to ENU.
     * 
     * @param cLL the wgs84 coordinate.
     * @return the ENU coordinate.
     */
    public Coordinate wgs84ToEnu( Coordinate cLL ) {
        Coordinate cEcef = wgs84ToEcef(cLL);
        Coordinate enu = ecefToEnu(cEcef);
        return enu;
    }
}
