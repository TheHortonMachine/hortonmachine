package org.hortonmachine.gears.io.wcs;

import org.hortonmachine.gears.io.wcs.models.WcsCapabilities;

public class WebCoverageService {
    public static WCSBase getWebCoverageService(String url, String version) throws Exception{
        return getWebCoverageService(url, version, null, null, 30, null, null);
    }

    public static WCSBase getWebCoverageService(String url, String version, String xml, String cookies, int timeout, Authentication auth, String headers) throws Exception{
    // ''' wcs factory function, returns a version specific WebCoverageService object '''

        if (auth==null)
            auth = new Authentication();

        if (version == null){
            if (xml == null) {
                WCSCapabilitiesReader reader = new WCSCapabilitiesReader(version, cookies, auth, timeout, headers);
                String request = reader.capabilities_url(url);
                WcsCapabilities wcsCapabilities = reader.read(request, timeout);
                System.out.println(wcsCapabilities);
    //         xml = openURL(
    //             request, cookies=cookies, timeout=timeout, auth=auth, headers=headers).read()

    //     capabilities = etree.etree.fromstring(xml)
    //     version = capabilities.get('version')
    //     del capabilities
        }
    }

    // clean_url = clean_ows_url(url)

    // if version == '1.0.0':
    //     return wcs100.WebCoverageService_1_0_0.__new__(
    //         wcs100.WebCoverageService_1_0_0, clean_url, xml, cookies, auth=auth, timeout=timeout, headers=headers)
    // elif version == '1.1.0':
    //     return wcs110.WebCoverageService_1_1_0.__new__(
    //         wcs110.WebCoverageService_1_1_0, url, xml, cookies, auth=auth, timeout=timeout, headers=headers)
    // elif version == '1.1.1':
    //     return wcs111.WebCoverageService_1_1_1.__new__(
    //         wcs111.WebCoverageService_1_1_1, url, xml, cookies, auth=auth, timeout=timeout, headers=headers)
    // elif version == '2.0.0':
    //     return wcs200.WebCoverageService_2_0_0.__new__(
    //         wcs200.WebCoverageService_2_0_0, url, xml, cookies, auth=auth, timeout=timeout, headers=headers)
    // elif version == '2.0.1':
    //     return wcs201.WebCoverageService_2_0_1.__new__(
    //         wcs201.WebCoverageService_2_0_1, url, xml, cookies, auth=auth, timeout=timeout, headers=headers)

        return null;
    }


    public static void main(String[] args) throws Exception {
        String SERVICE_URL = "https://geoservices9.civis.bz.it/geoserver/ows"; //?service=WCS&version=2.0.1&request=GetCapabilities";
        WCSBase wcs = WebCoverageService.getWebCoverageService(SERVICE_URL, null);
   

    }
}
