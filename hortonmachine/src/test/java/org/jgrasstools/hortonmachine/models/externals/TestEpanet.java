//package org.jgrasstools.hortonmachine.models.externals;
//
//import java.util.List;
//
//import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.jgrasstools.hortonmachine.externals.epanet.Epanet;
//import org.jgrasstools.hortonmachine.externals.epanet.core.types.Junction;
//import org.jgrasstools.hortonmachine.externals.epanet.core.types.Pipe;
//import org.jgrasstools.hortonmachine.externals.epanet.core.types.Pump;
//import org.jgrasstools.hortonmachine.externals.epanet.core.types.Reservoir;
//import org.jgrasstools.hortonmachine.externals.epanet.core.types.Tank;
//import org.jgrasstools.hortonmachine.externals.epanet.core.types.Valve;
//import org.jgrasstools.hortonmachine.utils.HMTestCase;
///**
// * Test Epanet file creation.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestEpanet extends HMTestCase {
//
//    public void testEpanet() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        String inp = "C:\\TMP\\epanettests\\test2\\input2.inp";
//        // String inp = "C:\\TMP\\epanettests\\test2\\aaaaa.inp";
//
//        Epanet gen = new Epanet();
//        gen.inInp = inp;
//        gen.inDll = "D:\\development\\hydrologis-hg\\hydrologis\\epanet\\eu.hydrologis.jgrass.epanet\\nativelibs\\epanet2_64bit.dll";
//
//        gen.initProcess();
//        while( gen.doProcess ) {
//            gen.process();
//
//            // System.out.println();
//            System.out.println("NODE RESULTS AT " + gen.tCurrent);
//            // List<Junction> junctionsList = gen.junctionsList;
//            // for( Junction junction : junctionsList ) {
//            // System.out.println(junction.toString());
//            // }
//            // List<Reservoir> reservoirsList = gen.reservoirsList;
//            // for( Reservoir reservoir : reservoirsList ) {
//            // System.out.println(reservoir.toString());
//            // }
//            // List<Tank> tankList = gen.tanksList;
//            // for( Tank tank : tankList ) {
//            // System.out.println(tank.toString());
//            // }
//            // System.out.println("LINK RESULTS AT " + gen.tCurrent);
//            // List<Pipe> pipesList = gen.pipesList;
//            // for( Pipe pipe : pipesList ) {
//            // System.out.println(pipe.toString());
//            // }
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
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//}
