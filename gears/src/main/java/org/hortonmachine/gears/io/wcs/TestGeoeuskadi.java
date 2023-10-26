package org.hortonmachine.gears.io.wcs;

import java.io.File;
import java.util.List;

import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.locationtech.jts.geom.Envelope;

public class TestGeoeuskadi {

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

        // String SERVICE_URL = "https://geo.hazi.eus/ows";
        String SERVICE_URL = "https://geo.hazi.eus/S2GEOEUSKADI_RGB/wcs";
        String coverageId = "S2GEOEUSKADI_RGB__S2A_20160703T110602307Z_RGB"; //S2GEOEUSKADI_RGB:S2A_20150818T110635806Z_RGB";
        String version = null;

        IWebCoverageService service = IWebCoverageService.getServiceForVersion(SERVICE_URL, version);

        System.out.println(service.getVersion());

        // service.dumpCoverageFootprints(outFolder);

        // print list of coverage ids
        System.out.println("Coverage ids:");
        List<String> coverageIds = service.getCoverageIds();
        int index = 1;
        for (String cid : coverageIds) {
            System.out.println("\t" + index + ")" + cid);
            index++;
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

        Envelope env = Wcs.evelope(504787.0, 4734309.0, 536275.0, 4754145.0);
        parameters.bbox(env, 25830);
        // parameters.scaleFactor(0.01);
        parameters.rowsCols(1000, 1000 ); 
        parameters.outputSrid(4326);

        File file = new File(outFolder, coverageId + "_1000.tiff");
        String url = service.getCoverage(file.getAbsolutePath(), parameters, null);

        System.out.println("Coverage url: " + url);


    }
}
