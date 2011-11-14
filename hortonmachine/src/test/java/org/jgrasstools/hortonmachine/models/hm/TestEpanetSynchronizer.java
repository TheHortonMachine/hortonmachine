package org.jgrasstools.hortonmachine.models.hm;

import java.io.File;
import java.net.URL;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.EpanetFeaturesSynchronizer;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
/**
 * Test {@link EpanetFeaturesSynchronizer}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestEpanetSynchronizer extends HMTestCase {

    private SimpleFeatureCollection jFC;
    private SimpleFeatureCollection tFC;
    private SimpleFeatureCollection puFC;
    private SimpleFeatureCollection piFC;
    private SimpleFeatureCollection vFC;
    private SimpleFeatureCollection rFC;

    protected void setUp() throws Exception {
        URL jUrl = this.getClass().getClassLoader().getResource("junctions.shp");
        String jPath = new File(jUrl.toURI()).getAbsolutePath();
        URL piUrl = this.getClass().getClassLoader().getResource("pipes.shp");
        String piPath = new File(piUrl.toURI()).getAbsolutePath();
        URL puUrl = this.getClass().getClassLoader().getResource("pumps.shp");
        String puPath = new File(puUrl.toURI()).getAbsolutePath();
        URL tUrl = this.getClass().getClassLoader().getResource("tanks.shp");
        String tPath = new File(tUrl.toURI()).getAbsolutePath();
        URL vUrl = this.getClass().getClassLoader().getResource("valves.shp");
        String vPath = new File(vUrl.toURI()).getAbsolutePath();
        URL rUrl = this.getClass().getClassLoader().getResource("reservoirs.shp");
        String rPath = new File(rUrl.toURI()).getAbsolutePath();

        jFC = ShapefileFeatureReader.readShapefile(jPath);
        tFC = ShapefileFeatureReader.readShapefile(tPath);
        puFC = ShapefileFeatureReader.readShapefile(puPath);
        piFC = ShapefileFeatureReader.readShapefile(piPath);
        vFC = ShapefileFeatureReader.readShapefile(vPath);
        rFC = ShapefileFeatureReader.readShapefile(rPath);
    }

    public void testEpanetSynchronizer() throws Exception {

        EpanetFeaturesSynchronizer sync = new EpanetFeaturesSynchronizer();
        sync.pm = pm;
        sync.inJunctions = jFC;
        sync.inTanks = tFC;
        sync.inPumps = puFC;
        sync.inPipes = piFC;
        sync.inValves = vFC;
        sync.inReservoirs = rFC;
        sync.process();

        SimpleFeatureCollection outJ = sync.inJunctions;
        SimpleFeatureCollection outT = sync.inTanks;
        SimpleFeatureCollection outPu = sync.inPumps;
        SimpleFeatureCollection outPi = sync.inPipes;
        SimpleFeatureCollection outV = sync.inValves;
        SimpleFeatureCollection outR = sync.inReservoirs;

        assertEquals(jFC.size(), outJ.size());
        assertEquals(tFC.size(), outT.size());
        assertEquals(puFC.size(), outPu.size());
        assertEquals(piFC.size(), outPi.size());
        assertEquals(vFC.size(), outV.size());
        assertEquals(rFC.size(), outR.size());

    }

    public void testEpanetSynchronizerPartial() throws Exception {

        EpanetFeaturesSynchronizer sync = new EpanetFeaturesSynchronizer();
        sync.pm = pm;
        sync.inJunctions = jFC;
        sync.inPipes = piFC;
        sync.inValves = vFC;
        sync.inReservoirs = rFC;
        sync.process();

        SimpleFeatureCollection outJ = sync.inJunctions;
        SimpleFeatureCollection outPi = sync.inPipes;
        SimpleFeatureCollection outV = sync.inValves;
        SimpleFeatureCollection outR = sync.inReservoirs;

        assertEquals(jFC.size(), outJ.size());
        assertEquals(piFC.size(), outPi.size());
        assertEquals(vFC.size(), outV.size());
        assertEquals(rFC.size(), outR.size());
    }

    public void testEpanetSynchronizerBasic() throws Exception {

        EpanetFeaturesSynchronizer sync = new EpanetFeaturesSynchronizer();
        sync.pm = pm;
        sync.inJunctions = jFC;
        sync.inPipes = piFC;
        sync.process();

        SimpleFeatureCollection outJ = sync.inJunctions;
        SimpleFeatureCollection outPi = sync.inPipes;

        assertEquals(jFC.size(), outJ.size());
        assertEquals(piFC.size(), outPi.size());
    }

}
