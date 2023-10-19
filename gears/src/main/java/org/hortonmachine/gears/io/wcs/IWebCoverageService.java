package org.hortonmachine.gears.io.wcs;

import java.util.HashMap;
import java.util.List;

import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201;

public interface IWebCoverageService {

    String getVersion() throws Exception;

    List<String> getCoverageIds() throws Exception;

    List<String> getSupportedFormats() throws Exception;

    int[] getSupportedSrids() throws Exception;

    String getCapabilitiesUrl();

    void getCoverage(String outputFilePath, CoverageReaderParameters parameters,
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