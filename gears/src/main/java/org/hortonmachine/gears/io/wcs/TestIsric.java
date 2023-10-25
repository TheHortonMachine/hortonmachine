package org.hortonmachine.gears.io.wcs;

import java.io.File;
import java.util.List;

import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.locationtech.jts.geom.Envelope;

public class TestIsric {

    /* working url
     * 
     * https://maps.isric.org/mapserv?map=/map/nitrogen.map
     *      &SERVICE=WCS
     *      &VERSION=2.0.1
     *      &REQUEST=GetCoverage
     *      &COVERAGEID=nitrogen_5-15cm_Q0.5
     *      &FORMAT=image/tiff
     *      &SUBSET=X(-1784000,-1140000)
     *      &SUBSET=Y(1356000,1863000)
     *      &SUBSETTINGCRS=http://www.opengis.net/def/crs/EPSG/0/152160
     */

    public static void main(String[] args) throws Exception {

        String outFolder = "/Users/hydrologis/TMP/KLAB/WCS/DUMPS/";

        String SERVICE_URL = "https://maps.isric.org/mapserv?map=/map/nitrogen.map"; 
        String coverageId = "nitrogen_0-5cm_Q0.95";
        String version = null;

        IWebCoverageService service = IWebCoverageService.getServiceForVersion(SERVICE_URL, version);

        System.out.println(service.getVersion());

        // service.dumpCoverageFootprints(outFolder);

        // print list of coverage ids
        System.out.println("Coverage ids:");
        List<String> coverageIds = service.getCoverageIds();
        for (String cid : coverageIds) {
            System.out.println("\t" + cid);
        }

        // print supported srids
        int[] supportedSrids = service.getSupportedSrids();
        if (supportedSrids != null) {
            System.out.println("Supported srids:");
            if (supportedSrids != null)
                for (int srid : supportedSrids) {
                    System.out.println("\t" + srid);
                }
        }

        // print supported formats
        List<String> supportedFormats = service.getSupportedFormats();
        if (supportedFormats != null) {
            System.out.println("Supported formats:");
            if (supportedFormats != null)
                for (String format : supportedFormats) {
                    System.out.println("\t" + format);
                }
        }

        // describe coverage
        String describeCoverageUrl = service.getDescribeCoverageUrl(coverageId);
        System.out.println(describeCoverageUrl);

        IDescribeCoverage describeCoverage = service.getDescribeCoverage(coverageId);
        System.out.println(describeCoverage);

        // get coverage
        CoverageReaderParameters parameters = new CoverageReaderParameters(service, coverageId);
        parameters.format("image/tiff");

        Envelope env = Wcs.evelope(12.263, -17.05, 16.763, -11.365);
        parameters.bbox(env, 4326);
        // Envelope env = Wcs.evelope(-1784000, 1356000, -1140000, 1863000);
        // parameters.bbox(env, 152160);
        // parameters.scaleFactor(0.01);
        // parameters.rowsCols(new int[] { 1000, 1000 }); // TODO in case of 100 a check on gridenvelope needs to be done
        parameters.outputSrid(4326);

        File file = new File(outFolder, coverageId + ".tiff");
        String url = service.getCoverage(file.getAbsolutePath(), parameters, null);

        System.out.println("Coverage url: " + url);


    }
}
