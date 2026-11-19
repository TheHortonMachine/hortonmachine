/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * Network related utilities, as the download of files.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NetworkUtilities {
    /** The user agent sent with the requests. */
    public static final String USER_AGENT = "HortonMachine";
    /** Default connect timeout in milliseconds. */
    public static final int DEFAULT_CONNECT_TIMEOUT = 30_000;
    /** Default read timeout in milliseconds. */
    public static final int DEFAULT_READ_TIMEOUT = 120_000;

    private static final int MAX_REDIRECTS = 10;
    private static final int BUFFER_SIZE = 128 * 1024;

    private NetworkUtilities() {
    }

    /**
     * Download a url to a file.
     *
     * @see #downloadFile(String, File, Map, IHMProgressMonitor)
     */
    public static long downloadFile( String url, File outFile, IHMProgressMonitor pm ) throws IOException {
        return downloadFile(url, outFile, null, pm);
    }

    /**
     * Download a url to a file.
     *
     * <p>
     * The download is written to a temporary file next to the output file and renamed only when complete,
     * so that an interrupted or canceled download never leaves a truncated file behind. An existing output
     * file is replaced. Missing parent folders are created.
     * </p>
     * <p>
     * Redirects are followed, also across protocols (http to https). The headers are sent to the original
     * server (protocol, host and port) only and are dropped once a redirect points elsewhere, so that
     * credentials are not leaked to a third party (for example a pre-signed object storage url).
     * </p>
     *
     * @param url the http(s) url to download.
     * @param outFile the file to write to.
     * @param headers optional additional request headers, or <code>null</code>.
     * @param pm the progress monitor, or <code>null</code>. Its cancel state stops the download.
     * @return the size in bytes of the downloaded file.
     * @throws IOException if the server answers with an error status, the download fails or is canceled.
     */
    public static long downloadFile( String url, File outFile, Map<String, String> headers, IHMProgressMonitor pm )
            throws IOException {
        Objects.requireNonNull(url, "The url is mandatory.");
        Objects.requireNonNull(outFile, "The output file is mandatory.");
        if (pm == null) {
            pm = new DummyProgressMonitor();
        }
        if (headers == null) {
            headers = Collections.emptyMap();
        }

        URLConnection connection = openConnection(url, headers);
        pm.beginTask("Downloading " + url, IHMProgressMonitor.UNKNOWN);
        try (InputStream is = connection.getInputStream()) {
            return copyToFile(is, connection.getContentLengthLong(), outFile, pm);
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
            pm.done();
        }
    }

    /**
     * Copy a stream to a file through a temporary file, reporting the progress to the monitor.
     *
     * <p>The stream is not closed, the monitor is neither begun nor done.</p>
     *
     * @param is the stream to copy.
     * @param contentLength the length of the content if known, else a value &lt;= 0. Used for the progress messages.
     * @param outFile the file to write to. An existing file is replaced, missing parent folders are created.
     * @param pm the progress monitor, or <code>null</code>. Its cancel state stops the copy.
     * @return the number of bytes written.
     * @throws IOException if the copy fails or is canceled.
     */
    public static long copyToFile( InputStream is, long contentLength, File outFile, IHMProgressMonitor pm )
            throws IOException {
        Path target = outFile.toPath().toAbsolutePath();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path part = target.resolveSibling(target.getFileName() + ".part");
        boolean done = false;
        try {
            long written;
            try (InputStream in = new BufferedInputStream(is, BUFFER_SIZE);
                    OutputStream os = new BufferedOutputStream(Files.newOutputStream(part), BUFFER_SIZE)) {
                written = copyWithProgress(in, os, contentLength, pm);
            }
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
            done = true;
            return written;
        } finally {
            if (!done) {
                Files.deleteIfExists(part);
            }
        }
    }

    /**
     * Copy a stream, reporting the progress every 10% (if the content length is known) and stopping
     * if the monitor is canceled.
     *
     * @param is the stream to read.
     * @param os the stream to write.
     * @param contentLength the length of the content if known, else a value &lt;= 0.
     * @param pm the progress monitor, or <code>null</code>.
     * @return the number of bytes copied.
     * @throws IOException if the copy fails or is canceled.
     */
    public static long copyWithProgress( InputStream is, OutputStream os, long contentLength, IHMProgressMonitor pm )
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long totalRead = 0;
        int read;
        int lastPercent = -1;
        while( (read = is.read(buffer)) != -1 ) {
            os.write(buffer, 0, read);
            totalRead += read;
            if (pm != null) {
                if (contentLength > 0) {
                    int percent = (int) (100L * totalRead / contentLength);
                    if (percent != lastPercent && percent % 10 == 0) {
                        pm.message("Downloaded: " + percent + "%...");
                        lastPercent = percent;
                    }
                }
                if (pm.isCanceled()) {
                    throw new IOException("Download canceled by user");
                }
            }
        }
        return totalRead;
    }

    /**
     * Open a connection following the redirects, checking the response status.
     */
    private static URLConnection openConnection( String url, Map<String, String> headers ) throws IOException {
        URL originalUrl = URI.create(url).toURL();
        String current = url;
        for( int i = 0; i <= MAX_REDIRECTS; i++ ) {
            URL u = URI.create(current).toURL();
            URLConnection connection = u.openConnection();
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (sameServer(originalUrl, u)) {
                headers.forEach(connection::setRequestProperty);
            }
            if (!(connection instanceof HttpURLConnection)) {
                return connection;
            }
            HttpURLConnection http = (HttpURLConnection) connection;
            // redirects are followed here, since HttpURLConnection does not follow them across protocols
            http.setInstanceFollowRedirects(false);
            int status = http.getResponseCode();
            if (status >= 300 && status < 400 && status != 304) {
                String location = http.getHeaderField("Location");
                http.disconnect();
                if (location == null) {
                    throw new IOException("Redirect (HTTP " + status + ") without location for: " + current);
                }
                current = URI.create(current).resolve(location).toString();
                continue;
            }
            if (status >= 400) {
                String message = readErrorBody(http);
                http.disconnect();
                throw new IOException("Unable to download " + current + " (HTTP " + status + ")"
                        + (message.isEmpty() ? "" : ": " + message));
            }
            return http;
        }
        throw new IOException("Too many redirects for: " + url);
    }

    /**
     * @return <code>true</code> if the urls point to the same protocol, host and port.
     */
    private static boolean sameServer( URL u1, URL u2 ) {
        int p1 = u1.getPort() != -1 ? u1.getPort() : u1.getDefaultPort();
        int p2 = u2.getPort() != -1 ? u2.getPort() : u2.getDefaultPort();
        return u1.getProtocol().equalsIgnoreCase(u2.getProtocol()) && p1 == p2
                && Objects.equals(u1.getHost(), u2.getHost());
    }

    private static String readErrorBody( HttpURLConnection http ) {
        try (InputStream es = http.getErrorStream()) {
            if (es == null) {
                return "";
            }
            String body = new String(es.readNBytes(500), StandardCharsets.UTF_8).trim();
            return body;
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Get the credentials of a host from the <code>.netrc</code> file of the user home.
     *
     * @see #getNetrcCredentials(File, String)
     */
    public static String[] getNetrcCredentials( String host ) throws IOException {
        return getNetrcCredentials(new File(System.getProperty("user.home"), ".netrc"), host);
    }

    /**
     * Get the credentials of a host from a netrc file, with lines as
     * <code>machine HOST login USER password PASSWORD</code>.
     *
     * <p>If the host has no entry, the <code>default</code> entry is used, if any.</p>
     *
     * @param netrcFile the netrc file.
     * @param host the host.
     * @return the login and password.
     * @throws IOException if the file or the credentials of the host are missing.
     */
    public static String[] getNetrcCredentials( File netrcFile, String host ) throws IOException {
        if (!netrcFile.exists()) {
            throw new IOException("The netrc file doesn't exist: " + netrcFile);
        }
        String[] tokens = FileUtilities.readFile(netrcFile).trim().split("\\s+");
        String[] credentials = netrcCredentials(tokens, host);
        if (credentials == null) {
            // the default entry, if any
            credentials = netrcCredentials(tokens, null);
        }
        if (credentials == null || credentials[0] == null || credentials[1] == null) {
            throw new IOException("No login and password for " + host + " in " + netrcFile);
        }
        return credentials;
    }

    /**
     * @return the login and password of the entry of the host, or of the default entry if the host is
     *          <code>null</code>; <code>null</code> if there is no such entry.
     */
    private static String[] netrcCredentials( String[] tokens, String host ) {
        for( int i = 0; i < tokens.length; i++ ) {
            int entryStart;
            if (host != null && tokens[i].equals("machine") && i + 1 < tokens.length && tokens[i + 1].equalsIgnoreCase(host)) {
                entryStart = i + 2;
            } else if (host == null && tokens[i].equals("default")) {
                entryStart = i + 1;
            } else {
                continue;
            }
            String[] credentials = new String[2];
            for( int j = entryStart; j < tokens.length - 1; j++ ) {
                if (tokens[j].equals("machine") || tokens[j].equals("default")) {
                    break;
                }
                if (tokens[j].equals("login")) {
                    credentials[0] = tokens[++j];
                } else if (tokens[j].equals("password")) {
                    credentials[1] = tokens[++j];
                }
            }
            return credentials;
        }
        return null;
    }

}
