package org.hortonmachine.gears.io.wcs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

// import org.apache.http.HttpResponse;
// import org.apache.http.client.HttpClient;
// import org.apache.http.client.methods.HttpGet;
// import org.apache.http.impl.client.HttpClientBuilder;
import org.hortonmachine.gears.io.wcs.readers.InsecureTrustManager;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * Utils methods for the WCS handlers.
 */
public class WcsUtils {

    /**
     * Returns the SRID (Spatial Reference ID) from a given SRS wcs url, or null.
     * 
     * <p>
     * Example urls handled are:
     * <ul>
     * <li>http://www.opengis.net/def/crs/EPSG/0/25832</li>
     * <li>http://www.opengis.net/def/crs/EPSG/0/EPSG:25832</li>
     * </ul>
     * </p>
     * </br>
     * 
     * @param srsName the SRS name to extract the SRID from
     * @return the SRID as an Integer, or null if the SRID cannot be extracted from
     *         the SRS name
     */
    public static Integer getSridFromSrsName(String srsName) {
        int index = srsName.lastIndexOf("EPSG:");
        String sridStr = null;
        if (srsName != null && index != -1) {
            sridStr = srsName.substring(index + 5);
        } else if (srsName != null && srsName.contains("EPSG")) {
            index = srsName.lastIndexOf("/");
            if (index != -1) {
                sridStr = srsName.substring(index + 1);
            }
        }
        if (sridStr != null) {
            int srid = Integer.parseInt(sridStr);
            return srid;
        }
        return null;
    }

    public static CoordinateReferenceSystem getCrsFromSrsName(String srsName) {
        Integer srid = getSridFromSrsName(srsName);
        if (srid != null)
            return CrsUtilities.getCrsFromSrid(srid);
        return null;
    }

    public static String nsWCS2(String tag) {
        return "{http://www.opengis.net/wcs/2.0}" + tag;
    }

    public static String nsCRS_WCS2(int epsgSrid) {
        return "http://www.opengis.net/def/crs/EPSG/0/" + epsgSrid;
    }

    public static String nsGRIDAXIS_WCS2(String axisName) {
        return "http://www.opengis.net/def/axis/OGC/1/" + axisName;
    }
    
    /**
     * Orders the given axis labels by putting the "long", "lon", "x" axis label
     * first and the "lat", "y" axis label second.
     * 
     * @param axisLabels the axis labels to order
     * @return the ordered axis labels
     * @throws IllegalArgumentException if the "long", "lon", "x" or "lat", "y" axis
     *                                  labels are not found in the given axis
     *                                  labels
     */
    public static String[] orderLabels(String[] axisLabels) {
        String[] ordered = new String[2];

        int[] lonLatPositions = getLonLatPositions(axisLabels);
        ordered[0] = axisLabels[lonLatPositions[0]];
        ordered[1] = axisLabels[lonLatPositions[1]];

        if (ordered[0] == null || ordered[1] == null) {
            throw new IllegalArgumentException("Could not find lat/lon or x/y axis labels in coverage description");
        }
        return ordered;
    }

    public static int[] getLonLatPositions(String[] axisLabels) {
        int[] positions = new int[2];
        List<String> possibleEastingLCLabels = getPossibleEastingLCLabels();
        List<String> possibleNorthingLCLabels = getPossibleNorthingLCLabels();
        boolean foundEasting = false;
        boolean foundNorthing = false;
        for (int i = 0; i < axisLabels.length; i++) {
            String label = axisLabels[i].toLowerCase();
            if (possibleEastingLCLabels.contains(label)) {
                positions[0] = i;
                foundEasting = true;
            } else if (possibleNorthingLCLabels.contains(label)) {
                positions[1] = i;
                foundNorthing = true;
            }
        }
        if (!foundEasting || !foundNorthing) {
            throw new IllegalArgumentException("Could not find lat/lon or x/y axis labels in coverage description");
        }
        return positions;
    }

    private static List<String> getPossibleEastingLCLabels() {
        List<String> labels = Arrays.asList("long", "lon", "x", "e", "xaxis", "x-axis", "x_axis", "long_axis",
                "long-axis", "lon_axis", "lon-axis", "easting", "easting_axis", "easting-axis", "e_axis");
        return labels;
    }

    private static List<String> getPossibleNorthingLCLabels() {
        List<String> labels = Arrays.asList("lat", "y", "n", "yaxis", "y-axis", "y_axis", "lat_axis", "lat-axis",
                "lat_axis", "northing", "northing_axis", "northing-axis", "n_axis");
        return labels;
    }

