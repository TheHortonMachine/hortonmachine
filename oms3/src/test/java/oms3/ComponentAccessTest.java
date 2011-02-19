/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Range;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Olaf David
 */
public class ComponentAccessTest {

    static class TC {

        @In public double d;
        @In public int i;

        @Execute
        public void execute() {
        }
    }

    public static class TC1 {

        @Range(min=1.0, max=5.0)
        @In public double d;

        @Execute
        public void execute() {
        }
    }


    @Test
    public void testInputData() throws IOException {
        TC tc = new TC();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("d", "1.23");
        input.put("i", "1");
        ComponentAccess.setInputData(input, tc, Logger.getAnonymousLogger());

        Assert.assertEquals(1.23, tc.d, 0.0);
        Assert.assertEquals(1, tc.i);
    }

    @Test
    public void testInputDataRange() throws IOException {
        TC1 tc = new TC1();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("d", "1.23");
        ComponentAccess.setInputData(input, tc, Logger.getAnonymousLogger());
        Assert.assertEquals(1.23, tc.d, 0.0);
    }

    @Test
    public void testInputDataRangeFail() throws IOException {
        TC1 tc = new TC1();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("d", ".9");
        ComponentAccess.setInputData(input, tc, Logger.getAnonymousLogger());
        Assert.assertEquals(.9, tc.d, 0.0);
    }

}
