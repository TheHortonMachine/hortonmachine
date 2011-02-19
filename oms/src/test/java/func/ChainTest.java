/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import java.util.Arrays;
import oms3.ComponentAccess;
import oms3.annotations.*;

import oms3.Compound;
import oms3.Notification.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Olaf David
 */
public class ChainTest {

    public static class HRU {
        public double solrad;
        public double[] depth;
        int count;
        String order = "";
    }

    public static class Cmd1 {

        @Out public double[] depth;

        @In @Out public HRU hru;

        @Execute
        public void execute() {
            System.out.println("1");
            hru.count++;
            hru.order += "1";
            hru.solrad = 2.3;
            depth = new double[] {4.4, 4.4};
        }
    }

    public static class Cmd2 {

        @In @Out public HRU hru;

        @Execute
        public void execute() {
            hru.count++;
            hru.order += "2";
            hru.depth[0] = 5.5;
        }
    }
    
    public static class Cmd3 {

        @In @Out public HRU hru;

        @Execute
        public void execute() {
            hru.count++;
            hru.order += "3";
        }
    }


    public static class C extends Compound {

        public HRU hru = new HRU();

        //creating the operations
        Cmd1 op1 = new Cmd1();
        Cmd2 op2 = new Cmd2();
        Cmd3 op3 = new Cmd3();

        @Initialize
        public void seq1() {
            field2in(this, "hru", op1, "hru");

            out2in(op1, "hru", op2, "hru");
            out2field(op1, "depth", hru, "depth");

            out2in(op2, "hru", op3, "hru");
            
            // maps the compound fields
//            out2field(op1, "io", hru, "out");
        }
    }

    @Test
    public void alterOutput() throws Exception {

        C c = new C();
        
        ComponentAccess.callAnnotated(c, Initialize.class, true);
        c.execute();
        assertEquals(2.3, c.hru.solrad, 0.0);
        assertNotNull(c.hru.depth);
        assertEquals(3, c.hru.count);
        assertEquals("123", c.hru.order);
        assertEquals(2, c.hru.depth.length);
        assertEquals(5.5, c.hru.depth[0], 0.0);
        assertEquals(4.4, c.hru.depth[1], 0.0);
//        System.out.println(c.s.out);
//        assertEquals("ioin", c.hru.out);
    }
}
