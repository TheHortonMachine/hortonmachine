/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import oms3.control.Iteration;
import oms3.annotations.*;

import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Olaf David
 */
public class SimpleIterationTest {
    
    static int i = 0;

    public static class Cmd1 {

        public @Out boolean done;

        @Execute public void execute() {
            i++;
            // System.out.println("Loop " + i);
            done = i < 10;
        }
    }

    public static class W extends Iteration {

        Cmd1 cmd1 = new Cmd1();

        public W() {
            conditional(cmd1, "done");
        }
    }

    @Test()
    public void simpleIteration() throws Exception {
        final W c = new W();
//        c.addListener(new Listeners.Printer());
        c.execute();
//        System.out.println(i);
        Assert.assertEquals(10, i);
    }
}
