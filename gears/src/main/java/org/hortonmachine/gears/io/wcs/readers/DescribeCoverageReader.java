package org.hortonmachine.gears.io.wcs.readers;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hortonmachine.gears.io.wcs.Authentication;
import org.hortonmachine.gears.io.wcs.IDescribeCoverage;
import org.hortonmachine.gears.io.wcs.XmlHelper;
import org.hortonmachine.gears.io.wcs.wcs201.models.DescribeCoverage;


/**
 * Read and parses WCS DescribeCoverage document into a lxml.etree infoset
 */
public class DescribeCoverageReader {

    private String version;
    private String identifier;
    private String cookies;
    private Authentication auth;
    private int timeout;
    private String headers;

    public DescribeCoverageReader(String version, String identifier, String cookies, Authentication auth, int timeout,
            String headers) {
        this.version = version;
        this.identifier = identifier;
        this.cookies = cookies;
        if (auth == null)
            auth = new Authentication();
        this.auth = auth;
        this.timeout = timeout;
        this.headers = headers;
    }

    public String descCov_url(String service_url) {
        // """Return a describe coverage url
        // @type service_url: string
        // @param service_url: base url of WCS service
        // @rtype: string
        // @return: getCapabilities URL
        // """
        List<String[]> qs = new ArrayList<>();
        List<String> params = new ArrayList<>();
        if (service_url.indexOf('?') != -1) {
            String string = service_url.split("?")[1];
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
            qs.add(new String[] { "request", "DescribeCoverage" });
        }
        if (!params.contains("version")) {
            qs.add(new String[] { "version", version });
        }
        if (version.equals("1.0.0")) {
            if (!params.contains("coverage")) {
                qs.add(new String[] { "coverage", identifier });
            }
        } else if (version.equals("2.0.0") || version.equals("2.0.1")) {
            if (!params.contains("CoverageID")) {
                qs.add(new String[] { "CoverageID", identifier });
            }
        } else if (version.equals("1.1.0") || version.equals("1.1.1")) {
            // # NOTE: WCS 1.1.0 is ambigous about whether it should be identifier
            // # or identifiers (see tables 9, 10 of specification)
            if (!params.contains("identifiers")) {
                qs.add(new String[] { "identifiers", identifier });
            }
            if (!params.contains("identifier")) {
                qs.add(new String[] { "identifier", identifier });
                qs.add(new String[] { "format", "text/xml" });
            }
        }

        // TODO urlencode
        String urlqs = "";
        for (String[] kv : qs) {
            urlqs += kv[0] + "=" + kv[1] + "&";
        }
        urlqs = urlqs.substring(0, urlqs.length() - 1);
        return service_url.split("\\?")[0] + "?" + urlqs;

        // urlqs = urlencode(tuple(qs))
        // return service_url.split('?')[0] + '?' + urlqs
    }

    public IDescribeCoverage read(String service_url, int timeout) throws Exception{
        String request = descCov_url(service_url);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet getCapabilitiesRequest = new HttpGet(request);
        HttpResponse response = httpClient.execute(getCapabilitiesRequest);

        XmlHelper xmlHelper = XmlHelper.fromStream(response.getEntity().getContent());

        // xmlHelper.printTree();

        DescribeCoverage describeCoverage = new DescribeCoverage();
        XmlHelper.apply(xmlHelper.getRootNode(), describeCoverage);

        return describeCoverage;
    }

}
