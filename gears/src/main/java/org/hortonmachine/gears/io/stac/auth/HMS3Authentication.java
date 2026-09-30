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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.davidmoten.aws.lw.client.Client;
import com.github.davidmoten.aws.lw.client.Credentials;
import com.github.davidmoten.aws.lw.client.ResponseInputStream;

/**
 * Authentication for data on AWS S3 (and compatible), signing the requests with AWS Signature V4.
 *
 * <p>The credentials can be read from a profile of the AWS credentials file (<code>~/.aws/credentials</code>,
 * the region from <code>~/.aws/config</code>), from the environment (<code>AWS_ACCESS_KEY_ID</code>,
 * <code>AWS_SECRET_ACCESS_KEY</code>, <code>AWS_SESSION_TOKEN</code>, <code>AWS_REGION</code>) or given directly.</p>
 *
 * <p>It applies to the given buckets or, if none is given, to all s3 addresses.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMS3Authentication implements IHMStacAuthentication {
    private final Credentials credentials;
    private String region;
    private final Set<String> buckets = new HashSet<>();
    private final Map<String, Client> clientsByRegion = new ConcurrentHashMap<>();

    /**
     * @param accessKey the access key id.
     * @param secretKey the secret access key.
     * @param sessionToken the session token of temporary credentials or null.
     * @param region the default region, used when the addresses don't contain it (e.g. s3://bucket/key).
     */
    public HMS3Authentication( String accessKey, String secretKey, String sessionToken, String region ) {
        if (accessKey == null || secretKey == null)
            throw new IllegalArgumentException("Access key and secret key are needed.");
        this.credentials = sessionToken == null || sessionToken.isBlank()
                ? Credentials.of(accessKey, secretKey)
                : Credentials.of(accessKey, secretKey, sessionToken);
        this.region = region;
    }

    /**
     * Read the credentials from a profile of the AWS credentials file.
     *
     * @param profile the profile name, <code>default</code> if null.
     * @return the authentication.
     * @throws IOException if the files can't be read or the profile has no credentials.
     */
    public static HMS3Authentication fromProfile( String profile ) throws IOException {
        if (profile == null || profile.isBlank())
            profile = "default";
        File awsFolder = new File(System.getProperty("user.home"), ".aws");
        Map<String, String> credentialValues = readIniSection(new File(awsFolder, "credentials"), profile);
        String accessKey = credentialValues.get("aws_access_key_id");
        String secretKey = credentialValues.get("aws_secret_access_key");
        if (accessKey == null || secretKey == null) {
            throw new IOException("No credentials found for profile '" + profile + "' in " + new File(awsFolder, "credentials"));
        }
        // in the config file the profiles are named "profile name", apart from the default
        String configSection = profile.equals("default") ? "default" : "profile " + profile;
        String region = readIniSection(new File(awsFolder, "config"), configSection).get("region");
        if (region == null)
            region = credentialValues.get("region");
        return new HMS3Authentication(accessKey, secretKey, credentialValues.get("aws_session_token"), region);
    }

    /**
     * @return the names of the profiles in the AWS credentials file, empty if not available.
     */
    public static List<String> listProfiles() {
        List<String> profiles = new ArrayList<>();
        File file = new File(new File(System.getProperty("user.home"), ".aws"), "credentials");
        if (!file.exists())
            return profiles;
        try {
            for( String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8) ) {
                line = line.trim();
                if (line.startsWith("[") && line.endsWith("]"))
                    profiles.add(line.substring(1, line.length() - 1).trim());
            }
        } catch (IOException e) {
            // no profiles
        }
        return profiles;
    }

    /**
     * Read the credentials from the environment variables.
     *
     * @return the authentication.
     */
    public static HMS3Authentication fromEnvironment() {
        String region = System.getenv("AWS_REGION");
        if (region == null)
            region = System.getenv("AWS_DEFAULT_REGION");
        return new HMS3Authentication(System.getenv("AWS_ACCESS_KEY_ID"), System.getenv("AWS_SECRET_ACCESS_KEY"),
                System.getenv("AWS_SESSION_TOKEN"), region);
    }

    /**
     * Limit the authentication to some buckets.
     *
     * @param bucketNames the bucket names.
     * @return this authentication.
     */
    public HMS3Authentication forBuckets( String... bucketNames ) {
        buckets.addAll(Arrays.asList(bucketNames));
        return this;
    }

    /**
     * @param region the default region, used when the addresses don't contain it.
     * @return this authentication.
     */
    public HMS3Authentication setRegion( String region ) {
        this.region = region;
        return this;
    }

    public String getRegion() {
        return region;
    }

    @Override
    public boolean appliesTo( String href ) {
        HMS3Location location = HMS3Location.parse(href);
        return location != null && (buckets.isEmpty() || buckets.contains(location.getBucket()));
    }

    @Override
    public String toHttpUrl( String href ) {
        HMS3Location location = HMS3Location.parse(href);
        if (location == null)
            return href;
        if (location.getRegion() == null && region == null)
            throw new IllegalArgumentException("The region is needed to access " + href);
        return location.toHttpsUrl(region);
    }

    @Override
    public HMStacResponse get( String href ) throws IOException {
        HMS3Location location = HMS3Location.parse(href);
        if (location == null)
            throw new IOException("Not an s3 address: " + href);
        String objectRegion = location.getRegion() != null ? location.getRegion() : region;
        if (objectRegion == null)
            throw new IOException("The region is needed to access " + href);

        Client client = getClient(objectRegion);
        ResponseInputStream stream;
        try {
            stream = client.path(location.getBucket(), location.getKey()).responseInputStream();
        } catch (RuntimeException e) {
            throw new IOException("Unable to read " + href + ": " + e.getMessage(), e);
        }
        if (stream.statusCode() >= 400) {
            String message = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            stream.close();
            throw new IOException("HTTP " + stream.statusCode() + " reading " + href + ": " + message);
        }
        String contentType = stream.header("Content-Type").orElse(null);
        // static catalogs on s3 are often uploaded without a proper content type
        if ((contentType == null || contentType.startsWith("binary/octet-stream") || contentType.startsWith("application/octet-stream"))
                && location.getKey().toLowerCase().endsWith(".json")) {
            contentType = "application/json";
        }
        long length = stream.header("Content-Length").map(Long::parseLong).orElse(-1L);
        return new HMStacResponse(stream, contentType, length);
    }

    /**
     * @param objectRegion the region.
     * @return the client signing requests for that region.
     */
    public Client getClient( String objectRegion ) {
        return clientsByRegion.computeIfAbsent(objectRegion, r -> Client.s3().region(r).credentials(credentials).build());
    }

    /**
     * Read a section of an ini file as the AWS ones.
     */
    private static Map<String, String> readIniSection( File file, String section ) throws IOException {
        Map<String, String> values = new HashMap<>();
        if (!file.exists())
            return values;
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        boolean inSection = false;
        for( String line : lines ) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";"))
                continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                inSection = line.substring(1, line.length() - 1).trim().equals(section);
            } else if (inSection) {
                int eq = line.indexOf('=');
                if (eq > 0)
                    values.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
        return values;
    }
}
