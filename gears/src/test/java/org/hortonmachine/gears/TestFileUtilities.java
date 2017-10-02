package org.hortonmachine.gears;

import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.files.FileUtilities;
/**
 * Test OmsFileIterator.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestFileUtilities extends HMTestCase {

    public void testBackSlashes() throws Exception {
        String path = "\\one\\two\\three\\four";

        String newPath = FileUtilities.replaceBackSlashesWithSlashes(path);
        String expected = "/one/two/three/four";
        assertEquals(expected, newPath);
    }

    public void testValidNames() {
        String name = "test";
        String safeFileName = FileUtilities.getSafeFileName(name);
        assertEquals(name, safeFileName);
        
        name = "test/asd?qwe><\\zxc*|\":.exe";
        safeFileName = FileUtilities.getSafeFileName(name);
        assertEquals("test_asd_qwe___zxc____.exe", safeFileName);

        name = "test/asd.exe.";
        safeFileName = FileUtilities.getSafeFileName(name);
        assertEquals("test_asd.exe", safeFileName);

        name = "test/asd.exe ";
        safeFileName = FileUtilities.getSafeFileName(name);
        assertEquals("test_asd.exe", safeFileName);
        
        name = "LPT8";
        safeFileName = FileUtilities.getSafeFileName(name);
        assertEquals("LPT8_", safeFileName);
    }
}
