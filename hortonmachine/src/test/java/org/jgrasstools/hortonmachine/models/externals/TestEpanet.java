package org.jgrasstools.hortonmachine.models.externals;

import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.externals.epanet.Epanet;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
/**
 * Test Epanet file creation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestEpanet extends HMTestCase {

    public void testEpanet() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

//        String inp = "D:\\data\\epanet\\Esempio1\\Esempio1.inp";
         String inp = "C:\\TMP\\epanettests\\test\\aaaaa.inp";

        Epanet gen = new Epanet();
        gen.inInp = inp;
        gen.process();

    }
    
    public static void main( String[] args ) {
        try {
            new TestEpanet().testEpanet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
