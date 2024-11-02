package org.hortonmachine.gears.io.wcs;

import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201;

public interface IWebCoverageService {

    String getCapabilitiesUrl();

    /**
     * Returns the version of the Web Coverage Service, as read from the 
     * capabilities document.
     *
     * @return the version of the Web Coverage Service
     * @throws Exception if an error occurs while retrieving the version
     */
    String getVersion() throws Exception;

    /**
     * Returns a list of coverage IDs available in the Web Coverage Service.
     *
     * @return a list of coverage IDs as read from the capabilities document
     * @throws Exception if an error occurs while retrieving the coverage IDs
     */
    List<String> getCoverageIds() throws Exception;

    /**
     * Returns a summary of the coverage with the given ID.
     * 
     * <p>The summary is read from the capabilities document. For more
     * information, getting the describe coverage document is recommended.</p>
     *
     * @param coverageId the ID of the coverage to retrieve the summary for
     * @return the coverage summary
     * @throws Exception if an error occurs while retrieving the summary
     */
    ICoverageSummary getCoverageSummary(String coverageId) throws Exception;

    /**
     * Returns a DescribeCoverage object for the specified coverage ID.
     *
     * @param coverageId the ID of the coverage to describe
     * @return the DescribeCoverage object for the specified coverage ID
     * @throws Exception if an error occurs while retrieving the DescribeCoverage object
     */
    IDescribeCoverage getDescribeCoverage(String coverageId) throws Exception;

    /**
     * Returns the URL for a DescribeCoverage request for the specified coverage ID.
     *
     * @param coverageId the ID of the coverage to describe
     * @return the URL for a DescribeCoverage request for the specified coverage ID
     * @throws Exception if an error occurs while constructing the URL
     */
    String getDescribeCoverageUrl(String coverageId) throws Exception;

    /**
     * Returns a list of supported formats for the Web Coverage Service, if
     * the capabilities document contains the information (ex. for 2.0.1).
     * 
     * <p>If null is returned, the supported formats should be retrieved from
     * the DescribeCoverage document of the single coverage.</p>
     *
     * @return a list of supported formats, else null.
     * @throws Exception if an error occurs while retrieving the supported formats
     */
    List<String> getSupportedFormats() throws Exception;

    /**
     * Returns an array of supported SRIDs (Spatial Reference IDs) by the
     * Web Coverage Service, if the capabilities document contains the 
     * information (ex. for 2.0.1).
     *
     * <p>If null is returned, the supported formats should be retrieved from
     * the DescribeCoverage document of the single coverage.</p>
     * 
     * @return an array of supported SRIDs
     * @throws Exception if an error occurs while retrieving the supported SRIDs
     */
    int[] getSupportedSrids() throws Exception;

    /**
     * Retrieves a coverage from the web coverage service and saves it to the specified output file path.
     *
     * @param outputFilePath the file path where the coverage will be saved
     * @param parameters the parameters used to read the coverage
     * @param additionalParameters additional parameters to be included in the request
     * @return the url used to do the getCoverage request
     * @throws Exception if an error occurs while retrieving or saving the coverage
     */
    String getCoverage(String outputFilePath, CoverageReaderParameters parameters,
            HashMap<String, String> additonalParameters)
            throws Exception;
    
    /**
     * Retrieves a GridCoverage2D object from the Web Coverage Service (WCS) using the provided parameters.
     * 
     * <P>Note that this still needs to save an intermediate file to disk, so it is not as efficient as one might think.</P>
     *
     * @param parameters the parameters used to read the coverage from the WCS
     * @param additonalParameters additional parameters to be included in the request
     * @return the GridCoverage2D object retrieved from the WCS
     * @throws Exception if an error occurs while retrieving the coverage
     */
    GridCoverage2D getCoverage(CoverageReaderParameters parameters,
            HashMap<String, String> additonalParameters)
            throws Exception;

    public static IWebCoverageService getServiceForVersion(String url, String version) throws Exception {
        return getServiceForVersion(url, version, null, null, 30, null, null);
    }

    public static IWebCoverageService getServiceForVersion(String url, String version, String xml, String cookies,
            int timeout, Authentication auth,
            String headers) throws Exception {
        if (version == null) {
            // try with latest
            WebCoverageService201 wcsService = new org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201(url,
                    version, xml, cookies, timeout, auth, headers);
            String defaultVersion = wcsService.getVersion();
            if (defaultVersion.equals("2.0.1")) {
                return wcsService;
            } else {
                // and try to read it later in that version
                version = defaultVersion;
            }
        }

        if (version.equals("1.0.0")) {
            return new org.hortonmachine.gears.io.wcs.wcs100.WebCoverageService100(url, version, xml, cookies, timeout,
                    auth, headers);
        } else if (version.equals("1.1.2") || version.equals("1.1.1") || version.equals("1.1.0")) {
            return new org.hortonmachine.gears.io.wcs.wcs111.WebCoverageService111(url, version, xml, cookies, timeout,
                    auth, headers);
        } else if (version.equals("2.0.1")) {
            return new org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201(url, version, xml, cookies, timeout,
                    auth, headers);
        }
        return null;
    }

    public void dumpCoverageFootprints(String outFolder) throws Exception;

}