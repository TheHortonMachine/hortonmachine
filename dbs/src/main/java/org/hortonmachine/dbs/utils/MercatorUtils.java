package org.hortonmachine.dbs.utils;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * Mercator tiling system utils.
 * 
 * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/
 *  and adapted.</p>
 * 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MercatorUtils {

    private static double originShift = 2 * Math.PI * 6378137 / 2.0;
    private static final double METER_TO_FEET_CONVERSION_FACTOR = 3.2808399;

    public static Coordinate convert3857To4326( Coordinate coordinate3857 ) {
        double[] latLon = metersToLatLon(coordinate3857.x, coordinate3857.y);
        return new Coordinate(latLon[1], latLon[0]);
    }

    public static Coordinate convert4326To3857( Coordinate coordinate4326 ) {
        double[] xy = latLonToMeters(coordinate4326.y, coordinate4326.x);
        return new Coordinate(xy[0], xy[1]);
    }

    public static Envelope convert3857To4326( Envelope envelope3857 ) {
        Coordinate ll3857 = new Coordinate(envelope3857.getMinX(), envelope3857.getMinY());
        Coordinate ur3857 = new Coordinate(envelope3857.getMaxX(), envelope3857.getMaxY());

        Coordinate ll4326 = convert3857To4326(ll3857);
        Coordinate ur4326 = convert3857To4326(ur3857);

        Envelope env4326 = new Envelope(ll4326, ur4326);
        return env4326;
    }

    public static Envelope convert4326To3857( Envelope envelope4326 ) {
        Coordinate ll4326 = new Coordinate(envelope4326.getMinX(), envelope4326.getMinY());
        Coordinate ur4326 = new Coordinate(envelope4326.getMaxX(), envelope4326.getMaxY());

        Coordinate ll3857 = convert4326To3857(ll4326);
        Coordinate ur3857 = convert4326To3857(ur4326);

        Envelope env3857 = new Envelope(ll3857, ur3857);
        return env3857;
    }

    public static int[] getTileNumberFrom3857( Coordinate coord3857, int zoom ) {
        Coordinate coord4326 = convert3857To4326(coord3857);
        return getTileNumber(coord4326.y, coord4326.x, zoom);
    }

    public static int[] getTileNumberFrom4326( Coordinate coord4326, int zoom ) {
        return getTileNumber(coord4326.y, coord4326.x, zoom);
    }

    /**
     * Returns bounds of the given tile in EPSG:4326 coordinates
     *
     * @param tx       tile x.
     * @param ty       tile y.
     * @param zoom     zoomlevel.
     * @return the Envelope.
     */
    public static Envelope tileBounds4326( final int x, final int y, final int zoom ) {
        double north = tile2lat(y, zoom);
        double south = tile2lat(y + 1, zoom);
        double west = tile2lon(x, zoom);
        double east = tile2lon(x + 1, zoom);
        Envelope envelope = new Envelope(west, east, south, north);
        return envelope;
    }

    /**
     * Returns bounds of the given tile in EPSG:3857 coordinates
     *
     * @param tx       tile x.
     * @param ty       tile y.
     * @param zoom     zoomlevel.
     * @return the Envelope.
     */
    public static Envelope tileBounds3857( final int x, final int y, final int zoom ) {
        Envelope env4326 = tileBounds4326(x, y, zoom);
        Coordinate ll4326 = new Coordinate(env4326.getMinX(), env4326.getMinY());
        Coordinate ur4326 = new Coordinate(env4326.getMaxX(), env4326.getMaxY());

        Coordinate ll3857transf = MercatorUtils.convert4326To3857(ll4326);
        Coordinate ur3857transf = MercatorUtils.convert4326To3857(ur4326);

        return new Envelope(ll3857transf, ur3857transf);
    }

    /**
     * Get the tiles that fit into a given tile at lower zoomlevel.
     * 
     * @param origTx the original tile x.
     * @param origTy the original tile y.
     * @param origZoom the original tile zoom.
     * @param higherZoom the requested zoom.
     * @param tileSize the used tile size.
     * @return the ordered list of tiles.
     */
    public static List<int[]> getTilesAtHigherZoom( int origTx, int origTy, int origZoom, int higherZoom, int tileSize ) {
        Envelope boundsLL = tileBounds4326(origTx, origTy, origZoom);

        int delta = higherZoom - origZoom;
        int splits = (int) Math.pow(2, delta);

        double intervalX = boundsLL.getWidth() / splits;
        double intervalY = boundsLL.getHeight() / splits;

        List<int[]> tilesList = new ArrayList<>();
        for( double y = boundsLL.getMaxY() - intervalY / 2.0; y > boundsLL.getMinY(); y = y - intervalY ) {
            for( double x = boundsLL.getMinX() + intervalX / 2.0; x < boundsLL.getMaxX(); x = x + intervalX ) {
                int[] tileNumber = getTileNumber(y, x, higherZoom);
                tilesList.add(tileNumber);
            }
        }
        return tilesList;
    }

    private static double tile2lon( int x, int z ) {
        return x / Math.pow(2.0, z) * 360.0 - 180.0;
    }

    private static double tile2lat( int y, int z ) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    /**
     * Converts TMS tile coordinates to Osm slippy map Tile coordinates.
     *
     * @param tx   the x tile number.
     * @param ty   the y tile number.
     * @param zoom the current zoom level.
     * @return the converted values.
     */
    public static int[] tmsTile2OsmTile( int tx, int ty, int zoom ) {
        return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
    }

    /**
     * Converts Osm slippy map tile coordinates to TMS Tile coordinates.
     *
     * @param tx   the x tile number.
     * @param ty   the y tile number.
     * @param zoom the current zoom level.
     * @return the converted values.
     */
    public static int[] osmTile2TmsTile( int tx, int ty, int zoom ) {
        return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
    }

    /**
     * Converts TMS tile coordinates to Microsoft QuadTree.
     *
     * @param tx   tile x.
     * @param ty   tile y.
     * @param zoom zoomlevel.
     * @return the quadtree key.
     */
    public static String quadTree( int tx, int ty, int zoom ) {
        String quadKey = ""; //$NON-NLS-1$
        ty = (int) ((Math.pow(2, zoom) - 1) - ty);
        for( int i = zoom; i < 0; i-- ) {
            int digit = 0;
            int mask = 1 << (i - 1);
            if ((tx & mask) != 0) {
                digit += 1;
            }
            if ((ty & mask) != 0) {
                digit += 2;
            }
            quadKey += (digit + ""); //$NON-NLS-1$
        }
        return quadKey;
    }

//    /**
//     * Get lat-long bounds from tile index.
//     *
//     * @param tx       tile x.
//     * @param ty       tile y.
//     * @param zoom     zoomlevel.
//     * @param tileSize tile size.
//     * @return [minx, miny, maxx, maxy]
//     */
//    public static double[] tileLatLonBounds( int tx, int ty, int zoom, int tileSize ) {
//        double[] bounds = tileBounds3857(tx, ty, zoom, tileSize);
//        double[] mins = metersToLatLon(bounds[0], bounds[1]);
//        double[] maxs = metersToLatLon(bounds[2], bounds[3]);
//        return new double[]{mins[1], maxs[0], maxs[1], mins[0]};
//    }

    /**
     * <p>Code copied from: http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Lon..2Flat._to_tile_numbers </p>
     * 20131128: corrections added to correct going over or under max/min extent
     * - was causing http 400 Bad Requests
     * - updated openstreetmap wiki
     *
     * @param zoom
     * @return [zoom, xtile, ytile_osm]
     */
    public static int[] getTileNumber( final double lat, final double lon, final int zoom ) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile_osm = (int) Math.floor(
                (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile_osm < 0)
            ytile_osm = 0;
        if (ytile_osm >= (1 << zoom))
            ytile_osm = ((1 << zoom) - 1);
        return new int[]{zoom, xtile, ytile_osm};
    }

    /**
     * Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84
     * Datum
     *
     * @param mx x
     * @param my y
     * @return lat long
     */
    public static double[] metersToLatLon( double mx, double my ) {

        double lon = (mx / originShift) * 180.0;
        double lat = (my / originShift) * 180.0;

        lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return new double[]{lat, lon};
    }

    /**
     * Equatorial radius of earth is required for distance computation.
     */
    public static final double EQUATORIALRADIUS = 6378137.0;

    /**
     * Convert a longitude coordinate (in degrees) to a horizontal distance in meters from the
     * zero meridian
     *
     * @param longitude in degrees
     * @return longitude in meters in spherical mercator projection
     */
    public static double longitudeToMetersX( double longitude ) {
        return EQUATORIALRADIUS * Math.toRadians(longitude);
    }

    /**
     * Convert a meter measure to a longitude
     *
     * @param x in meters
     * @return longitude in degrees in spherical mercator projection
     */
    public static double metersXToLongitude( double x ) {
        return Math.toDegrees(x / EQUATORIALRADIUS);
    }

    /**
     * Convert a meter measure to a latitude
     *
     * @param y in meters
     * @return latitude in degrees in spherical mercator projection
     */
    public static double metersYToLatitude( double y ) {
        return Math.toDegrees(Math.atan(Math.sinh(y / EQUATORIALRADIUS)));
    }

    /**
     * Convert a latitude coordinate (in degrees) to a vertical distance in meters from the
     * equator
     *
     * @param latitude in degrees
     * @return latitude in meters in spherical mercator projection
     */
    public static double latitudeToMetersY( double latitude ) {
        return EQUATORIALRADIUS * Math.log(Math.tan(Math.PI / 4 + 0.5 * Math.toRadians(latitude)));
    }

    /**
     * Convert a east-longitude,west-longitude coordinate (in degrees) to distance in meters
     *
     * @param east_longitude longitude in degrees
     * @param west_longitude longitude in degrees
     * @return meters in spherical mercator projection
     */
    public static double longitudeToMeters( double east_longitude, double west_longitude ) {
        return longitudeToMetersX(east_longitude) - longitudeToMetersX(west_longitude);
    }

    /**
     * Convert a north-latitude,south-latitude coordinate (in degrees) to distance in meters
     *
     * @param north_latitude latitude in degrees
     * @param south_latitude latitude in degrees
     * @return meters in spherical mercator projection
     */
    public static double latitudeToMeters( double north_latitude, double south_latitude ) {
        return latitudeToMetersY(north_latitude) - latitudeToMetersY(south_latitude);
    }

    /**
     * Converts given lat/lon in WGS84 Datum to XY in Spherical Mercator
     * EPSG:900913
     *
     * @param lat
     * @param lon
     * @return
     */
    public static double[] latLonToMeters( double lat, double lon ) {
        double mx = lon * originShift / 180.0;
        double my = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        my = my * originShift / 180.0;
        return new double[]{mx, my};
    }

    /**
     * Converts pixel coordinates in given zoom level of pyramid to EPSG:900913
     *
     * @param px       pixel x.
     * @param py       pixel y.
     * @param zoom     zoomlevel.
     * @param tileSize tile size.
     * @return converted coordinate.
     */
    public static double[] pixelsToMeters( double px, double py, int zoom, int tileSize ) {
        double res = getResolution(zoom, tileSize);
        double mx = px * res - originShift;
        double my = py * res - originShift;
        return new double[]{mx, my};
    }

    /**
     *
     * @param px
     * @param py
     * @return
     */
    public static int[] pixelsToTile( int px, int py, int tileSize ) {
        int tx = (int) Math.ceil(px / ((double) tileSize) - 1);
        int ty = (int) Math.ceil(py / ((double) tileSize) - 1);
        return new int[]{tx, ty};
    }

    /**
     * Converts EPSG:900913 to pyramid pixel coordinates in given zoom level
     *
     * @param mx
     * @param my
     * @param zoom
     * @return
     */
    public static int[] metersToPixels( double mx, double my, int zoom, int tileSize ) {
        double res = getResolution(zoom, tileSize);
        int px = (int) Math.round((mx + originShift) / res);
        int py = (int) Math.round((my + originShift) / res);
        return new int[]{px, py};
    }

    /**
     * Returns tile for given mercator coordinates
     *
     * @return
     */
    public static int[] metersToTile( double mx, double my, int zoom, int tileSize ) {
        int[] p = metersToPixels(mx, my, zoom, tileSize);
        return pixelsToTile(p[0], p[1], tileSize);
    }

    /**
     * Resolution (meters/pixel) for given zoom level (measured at Equator)
     *
     * @param zoom     zoomlevel.
     * @param tileSize tile size.
     * @return resolution.
     */
    public static double getResolution( int zoom, int tileSize ) {
        // return (2 * Math.PI * 6378137) / (this.tileSize * 2**zoom)
        double initialResolution = 2 * Math.PI * 6378137 / tileSize;
        return initialResolution / Math.pow(2, zoom);
    }

    /**
     * Convert meters to feet.
     *
     * @param meters the value in meters to convert to feet.
     * @return meters converted to feet.
     */
    public static double toFeet( final double meters ) {
        return meters * METER_TO_FEET_CONVERSION_FACTOR;
    }

}
