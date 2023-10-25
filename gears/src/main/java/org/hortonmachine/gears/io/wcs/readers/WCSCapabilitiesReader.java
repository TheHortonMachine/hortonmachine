package org.hortonmachine.gears.io.wcs.readers;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.wcs.Authentication;
import org.hortonmachine.gears.io.wcs.IWcsCapabilities;
import org.hortonmachine.gears.io.wcs.WcsUtils;
import org.hortonmachine.gears.io.wcs.XmlHelper;

/**
 * Read and parses WCS capabilities document into a a dictionary structure.
 */
public class WCSCapabilitiesReader {
    private String version;
    private String cookies;
    private Authentication auth;
    private int timeout;
    private String headers;
    private XmlHelper xmlHelper;

    public WCSCapabilitiesReader(String version, String cookies, Authentication auth, int timeout, String headers) {
        this.version = version;
        this.cookies = cookies;
        if (auth == null)
            auth = new Authentication();
        this.auth = auth;
        this.timeout = timeout;
        this.headers = headers;
    }

    public static String capabilities_url(String service_url, String version) {
        // """Return a capabilities url
        // @type service_url: string
        // @param service_url: base url of WCS service
        // @rtype: string
        // @return: getCapabilities URL
        // """

        List<String[]> qs = new ArrayList<>();
        List<String> params = new ArrayList<>();
        if (service_url.indexOf('?') != -1) {
            String string = service_url.split("\\?")[1];
            // qs = parse_qsl()
            for (String pair : string.split("&")) {
                String[] kv = pair.split("=");
                qs.add(kv);
                params.add(kv[0]);
            }
        }

        if (!params.contains("service")) {
            qs.add(new String[] { "service", "WCS" });
        }
        if (!params.contains("request")) {
            qs.add(new String[] { "request", "GetCapabilities" });
        }
        if (!params.contains("version") && version != null) {
            qs.add(new String[] { "version", version });
        }
        // qs = []
        // if service_url.find('?') != -1:
        // qs = parse_qsl(service_url.split('?')[1])

        // params = [x[0] for x in qs]

        // if 'service' not in params:
        // qs.append(('service', 'WCS'))
        // if 'request' not in params:
        // qs.append(('request', 'GetCapabilities'))
        // if ('version' not in params) and (self.version is not None):
        // qs.append(('version', self.version))

        // urlqs = urlencode(tuple(qs))
        // return service_url.split('?')[0] + '?' + urlqs

        // TODO urlencode
        String urlqs = "";
        for (String[] kv : qs) {
            urlqs += kv[0] + "=" + kv[1] + "&";
        }
        urlqs = urlqs.substring(0, urlqs.length() - 1);
        return service_url.split("\\?")[0] + "?" + urlqs;
    }

    public XmlHelper getXmlHelper(String service_url, int timeout) throws Exception {
        if (xmlHelper != null) {
            return xmlHelper;
        }
        String request = capabilities_url(service_url, version);

        xmlHelper = WcsUtils.getXmlHelperForRequest(request);

        return xmlHelper;
    }

    public String getVersion(String service_url, int timeout) throws Exception {
        if (this.version != null) {
            return version;
        }
        getXmlHelper(service_url, timeout);
        String version = XmlHelper.findAttribute(xmlHelper.getRootNode(), "version");
        if (this.version == null) {
            this.version = version;
        }
        return version;
    }

    public IWcsCapabilities read(String service_url, int timeout) throws Exception {
        XmlHelper xmlHelper = getXmlHelper(service_url, timeout);

        IWcsCapabilities wcsCapabilities = null;
        if (version.equals("2.0.1")) {
            wcsCapabilities = new org.hortonmachine.gears.io.wcs.wcs201.models.WcsCapabilities();
            XmlHelper.apply(xmlHelper.getRootNode(), wcsCapabilities);
        } else if (version.equals("1.1.1") || version.equals("1.1.0")) {
            wcsCapabilities = new org.hortonmachine.gears.io.wcs.wcs111.models.WcsCapabilities();
            XmlHelper.apply(xmlHelper.getRootNode(), wcsCapabilities);
        } else if (version.equals("1.0.0")) {
            wcsCapabilities = new org.hortonmachine.gears.io.wcs.wcs100.models.WcsCapabilities();
            XmlHelper.apply(xmlHelper.getRootNode(), wcsCapabilities);
        } else if (version == null) {
            // no version supplied, get default
            version = getVersion(service_url, timeout);
        }

        if (wcsCapabilities == null)
            throw new Exception("Unsupported WCS version: " + version);

        this.version = wcsCapabilities.getVersion();

        return wcsCapabilities;
    }

}
