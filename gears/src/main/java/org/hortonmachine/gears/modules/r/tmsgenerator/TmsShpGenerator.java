package org.hortonmachine.gears.modules.r.tmsgenerator;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TmsShpGenerator {

    private static final GeometryFactory gf = GeometryUtilities.gf();

    public static void main( String[] args ) throws Exception {

        String EPSG_MERCATOR = "EPSG:3857";
        CoordinateReferenceSystem mercatorCrs = CrsUtilities.getCrsFromEpsg(EPSG_MERCATOR, null);

        double w = -180;
        double e = 180;
        double s = -90;
        double n = 90;
        int pMinzoom = 1;
        int pMaxzoom = 7;

        String folder = "/home/moovida/TMP/AAAAAAAAA_BM/mappe_x_android/outtiles/shps/";

        GlobalMercator mercator = new GlobalMercator();
        for( int z = pMinzoom; z <= pMaxzoom; z++ ) {

            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
            // List<Geometry> g = new ArrayList<Geometry>();
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("typename");
            b.setCRS(mercatorCrs);
            b.add("the_geom", Polygon.class);
            b.add("tms", String.class);
            b.add("google", String.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

            // get ul and lr tile number in GOOGLE tiles
            int[] llTileXY = mercator.GoogleTile(s, w, z);
            int[] urTileXY = mercator.GoogleTile(n, e, z);

            int startXTile = Math.min(llTileXY[0], urTileXY[0]);
            int endXTile = Math.max(llTileXY[0], urTileXY[0]);
            int startYTile = Math.min(llTileXY[1], urTileXY[1]);
            int endYTile = Math.max(llTileXY[1], urTileXY[1]);

            for( int i = startXTile; i <= endXTile; i++ ) {
                for( int j = startYTile; j <= endYTile; j++ ) {

                    double[] bounds = mercator.TileBounds(i, j, z);
                    double west = bounds[0];
                    double south = bounds[1];
                    double east = bounds[2];
                    double north = bounds[3];

                    Coordinate c1 = new Coordinate(west, south);
                    Coordinate c2 = new Coordinate(west, north);
                    Coordinate c3 = new Coordinate(east, north);
                    Coordinate c4 = new Coordinate(east, south);
                    Coordinate c5 = new Coordinate(west, south);
                    Coordinate[] c = {c1, c2, c3, c4, c5};
                    Polygon p = gf.createPolygon(gf.createLinearRing(c), null);

                    String google = z + "/" + i + "/" + j;
                    int[] tmsTile = mercator.TMSTileFromGoogleTile(i, j, z);
                    String tms = z + "/" + tmsTile[0] + "/" + tmsTile[1];
                    Object[] values = new Object[]{p, google, tms};
                    builder.addAll(values);
                    SimpleFeature feature = builder.buildFeature(null);
                    newCollection.add(feature);
                }
            }

            String name = "tiles_" + z + ".shp";
            OmsVectorWriter.writeVector(folder + name, newCollection);
        }

    }
}
