/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import oms3.Compound;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Out;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author olafdavid
 */
public class CylinderTest {

    static int inits = 0;

    // all annotations are in the CompInfo Class
    public static class CircleArea {

        public double radius;                     // (2)
        public double area;                       // (2)

        public void initialize() {
            inits++;
        }

        public void done() {
            inits--;
        }


        public void execute() {
            area = Math.PI * radius * radius;      // (3)
        }
    }

    public abstract static class CircleAreaCompInfo {

        @In  public double radius;                     // (2)
        @Out public double area;                       // (2)

        @Initialize
        abstract public void initialize();
        @Finalize
        abstract public void done();


        @Execute
        abstract public void execute();          //(3)
    }
      
    public static class CirclePerimeter {

        @In  public double radius;
        @Out public double perimeter;

         @Initialize
        public void initialize() {
            inits++;
        }

                 @Finalize
        public void done() {
            inits--;
        }


        @Execute
        public void execute() {
            perimeter = 2 * Math.PI * radius;
        }
    }

    public class CylSurface {

        @In public double area;                       // (2)
        @In public double height;
        @In public double perimeter;
        @Out public double surface;                    // (2)

        @Initialize
        public void initialize() {
            inits++;
        }

        @Finalize
        public void done() {
            inits--;
        }

        @Execute
        public void execute() {
            surface = 2 * area + height * perimeter;  // (3)
        }
    }

    public class CylinderCompound extends Compound {  // (1)

        @In public double rad;
        @In public double height;
        @Out public double surface;                   // (2)
        
        CirclePerimeter p = new CirclePerimeter();           // (3)
        CylSurface s = new CylSurface();
        CircleArea a = new CircleArea();

        public CylinderCompound() {                  // (4)

            out2in(a, "area", s, "area");
            out2in(p, "perimeter", s, "perimeter");

            in2in("height", s, "height");             // (6)
            in2in("rad", a, "radius");
            in2in("rad", p, "radius");

            out2out("surface", s, "surface");          // (7)
        }
    }
    
    @Test
    public void init() throws Exception {
         final CylinderCompound c = new CylinderCompound();
         c.initializeComponents();
         assertEquals(3, inits);

         c.finalizeComponents();
         assertEquals(0, inits);
    }

    @Test
    public void cylinder() throws Exception {

        final CylinderCompound c = new CylinderCompound();
//        c.addListener(new Listeners.Printer());
        c.height = 20.0;
        c.rad = 2.5;
        c.execute();
//        System.out.println(c.surface);
        double s = 2 * (Math.PI * c.rad * c.rad) + (2 * Math.PI * c.rad) * c.height;
        assertEquals(c.surface, s, 0.0000001);
    }
}
