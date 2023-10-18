package org.hortonmachine.gears.io.wcs;

import java.util.List;

import org.hortonmachine.gears.io.wcs.models.CoverageSummary;
import org.hortonmachine.gears.io.wcs.models.DescribeCoverage;
import org.hortonmachine.gears.io.wcs.models.WcsCapabilities;
import org.hortonmachine.gears.io.wcs.readers.DescribeCoverageReader;
import org.hortonmachine.gears.io.wcs.readers.WCSCapabilitiesReader;

public class WebCoverageService {
    WcsCapabilities wcsCapabilities;
    private String url;
    private String baseUrl;
    private String version;
    private String xml;
    private String cookies;
    private int timeout;
    private Authentication auth;
    private String headers;

    public WebCoverageService(String url, String version) throws Exception {
        this(url, version, null, null, 30, null, null);
    }

    public WebCoverageService(String url, String version, String xml, String cookies, int timeout, Authentication auth,
            String headers) throws Exception {
        this.url = url;
        this.version = version;
        this.xml = xml;
        this.cookies = cookies;
        this.timeout = timeout;
        this.auth = auth;
        this.headers = headers;
        if (auth == null)
            auth = new Authentication();
    }

    private void init() throws Exception {
        if (wcsCapabilities != null)
            return;
        WCSCapabilitiesReader reader = new WCSCapabilitiesReader(version, cookies, auth, timeout, headers);
        if (url.indexOf('?') != -1) {
            baseUrl = url.split("\\?")[0];
        }
        wcsCapabilities = reader.read(url, timeout);
        if(version == null)
            version = wcsCapabilities.getVersion();
        // System.out.println(wcsCapabilities);
    }

    public WcsCapabilities getCapabilities() throws Exception {
        init();
        return wcsCapabilities;
    }

    public DescribeCoverage getDescribeCoverage(CoverageSummary coverageSummary) throws Exception {
        init();
        DescribeCoverageReader reader = new DescribeCoverageReader(version, coverageSummary.coverageId, cookies, auth, timeout, headers);
     
        String describeCoverageUrl = wcsCapabilities.getOperationsMetadata().getDescribeCoverageUrl();
        if(describeCoverageUrl == null)
            describeCoverageUrl = baseUrl;
     
        DescribeCoverage dc = reader.read(describeCoverageUrl, timeout);
        return dc;
    }

    /**
     *  <p>example 2.0.1 URL
     *  http://earthserver.pml.ac.uk/rasdaman/ows?
     *      &SERVICE=WCS
     *      &VERSION=2.0.1
     *      &REQUEST=GetCoverage
     *      &COVERAGEID=V2_monthly_CCI_chlor_a_insitu_test
     *      &SUBSET=Lat(40,50)
     *      &SUBSET=Long(-10,0)
     *      &SUBSET=ansi(144883,145000)
     *      &FORMAT=application/netcdf
     *
     * @throws Exception
     */
    public void getCoverage() throws Exception {
        init();

    }

    public static void main(String[] args) throws Exception {
        String SERVICE_URL = "https://geoservices9.civis.bz.it/geoserver/ows"; // ?service=WCS&version=2.0.1&request=GetCapabilities";
        WebCoverageService wcs = new WebCoverageService(SERVICE_URL, null);

        WcsCapabilities capabilities = wcs.getCapabilities();
        List<String> coverageIds = capabilities.getCoverageIds();

        CoverageSummary coverageSummary = capabilities.getCoverageSummaryById(coverageIds.get(0));
        DescribeCoverage describeCoverage = wcs.getDescribeCoverage(coverageSummary);
        
        System.out.println(describeCoverage);

    }
}
