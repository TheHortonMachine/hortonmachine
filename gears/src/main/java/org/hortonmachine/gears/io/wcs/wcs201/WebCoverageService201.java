package org.hortonmachine.gears.io.wcs.wcs201;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.io.wcs.Authentication;
import org.hortonmachine.gears.io.wcs.ICoverageSummary;
import org.hortonmachine.gears.io.wcs.IDescribeCoverage;
import org.hortonmachine.gears.io.wcs.IWebCoverageService;
import org.hortonmachine.gears.io.wcs.WcsUtils;
import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.hortonmachine.gears.io.wcs.readers.DescribeCoverageReader;
import org.hortonmachine.gears.io.wcs.readers.WCSCapabilitiesReader;
import org.hortonmachine.gears.io.wcs.wcs201.models.WcsCapabilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

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

    public WebCoverageService201(String url, String version, String xml, String cookies, int timeout,
            Authentication auth,
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
    public List<String> getCoverageIds() throws Exception {
        init();
        return wcsCapabilities.getCoverageIds();
    }

    @Override
    public ICoverageSummary getCoverageSummary(String coverageId) throws Exception {
        init();
        return wcsCapabilities.getCoverageSummaryById(coverageId);
    }

    @Override
    public List<String> getSupportedFormats() throws Exception {
        init();
        return wcsCapabilities.getServiceMetadata().getSupportedFormats();
    }

    @Override
    public int[] getSupportedSrids() throws Exception {
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
    public String getCapabilitiesUrl() {
        return WCSCapabilitiesReader.capabilities_url(url, version);
    }

    public WcsCapabilities getCapabilities() throws Exception {
        init();
        return wcsCapabilities;
    }

    @Override
    public String getDescribeCoverageUrl(String coverageId) throws Exception {
        init();
        DescribeCoverageReader reader = new DescribeCoverageReader(version, coverageId, cookies, auth,
                timeout, headers);
        
        String describeCoverageBaseUrl = wcsCapabilities.getOperationsMetadata().getDescribeCoverageBaseUrl();
        if (describeCoverageBaseUrl == null)
            describeCoverageBaseUrl = baseUrl;

        return reader.descCov_url(describeCoverageBaseUrl);
    }

    @Override
    public IDescribeCoverage getDescribeCoverage(String coverageId) throws Exception {
        init();
        IDescribeCoverage dc;
        try {
            DescribeCoverageReader reader = new DescribeCoverageReader(version, coverageId, cookies, auth,
                    timeout, headers);

            String describeCoverageUrl = wcsCapabilities.getOperationsMetadata().getDescribeCoverageBaseUrl();
            if (describeCoverageUrl == null)
                describeCoverageUrl = baseUrl;

            dc = reader.read(describeCoverageUrl, timeout);
            return dc;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public GridCoverage2D getCoverage(CoverageReaderParameters parameters,
            HashMap<String, String> additonalParameters)
            throws Exception {
        // create a tmp file for the coverage
        Path tempFile = Files.createTempFile("wcs", ".tif");
        String outputFilePath = tempFile.toString();

        // force tiff format
        parameters.format("image/tiff");

        getCoverage(outputFilePath, parameters, additonalParameters);

        return OmsRasterReader.readRaster(outputFilePath);
    }

    @Override
    public String getCoverage(String outputFilePath, CoverageReaderParameters parameters,
            HashMap<String, String> additonalParameters)
            throws Exception {
        init();
        String url = wcsCapabilities.getOperationsMetadata().getGetCoverageBaseUrl();
        if (url == null)
            url = baseUrl;

        String paramsUrl = parameters.toUrl(additonalParameters);
        String sep = "?";
        if (url.indexOf('?') != -1)
            sep = "&";
        String finalUrl = url + sep + paramsUrl;

        WcsUtils.requestToFile(finalUrl, outputFilePath);

        // // make the request
        // Builder uriBuilder = HttpRequest.newBuilder()
        //         .uri(URI.create(finalUrl));
        // if (auth != null) {
        //     // Create an HTTP client with basic authentication
        //     String credentials = auth.username + ":" + auth.password;
        //     String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        //     uriBuilder.header("Authorization", "Basic " + base64Credentials);
        // }
        // HttpClient client = HttpClient.newBuilder().build();
        // HttpRequest request = uriBuilder.GET().build();
        // // Send the request and retrieve the response
        // HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // if (response.statusCode() == 200) {
        //     // Save the response to a file
        //     Path outputPath = Path.of(outputFilePath);
        //     Files.copy(response.body(), outputPath, StandardCopyOption.REPLACE_EXISTING);
        // } else {
        //     InputStream responseStream = response.body();
        //     byte[] responseBytes = responseStream.readAllBytes();
        //     String responseString = new String(responseBytes);

        //     throw new Exception("Error while retrieving coverage" + responseString + " \nwith URL:" + finalUrl);
        // }

        return finalUrl;
    }

    @Override
    public void dumpCoverageFootprints(String outFolder) throws Exception {
        File outFolderFile = new File(outFolder);
        if (!outFolderFile.exists())
            outFolderFile.mkdirs();
        String gpkgName = "wcs_footprints.gpkg";

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        SimpleFeatureTypeBuilder b = null;
        SimpleFeatureBuilder builder = null;
        String name = "wgs84footprints_coveragesummary";
        for (String id : getCoverageIds()) {
            ICoverageSummary coverageSummary = getCoverageSummary(id);
            ReferencedEnvelope wgs84BoundingBox = coverageSummary.getWgs84BoundingBox();
            if (b == null || builder == null) {
                b = new SimpleFeatureTypeBuilder();
                b.setName(name);
                b.setCRS(wgs84BoundingBox.getCoordinateReferenceSystem());
                b.add("the_geom", Polygon.class);
                b.add("coverageid", String.class);
                SimpleFeatureType type = b.buildFeatureType();
                builder = new SimpleFeatureBuilder(type);
            }

            Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(wgs84BoundingBox);
            Object[] values = new Object[] { polygon, id };
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            fc.add(feature);
        }
        File gpkgFile = new File(outFolderFile, gpkgName);
        OmsVectorWriter.writeVector(gpkgFile.getAbsolutePath() + "#" + name, fc);

        fc = new DefaultFeatureCollection();
        b = null;
        builder = null;
        name = "data_footprints_coveragesummary";
        for (String id : getCoverageIds()) {
            ICoverageSummary coverageSummary = getCoverageSummary(id);
            Envelope boundingBox = coverageSummary.getBoundingBox();
            if (b == null || builder == null) {
                b = new SimpleFeatureTypeBuilder();
                b.setName(name);
                b.setCRS(CRS.decode("EPSG:" + coverageSummary.getBoundingBoxSrid()));
                b.add("the_geom", Polygon.class);
                b.add("coverageid", String.class);
                SimpleFeatureType type = b.buildFeatureType();
                builder = new SimpleFeatureBuilder(type);
            }

            Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(boundingBox);
            Object[] values = new Object[] { polygon, id };
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            fc.add(feature);
        }
        gpkgFile = new File(outFolderFile, gpkgName);
        OmsVectorWriter.writeVector(gpkgFile.getAbsolutePath() + "#" + name, fc);

    }

}
