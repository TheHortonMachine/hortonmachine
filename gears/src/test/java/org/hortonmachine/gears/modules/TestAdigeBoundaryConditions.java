package org.hortonmachine.gears.modules;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hortonmachine.gears.io.adige.AdigeBoundaryCondition;
import org.hortonmachine.gears.io.adige.AdigeBoundaryConditionReader;
import org.hortonmachine.gears.io.adige.AdigeBoundaryConditionWriter;
import org.hortonmachine.gears.utils.HMTestCase;
/**
 * Test AdigeBoundaryConditions reader and writer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestAdigeBoundaryConditions extends HMTestCase {

    public void testAdigeBoundaryConditions() throws Exception {
        File tmpAbc = File.createTempFile("testadigeboundcond", ".csv");

        AdigeBoundaryCondition abc1 = new AdigeBoundaryCondition();
        abc1.setBasinId(1);
        abc1.setDischarge(10.0);
        abc1.setDischargeSub(5.0);
        abc1.setS1(6.0);
        abc1.setS2(7.0);
        AdigeBoundaryCondition abc2 = new AdigeBoundaryCondition();
        abc2.setBasinId(2);
        abc2.setDischarge(20.0);
        abc2.setDischargeSub(10.0);
        abc2.setS1(12.0);
        abc2.setS2(14.0);

        HashMap<Integer, AdigeBoundaryCondition> condList = new HashMap<Integer, AdigeBoundaryCondition>();
        condList.put(abc1.getBasinId(), abc1);
        condList.put(abc2.getBasinId(), abc2);

        AdigeBoundaryConditionWriter writer = new AdigeBoundaryConditionWriter();
        writer.file = tmpAbc.getAbsolutePath();
        writer.tablename = "conditions";
        writer.data = condList;
        writer.write();
        writer.close();

        AdigeBoundaryConditionReader reader = new AdigeBoundaryConditionReader();
        reader.file = tmpAbc.getAbsolutePath();
        reader.read();
        Map<Integer, AdigeBoundaryCondition> data = reader.data;

        AdigeBoundaryCondition readAbc1 = data.get(1);
        assertEquals(1, readAbc1.getBasinId());
        assertEquals(10.0, readAbc1.getDischarge());
        assertEquals(5.0, readAbc1.getDischargeSub());

        AdigeBoundaryCondition readAbc2 = data.get(2);
        assertEquals(2, readAbc2.getBasinId());
        assertEquals(20.0, readAbc2.getDischarge());
        assertEquals(10.0, readAbc2.getDischargeSub());

        reader.close();

        if (tmpAbc.exists()) {
            if (!tmpAbc.delete())
                tmpAbc.deleteOnExit();
        }
    }
}
