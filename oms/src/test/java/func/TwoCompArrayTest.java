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
public class TwoCompArrayTest {

    public static class Cmd1 {

        @In public double[] in1;
        @Out public double[] out1;

        @Execute 
        public void execute() {
            in1[in1.length -1 ]++;
            out1 = in1;
        }
    }

    public static class Cmd2 {

        @In public double[] in2;
        @Out public double[] out2;

        @Execute
        public void whatever() {
             in2[in2.length - 1]++;
            out2 = in2;
        }
    }

    public static class C extends Compound {

        @In public double[]  in;
        @Out public double[]  out;
         
        //creating the operations
        Cmd1 op1 = new Cmd1();
        Cmd2 op2 = new Cmd2();

        public C() {
            // connects the two internals
            out2in(op1, "out1", op2, "in2");
            
            // maps the compound fields
            in2in("in", op1, "in1");
            out2out("out", op2, "out2");
            initializeComponents();
        }
    }

    @Test
    public void twoCompChain() throws Exception {
        C c = new C();
        c.in = new double[] {1.1};
        c.execute();
        System.out.println(c.out[0]);
//        assertEquals("CMD2(CMD1(1))", c.out);
    }
}
