//package org.jgrasstools.hortonmachine.models.hm;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
//import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
//import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.jgrasstools.hortonmachine.modules.networktools.epanet.EpanetFeaturesSynchronizer;
//import org.jgrasstools.hortonmachine.utils.HMTestCase;
///**
// * Test {@link EpanetFeaturesSynchronizer}.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestEpanetSynchronizerReal {
//
//    public static void main( String[] args ) throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//        String folder = //
//        "D:\\TMP\\epanet-tests\\rehydromatesprogettopertestsync\\test_snap\\";//output\\";
//
//        String outfolder = folder + "output\\";
//        File outfolderFile = new File(outfolder);
//        outfolderFile.mkdir();
//
//        String jPath = folder + "junctions.shp";
//        String puPath = folder + "pumps.shp";
//        String piPath = folder + "pipes.shp";
//        String vPath = folder + "valves.shp";
//        String rPath = folder + "reservoirs.shp";
//        String tPath = folder + "tanks.shp";
//        SimpleFeatureCollection jFC = ShapefileFeatureReader.readShapefile(jPath);
//        SimpleFeatureCollection tFC = ShapefileFeatureReader.readShapefile(tPath);
//        SimpleFeatureCollection puFC = ShapefileFeatureReader.readShapefile(puPath);
//        SimpleFeatureCollection piFC = ShapefileFeatureReader.readShapefile(piPath);
//        SimpleFeatureCollection vFC = ShapefileFeatureReader.readShapefile(vPath);
//        SimpleFeatureCollection rFC = ShapefileFeatureReader.readShapefile(rPath);
//
//        EpanetFeaturesSynchronizer sync = new EpanetFeaturesSynchronizer();
//        sync.pm = pm;
//        sync.inJunctions = jFC;
//        sync.inTanks = tFC;
//        sync.inPumps = puFC;
//        sync.inPipes = piFC;
//        sync.inValves = vFC;
//        sync.inReservoirs = rFC;
////        sync.pTol = 0.0001;
//        sync.process();
//
//        SimpleFeatureCollection outJ = sync.inJunctions;
//        SimpleFeatureCollection outT = sync.inTanks;
//        SimpleFeatureCollection outPu = sync.inPumps;
//        SimpleFeatureCollection outPi = sync.inPipes;
//        SimpleFeatureCollection outV = sync.inValves;
//        SimpleFeatureCollection outR = sync.inReservoirs;
//        VectorWriter.writeVector(outfolder + "junctions.shp", outJ);
//        VectorWriter.writeVector(outfolder + "pumps.shp", outPu);
//        VectorWriter.writeVector(outfolder + "pipes.shp", outPi);
//        VectorWriter.writeVector(outfolder + "tanks.shp", outT);
//        VectorWriter.writeVector(outfolder + "reservoirs.shp", outR);
//        VectorWriter.writeVector(outfolder + "valves.shp", outV);
//    }
//
//}
