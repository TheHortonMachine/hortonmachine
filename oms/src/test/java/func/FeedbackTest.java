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
public class FeedbackTest {

    public static class Cmd1 {

        @Out public String s_out;  // synchron out
        @In  public String fb_in;

        @Execute 
        public void execute() {
            s_out = "out1";
            System.out.println("1: " + this.hashCode() + " " + fb_in);
        }
    }

    public static class Cmd2 {

        @In public String s_in;     // synchron in
        @Out public String fb_out = "1";

        @Execute
        public void execute() {
            fb_out = fb_out + "_";
            System.out.println("2: " + this.hashCode() + " "  + s_in + " " + fb_out);
        }
    }

    public static class Cmd3 {

        @In public String fb_next;

        @Execute
        public void execute() {
            System.out.println("3: " + this.hashCode() + " " + fb_next);
        }
    }

    public static class C extends Compound {

        //creating the operations
        Cmd1 op1 = new Cmd1();
        Cmd2 op2 = new Cmd2();
        Cmd3 op3 = new Cmd3();

        void seq1() {
            out2in(op1, "s_out", op2, "s_in");
            feedback(op2, "fb_out", op1, "fb_in");
            out2in(op2, "fb_out", op3, "fb_next");
        }

    }

    @Test
    public void fieldOutput() throws Exception {

        C c = new C();
        c.seq1();
        c.execute();
        assertEquals(null, c.op1.fb_in);
        assertEquals(c.op1.s_out, c.op2.s_in);
        c.execute();
        assertEquals("1_", c.op1.fb_in);
        assertEquals(c.op1.s_out, c.op2.s_in);
        c.execute();
        assertEquals("1__", c.op1.fb_in);
        assertEquals(c.op1.s_out, c.op2.s_in);
    }

}
