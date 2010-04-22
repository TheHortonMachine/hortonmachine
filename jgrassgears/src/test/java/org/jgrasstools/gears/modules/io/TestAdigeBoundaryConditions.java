package org.jgrasstools.gears.modules.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.gears.io.adige.AdigeBoundaryCondition;
import org.jgrasstools.gears.io.adige.AdigeBoundaryConditionReader;
import org.jgrasstools.gears.io.adige.AdigeBoundaryConditionWriter;
import org.jgrasstools.gears.utils.HMTestCase;
/**
 * Test AdigeBoundaryConditions reader and writer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestAdigeBoundaryConditions extends HMTestCase {

    public void testAdigeBoundaryConditions() throws Exception {
        File tmpAbc = File.createTempFile("testadigeboundcond", ".csv");
        
        AdigeBoundaryCondition abc1 = new AdigeBoundaryCondition();
        abc1.basinId = 1;
        abc1.discharge = 10.0;
        abc1.dischargeSub = 5.0;
        abc1.S1 = 6.0;
        abc1.S2 = 7.0;
        AdigeBoundaryCondition abc2 = new AdigeBoundaryCondition();
        abc2.basinId = 2;
        abc2.discharge = 20.0;
        abc2.dischargeSub = 10.0;
        abc2.S1 = 12.0;
        abc2.S2 = 14.0;
        
        List<AdigeBoundaryCondition> condList = new ArrayList<AdigeBoundaryCondition>();
        condList.add(abc1);
        condList.add(abc2);
        
        AdigeBoundaryConditionWriter writer = new AdigeBoundaryConditionWriter();
        writer.file = tmpAbc.getAbsolutePath();
        writer.tablename = "conditions";
        writer.data = condList;
        writer.write();
        writer.close();
        
        
        AdigeBoundaryConditionReader reader = new AdigeBoundaryConditionReader();
        reader.file = tmpAbc.getAbsolutePath();
        reader.read();
        List<AdigeBoundaryCondition> data = reader.data;
        
        AdigeBoundaryCondition readAbc1 = data.get(0);
        assertEquals(1, readAbc1.basinId);
        assertEquals(10.0, readAbc1.discharge);
        assertEquals(5.0, readAbc1.dischargeSub);

        AdigeBoundaryCondition readAbc2 = data.get(1);
        assertEquals(2, readAbc2.basinId);
        assertEquals(20.0, readAbc2.discharge);
        assertEquals(10.0, readAbc2.dischargeSub);
        
        reader.close();
        
        if (tmpAbc.exists()) {
            tmpAbc.delete();
        }
    }
}
