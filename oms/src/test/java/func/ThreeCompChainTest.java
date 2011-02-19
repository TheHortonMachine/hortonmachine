/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.Compound;
import oms3.annotations.Execute;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Olaf David
 */
public class ThreeCompChainTest {

    public static class Cmd1 {

        @In  public  String in;
        @Out public  String out;

        @Execute 
        public void execute() {
            out = "CMD1(" + in + ")";
        }
    }

    public static class Cmd2 {

        @In  public String in;
        @Out public String out;

        @Execute 
        public void execute() {
            out = "CMD2(" + in + ")";
        }
    }

    public static class Cmd3 {

        @In public String in1 = "1.2";
        @In public String in;
        @Out public String out;

        @Execute 
        public void execute() {
            out = "CMD3(" + in + in1 + ")";
        }
    }

    public static class C extends Compound {
        
        @In public String in;
        @Out public String out;

        //creating the operations
        private Cmd1 op1 = new Cmd1();
        private Cmd2 op2 = new Cmd2();
        private Cmd3 op3 = new Cmd3();

        public C() throws Exception {
            out2in(op1, "out", op2, "in");
            out2in(op2, "out", op3, "in");
            
            // maps the compound fields
            in2in("in", op1);
            out2out("out", op3, "out");
        }
    }

    @Test
    public void threeCompChain() throws Exception {
        C c = new C();
//        c.addListener(new Listeners.Printer());

        c.in = "1";
        c.execute();
//        System.out.println(c.out);
        assertEquals("CMD3(CMD2(CMD1(1))1.2)", c.out);
    }
}
