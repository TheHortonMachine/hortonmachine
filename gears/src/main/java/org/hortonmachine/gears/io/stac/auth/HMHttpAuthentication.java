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
package org.hortonmachine.gears.io.stac.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The authentication of the catalogs and assets served over http(s): a token, a user and password or an api key,
 * sent with every request to the addresses of a server.
 *
 * <p>It applies only to the addresses that start with its base url, so that the credentials are never sent to other
 * servers, as those of the assets of a catalog hosted elsewhere:
 * <ul>
 * <li>{@link #bearer(String, String)}: <code>Authorization: Bearer &lt;token&gt;</code>, as the HortonMachine server,
 *     OAuth2 and OpenID Connect protected catalogs;</li>
 * <li>{@link #basic(String, String, String)}: <code>Authorization: Basic ...</code>, HTTP basic authentication;</li>
 * <li>{@link #header(String, String, String)}: any header, as the api keys <code>X-API-Key: &lt;key&gt;</code>;</li>
 * <li>{@link #queryParameter(String, String, String)}: a parameter added to the addresses, as
 *     <code>?api_key=&lt;key&gt;</code>, for the servers that want the key in the url.</li>
 * </ul>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class HMHttpAuthentication implements IHMStacAuthentication {
    private static final int CONNECT_TIMEOUT = 30_000;
    private static final int READ_TIMEOUT = 60_000;
    /** The characters of an error answer kept in the message of the exception. */
    private static final int MAX_ERROR_CHARS = 500;

    private final String baseUrl;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private String parameterName;
    private String parameterValue;
    private final String description;

    private HMHttpAuthentication( String baseUrl, String description ) {
        if (baseUrl == null || !(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            throw new IllegalArgumentException("The base url must be an http(s) address: " + baseUrl);
        }
        // without the path of the catalog: the assets can be anywhere on the server
        URI uri = URI.create(baseUrl.trim());
        this.baseUrl = uri.getScheme() + "://" + uri.getRawAuthority();
        this.description = description;
    }

    /**
     * @param baseUrl an address of the server, as the url of its catalog: the credentials are sent to all its
     *          addresses (same scheme, host and port), never to others.
     * @param token the token.
     * @return the authentication with <code>Authorization: Bearer &lt;token&gt;</code>.
     */
    public static HMHttpAuthentication bearer( String baseUrl, String token ) {
        checkValue("token", token);
        HMHttpAuthentication authentication = new HMHttpAuthentication(baseUrl, "Bearer token");
        authentication.headers.put("Authorization", "Bearer " + token.trim());
        return authentication;
    }

    /**
     * @param baseUrl an address of the server, see {@link #bearer(String, String)}.
     * @return the authentication with the HTTP basic authentication of the user.
     */
    public static HMHttpAuthentication basic( String baseUrl, String user, String password ) {
        checkValue("user", user);
        HMHttpAuthentication authentication = new HMHttpAuthentication(baseUrl, "Basic, user " + user);
        String credentials = user + ":" + (password == null ? "" : password);
        authentication.headers.put("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        return authentication;
    }

    /**
     * @param baseUrl an address of the server, see {@link #bearer(String, String)}.
     * @param name the name of the header, as X-API-Key.
     * @param value its value.
     * @return the authentication with the header.
     */
    public static HMHttpAuthentication header( String baseUrl, String name, String value ) {
        checkValue("header name", name);
        checkValue("header value", value);
        if (!name.trim().matches("[A-Za-z0-9!#$%&'*+.^_`|~-]+")) {
            throw new IllegalArgumentException("Not a valid header name: " + name);
        }
        HMHttpAuthentication authentication = new HMHttpAuthentication(baseUrl, "Header " + name.trim());
        authentication.headers.put(name.trim(), value.trim());
        return authentication;
    }

    /**
     * @param baseUrl an address of the server, see {@link #bearer(String, String)}.
     * @param name the name of the parameter, as api_key.
     * @param value its value.
     * @return the authentication that adds the parameter to the addresses.
     */
    public static HMHttpAuthentication queryParameter( String baseUrl, String name, String value ) {
        checkValue("parameter name", name);
        checkValue("parameter value", value);
        HMHttpAuthentication authentication = new HMHttpAuthentication(baseUrl, "Parameter " + name.trim());
        authentication.parameterName = name.trim();
        authentication.parameterValue = value.trim();
        return authentication;
    }

    private static void checkValue( String what, String value ) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("The " + what + " is missing.");
        }
    }

    /**
     * @return the server the credentials are sent to, as https://host:port.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @return a description of the authentication without the secret, as "Bearer token".
     */
    @Override
    public String toString() {
        return description + " for " + baseUrl;
    }

    @Override
    public boolean appliesTo( String href ) {
        if (href == null || !href.startsWith(baseUrl)) {
            return false;
        }
        // not https://host:80 for https://host:8080
        if (href.length() == baseUrl.length()) {
            return true;
        }
        char next = href.charAt(baseUrl.length());
        return next == '/' || next == '?' || next == '#';
    }

    @Override
    public String toHttpUrl( String href ) {
        if (parameterName == null || !appliesTo(href)) {
            return href;
        }
        String encoded = URLEncoder.encode(parameterName, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(parameterValue, StandardCharsets.UTF_8);
        int fragment = href.indexOf('#');
        String base = fragment >= 0 ? href.substring(0, fragment) : href;
        String rest = fragment >= 0 ? href.substring(fragment) : "";
        if (base.contains("?" + encoded) || base.contains("&" + encoded)) {
            return href;
        }
        return base + (base.contains("?") ? "&" : "?") + encoded + rest;
    }

    @Override
    public Map<String, String> getHeaders( String href ) {
        return appliesTo(href) ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
    }

    @Override
    public HMStacResponse get( String href ) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(toHttpUrl(href)).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        // the credentials are not repeated to the server of a redirect
        connection.setInstanceFollowRedirects(false);
        for( Map.Entry<String, String> header : getHeaders(href).entrySet() ) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        int code = connection.getResponseCode();
        if (code >= 300 && code < 400) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null) {
                throw new IOException("HTTP " + code + " without location reading " + href);
            }
            String target = URI.create(href).resolve(location).toString();
            // with the credentials only if still on the same server
            return appliesTo(target) ? get(target) : new HMStacAccess().get(target);
        }
        if (code >= 400) {
            String message = "";
            try (InputStream error = connection.getErrorStream()) {
                if (error != null) {
                    message = new String(error.readNBytes(MAX_ERROR_CHARS), StandardCharsets.UTF_8);
                }
            }
            throw new IOException("HTTP " + code + " reading " + href + (message.isEmpty() ? "" : ": " + message));
        }
        return new HMStacResponse(connection.getInputStream(), connection.getContentType(), connection.getContentLengthLong());
    }
}
