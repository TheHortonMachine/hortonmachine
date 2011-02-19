/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import java.util.EventObject;
import oms3.annotations.*;

import oms3.Compound;
import oms3.Notification.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Olaf David
 */
public class FieldAlterationTest {

    public static class Cmd1 {

        @In public String in1;
        @Out public double out1;

        @Execute 
        public void execute() {
            out1 = 1.2;
        }
    }

    public static class Cmd2 {

        @In public double in2;
        @Out public String out2;

        @Execute
        public void whatever() {
            out2 = "CMD2(" + in2 + ")";
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

    @Test(timeout = 700)
    public void alterOutput() throws Exception {

        C c = new C();
        c.addListener(new Listener() {

            @Override
            public void notice(Type T, EventObject E) {
                if (T == Type.OUT) {
                    DataflowEvent ce = (DataflowEvent) E;
                    if (ce.getValue() instanceof Double) {
                        assertEquals(1.2, ce.getValue());
                        ce.setValue(new Double(3.2));
                    }
//                    System.out.println(" ---- " + ce.getValue() + " " + ce.getValue().getClass());
                }
            }
        });
        c.in = "1";
        c.execute();
        //System.out.println(c.get(c.op2, "cmdOut"));
        assertEquals("CMD2(3.2)", c.out);
    }
}
