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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The access context of a stac catalog: the authentications to use for the protected addresses.
 *
 * <p>The first authentication that applies to an address is used, addresses no authentication
 * applies to are read without authentication: the s3:// ones as objects of public buckets, on their
 * https address (on the endpoint and region declared by the storage extension, if any).</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMStacAccess {
    private final List<IHMStacAuthentication> authentications = new ArrayList<>();
    private String defaultS3Region;

    public HMStacAccess() {
    }

    public HMStacAccess( IHMStacAuthentication... authentications ) {
        for( IHMStacAuthentication authentication : authentications ) {
            addAuthentication(authentication);
        }
    }

    public HMStacAccess addAuthentication( IHMStacAuthentication authentication ) {
        authentications.add(authentication);
        return this;
    }

    public List<IHMStacAuthentication> getAuthentications() {
        return authentications;
    }

    /**
     * @param href the address.
     * @return the authentication to use for the address or null if none applies.
     */
    public IHMStacAuthentication getAuthentication( String href ) {
        if (href == null)
            return null;
        for( IHMStacAuthentication authentication : authentications ) {
            if (authentication.appliesTo(href))
                return authentication;
        }
        return null;
    }

    /**
     * @param href the address.
     * @return <code>true</code> if the address needs authentication.
     */
    public boolean isAuthenticated( String href ) {
        return getAuthentication(href) != null;
    }

    /**
     * Set the region of the public buckets read without credentials, used when neither the address nor the
     * storage extension declare it.
     *
     * <p>If no region is known the legacy global endpoint (<code>bucket.s3.amazonaws.com</code>) is used,
     * which AWS routes to the region of the bucket.</p>
     *
     * @param defaultS3Region the region, e.g. us-west-2, or null.
     * @return this access.
     */
    public HMStacAccess setDefaultS3Region( String defaultS3Region ) {
        this.defaultS3Region = defaultS3Region == null || defaultS3Region.isBlank() ? null : defaultS3Region.trim();
        return this;
    }

    public String getDefaultS3Region() {
        return defaultS3Region;
    }

    /**
     * @param href the address.
     * @return <code>true</code> if the address is an s3:// one, which http clients can't read as it is.
     */
    public static boolean isS3Scheme( String href ) {
        return href != null && href.regionMatches(true, 0, "s3://", 0, 5);
    }

    /**
     * @param href the address.
     * @return the address usable by http clients (e.g. s3 urls converted to https).
     */
    public String toHttpUrl( String href ) {
        return toHttpUrl(href, null, null);
    }

    /**
     * @param href the address.
     * @param storageEndpoint the endpoint of the S3 compatible service holding the object, as declared by the
     *          storage extension, used for the public s3 addresses. Can be null.
     * @param storageRegion the region of the object, as declared by the storage extension, used for the
     *          public s3 addresses. Can be null.
     * @return the address usable by http clients (e.g. s3 urls converted to https).
     */
    public String toHttpUrl( String href, String storageEndpoint, String storageRegion ) {
        IHMStacAuthentication authentication = getAuthentication(href);
        if (authentication != null)
            return authentication.toHttpUrl(href);
        if (isS3Scheme(href))
            return toPublicS3Url(href, storageEndpoint, storageRegion);
        return href;
    }

    /**
     * The https address of an object of a public bucket, readable without credentials.
     */
    private String toPublicS3Url( String href, String storageEndpoint, String storageRegion ) {
        HMS3Location location = HMS3Location.parse(href);
        if (location == null && isS3Scheme(href)) {
            // keys with characters not valid in an uri, as spaces
            String path = href.substring(5);
            int slash = path.indexOf('/');
            if (slash > 0)
                location = new HMS3Location(path.substring(0, slash), path.substring(slash + 1), null);
        }
        if (location == null)
            return href;
        if (storageEndpoint != null && !storageEndpoint.isBlank()) {
            String endpoint = storageEndpoint.trim();
            while( endpoint.endsWith("/") )
                endpoint = endpoint.substring(0, endpoint.length() - 1);
            return endpoint + "/" + location.getBucket() + "/" + encodeKey(location.getKey());
        }
        String region = storageRegion != null && !storageRegion.isBlank() ? storageRegion.trim() : defaultS3Region;
        HMS3Location encoded = new HMS3Location(location.getBucket(), encodeKey(location.getKey()), location.getRegion());
        return encoded.toHttpsUrl(region);
    }

    /**
     * Encode the characters of an object key that are not valid in an url path, keeping the slashes.
     */
    private static String encodeKey( String key ) {
        StringBuilder sb = new StringBuilder();
        for( byte b : key.getBytes(StandardCharsets.UTF_8) ) {
            int c = b & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || "-_.~/!$&'()*+,;=:@".indexOf(c) >= 0) {
                sb.append((char) c);
            } else {
                sb.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(c & 0xf, 16)));
            }
        }
        return sb.toString();
    }

    /**
     * Read an address, with authentication if needed.
     *
     * @param href the address.
     * @return the response, to be closed after use.
     * @throws IOException if the request fails.
     */
    public HMStacResponse get( String href ) throws IOException {
        return get(href, null, null);
    }

    /**
     * Read an address, with authentication if needed, on the storage the address is declared to be on.
     *
     * @param href the address.
     * @param storageEndpoint the endpoint of the S3 compatible service holding the object, as declared by the
     *          storage extension of an asset (see {@link org.hortonmachine.gears.io.stac.HMStacAsset#getStorageEndpoint()}),
     *          used for s3 addresses if the authentication has no endpoint set. Can be null.
     * @return the response, to be closed after use.
     * @throws IOException if the request fails.
     */
    public HMStacResponse get( String href, String storageEndpoint ) throws IOException {
        return get(href, storageEndpoint, null);
    }

    /**
     * Read an address, with authentication if needed, on the storage the address is declared to be on.
     *
     * <p>The s3:// addresses no authentication applies to are read without credentials, as the objects of
     * public buckets.</p>
     *
     * @param href the address.
     * @param storageEndpoint see {@link #get(String, String)}.
     * @param storageRegion the region of the object, as declared by the storage extension of an asset
     *          (see {@link org.hortonmachine.gears.io.stac.HMStacAsset#getStorageRegion()}). Can be null.
     * @return the response, to be closed after use.
     * @throws IOException if the request fails.
     */
    public HMStacResponse get( String href, String storageEndpoint, String storageRegion ) throws IOException {
        IHMStacAuthentication authentication = getAuthentication(href);
        if (authentication instanceof HMS3Authentication s3Authentication) {
            return s3Authentication.get(href, storageEndpoint, storageRegion);
        }
        if (authentication != null) {
            return authentication.get(href);
        }
        boolean publicS3 = isS3Scheme(href);
        String url = publicS3 ? toPublicS3Url(href, storageEndpoint, storageRegion) : href;
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(60_000);
        if (connection instanceof HttpURLConnection httpConnection) {
            int code = httpConnection.getResponseCode();
            if (code >= 400) {
                httpConnection.disconnect();
                if (publicS3 && (code == 401 || code == 403)) {
                    throw new IOException(notPublicMessage(href, storageEndpoint, code));
                }
                throw new IOException("HTTP " + code + " reading " + url);
            }
        }
        return new HMStacResponse(connection.getInputStream(), connection.getContentType(), connection.getContentLengthLong());
    }

    /**
     * Read a range of bytes of an address, with authentication if needed, as the blocks of a cloud optimized
     * GeoTIFF. The s3:// addresses no authentication applies to are read without credentials.
     *
     * @param href the address.
     * @param storageEndpoint see {@link #get(String, String, String)}.
     * @param storageRegion see {@link #get(String, String, String)}.
     * @param start the first byte.
     * @param end the last byte, inclusive.
     * @return the bytes, fewer than asked if the object ends before.
     * @throws IOException if the request fails.
     */
    public byte[] getRange( String href, String storageEndpoint, String storageRegion, long start, long end ) throws IOException {
        IHMStacAuthentication authentication = getAuthentication(href);
        if (authentication instanceof HMS3Authentication s3Authentication) {
            return s3Authentication.getRange(href, storageEndpoint, storageRegion, start, end);
        }
        if (authentication != null) {
            return authentication.getRange(href, start, end);
        }
        boolean publicS3 = isS3Scheme(href);
        String url = publicS3 ? toPublicS3Url(href, storageEndpoint, storageRegion) : href;
        try {
            return HMHttpRanges.readRange(url, start, end, null);
        } catch (HMHttpRanges.HttpRangeException e) {
            if (publicS3 && (e.getStatusCode() == 401 || e.getStatusCode() == 403))
                throw new IOException(notPublicMessage(href, storageEndpoint, e.getStatusCode()), e);
            throw e;
        }
    }

    private static String notPublicMessage( String href, String storageEndpoint, int code ) {
        HMS3Location location = HMS3Location.parse(href);
        String bucket = location != null ? location.getBucket() : "bucket";
        return "The asset " + href + " is on S3" + (storageEndpoint != null ? " (" + storageEndpoint + ")" : "")
                + " and is not publicly readable (HTTP " + code + "): it needs credentials for s3://" + bucket + ".";
    }
}
