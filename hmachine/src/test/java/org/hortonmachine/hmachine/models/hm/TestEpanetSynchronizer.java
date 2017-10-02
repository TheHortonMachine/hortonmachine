package org.hortonmachine.hmachine.models.hm;

import java.io.File;
import java.net.URL;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.hmachine.modules.networktools.epanet.OmsEpanetFeaturesSynchronizer;
import org.hortonmachine.hmachine.utils.HMTestCase;
/**
 * Test {@link OmsEpanetFeaturesSynchronizer}.
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

        jFC = OmsShapefileFeatureReader.readShapefile(jPath);
        tFC = OmsShapefileFeatureReader.readShapefile(tPath);
        puFC = OmsShapefileFeatureReader.readShapefile(puPath);
        piFC = OmsShapefileFeatureReader.readShapefile(piPath);
        vFC = OmsShapefileFeatureReader.readShapefile(vPath);
        rFC = OmsShapefileFeatureReader.readShapefile(rPath);
    }

    public void testEpanetSynchronizer() throws Exception {

        OmsEpanetFeaturesSynchronizer sync = new OmsEpanetFeaturesSynchronizer();
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

        OmsEpanetFeaturesSynchronizer sync = new OmsEpanetFeaturesSynchronizer();
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

        OmsEpanetFeaturesSynchronizer sync = new OmsEpanetFeaturesSynchronizer();
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
