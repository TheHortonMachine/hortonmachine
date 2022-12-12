package org.hortonmachine.gears.io.stac;

import java.io.File;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestStacAssetDownloader extends TestVariables {

    public TestStacAssetDownloader() throws Exception {
        LogProgressMonitor pm = new LogProgressMonitor();
        try (HMStacManager stacManager = new HMStacManager(repoUrl, pm)) {
            stacManager.open();

            HMStacCollection collection = stacManager.getCollectionById(collectionQuery);

            List<HMStacItem> items = collection.setDayFilter(dayQuery, null).setGeometryFilter(intersectionGeometry)
                    .setCqlFilter(CQL_FILTER).setLimit(limit).searchItems();
            int size = items.size();
            System.out.println("Found " + size + " items matching the query.");
            System.out.println();

            if (size > 0) {
                Geometry coveredAreas = HMStacCollection.getCoveredArea(items);
                Geometry commonArea = coveredAreas.intersection(intersectionGeometry);
                double coveredArea = commonArea.getArea();
                double roiArea = intersectionGeometry.getArea();
                int percentage = (int) Math.round(coveredArea * 100 / roiArea);
                System.out.println("Region of interest is covered by data in amout of " + percentage + "%");
                System.out.println();

                Integer srid = items.get(0).getEpsg();
                CoordinateReferenceSystem outputCrs = CrsUtilities.getCrsFromSrid(srid);
                ReferencedEnvelope roiEnvelope = new ReferencedEnvelope(intersectionGeometry.getEnvelopeInternal(),
                        DefaultGeographicCRS.WGS84).transform(outputCrs, true);

                RegionMap regionMap = RegionMap.fromEnvelopeAndGrid(roiEnvelope, downloadCols, downloadRows);

                HMRaster outRaster = HMStacCollection.readRasterBandOnRegion(regionMap, band, items, pm);
                File downloadFolderFile = new File(downloadFolder);
                File downloadFile = new File(downloadFolderFile, outRaster.getName());

                OmsRasterWriter.writeRaster(downloadFile.getAbsolutePath(), outRaster.buildCoverage());
            }
        }

    }

    public static void main( String[] args ) throws Exception {
        new TestStacAssetDownloader();
    }

}
