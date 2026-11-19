package org.hortonmachine.gears.io.stac;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

public class PlanetaryComputerMicrosoft {
    /**
     * Planetary Computer assets are usually stored as Azure blobs.
     * @param href
     * @return
     */
    public static boolean isAzureBlob(String href) {
        return href.contains("blob.core.windows.net");
    }

    /** The tokens are reused until this long before their expiry. */
    private static final long EXPIRY_MARGIN_MILLIS = 5 * 60_000L;
    /** The validity assumed for a token whose expiry is not given. */
    private static final long DEFAULT_VALIDITY_MILLIS = 20 * 60_000L;

    /** The access tokens (query of the signed href and expiry millis) by storage container. */
    private static final Map<String, Object[]> TOKENS = new ConcurrentHashMap<>();

    /**
     * A token is needed to access Planetary Computer assets under Azure blob.
     * This method gets an Azure blob href and returns a curated href for the same resource.
     * https://planetarycomputer.microsoft.com/docs/concepts/sas/
     *
     * <p>A token is valid for all the blobs of a storage container: it is asked once and reused for
     * the other blobs of the container until a few minutes before it expires, not to hit the rate
     * limits of the signing service when many assets are read.</p>
     *
     * @param href
     * @return href with token
     * @throws IOException
     * @throws InterruptedException
     */
    public static String getHrefWithToken(String href) throws IOException, InterruptedException {
        String container = getContainer(href);
        Object[] token = container != null ? TOKENS.get(container) : null;
        if (token != null && System.currentTimeMillis() < (long) token[1] - EXPIRY_MARGIN_MILLIS) {
            return href + token[0];
        }

        String getReadAccess = "https://planetarycomputer.microsoft.com/api/sas/v1/sign?href=" + href;
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest.Builder uriBuilder = HttpRequest.newBuilder()
                .uri(URI.create(getReadAccess));
        HttpRequest request = uriBuilder.GET().build();
        // Send the request and retrieve the response
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        int status = response.statusCode();
        if (status != 200) {
            throw new IOException(new String(response.body().readAllBytes()));
        }
        JSONObject body = new JSONObject(new String(response.body().readAllBytes()));
        String signedHref = body.getString("href");
        int queryIndex = signedHref.indexOf('?');
        if (container != null && queryIndex > 0 && signedHref.substring(0, queryIndex).equals(href)) {
            TOKENS.put(container, new Object[]{signedHref.substring(queryIndex), getExpiryMillis(body)});
        }
        return signedHref;
    }

    /**
     * @param href an Azure blob href.
     * @return the storage container of the blob (scheme, account host and container), or <code>null</code>
     *         if the href has a query or no container.
     */
    static String getContainer( String href ) {
        if (href.contains("?")) {
            return null;
        }
        String[] parts = href.split("/", 5);
        // https:, "", account host, container, blob path
        if (parts.length < 5 || parts[3].isEmpty()) {
            return null;
        }
        return parts[0] + "//" + parts[2] + "/" + parts[3];
    }

    private static long getExpiryMillis( JSONObject body ) {
        String expiry = body.optString("msft:expiry", null);
        if (expiry != null) {
            try {
                return OffsetDateTime.parse(expiry.endsWith("Z") || expiry.matches(".*[+-]\\d\\d:\\d\\d$") ? expiry : expiry + "Z")
                        .toInstant().toEpochMilli();
            } catch (DateTimeParseException e) {
                // fall back to the default validity
            }
        }
        return System.currentTimeMillis() + DEFAULT_VALIDITY_MILLIS;
    }
}
