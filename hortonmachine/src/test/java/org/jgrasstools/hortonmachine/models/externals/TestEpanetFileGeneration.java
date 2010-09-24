package org.jgrasstools.hortonmachine.models.externals;

import java.io.File;

import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.hortonmachine.externals.epanet.EpanetProjectFilesGenerator;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Junctions;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
/**
 * Test Epanet file creation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestEpanetFileGeneration extends HMTestCase {

    public void testEpanet() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        EpanetProjectFilesGenerator gen = new EpanetProjectFilesGenerator();
        gen.pm = pm;
        gen.pCode = "EPSG:32632";

        File here = new File("C:\\TMP\\epanettests\\");
        File folder = new File(here.getAbsolutePath() + File.separator + "test");
        if (!folder.exists())
            assertTrue(folder.mkdir());

        gen.inFolder = folder.getAbsolutePath();
        gen.process();

        File junctions = new File(folder, Junctions.ID.getShapefileName());
        assertTrue(junctions.exists());

//        assertTrue(FileUtilities.deleteFileOrDir(folder));

    }

}
