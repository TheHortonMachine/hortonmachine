/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.util;

import oms3.util.Ranges;
import java.util.Random;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.Range;
import org.junit.Test;

/**
 *
 * @author Olaf David
 */
public class RangesTest {

    public static class T {

        @Range(min = 10, max = 20)
        @In public double in;
        
        @Range(min = 10, max = 20)
        @Out public double out;

        @Execute
        public void execute() {
            out = in;
        }
    }

    @Test(timeout = 5000)
    public void rangeTest() {
        Random r = new Random();
        T t = new T();
        Ranges.Gen in = new Ranges.Gen(t, "in");
        Ranges.Check out = new Ranges.Check(t, "out");
        for (int i = 0; i < 2000; i++) {
            in.next(r);
            t.execute();
            out.check();
        }
    }
}
