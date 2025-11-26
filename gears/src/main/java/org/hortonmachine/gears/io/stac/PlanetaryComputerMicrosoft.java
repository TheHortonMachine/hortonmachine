package org.hortonmachine.gears.io.stac;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlanetaryComputerMicrosoft {
    /**
     * Planetary Computer assets are usually stored as Azure blobs.
     * @param href
     * @return
     */
    public static boolean isAzureBlob(String href) {
        return href.contains("blob.core.windows.net");
    }

    /**
     * A token is needed to access Planetary Computer assets under Azure blob.
     * This method gets an Azure blob href and returns a curated href for the same resource.
     * https://planetarycomputer.microsoft.com/docs/concepts/sas/
     * @param href
     * @return href with token
     * @throws IOException
     * @throws InterruptedException
     */
    public static String getHrefWithToken(String href) throws IOException, InterruptedException {
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
        return body.getString("href");
    }
}
