/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import oms3.annotations.*;

import oms3.Compound;
import oms3.Notification.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Olaf David
 */
public class InOutTest {

    public static class Cmd1 {

        @In public String in1;
        @In @Out public String io;

        @Execute 
        public void execute() {
            io  =  io + in1;
        }
    }

    public static class Store {
        public String in = "in";
        public String io = "io";
        public String out;
    }


    public static class C extends Compound {

        Store s = new Store();

        //creating the operations
        Cmd1 op1 = new Cmd1();

        void seq1() {
            field2in(s, "in", op1, "in1");
            field2in(s, "io", op1, "io");

            // maps the compound fields
            out2field(op1, "io", s, "out");
        }

    }

    public static class C1 extends Compound {

        Store s = new Store();

        //creating the operations
        Cmd1 op1 = new Cmd1();

        void seq1() {
            val2in("in", op1, "in1");
            val2in("io", op1, "io");

            // maps the compound fields
            out2field(op1, "io", s, "out");
        }

    }

    @Test
    public void fieldOutput() throws Exception {

        C c = new C();
        c.seq1();
        c.execute();
        assertEquals("ioin", c.s.out);
    }

    @Test
    public void varInput() throws Exception {

        C1 c = new C1();
        c.seq1();
        c.execute();
//        System.out.println(c.s.out);
        assertEquals("ioin", c.s.out);
    }


}
