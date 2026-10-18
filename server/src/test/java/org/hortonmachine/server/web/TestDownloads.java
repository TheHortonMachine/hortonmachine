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
import java.nio.file.Files;
import java.util.Arrays;

import org.hortonmachine.gears.utils.files.FileUtilities;

import junit.framework.TestCase;

/**
 * Tests of the byte ranges of the downloads and of the signed links.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestDownloads extends TestCase {

    private static String range( String header, long size ) {
        return Arrays.toString(Downloads.range(header, size));
    }

    public void testRanges() throws Exception {
        assertEquals("[0, 99]", range("bytes=0-99", 1000));
        assertEquals("[100, 999]", range("bytes=100-", 1000));
        assertEquals("[900, 999]", range("bytes=-100", 1000));
        assertEquals("[0, 999]", range("bytes=-5000", 1000));
        assertEquals("[990, 999]", range("bytes=990-5000", 1000));
        assertEquals("[0, 0]", range("BYTES=0-0", 1000));

        // outside the file
        assertEquals("[]", range("bytes=1000-", 1000));
        assertEquals("[]", range("bytes=-0", 1000));
        assertEquals("[]", range("bytes=0-", 0));

        // the whole file: none, several, not understood
        assertEquals("null", range(null, 1000));
        assertEquals("null", range("bytes=0-9,20-29", 1000));
        assertEquals("null", range("bytes=10-5", 1000));
        assertEquals("null", range("bytes=-", 1000));
        assertEquals("null", range("items=0-9", 1000));
        assertEquals("null", range("bytes=a-9", 1000));
    }

    public void testSigner() throws Exception {
        File workspace = Files.createTempDirectory("hm-signer-test").toFile();
        try {
            UrlSigner signer = new UrlSigner(workspace, 60);
            String path = "/api/jobs/abc/files/pit.tif";
            String query = signer.sign(path, "token-1");
            String expires = query.replaceAll(".*expires=(\\d+).*", "$1");
            String signature = query.replaceAll(".*signature=([A-Za-z0-9_-]+).*", "$1");
            assertTrue(query.contains("token=token-1"));
            assertTrue(signer.verify(path, expires, "token-1", signature));

            // nothing can be changed
            assertFalse(signer.verify("/api/jobs/abc/files/other.tif", expires, "token-1", signature));
            assertFalse(signer.verify(path, String.valueOf(Long.parseLong(expires) + 1), "token-1", signature));
            assertFalse(signer.verify(path, expires, "token-2", signature));
            assertFalse(signer.verify(path, expires, "token-1", signature.substring(1)));
            assertFalse(signer.verify(path, "1", "token-1", signature));
            assertFalse(signer.verify(path, "-1", "token-1", signature));
            assertNull(signer.sign(path, null));

            // the key survives a restart, a new key ends the links
            assertTrue(new UrlSigner(workspace, 60).verify(path, expires, "token-1", signature));
            signer.renewKey();
            assertFalse(signer.verify(path, expires, "token-1", signature));
            assertNotNull(FileUtilities.readFile(new File(workspace, UrlSigner.KEY_FILE)));

            // off: no links and those given before end
            query = signer.sign(path, "token-1");
            signature = query.replaceAll(".*signature=([A-Za-z0-9_-]+).*", "$1");
            expires = query.replaceAll(".*expires=(\\d+).*", "$1");
            signer.setMinutes(0);
            assertNull(signer.sign(path, "token-1"));
            assertFalse(signer.verify(path, expires, "token-1", signature));
        } finally {
            FileUtilities.deleteFileOrDir(workspace);
        }
    }
}
