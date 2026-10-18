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
package org.hortonmachine.server.web;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * The signed links to the files of the api, which work for a while without the Authorization header, as
 * for GDAL, QGIS or a browser.
 *
 * <p>A link is the path of the file with {@value #EXPIRES}, {@value #TOKEN} and {@value #SIGNATURE} query
 * parameters; the signature is an HMAC-SHA256 of the path, the expiry and the id of the token of the request
 * that got the link, with the key kept in the {@value #KEY_FILE} file of the workspace. A link works until
 * it expires, its token is revoked or its user is disabled or deleted, and the user can still read the file;
 * renewing the key or setting the minutes to 0 ends all the links at once.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class UrlSigner {
    /** The file of the key, in the workspace. */
    public static final String KEY_FILE = "hm-server-signing.key";
    public static final String EXPIRES = "expires";
    public static final String TOKEN = "token";
    public static final String SIGNATURE = "signature";

    private static final String ALGORITHM = "HmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final File keyFile;
    private volatile byte[] key;
    private volatile int minutes;

    /**
     * @param workspace the workspace, with the file of the key, created if missing.
     * @param minutes the minutes the links work, 0 for no links.
     */
    public UrlSigner( File workspace, int minutes ) throws Exception {
        this.keyFile = new File(workspace, KEY_FILE);
        this.minutes = minutes;
        if (keyFile.isFile()) {
            key = Base64.getDecoder().decode(FileUtilities.readFile(keyFile).trim());
        } else {
            renewKey();
        }
    }

    /**
     * Create a new key: the links given until now don't work anymore.
     */
    public synchronized void renewKey() throws Exception {
        byte[] newKey = new byte[32];
        RANDOM.nextBytes(newKey);
        FileUtilities.writeFile(Base64.getEncoder().encodeToString(newKey), keyFile);
        try {
            Files.setPosixFilePermissions(keyFile.toPath(), PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // not a posix file system
        }
        key = newKey;
    }

    /**
     * @param minutes the minutes the new links work, 0 to give none and refuse those given before.
     */
    public void setMinutes( int minutes ) {
        this.minutes = minutes;
    }

    /**
     * @return <code>true</code> if links are given.
     */
    public boolean isEnabled() {
        return minutes > 0;
    }

    /**
     * Sign the link to a file.
     *
     * @param path the path of the file, as in the url: absolute, url encoded and without query.
     * @param tokenId the token of the request.
     * @return the query that makes the path a signed link, without the '?', or <code>null</code> if links are
     *         not given or there is no token.
     */
    public String sign( String path, String tokenId ) throws Exception {
        int currentMinutes = minutes;
        if (currentMinutes <= 0 || tokenId == null) {
            return null;
        }
        long expires = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + TimeUnit.MINUTES.toSeconds(currentMinutes);
        return EXPIRES + "=" + expires + "&" + TOKEN + "=" + tokenId + "&" + SIGNATURE + "=" + signature(path, expires, tokenId);
    }

    /**
     * Check a signed link.
     *
     * @param path the path of the request, as in the url.
     * @param expires the value of the {@value #EXPIRES} parameter.
     * @param tokenId the value of the {@value #TOKEN} parameter.
     * @param signature the value of the {@value #SIGNATURE} parameter.
     * @return <code>true</code> if the signature is right and the link has not expired; the token and the access
     *         of its user to the file are still to check.
     */
    public boolean verify( String path, String expires, String tokenId, String signature ) throws Exception {
        if (minutes <= 0 || path == null || expires == null || tokenId == null || signature == null || !expires.matches("\\d{1,12}")) {
            return false;
        }
        long expiresSeconds = Long.parseLong(expires);
        if (expiresSeconds < TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())) {
            return false;
        }
        byte[] expected = signature(path, expiresSeconds, tokenId).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    private String signature( String path, long expires, String tokenId ) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(key, ALGORITHM));
        byte[] hash = mac.doFinal(("v1\n" + path + "\n" + expires + "\n" + tokenId).getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
