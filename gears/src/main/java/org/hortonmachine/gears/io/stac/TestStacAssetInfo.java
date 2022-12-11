package org.hortonmachine.gears.io.stac;

import java.util.List;

import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;

public class TestStacAssetInfo extends TestVariables {

    public TestStacAssetInfo() throws Exception {

        try (HMStacManager stacManager = new HMStacManager(repoUrl, new LogProgressMonitor())) {
            stacManager.open();

            HMStacCollection collection = stacManager.getCollectionById(collectionQuery);

            List<HMStacItem> items = collection.setDayFilter(dayQuery, null).setGeometryFilter(intersectionGeometry)
                    .setCqlFilter(CQL_FILTER).setLimit(limit).searchItems();
            int size = items.size();
            System.out.println("Found " + size + " items matching the query.");
            System.out.println();

            if (size > 0) {
                int featureCount = 0;
                for( HMStacItem item : items ) {

                    String id = item.getId();
                    System.out.println(++featureCount + ") id=" + id);
                    System.out.println("------------------------------------");
                    System.out.println(item.toString());

                    List<HMStacAsset> assets = item.getAssets();
                    System.out.println("\tList of Assets: ");
                    int count = 1;
                    for( HMStacAsset asset : assets ) {
                        System.out.println("\t" + featureCount + "." + count++ + ") '" + asset.getTitle() + "' --> ("
                                + asset.getType() + ")");
                    }
                }
            }
        }

    }

    public static void main( String[] args ) throws Exception {
        new TestStacAssetInfo();
    }

}
