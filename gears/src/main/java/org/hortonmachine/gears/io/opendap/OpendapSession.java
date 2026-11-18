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
package org.hortonmachine.gears.io.opendap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.NetworkUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * An http session for OPeNDAP servers, also those behind a login, as the NASA Earthdata ones.
 *
 * <p>The redirects are followed by the session and the cookies are kept, which is what the Earthdata login needs: the
 * data server redirects to the login server, which takes the credentials and redirects back with a session cookie.
 * The credentials are sent only to the login host, the token only to the server of the requested url.</p>
 *
 * <pre>
 * OpendapSession session = OpendapSession.fromNetrc("urs.earthdata.nasa.gov");
 * </pre>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class OpendapSession {

    /** The login host of NASA Earthdata. */
    public static final String EARTHDATA_LOGIN_HOST = "urs.earthdata.nasa.gov";

    private static final int MAX_REDIRECTS = 20;

    /** The waits before each retry of a request that failed with a server error or a network failure. */
    private static final int[] RETRY_WAITS_SECONDS = {5, 15, 30, 60};

    private final String authHost;
    private final String basicAuthorization;
    private final String token;
    private final HttpClient client;

    private OpendapSession( String authHost, String user, String password, String token ) {
        this.authHost = authHost == null ? null : authHost.toLowerCase(Locale.ROOT);
        if (user != null && password != null) {
            basicAuthorization = "Basic "
                    + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        } else {
            basicAuthorization = null;
        }
        this.token = token;
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(NetworkUtilities.DEFAULT_CONNECT_TIMEOUT)).build();
    }

    /**
     * @return a session without authentication, for public servers.
     */
    public static OpendapSession anonymous() {
        return new OpendapSession(null, null, null, null);
    }

    /**
     * @param authHost the host of the login server, as {@link #EARTHDATA_LOGIN_HOST}.
     * @param user the user.
     * @param password the password.
     * @return a session that sends the credentials to the login host.
     */
    public static OpendapSession withCredentials( String authHost, String user, String password ) {
        Objects.requireNonNull(authHost, "The login host is mandatory.");
        Objects.requireNonNull(user, "The user is mandatory.");
        Objects.requireNonNull(password, "The password is mandatory.");
        return new OpendapSession(authHost, user, password, null);
    }

    /**
     * @param token a bearer token, as an Earthdata user token.
     * @return a session that sends the token to the server of the requested urls, not to the ones it redirects to.
     */
    public static OpendapSession withToken( String token ) {
        Objects.requireNonNull(token, "The token is mandatory.");
        return new OpendapSession(null, null, null, token);
    }

    /**
     * Create a session with the credentials of the login host in the <code>.netrc</code> file of the user home.
     *
     * @param authHost the host of the login server, as {@link #EARTHDATA_LOGIN_HOST}.
     * @return the session.
     * @throws IOException if the file or the credentials of the host are missing.
     */
    public static OpendapSession fromNetrc( String authHost ) throws IOException {
        return fromNetrc(new File(System.getProperty("user.home"), ".netrc"), authHost);
    }

    /**
     * Create a session with the credentials of the login host in a netrc file, with lines as
     * <code>machine urs.earthdata.nasa.gov login USER password PASSWORD</code>.
     *
     * @param netrcFile the netrc file.
     * @param authHost the host of the login server.
     * @return the session.
     * @throws IOException if the file or the credentials of the host are missing.
     */
    public static OpendapSession fromNetrc( File netrcFile, String authHost ) throws IOException {
        if (!netrcFile.exists()) {
            throw new IOException("The netrc file doesn't exist: " + netrcFile);
        }
        String[] tokens = FileUtilities.readFile(netrcFile).trim().split("\\s+");
        String[] credentials = netrcCredentials(tokens, authHost);
        if (credentials == null) {
            // the default entry, if any
            credentials = netrcCredentials(tokens, null);
        }
        if (credentials == null || credentials[0] == null || credentials[1] == null) {
            throw new IOException("No login and password for " + authHost + " in " + netrcFile);
        }
        return new OpendapSession(authHost, credentials[0], credentials[1], null);
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

    /**
     * Read a url as text.
     *
     * @param url the url.
     * @return the text.
     * @throws IOException if the server answers with an error.
     */
    public String getText( String url ) throws IOException {
        HttpResponse<InputStream> response = send(url, null);
        try (InputStream is = response.body()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Download a url to a file, through a temporary file renamed only when complete.
     *
     * @param url the url.
     * @param outFile the file to write. Missing parent folders are created, an existing file is replaced.
     * @param pm the monitor, can be <code>null</code>. Its cancel state stops the download.
     * @return the size in bytes of the file.
     * @throws IOException if the server answers with an error or with a web page instead of data, as a login page.
     */
    public long download( String url, File outFile, IHMProgressMonitor pm ) throws IOException {
        if (pm == null) {
            pm = new DummyProgressMonitor();
        }
        HttpResponse<InputStream> response = send(url, pm);
        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        long length = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        long size;
        try (InputStream is = response.body()) {
            size = NetworkUtilities.copyToFile(is, length, outFile, pm);
        }
        if (contentType.startsWith("text/html") || startsWithMarkup(outFile)) {
            Files.deleteIfExists(outFile.toPath());
            throw new IOException("The server returned a web page instead of data, check the credentials: " + url);
        }
        return size;
    }

    private static boolean startsWithMarkup( File file ) throws IOException {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] start = is.readNBytes(64);
            return new String(start, StandardCharsets.ISO_8859_1).trim().startsWith("<");
        }
    }

    /**
     * Send a GET, following the redirects and retrying on server errors (5xx) and network failures, which on the
     * Earthdata servers are often transient (as a CMR lookup of the granule failing).
     */
    private HttpResponse<InputStream> send( String url, IHMProgressMonitor pm ) throws IOException {
        for( int attempt = 0;; attempt++ ) {
            try {
                return sendOnce(url);
            } catch (RetriableException e) {
                if (attempt >= RETRY_WAITS_SECONDS.length) {
                    throw new IOException(e.getMessage() + " (failed after " + (attempt + 1) + " attempts)", e.getCause());
                }
                int wait = RETRY_WAITS_SECONDS[attempt];
                if (pm != null) {
                    pm.message(e.shortMessage + ", retrying in " + wait + " s...");
                }
                try {
                    Thread.sleep(wait * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting to retry " + url, ie);
                }
            }
        }
    }

    private static class RetriableException extends IOException {
        private static final long serialVersionUID = 1L;
        final String shortMessage;
        RetriableException( String shortMessage, String message, Throwable cause ) {
            super(message, cause);
            this.shortMessage = shortMessage;
        }
    }

    /**
     * Send a GET once, following the redirects from the original url.
     */
    private HttpResponse<InputStream> sendOnce( String url ) throws IOException {
        URI original = URI.create(url);
        URI current = original;
        for( int i = 0; i <= MAX_REDIRECTS; i++ ) {
            HttpRequest.Builder request = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofMillis(NetworkUtilities.DEFAULT_READ_TIMEOUT * 10L))
                    .header("User-Agent", NetworkUtilities.USER_AGENT).GET();
            String host = current.getHost() == null ? "" : current.getHost().toLowerCase(Locale.ROOT);
            if (basicAuthorization != null && host.equals(authHost)) {
                request.header("Authorization", basicAuthorization);
            } else if (token != null && sameServer(original, current)) {
                request.header("Authorization", "Bearer " + token);
            }
            HttpResponse<InputStream> response;
            try {
                response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while connecting to " + current, e);
            } catch (IOException e) {
                throw new RetriableException("Connection to " + current.getHost() + " failed",
                        "Connection to " + current + " failed: " + e.getMessage(), e);
            }
            int status = response.statusCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = response.headers().firstValue("Location").orElse(null);
                response.body().close();
                if (location == null) {
                    throw new IOException("Redirect without location from " + current);
                }
                current = current.resolve(location);
                continue;
            }
            if (status != 200) {
                String message;
                try (InputStream is = response.body()) {
                    message = new String(is.readNBytes(2000), StandardCharsets.UTF_8);
                }
                message = message.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                // the Hyrax error pages end with a long support mail template
                int supportIndex = message.indexOf("If you think that the server is broken");
                if (supportIndex > 0) {
                    message = message.substring(0, supportIndex).trim();
                }
                String hint = status == 401 || status == 403 ? " (check the credentials and that the data provider is authorized for the user)" : "";
                String fullMessage = "Error " + status + hint + " from " + current + ": " + message;
                if (status >= 500) {
                    throw new RetriableException("Server error " + status + " from " + current.getHost(), fullMessage, null);
                }
                throw new IOException(fullMessage);
            }
            return response;
        }
        throw new IOException("Too many redirects for " + url);
    }

    private static boolean sameServer( URI a, URI b ) {
        return Objects.equals(a.getScheme(), b.getScheme()) && Objects.equals(a.getHost(), b.getHost())
                && a.getPort() == b.getPort();
    }
}
