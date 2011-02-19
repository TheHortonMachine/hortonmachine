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
public class AlternateProcTest {

    public static class Cmd1 {

        @In public String in1;
        @Out public String out1;

        @Execute 
        public void execute() {
            out1 = in1;
        }
    }

    public static class Cmd2 {

        @In public String in2;
        @Out public String out2;

        @Execute
        public void whatever() {
            out2 = "CMD2(" + in2 + ")";
        }
    }

    public static class Backend {
        public String v1;
        public String v2;
        public String v3;
    }

    public static class C extends Compound {

        Backend be;

        public C(Backend be) {
            this.be = be;
        }

        //creating the operations
        Cmd1 op1 = new Cmd1();
        Cmd2 op2 = new Cmd2();

        void seq1() {
            field2in(be, "v1", op1, "in1");

            // connects the two internals
            out2in(op1, "out1", op2, "in2");

            // maps the compound fields
            out2field(op1, "out1", be, "v2");
            out2field(op2, "out2", be, "v3");
        }

    }

    @Test
    public void alterOutput() throws Exception {

        Backend be = new Backend();
        be.v1 = "value";

        C c = new C(be);
        c.seq1();
        c.execute();
        assertEquals("value", be.v2);
        assertEquals("CMD2(value)", be.v3);
    }
}
