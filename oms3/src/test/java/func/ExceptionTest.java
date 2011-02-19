/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import oms3.ComponentException;
import oms3.annotations.*;

import oms3.Compound;
import oms3.Notification.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Olaf David
 */
public class ExceptionTest {


    public static class Cmd1 {

        @In  public String in1;
        @Out public double out1;

        @Execute
        public void execute() {
            out1 = 1.2;
        }
    }

    public static class Cmd2 {

        @In  public double in2;
        @Out public String out2;

        @Execute
        public void whatever() {
            out2 = "CMD2(" + in2 + ")";
            throw new RuntimeException("crash");
        }
    }

    public static class C extends Compound {

        @In public String in;
        @Out public String out;
        //creating the operations
        Cmd1 op1 = new Cmd1();
        Cmd2 op2 = new Cmd2();

        public C() {
            // connects the two internals
            out2in(op1, "out1", op2, "in2");

            // maps the compound fields
            in2in("in", op1, "in1");
            out2out("out", op2, "out2");
        }
    }

    @Test
    public void throwException() {

        C c = new C();
        c.in = "1";
        try {
            c.execute();
            fail("Exception expected");
        } catch (ComponentException E) {
            assertEquals(c.op2, E.getSource());
            assertEquals("crash", E.getCause().getMessage());
        }
    }
}
