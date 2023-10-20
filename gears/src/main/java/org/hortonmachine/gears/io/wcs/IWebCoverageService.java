package org.hortonmachine.gears.io.wcs;

import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201;

public interface IWebCoverageService {

    String getVersion() throws Exception;

    List<String> getCoverageIds() throws Exception;

    List<String> getSupportedFormats() throws Exception;

    int[] getSupportedSrids() throws Exception;

    String getCapabilitiesUrl();

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
            // TODO
        } else if (version.equals("1.1.0")) {
            // TODO
        } else if (version.equals("1.1.1")) {
            // TODO
        } else if (version.equals("2.0.1")) {
            return new org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201(url, version, xml, cookies, timeout,
                    auth, headers);
        }
        return null;
    }

}