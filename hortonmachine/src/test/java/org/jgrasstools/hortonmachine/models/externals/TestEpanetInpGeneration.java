package org.jgrasstools.hortonmachine.models.externals;

import java.io.File;

import org.geotools.feature.FeatureCollection;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.FileUtilities;
import org.jgrasstools.hortonmachine.externals.epanet.EpanetInpGenerator;
import org.jgrasstools.hortonmachine.externals.epanet.EpanetProjectFilesGenerator;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Junctions;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
/**
 * Test Epanet file creation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestEpanetInpGeneration extends HMTestCase {

    public void testEpanet() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        String jPath;
        String tPath;
        String puPath;
        String piPath;
        String vPath;
        String rPath;
        FeatureCollection<SimpleFeatureType, SimpleFeature> jFC = ShapefileFeatureReader.readShapefile(jPath);

        EpanetInpGenerator gen = new EpanetInpGenerator();
        gen.pm = pm;
        gen.inJunctions = jFC;

        File here = new File(".");
        File folder = new File(here.getAbsolutePath() + File.separator + "test");
        if (!folder.exists())
            assertTrue(folder.mkdir());

        gen.inFolder = folder.getAbsolutePath();
        gen.process();

        File junctions = new File(folder, Junctions.DC_ID.getShapefileName());
        assertTrue(junctions.exists());

        assertTrue(FileUtilities.deleteFileOrDir(folder));

    }

}
