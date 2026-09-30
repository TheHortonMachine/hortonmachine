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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The location of an object on AWS S3, parsed from its different address forms:
 *
 * <ul>
 * <li><code>s3://bucket/key</code></li>
 * <li><code>https://bucket.s3.region.amazonaws.com/key</code> (virtual hosted, also without region)</li>
 * <li><code>https://s3.region.amazonaws.com/bucket/key</code> (path style, also without region)</li>
 * </ul>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMS3Location {
    private static final Pattern VIRTUAL_HOST = Pattern.compile("^(.+)\\.s3[.-](?:([a-z0-9-]+)\\.)?amazonaws\\.com$");
    private static final Pattern PATH_STYLE_HOST = Pattern.compile("^s3[.-](?:([a-z0-9-]+)\\.)?amazonaws\\.com$|^s3\\.amazonaws\\.com$");

    private final String bucket;
    private final String key;
    private final String region;

    public HMS3Location( String bucket, String key, String region ) {
        this.bucket = bucket;
        this.key = key;
        this.region = region;
    }

    /**
     * Parse an address.
     *
     * @param href the address.
     * @return the location or null if the address is not an s3 one.
     */
    public static HMS3Location parse( String href ) {
        if (href == null)
            return null;
        try {
            URI uri = URI.create(href);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null)
                return null;
            String path = uri.getRawPath() == null ? "" : URLDecoder.decode(uri.getRawPath(), StandardCharsets.UTF_8);
            if (path.startsWith("/"))
                path = path.substring(1);

            if (scheme.equalsIgnoreCase("s3")) {
                return new HMS3Location(host, path, null);
            }
            if (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))
                return null;
            String lowerHost = host.toLowerCase();
            Matcher virtual = VIRTUAL_HOST.matcher(lowerHost);
            if (virtual.matches()) {
                return new HMS3Location(host.substring(0, virtual.end(1)), path, normalizeRegion(virtual.group(2)));
            }
            Matcher pathStyle = PATH_STYLE_HOST.matcher(lowerHost);
            if (pathStyle.matches()) {
                int slash = path.indexOf('/');
                if (slash <= 0)
                    return null;
                return new HMS3Location(path.substring(0, slash), path.substring(slash + 1), normalizeRegion(pathStyle.group(1)));
            }
        } catch (IllegalArgumentException e) {
            // not a valid uri
        }
        return null;
    }

    private static String normalizeRegion( String region ) {
        // hosts like bucket.s3.dualstack.us-west-2.amazonaws.com are not handled, plain s3 means us-east-1
        if (region == null || region.isEmpty())
            return null;
        return region;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    /**
     * @return the region if part of the address, else null.
     */
    public String getRegion() {
        return region;
    }

    /**
     * @param defaultRegion the region to use if the address doesn't contain it.
     * @return the virtual hosted https url of the object.
     */
    public String toHttpsUrl( String defaultRegion ) {
        String r = region != null ? region : defaultRegion;
        String host = r != null ? bucket + ".s3." + r + ".amazonaws.com" : bucket + ".s3.amazonaws.com";
        return "https://" + host + "/" + key;
    }

    @Override
    public String toString() {
        return "s3://" + bucket + "/" + key + (region != null ? " (" + region + ")" : "");
    }
}
