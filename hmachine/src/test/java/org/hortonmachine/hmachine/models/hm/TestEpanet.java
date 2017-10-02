package org.hortonmachine.hmachine.models.hm;
//package org.hortonmachine.hmachine.models.hm;
//
//import java.util.List;
//
//import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.hortonmachine.hmachine.modules.networktools.epanet.OmsEpanet;
//import org.hortonmachine.hmachine.modules.networktools.epanet.core.types.Junction;
//import org.hortonmachine.hmachine.modules.networktools.epanet.core.types.Pipe;
//import org.hortonmachine.hmachine.utils.HMTestCase;
///**
// * Test OmsEpanet file creation.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestEpanet extends HMTestCase {
//
//    public void testEpanet() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        String inp = "D:\\TMP\\epanet-tests\\test4\\fail.inp";
//        // String inp =
//        // "D:\\development\\epanet-hydromates-hg\\testdata\\Davide_2011_03_21\\prova.inp";
//        // String inp = "D:\\TMP\\epanet-tests\\esempio_ravina\\test.inp";
//
//        OmsEpanet gen = new OmsEpanet();
//        gen.inInp = inp;
//        gen.pm = pm;
//        // gen.inDll =
//        // "D:\\development\\hydrologis-hg\\hydrologis\\epanet\\eu.hydrologis.jgrass.epanet\\nativelibs\\epanet2_64bit.dll";
//        // gen.inDll =
//        // "D:\\development\\nettools-hg\\eu.hydrologis.jgrass.epanet\\nativelibs\\epanet2.dll";
//        // gen.inDll = "\D:\\development\\epanet-hydromates-hg\\EN2source\\epanet2\\epanet2.dll";
//        gen.inDll = "D:\\development\\epanet-hydromates-hg\\EN2source\\epanet2\\epanet2_64bit.dll";
//
//        gen.initProcess();
//        while( gen.doProcess ) {
//            gen.process();
//
//            // System.out.println();
//            System.out.println("NODE RESULTS AT " + gen.tCurrent);
//            List<Junction> junctionsList = gen.junctionsList;
//            for( Junction junction : junctionsList ) {
//                System.out.println(junction.toString());
//            }
//            // List<Reservoir> reservoirsList = gen.reservoirsList;
//            // for( Reservoir reservoir : reservoirsList ) {
//            // System.out.println(reservoir.toString());
//            // }
//            // List<Tank> tankList = gen.tanksList;
//            // for( Tank tank : tankList ) {
//            // System.out.println(tank.toString());
//            // }
//            System.out.println("LINK RESULTS AT " + gen.tCurrent);
//            List<Pipe> pipesList = gen.pipesList;
//            for( Pipe pipe : pipesList ) {
//                System.out.println(pipe.toString());
//            }
//            // List<Pump> pumpsList = gen.pumpsList;
//            // for( Pump pump : pumpsList ) {
//            // System.out.println(pump.toString());
//            // }
//            // List<Valve> valvesList = gen.valvesList;
//            // for( Valve valve : valvesList ) {
//            // System.out.println(valve.toString());
//            // }
//        }
//        gen.finish();
//
//    }
//
//    public static void main( String[] args ) {
//        try {
//            new TestEpanet().testEpanet();
//            new TestEpanet().testEpanet();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//}
