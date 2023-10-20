package org.hortonmachine.gears.io.wcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    
    public static String nsCRS_WCS2(int epsgSrid) {
        return "http://www.opengis.net/def/crs/EPSG/0/" + epsgSrid;
    }

    /**
     * Orders the given axis labels by putting the "long", "lon", "x" axis label first and the "lat", "y" axis label second.
     * 
     * @param axisLabels the axis labels to order
     * @return the ordered axis labels
     * @throws IllegalArgumentException if the "long", "lon", "x" or "lat", "y" axis labels are not found in the given axis labels
     */
    public static String[] orderLabels(String[] axisLabels) {
        String[] ordered = new String[2];

        int[] lonLatPositions = getLonLatPositions(axisLabels);
        ordered[0] = axisLabels[lonLatPositions[0]];
        ordered[1] = axisLabels[lonLatPositions[1]];
        
        if (ordered[0] == null || ordered[1] == null) {
            throw new IllegalArgumentException("Could not find lat/lon or x/y axis labels in coverage description");
        }
        return ordered;
    }

    public static int[] getLonLatPositions(String[] axisLabels) {
        int[] positions = new int[2];
        List<String> possibleEastingLCLabels = getPossibleEastingLCLabels();
        List<String> possibleNorthingLCLabels = getPossibleNorthingLCLabels();
        boolean foundEasting = false;
        boolean foundNorthing = false;
        for (int i = 0; i < axisLabels.length; i++) {
            String label = axisLabels[i].toLowerCase();
            if (possibleEastingLCLabels.contains(label)) {
                positions[0] = i;
                foundEasting = true;
            } else if (possibleNorthingLCLabels.contains(label)) {
                positions[1] = i;
                foundNorthing = true;
            }
        }
        if (!foundEasting || !foundNorthing) {
            throw new IllegalArgumentException("Could not find lat/lon or x/y axis labels in coverage description");
        }
        return positions;
    }

    private static List<String> getPossibleEastingLCLabels(){
        List<String> labels = Arrays.asList("long", "lon", "x", "e", "xaxis", "x-axis", "x_axis", "long_axis", "long-axis", "lon_axis", "lon-axis", "easting", "easting_axis", "easting-axis", "e_axis");
        return labels;
    }

    private static List<String> getPossibleNorthingLCLabels(){
        List<String> labels = Arrays.asList("lat", "y", "n", "yaxis", "y-axis", "y_axis", "lat_axis", "lat-axis", "lat_axis", "northing", "northing_axis", "northing-axis", "n_axis");
        return labels;
    }
}
