package org.hortonmachine.gears.io.wcs;

import org.hortonmachine.gears.utils.CrsUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Utils methods for the WCS handlers.
 */
public class WcsUtils {

    /**
     * Returns the SRID (Spatial Reference ID) from a given SRS wcs url, or null.
     * 
     * <p>
     * Example urls handled are:
     * <ul>
     * <li>http://www.opengis.net/def/crs/EPSG/0/25832</li>
     * <li>http://www.opengis.net/def/crs/EPSG/0/EPSG:25832</li>
     * </ul>
     * </p>
     * </br>
     * 
     * @param srsName the SRS name to extract the SRID from
     * @return the SRID as an Integer, or null if the SRID cannot be extracted from
     *         the SRS name
     */
    public static Integer getSridFromSrsName(String srsName) {
        int index = srsName.lastIndexOf("EPSG:");
        String sridStr = null;
        if (srsName != null && index != -1) {
            sridStr = srsName.substring(index + 5);
        } else if (srsName != null && srsName.contains("EPSG")) {
            index = srsName.lastIndexOf("/");
            if (index != -1) {
                sridStr = srsName.substring(index + 1);
            }
        }
        if (sridStr != null) {
            int srid = Integer.parseInt(sridStr);
            return srid;
        }
        return null;
    }

    public static CoordinateReferenceSystem getCrsFromSrsName(String srsName) {
        Integer srid = getSridFromSrsName(srsName);
        if (srid != null)
            return CrsUtilities.getCrsFromSrid(srid);
        return null;
    }

    public static String nsWCS2(String tag) {
        return "{http://www.opengis.net/wcs/2.0}" + tag;
    }
}
