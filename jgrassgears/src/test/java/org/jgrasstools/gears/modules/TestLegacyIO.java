package org.jgrasstools.gears.modules;

import org.jgrasstools.gears.io.grasslegacy.GrassLegacyReader;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.HMTestCase;
/**
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestLegacyIO extends HMTestCase {

    public void testRasterReader() throws Exception {

        String inPath = "D:\\data\\hydrocare_workspace\\grassdb\\utm32n_etrf89\\aidi\\cell\\bacino_adige_pit";
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
        
        GrassLegacyReader reader = new GrassLegacyReader();
        reader.file = inPath;
        reader.pm = pm;
        reader.doActive = true;
        reader.readCoverage();
        double[][] readMap = reader.geodata;
        
        System.out.println(readMap.length);
        System.out.println(readMap[0].length);
        

    }
}
