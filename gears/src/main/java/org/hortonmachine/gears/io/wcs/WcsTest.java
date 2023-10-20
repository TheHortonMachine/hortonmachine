package org.hortonmachine.gears.io.wcs;

import java.io.File;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.locationtech.jts.geom.Envelope;

public class WcsTest {

    public static void main(String[] args) throws Exception {

            /**
     * <p>
     * example 2.0.1 URL
     * http://earthserver.pml.ac.uk/rasdaman/ows?
     * &SERVICE=WCS
     * &VERSION=2.0.1
     * &REQUEST=GetCoverage
     * &COVERAGEID=V2_monthly_CCI_chlor_a_insitu_test
     * &SUBSET=Lat(40,50)
     * &SUBSET=Long(-10,0)
     * &SUBSET=ansi(144883,145000)
     * &FORMAT=application/netcdf
     *
     * @throws Exception
     */
        
        // https://www.wcs.nrw.de/geobasis/wcs_nw_dgm
        //      ?VERSION=2.0.1
        //      &SERVICE=wcs
        //      &REQUEST=GetCoverage
        //      &COVERAGEID=nw_dgm
        //      &FORMAT=image/tiff
        //      &SUBSET=x(372511,374511)
        //      &SUBSET=y(5613116,5615116)
        //      &SCALEFACTOR=1
        //      &SUBSETTINGCRS=EPSG:25832

        // http://ogcdev.bgs.ac.uk/geoserver/OneGDev/wcs?
        //     service=WCS
        //     &version=2.0.1
        //     &CoverageId=OneGDev__AegeanLevantineSeas-MCol
        //     &request=GetCoverage
        //     &format=image/png&

        // http://ogcdev.bgs.ac.uk/geoserver/OneGDev/wcs?service=WCS
        //     &version=2.0.1
        //     &CoverageId=OneGDev__AegeanLevantineSeas-MCol
        //     &request=GetCoverage
        //     &format=image/png
        //     &subset=Lat(34.54889,37.31744)
        //     &subset=Long(26.51071,29.45505)

        String outFolder = "/home/hydrologis/TMP/KLAB/WCS/DUMPS/";

        // String SERVICE_URL = "https://geoservices9.civis.bz.it/geoserver/ows"; // ?service=WCS&version=2.0.1&request=GetCapabilities";
        String SERVICE_URL = "http://ogcdev.bgs.ac.uk/geoserver/OneGDev/wcs";

        IWebCoverageService service = IWebCoverageService.getServiceForVersion(SERVICE_URL, null);

        System.out.println( service.getVersion());

        // print list of coverage ids
        System.out.println("Coverage ids:");
        List<String> coverageIds = service.getCoverageIds();
        for (String coverageId : coverageIds) {
            System.out.println("\t" + coverageId);
        }

        // print supported srids
        System.out.println("Supported srids:");
        int[] supportedSrids = service.getSupportedSrids();
        for (int srid : supportedSrids) {
            System.out.println("\t" + srid);
        }

        // print supported formats
        System.out.println("Supported formats:");
        List<String> supportedFormats = service.getSupportedFormats();
        for (String format : supportedFormats) {
            System.out.println("\t" + format);
        }

        // get coverage
        // String coverageId = "p_bz-Elevation__DigitalTerrainModel-2.5m";
        String coverageId = "OneGDev__AegeanLevantineSeas-MCol";
        Envelope env = new Envelope(26.51071, 29.45505, 35.5, 36.0);

        CoverageReaderParameters parameters = new CoverageReaderParameters(service, coverageId);
        parameters.format("image/tiff");
        parameters.bbox(env, 4326);
        // parameters.scaleFactor(0.01);
        parameters.rowsCols(new int[] { 100, 100 });
        parameters.outputSrid(32633);
        
        File file = new File(outFolder, coverageId + "_bbox_parameter_scale05_100x100_32633.tiff");
        String url = service.getCoverage(file.getAbsolutePath(), parameters, null);

        System.out.println("Coverage url: " + url);

        
        // WebCoverageService201 wcs = new WebCoverageService201(SERVICE_URL, "2.0.1");
        // String defaultVersion = wcs.getVersion();

        // System.out.println( wcs.getCapabilitiesUrl());
        // WcsCapabilities capabilities = wcs.getCapabilities();

        // List<String> coverageIds = capabilities.getCoverageIds();

        // String coverageId = coverageIds.get(0);
        // CoverageSummary coverageSummary =
        // capabilities.getCoverageSummaryById(coverageId);
        // DescribeCoverage describeCoverage = wcs.getDescribeCoverage(coverageSummary);

        // System.out.println(describeCoverage);

    }
}
