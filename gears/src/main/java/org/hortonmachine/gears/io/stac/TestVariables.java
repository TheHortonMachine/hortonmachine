package org.hortonmachine.gears.io.stac;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.feature.simple.SimpleFeature;

import com.fasterxml.jackson.databind.JsonNode;

public class TestVariables {

    static String repoUrl = "https://earth-search.aws.element84.com/v1";

    // search by collection name
    static String collectionQuery = "sentinel-2-l2a";
    // and by date range
    static String dayQuery = "2022-12-07";
    static String dateQuery = dayQuery + "T00:00:00.000000Z/" + dayQuery + "T23:59:59.999999Z";

    static String band = "Blue (band 2) - 10m";

    static int downloadRows = 200;
    static int downloadCols = 300;

    static DateFormat dateFormatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
    static DateFormat fileNameDateFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss");

    static boolean shortInfo = false;

    static int limit = 1;

    static String CQL_FILTER = "eo:cloud_cover < 25";

    // and by intersection
//    ReferencedEnvelope italyEnv = new ReferencedEnvelope(5.450, 20.082, 35.530, 46.862, DefaultGeographicCRS.WGS84);
    static Geometry intersectionGeometry = null;
    static {
        try {
            intersectionGeometry = new WKTReader().read(
                    "Polygon ((-4.0558457000639665 43.59500243372504258, -4.51395914324252967 43.17884817856538149, -4.65491712575901229 42.74043030189424286, -4.46990977370612974 42.24021749842827234, -4.06465557397124666 41.88047506442949697, -2.87532259648843569 41.65048745212956049, -1.80051797980026707 41.8935923086976203, -1.18382680629066117 42.42257588059123208, -1.13096756284698063 42.92133362756125337, -1.28073541927074164 43.5056106430706393, -2.00314507966770883 43.78610975973865038, -3.21009780496507791 43.74793701127006074, -4.0558457000639665 43.59500243372504258))");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    static String downloadFolder = "/home/hydrologis/TMP/KLAB/STAC/";
    static String overviewShp = downloadFolder + "overview.shp";

    @SuppressWarnings("unchecked")
    protected StacFeatures getUniqueFeatures( SimpleFeatureCollection fc ) throws Exception {
        SimpleFeatureIterator iterator = fc.features();
        int featureCount = 0;
        System.out.println("Extracting unique features...");
        StacFeatures stacFeatures = new StacFeatures();
        TreeSet<String> uniqueFeatures = new TreeSet<>();
        while( iterator.hasNext() ) {
            if (featureCount++ % 10 == 0) {
                System.out.println(featureCount);
            }
            SimpleFeature f = iterator.next();
            Map<String, JsonNode> top = (Map<String, JsonNode>) f.getUserData().get(GeoJSONReader.TOP_LEVEL_ATTRIBUTES);
            String id = top.get("id").textValue();
            String createdDateCet = f.getAttribute("created").toString();
            Date dateCet = dateFormatter.parse(createdDateCet);
            String ts = fileNameDateFormatter.format(dateCet);
            Object epsg = f.getAttribute("proj:epsg");
            Integer epsgInt = null;
            if (epsg instanceof Integer) {
                epsgInt = (Integer) epsg;
            }

            if (epsgInt != null && uniqueFeatures.add(ts + " " + id)) {
                Geometry geometry = (Geometry) f.getDefaultGeometry();
                stacFeatures.ids.add(id);
                stacFeatures.timestamps.add(ts);
                stacFeatures.geometries.add(geometry);
                stacFeatures.features.add(f);
                stacFeatures.epsgs.add(epsgInt);
            }
        }
        iterator.close();
        System.out.println("Done.");
        return stacFeatures;
    }

    protected static class StacFeatures {
        public List<String> ids = new ArrayList<>();
        public List<String> timestamps = new ArrayList<>();
        public List<Geometry> geometries = new ArrayList<>();
        public List<SimpleFeature> features = new ArrayList<>();
        public List<Integer> epsgs = new ArrayList<>();

        public Geometry getCoveredAreas() {
            Geometry union = CascadedPolygonUnion.union(geometries);
            return union;
        }

        public int getSize() {
            return features.size();
        }
    }
}
