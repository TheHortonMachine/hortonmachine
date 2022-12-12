package org.hortonmachine.gears.io.stac;

import java.util.List;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class TestStacAssetFootprint extends TestVariables {
    public TestStacAssetFootprint() throws Exception {

        try (HMStacManager stacManager = new HMStacManager(repoUrl, new LogProgressMonitor())) {
            stacManager.open();

            HMStacCollection collection = stacManager.getCollectionById(collectionQuery);

            List<HMStacItem> items = collection.setDayFilter(dayQuery, null).setGeometryFilter(intersectionGeometry)
                    .setCqlFilter(CQL_FILTER).setLimit(limit).searchItems();
            int size = items.size();
            System.out.println("Found " + size + " items matching the query.");

            Geometry coveredAreas = HMStacCollection.getCoveredArea(items);
            Geometry commonArea = coveredAreas.intersection(intersectionGeometry);
            double coveredArea = commonArea.getArea();
            double roiArea = intersectionGeometry.getArea();
            int percentage = (int) Math.round(coveredArea * 100 / roiArea);
            System.out.println("Region of interest is covered by data in amout of " + percentage + "%");
            System.out.println();

            if (size > 0) {
                SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
                b.setName("typename");
                b.setCRS(DefaultGeographicCRS.WGS84);
                b.add("the_geom", Polygon.class);
                b.add("stacid", String.class);
                b.add("timestamp", String.class);
                SimpleFeatureType type = b.buildFeatureType();
                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
                DefaultFeatureCollection outFC = new DefaultFeatureCollection();

                System.out.println("Processing items...");
                for( HMStacItem item : items ) {
                    Object[] values = new Object[]{item.getGeometry(), item.getId(), item.getTimestamp()};
                    builder.addAll(values);
                    SimpleFeature feature = builder.buildFeature(null);
                    outFC.add(feature);
                }
                System.out.println("Done");

                Object[] values = new Object[]{intersectionGeometry, "ROI", ""};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                outFC.add(feature);

                OmsVectorWriter.writeVector(overviewShp, outFC);
            }
        }

    }

    public static void main( String[] args ) throws Exception {
        new TestStacAssetFootprint();
    }

}
