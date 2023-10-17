package org.hortonmachine.gears.io.wcs;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hortonmachine.gears.io.wcs.models.WcsCapabilities;

/**
 * Read and parses WCS capabilities document into a a dictionary structure.
 */
public class WCSCapabilitiesReader {
    private String version;
    private String cookies;
    private Authentication auth;
    private int timeout;
    private String headers;

    public WCSCapabilitiesReader(String version, String cookies, Authentication auth, int timeout, String headers) {
        this.version = version;
        this.cookies = cookies;
        if (auth == null)
            auth = new Authentication();
        this.auth = auth;
        this.timeout = timeout;
        this.headers = headers;
    }

    String capabilities_url(String service_url){
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

    WcsCapabilities read(String service_url, int timeout) throws Exception{
        // """Get and parse a WCS capabilities document, returning an
        // elementtree tree

        // @type service_url: string
        // @param service_url: The base url, to which is appended the service,
        // version, and request parameters
        // @rtype: elementtree tree
        // @return: An elementtree tree representation of the capabilities document
        // """

        String request = capabilities_url(service_url);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet getCapabilitiesRequest = new HttpGet(request);
        HttpResponse response = httpClient.execute(getCapabilitiesRequest);

        XmlHelper xmlHelper = XmlHelper.fromStream(response.getEntity().getContent());

        WcsCapabilities wcsCapabilities = new WcsCapabilities();
        XmlHelper.apply(xmlHelper.getRootNode(), wcsCapabilities);

        return wcsCapabilities;
    }


}
