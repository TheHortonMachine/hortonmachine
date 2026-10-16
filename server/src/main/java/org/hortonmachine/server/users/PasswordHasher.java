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
package org.hortonmachine.server.users;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes the passwords with PBKDF2 (HMAC SHA-512) and a random salt, as provided by the JDK.
 *
 * <p>The hashes are stored as <code>pbkdf2-sha512:&lt;iterations&gt;:&lt;salt&gt;:&lt;hash&gt;</code>,
 * salt and hash in Base64, so that the iterations can be raised later without breaking the stored passwords.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final String PREFIX = "pbkdf2-sha512";
    /** As recommended by OWASP for PBKDF2-HMAC-SHA512. */
    public static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 512;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * @param password the password.
     * @return the hash to store.
     */
    public static String hash( char[] password ) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder();
        return PREFIX + ":" + ITERATIONS + ":" + encoder.encodeToString(salt) + ":" + encoder.encodeToString(hash);
    }

    /**
     * @param password the password to check.
     * @param stored the stored hash.
     * @return <code>true</code> if the password is the one of the hash.
     */
    public static boolean verify( char[] password, String stored ) {
        String[] parts = stored.split(":");
        if (parts.length != 4 || !parts[0].equals(PREFIX)) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expected = decoder.decode(parts[3]);
            // in constant time, not to tell how much of the hash matches
            return MessageDigest.isEqual(expected, pbkdf2(password, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2( char[] password, byte[] salt, int iterations ) {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("The password can't be hashed.", e);
        }
    }
}