    public static XmlHelper getXmlHelperForRequest(String urlString) throws Exception {
        XmlHelper xmlHelper = null;
        try {
            // HttpClient httpClient = HttpClientBuilder.create().build();
            // HttpGet getCapabilitiesRequest = new HttpGet(request);
            // HttpResponse response = httpClient.execute(getCapabilitiesRequest);
            // xmlHelper = XmlHelper.fromStream(response.getEntity().getContent());

            Builder uriBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(urlString));
            // if (auth != null) {
            // // Create an HTTP client with basic authentication
            // String credentials = auth.username + ":" + auth.password;
            // String base64Credentials =
            // Base64.getEncoder().encodeToString(credentials.getBytes());
            // uriBuilder.header("Authorization", "Basic " + base64Credentials);
            // }
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = uriBuilder.GET().build();
            // Send the request and retrieve the response
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status >= 400) {
                String error = "Unable to get data form: " + urlString;
                throw new IOException(error);
            }
            xmlHelper = XmlHelper.fromStream(response.body());

        } catch (SSLHandshakeException e) {
            // try to do it ignoring security
            HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
            SSLSocketFactory defaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
            try {
                TrustManager[] trustManagers = { new InsecureTrustManager() };
                URL url = new URL(urlString);
                // Create a custom HostnameVerifier that bypasses hostname validation
                HostnameVerifier allowAllHostnames = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };

                // Set the custom trust manager and hostname verifier
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustManagers, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(allowAllHostnames);

                // Open the connection
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                try {
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        // Get the input stream
                        InputStream inputStream = connection.getInputStream();
                        xmlHelper = XmlHelper.fromStream(inputStream);
                    } else {
                        throw new Exception("HTTP response code: " + responseCode);
                    }
                } finally {
                    connection.disconnect();
                }
            } finally {
                // Reset the default trust manager and hostname verifier
                HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSocketFactory);
                HttpsURLConnection.setDefaultHostnameVerifier(defaultHostnameVerifier);
            }
        }
        return xmlHelper;
    }

    public static void requestToFile(String urlString, String outputFilePath) throws Exception {
        try {

            Builder uriBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(urlString));
            // if (auth != null) {
            // // Create an HTTP client with basic authentication
            // String credentials = auth.username + ":" + auth.password;
            // String base64Credentials =
            // Base64.getEncoder().encodeToString(credentials.getBytes());
            // uriBuilder.header("Authorization", "Basic " + base64Credentials);
            // }
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = uriBuilder.GET().build();
            // Send the request and retrieve the response
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // HttpClient httpClient = HttpClientBuilder.create().build();
            // HttpGet getCapabilitiesRequest = new HttpGet(request);
            // HttpResponse response = httpClient.execute(getCapabilitiesRequest);
            // InputStream stream = response.getEntity().getContent();
            if (response.statusCode() == 200) {
                // Save the response to a file
                Path outputPath = Path.of(outputFilePath);
                Files.copy(response.body(), outputPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                InputStream responseStream = response.body();
                byte[] responseBytes = responseStream.readAllBytes();
                String responseString = new String(responseBytes);
                throw new Exception("Error while retrieving coverage" + responseString + " \nwith URL:" + urlString);
            }
        } catch (SSLHandshakeException e) {
            // try to do it ignoring security
            HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
            SSLSocketFactory defaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
            try {
                TrustManager[] trustManagers = { new InsecureTrustManager() };
                URL url = new URL(urlString);
                // Create a custom HostnameVerifier that bypasses hostname validation
                HostnameVerifier allowAllHostnames = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };

                // Set the custom trust manager and hostname verifier
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustManagers, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(allowAllHostnames);

                // Open the connection
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                try {
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        // Get the input stream
                        InputStream inputStream = connection.getInputStream();
                        // Save the response to a file
                        Path outputPath = Path.of(outputFilePath);
                        Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        InputStream responseStream = connection.getInputStream();
                        byte[] responseBytes = responseStream.readAllBytes();
                        String responseString = new String(responseBytes);
                        throw new Exception(
                                "Error while retrieving coverage" + responseString + " \nwith URL:" + urlString);
                    }
                } finally {
                    connection.disconnect();
                }
            } finally {
                // Reset the default trust manager and hostname verifier
                HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSocketFactory);
                HttpsURLConnection.setDefaultHostnameVerifier(defaultHostnameVerifier);
            }
        }
    }
}
