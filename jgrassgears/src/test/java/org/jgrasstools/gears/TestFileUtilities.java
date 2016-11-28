package org.jgrasstools.gears;

import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.files.FileUtilities;
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

}
