/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import java.util.ArrayList;
import java.util.List;
import oms3.*;
import oms3.annotations.*;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Olaf David
 */
public class Func5_Test {

    public static class SimpleProcess {

        @In public String in1;
        @Out public String out1;

        @Execute public void execute() {
            out1 = "CMD1(" + in1 + ")";
        }
    }

    public static class ComplexProcess {

        @In public String in2;
        @Out public String out2;

        @Execute
        public void whatever() {
            out2 = "CMD2(" + in2 + ")";
            run();
        }
        
        void run() {
//            System.out.println("Started  " + in2);
            for (int i = 0; i < 1000000; i++) {
                double f = i + Math.PI * i / 12.3456 * i;
                double g = Math.sin(f * f) / (i + 1) * Math.PI;
            }
//            System.out.println("Done  " + in2);
        }
    }

    public static class HRU extends Compound {

        @In public String in;
        @Out public String out;
        //creating the operations
        
        SimpleProcess op1 = new SimpleProcess();
        ComplexProcess op2 = new ComplexProcess();

        public HRU() {
            // connects the two internals
            out2in(op1, "out1", op2, "in2");

            // maps the compound fields
            in2in("in", op1, "in1");
            out2out("out", op2, "out2");
        }
    }

    @Test
    public void func1() throws Exception {

        List<Compound> l = new ArrayList<Compound>();
        for (int i = 0; i < 10; i++) {
            HRU c = new HRU();

          c.in = "1-" + i;
          l.add(c);
        }

//        long start = System.currentTimeMillis();
//        Runner.parallel2(l);
//        long end = System.currentTimeMillis();
        
        HRU c = new HRU();
        c.in = "1";
        c.execute();
        Assert.assertEquals("CMD2(CMD1(1))", c.out);
    }
}
