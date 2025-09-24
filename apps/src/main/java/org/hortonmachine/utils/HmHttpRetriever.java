package org.hortonmachine.utils;

import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.URLRetriever;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class HmHttpRetriever extends URLRetriever {
    private int responseCode;
    private String responseMessage;

    public HmHttpRetriever(URL url, RetrievalPostProcessor postProcessor) {
        super(url, postProcessor);
    }

    @Override
    protected URLConnection openConnection() throws IOException {
        // Let the base class handle proxy, SSL, timeouts, etc.
        URLConnection conn = super.openConnection();

        // Add your User-Agent (and any other headers) here:
        if (conn instanceof HttpURLConnection) {
            conn.setRequestProperty("User-Agent", "HortonMachine-NWW/1.0 (info@hortonmachine.org)");
            // Optionally:
            // conn.setRequestProperty("Referer", "https://your.app/");
            // conn.setRequestProperty("From", "contact@example.com");
        }
        return conn;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getResponseMessage() {
        return this.responseMessage;
    }

    @Override
    protected java.nio.ByteBuffer doRead(URLConnection connection) throws Exception {
        if (connection == null) {
            String msg = gov.nasa.worldwind.util.Logging.getMessage("nullValue.ConnectionIsNull");
            gov.nasa.worldwind.util.Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        HttpURLConnection htpc = (HttpURLConnection) connection;
        this.responseCode = htpc.getResponseCode();
        this.responseMessage = htpc.getResponseMessage();
        String contentType = connection.getContentType();

        gov.nasa.worldwind.util.Logging.logger().log(
            java.util.logging.Level.FINE, "HTTPRetriever.ResponseInfo",
            new Object[]{ this.responseCode, connection.getContentLength(),
                          contentType != null ? contentType : "content type not returned",
                          connection.getURL() });

        if (this.responseCode == HttpURLConnection.HTTP_OK)
            return super.doRead(connection);

        return null;
    }
}
