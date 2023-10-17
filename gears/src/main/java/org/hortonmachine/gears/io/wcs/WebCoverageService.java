package org.hortonmachine.gears.io.wcs;

import org.hortonmachine.gears.io.wcs.models.WcsCapabilities;

public class WebCoverageService {
    WcsCapabilities wcsCapabilities;
    private String url;
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
        String request = reader.capabilities_url(url);
        wcsCapabilities = reader.read(request, timeout);
        // System.out.println(wcsCapabilities);
    }

    public WcsCapabilities getCapabilities() throws Exception {
        init();
        return wcsCapabilities;
    }

    public static void main(String[] args) throws Exception {
        String SERVICE_URL = "https://geoservices9.civis.bz.it/geoserver/ows"; // ?service=WCS&version=2.0.1&request=GetCapabilities";
        WebCoverageService wcs = new WebCoverageService(SERVICE_URL, null);
        
        System.out.println(wcs.getCapabilities());

    }
}
