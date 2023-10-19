package org.hortonmachine.gears.io.wcs.wcs201;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import org.hortonmachine.gears.io.wcs.Authentication;
import org.hortonmachine.gears.io.wcs.IWebCoverageService;
import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.hortonmachine.gears.io.wcs.readers.WCSCapabilitiesReader;
import org.hortonmachine.gears.io.wcs.wcs201.models.CoverageSummary;
import org.hortonmachine.gears.io.wcs.wcs201.models.DescribeCoverage;
import org.hortonmachine.gears.io.wcs.wcs201.models.WcsCapabilities;
import org.hortonmachine.gears.io.wcs.wcs201.readers.DescribeCoverageReader;

public class WebCoverageService201 implements IWebCoverageService {
    WcsCapabilities wcsCapabilities;
    private String url;
    private String baseUrl;
    private String version;
    private String xml;
    private String cookies;
    private int timeout;
    private Authentication auth;
    private String headers;
    private WCSCapabilitiesReader reader;

    public WebCoverageService201(String url, String version) throws Exception {
        this(url, version, null, null, 30, null, null);
    }

    public WebCoverageService201(String url, String version, String xml, String cookies, int timeout, Authentication auth,
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
        if (url.indexOf('?') != -1) {
            baseUrl = url.split("\\?")[0];
        }
    }

    @Override
    public String getVersion() throws Exception {
        if (reader == null)
            reader = new WCSCapabilitiesReader(version, cookies, auth, timeout, headers);
        return reader.getVersion(url, timeout);
    }

    @Override
    public List<String> getCoverageIds() throws Exception{
        init();
        return wcsCapabilities.getCoverageIds();
    }

    @Override
    public List<String> getSupportedFormats() throws Exception{
        init();
        return wcsCapabilities.getServiceMetadata().getSupportedFormats();
    }

    @Override
    public int[] getSupportedSrids() throws Exception{
        init();
        return wcsCapabilities.getServiceMetadata().getSupportedSrids();
    }

    private void init() throws Exception {
        if (wcsCapabilities != null)
            return;
        if (reader == null)
            reader = new WCSCapabilitiesReader(version, cookies, auth, timeout, headers);
        wcsCapabilities = (WcsCapabilities) reader.read(url, timeout);
        if (version == null)
            version = wcsCapabilities.getVersion();
        // System.out.println(wcsCapabilities);
    }

    @Override
    public String getCapabilitiesUrl(){
        return WCSCapabilitiesReader.capabilities_url(url, version);
    }

    public WcsCapabilities getCapabilities() throws Exception {
        init();
        return wcsCapabilities;
    }

    public DescribeCoverage getDescribeCoverage(String coverageId) throws Exception {
        init();
        DescribeCoverageReader reader = new DescribeCoverageReader(version, coverageId, cookies, auth,
                timeout, headers);

        String describeCoverageUrl = wcsCapabilities.getOperationsMetadata().getDescribeCoverageUrl();
        if (describeCoverageUrl == null)
            describeCoverageUrl = baseUrl;

        DescribeCoverage dc = reader.read(describeCoverageUrl, timeout);
        return dc;
    }

    @Override
    public String getCoverage(String outputFilePath, CoverageReaderParameters parameters, HashMap<String, String> additonalParameters)
            throws Exception {
        init();
        String url = wcsCapabilities.getOperationsMetadata().getGetCoverageUrl();
        if (url == null)
            url = baseUrl;

        String paramsUrl = parameters.toUrl(additonalParameters);

        // String encparamsUrl = URLEncoder.encode(paramsUrl, "UTF-8");
        String finalUrl = url + "?" + paramsUrl;

        // make the request
        Builder uriBuilder = HttpRequest.newBuilder()
                .uri(URI.create(finalUrl));
        if (auth != null) {
            // Create an HTTP client with basic authentication
            String credentials = auth.username + ":" + auth.password;
            String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            uriBuilder.header("Authorization", "Basic " + base64Credentials);
        }

        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = uriBuilder.GET().build();

        try {
            // Send the request and retrieve the response
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                // Save the response to a file
                Path outputPath = Path.of(outputFilePath);
                Files.copy(response.body(), outputPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Response saved to " + outputFilePath);
            } else {
                InputStream responseStream = response.body();
                byte[] responseBytes = responseStream.readAllBytes();
                String responseString = new String(responseBytes);

                System.err.println("Request failed with status code: " + response.statusCode());
                System.err.println("Call done: " + finalUrl);
                System.err.println("Message: " + responseString);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return finalUrl;
    }


}
