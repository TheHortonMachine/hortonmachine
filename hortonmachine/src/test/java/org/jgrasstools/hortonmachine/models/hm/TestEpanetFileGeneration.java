//package org.jgrasstools.hortonmachine.models.hm;
//
//import java.io.File;
//
//import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.jgrasstools.hortonmachine.modules.networktools.epanet.EpanetProjectFilesGenerator;
//import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Junctions;
//import org.jgrasstools.hortonmachine.utils.HMTestCase;
///**
// * Test Epanet file creation.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestEpanetFileGeneration extends HMTestCase {
//
//    public void testEpanet() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        EpanetProjectFilesGenerator gen = new EpanetProjectFilesGenerator();
//        gen.pm = pm;
//        gen.pCode = "EPSG:32632";
//
//        File here = new File("D:\\TMP\\epanet-tests\\");
//        File folder = new File(here.getAbsolutePath() + File.separator + "test");
//        if (!folder.exists())
//            assertTrue(folder.mkdir());
//
//        gen.inFolder = folder.getAbsolutePath();
//        gen.process();
//
//        File junctions = new File(folder, Junctions.ID.getShapefileName());
//        assertTrue(junctions.exists());
//
////        assertTrue(FileUtilities.deleteFileOrDir(folder));
//
//    }
//
// }
