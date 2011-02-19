/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import oms3.Compound;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Olaf David
 */
public class ColabCompoundTest {

    public class Op1 {

        @In  public String in;
        @Out public String out1;
        @Out public String out2;

        @Execute
        public void execute() {
            out1 = "Op1[" + in + "]";
            out2 = "Op1[" + in + "]";
        }
    }

    public class Op2 {

        @In public String in1;
        @In public String in2;
        @Out public String out = new String();

        @Execute
        public void execute() {
            out = "Op2[" + in1 + "," + in2 + "]";
        }
    }

    public class Op3 {

        @In public String in;
        @Out public String out;

        @Execute
        public void execute() {
            out = "Op3[" + in + "]";
        }
    }

    public class Op4 {

        @In public String in1;
        @In public String in2;
        @Out public String out;

        @Execute
        public void execute() {
            String s = "Op4[" + in1 + "," + in2 + "]";
            out = s;
        }
    }

    public class Op5 {

        @In  public String in;
        @Out public String out;

        @Execute
        public void execute() {
            out = "Op5[" + in + "]";
        }
    }

    public class C extends Compound {

        @In public String inOp1;
        @In public String inOp5;
        
        Op1 op1 = new Op1();
        Op2 op2 = new Op2();
        Op3 op3 = new Op3();
        Op4 op4 = new Op4();
        Op5 op5 = new Op5();

        public C() {
            out2in(op1, "out1", op2, "in1"); // D1
       //     connect(op1, "out2", op2, "in2"); // ??
            out2in(op1, "out1", op3, "in"); // D1
            out2in(op2, "out", op4, "in1"); // D3
            out2in(op1, "out2", op4, "in2"); // D2
            out2in(op5, "out", op2, "in2"); //D4
            
            in2in("inOp1", op1, "in");
            in2in("inOp5", op5, "in");
        }
    }

    @Test
    public void colab() throws Exception {
        C c = new C();
        c.inOp1 = "1";
        c.inOp5 = "5";

        c.execute();
        assertEquals("Op4[Op2[Op1[1],Op5[5]],Op1[1]]", c.op4.out);
    }
}
