package org.jgrasstools.hortonmachine.models.hm;
//package org.jgrasstools.hortonmachine.models.externals;
//
//import java.io.File;
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
//import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.jgrasstools.hortonmachine.externals.epanet.EpanetInpGenerator;
//import org.jgrasstools.hortonmachine.externals.epanet.EpanetParametersOptions;
//import org.jgrasstools.hortonmachine.externals.epanet.EpanetParametersTime;
//import org.jgrasstools.hortonmachine.externals.epanet.core.Headloss;
//import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Junctions;
//import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pipes;
//import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pumps;
//import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Reservoirs;
//import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Tanks;
//import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Valves;
//import org.jgrasstools.hortonmachine.utils.HMTestCase;
///**
// * Test Epanet file creation.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestEpanetInpGeneration extends HMTestCase {
//
//    public void testEpanet() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        String folder = "C:\\TMP\\epanettests\\test2\\";
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
//        SimpleFeatureCollection jFC = ShapefileFeatureReader.readShapefile(jPath);
//        SimpleFeatureCollection tFC = ShapefileFeatureReader.readShapefile(tPath);
//        SimpleFeatureCollection puFC = ShapefileFeatureReader.readShapefile(puPath);
//        SimpleFeatureCollection piFC = ShapefileFeatureReader.readShapefile(piPath);
//        SimpleFeatureCollection vFC = ShapefileFeatureReader.readShapefile(vPath);
//        SimpleFeatureCollection rFC = ShapefileFeatureReader.readShapefile(rPath);
//
//        EpanetParametersTime time = new EpanetParametersTime();
//        time.duration = 7200.0;
//        time.hydraulicTimestep = 60.0;
//        time.patternTimestep = 60.0;
//        time.patternStart = 0.0;
//        time.startClockTime = "12 AM";
//        time.statistic = "NONE";
//        time.process();
//
//        EpanetParametersOptions options = new EpanetParametersOptions();
//        options.headloss = Headloss.C_M.getName();
//        options.unbalanced = "CONTINUE 10";
//        options.process();
//
//        EpanetInpGenerator gen = new EpanetInpGenerator();
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
