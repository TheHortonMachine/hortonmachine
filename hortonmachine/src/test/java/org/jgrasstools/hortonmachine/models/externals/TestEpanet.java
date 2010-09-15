package org.jgrasstools.hortonmachine.models.externals;

import java.io.File;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.FileUtilities;
import org.jgrasstools.hortonmachine.externals.epanet.Epanet;
import org.jgrasstools.hortonmachine.externals.epanet.EpanetInpGenerator;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Junctions;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pipes;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pumps;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Reservoirs;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Tanks;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Valves;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
/**
 * Test Epanet file creation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestEpanet extends HMTestCase {

    public void testEpanet() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        Epanet gen = new Epanet();

        gen.process();

    }

}
