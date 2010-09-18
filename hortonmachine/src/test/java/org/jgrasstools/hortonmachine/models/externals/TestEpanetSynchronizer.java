package org.jgrasstools.hortonmachine.models.externals;

import java.io.File;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureWriter;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.externals.epanet.EpanetFeaturesSynchronizer;
import org.jgrasstools.hortonmachine.externals.epanet.EpanetInpGenerator;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Junctions;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pipes;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pumps;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Reservoirs;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Tanks;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Valves;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
/**
 * Test Epanet sync.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestEpanetSynchronizer extends HMTestCase {

    public void testEpanet() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        String folder = "C:\\TMP\\epanettests\\shape_orig_esempio1\\";
        String newFolder = "C:\\TMP\\epanettests\\test_out\\";

        String jPath = folder + File.separator + Junctions.ID.getShapefileName();
        String tPath = folder + File.separator + Tanks.ID.getShapefileName();
        String puPath = folder + File.separator + Pumps.ID.getShapefileName();
        String piPath = folder + File.separator + Pipes.ID.getShapefileName();
        String vPath = folder + File.separator + Valves.ID.getShapefileName();
        String rPath = folder + File.separator + Reservoirs.ID.getShapefileName();
        SimpleFeatureCollection jFC = ShapefileFeatureReader.readShapefile(jPath);
        SimpleFeatureCollection tFC = ShapefileFeatureReader.readShapefile(tPath);
        SimpleFeatureCollection puFC = ShapefileFeatureReader.readShapefile(puPath);
        SimpleFeatureCollection piFC = ShapefileFeatureReader.readShapefile(piPath);
        SimpleFeatureCollection vFC = ShapefileFeatureReader.readShapefile(vPath);
        SimpleFeatureCollection rFC = ShapefileFeatureReader.readShapefile(rPath);

        EpanetFeaturesSynchronizer sync = new EpanetFeaturesSynchronizer();
        sync.pm = pm;
        sync.inJunctions = jFC;
        sync.inTanks = tFC;
        sync.inPumps = puFC;
        sync.inPipes = piFC;
        sync.inValves = vFC;
        sync.inReservoirs = rFC;

        sync.process();

        File newFolderFile = new File(newFolder);
        if (!newFolderFile.exists())
            assertTrue(newFolderFile.mkdir());

        jPath = newFolder + File.separator + Junctions.ID.getShapefileName();
        piPath = newFolder + File.separator + Pipes.ID.getShapefileName();
        ShapefileFeatureWriter.writeShapefile(jPath, sync.inJunctions);
        ShapefileFeatureWriter.writeShapefile(piPath, sync.inPipes);

    }

}
