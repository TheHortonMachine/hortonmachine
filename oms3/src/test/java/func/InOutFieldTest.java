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
public class InOutFieldTest {

    public static class Backend {
        public double val = 1.0;
    }
    
    public static class Cmd2 {

        @In public double in;
        @Out public double out;

        @Execute
        public void whatever() {
            out = in + 1.1;
        }
    }

    public static class C extends Compound {

        Backend be;

        public C(Backend be) {
            this.be = be;
        }

        //creating the operations
        Cmd2 op = new Cmd2();

        void seq1() {
            field2in(be, "val", op, "in");
            out2field(op, "out", be, "val");
        }

    }

    @Test(timeout = 1000)
    public void alterField() throws Exception {

        Backend be = new Backend();
        C c = new C(be);
        c.seq1();
        c.execute();
        c.execute();
        assertEquals(3.2, be.val, 0.0);
    }
}
