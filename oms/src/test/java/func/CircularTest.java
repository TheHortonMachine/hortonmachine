/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import oms3.annotations.*;
import oms3.Compound;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Olaf David
 */
public class CircularTest {

    public static class Cmd1 {

        @In
        public String in1;
        @Out
        public String out1;

        @Execute
        public void execute() {
            out1 = "CMD1(" + in1 + ")";
        }
    }

    public static class Cmd2 {

        @In
        public String in2;
        @Out
        public String out2;

        @Execute
        public void whatever() {
            out2 = "CMD2(" + in2 + ")";
        }
    }

    public class C extends Compound {

        @In
        public String in;
        @Out
        public String out;
        //creating the operations
        Cmd1 op1 = new Cmd1();
        Cmd2 op2 = new Cmd2();

        public C() {
            // connects the two internals
            out2in(op1, "out1", op2, "in2");
            out2in(op2, "out2", op1, "in1"); // creste a circular ref.

            // maps the compound fields
            in2in("in", op1, "in1");
            out2out("out", op2, "out2");
        }
    }

    @Test
    public void twoCompCircular() throws Exception {

        try {
            C c = new C();
            fail();
        } catch (RuntimeException E) {
            assertTrue(E.getMessage().contains("Circular"));
        }
    }

        public class C1 extends Compound {

            @In
            public String in;
            @Out
            public String out;
            //creating the operations
            Cmd1 op1 = new Cmd1();
            Cmd2 op2 = new Cmd2();

            public C1() {
                // connects the two internals
                out2in(op1, "out1", op1, "in1");
            }
        }

    @Test
    public void twoCompCircular1() throws Exception {

        try {
            C1 c = new C1();
            fail();
        } catch (RuntimeException E) {
            assertTrue(E.getMessage().contains("src == dest"));
        }
    }

        public class C3 extends Compound {

            @In
            public String in;
            @Out
            public String out;
            //creating the operations
            Cmd1 op1 = new Cmd1();
            Cmd2 op2 = new Cmd2();

            public C3() {
                // connects the two internals
                out2in(op1, "out1", op2, "in222");

                // maps the compound fields
                in2in("in", op1, "in1");
                out2out("out", op2, "out2");
            }
        }
        
    @Test
    public void twoCompCircular3() throws Exception {

        try {
            C3 c = new C3();
            fail();
        } catch (RuntimeException E) {
        //    E.printStackTrace();
//            assertTrue(E.getMessage().contains("src == dest"));
        }
    }
}
