package org.hortonmachine.hmachine.models.hm;
//package org.hortonmachine.hmachine.models.hm;
//
//import java.io.File;
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
//import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.hortonmachine.hmachine.modules.networktools.epanet.OmsEpanetInpGenerator;
//import org.hortonmachine.hmachine.modules.networktools.epanet.OmsEpanetParametersOptions;
//import org.hortonmachine.hmachine.modules.networktools.epanet.OmsEpanetParametersTime;
//import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Junctions;
//import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Pipes;
//import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Pumps;
//import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Reservoirs;
//import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Tanks;
//import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Valves;
//import org.hortonmachine.hmachine.modules.networktools.epanet.core.Headloss;
//import org.hortonmachine.hmachine.utils.HMTestCase;
///**
// * Test OmsEpanet file creation.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestEpanetInpGeneration extends HMTestCase {
//
//    public void testEpanet() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        String folder = "D:\\TMP\\epanet-tests\\test3\\test_pdemand\\";
//        String extraFolder = folder + File.separator + "extra";
//
//        String inpFilePath = folder + File.separator + "aaaaa.inp";
//
//        String jPath = folder + File.separator + Junctions.ID.getShapefileName();
//        String tPath = folder + File.separator + Tanks.ID.getShapefileName();
//        String puPath = folder + File.separator + Pumps.ID.getShapefileName();
//        String piPath = folder + File.separator + Pipes.ID.getShapefileName();
//        String vPath = folder + File.separator + Valves.ID.getShapefileName();
//        String rPath = folder + File.separator + Reservoirs.ID.getShapefileName();
//        SimpleFeatureCollection jFC = OmsShapefileFeatureReader.readShapefile(jPath);
//        SimpleFeatureCollection tFC = OmsShapefileFeatureReader.readShapefile(tPath);
//        SimpleFeatureCollection puFC = OmsShapefileFeatureReader.readShapefile(puPath);
//        SimpleFeatureCollection piFC = OmsShapefileFeatureReader.readShapefile(piPath);
//        SimpleFeatureCollection vFC = OmsShapefileFeatureReader.readShapefile(vPath);
//        SimpleFeatureCollection rFC = OmsShapefileFeatureReader.readShapefile(rPath);
//
//        OmsEpanetParametersTime time = new OmsEpanetParametersTime();
//        time.duration = 7200.0;
//        time.hydraulicTimestep = 60.0;
//        time.patternTimestep = 60.0;
//        time.patternStart = 0.0;
//        time.startClockTime = "12 AM";
//        time.statistic = "NONE";
//        time.process();
//
//        OmsEpanetParametersOptions options = new OmsEpanetParametersOptions();
//        options.headloss = Headloss.C_M.getName();
//        options.unbalanced = "CONTINUE 10";
//        options.process();
//
//        OmsEpanetInpGenerator gen = new OmsEpanetInpGenerator();
//        gen.pm = pm;
//        gen.inJunctions = jFC;
//        gen.inTanks = tFC;
//        gen.inPumps = puFC;
//        gen.inPipes = piFC;
//        gen.inValves = vFC;
//        gen.inReservoirs = rFC;
//        gen.inTime = time;
//        gen.inOptions = options;
//        gen.inExtras = extraFolder;
//        gen.outFile = inpFilePath;
//
//        gen.process();
//
//        File file = new File(inpFilePath);
//        assertTrue(file.exists());
//
//    }
//
//}
