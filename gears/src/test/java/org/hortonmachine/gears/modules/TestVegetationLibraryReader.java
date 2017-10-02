package org.hortonmachine.gears.modules;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.hortonmachine.gears.io.adige.VegetationLibraryReader;
import org.hortonmachine.gears.io.adige.VegetationLibraryRecord;
import org.hortonmachine.gears.utils.HMTestCase;
/**
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVegetationLibraryReader extends HMTestCase {

    public void testVegetationLibraryReader() throws Exception {
        URL vegetationUrl = this.getClass().getClassLoader().getResource("vegetation.csv");

        VegetationLibraryReader reader = new VegetationLibraryReader();
        reader.file = new File(vegetationUrl.toURI()).getAbsolutePath();
        reader.read();
        Map<Integer, VegetationLibraryRecord> data = reader.data;

        assertTrue(data.size() == 15);

        VegetationLibraryRecord record = data.get(3);

        assertEquals(record.getTrunkRatio(), 0.2);
        assertEquals(record.getWindAtten(), 0.5);
        assertEquals(record.getRadAtten(), 0.5);
        assertEquals(record.getRgl(), 30.0);

        assertEquals(record.getArchitecturalResistance(), 60.0);
        assertEquals(record.getMinStomatalResistance(), 250.0);
        assertEquals(record.getLai(1), 5.12);
        assertEquals(record.getLai(2), 5.12);
        
        reader.close();
    }
}
