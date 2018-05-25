/*
 * RandomNormal.java
 *
 * Created on April 27, 2007, 3:55 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package oms3.ngmf.ui.calc;

import java.util.Random;

/**
 * Utility class that generates normally-distributed
 * random values using several algorithms.
 */
public class RandomNormal {
    
    /** mean */
    private float mean;
    /** standard deviation */
    private float stddev;
    
    /** next random value from
     * the polar algorithm  */
    private float   nextPolar;
    /** true if the next polar
     * value is available  */
    private boolean haveNextPolar = false;
    
    /** generator of uniformly-distributed random values */
    private static Random gen = new Random();
    
    /**
     * Set the mean and standard deviation.
     * @param mean the mean
     * @param stddev the standard deviation
     */
    public void setParameters(float mean, float stddev) {
        this.mean   = mean;
        this.stddev = stddev;
    }
    
    /**
     * Compute the next random value using the Central Limit Theorem,
     * which states that the averages of sets of uniformly-distributed
     * random values are normally distributed.
     */
    public float nextCentral() {
        // Average 12 uniformly-distributed random values.
        float sum = 0.0f;
        for (int j = 0; j < 12; ++j)
            sum += gen.nextFloat();
        
        // Subtract 6 to center about 0.
        return stddev*(sum - 6) + mean;
    }
    
    /**
     * Compute the next randomn value using the polar algorithm.
     * Requires two uniformly-distributed random values in [-1, +1).
     * Actually computes two random values and saves the second one
     * for the next invokation.
     */
    public float nextPolar() {
        // If there's a saved value, return it.
        if (haveNextPolar) {
            haveNextPolar = false;
            return nextPolar;
        }
        
        float u1, u2, r;    // point coordinates and their radius
        
        do {
            // u1 and u2 will be uniformly-distributed
            // random values in [-1, +1).
            u1 = 2*gen.nextFloat() - 1;
            u2 = 2*gen.nextFloat() - 1;
            
            // Want radius r inside the unit circle.
            r = u1*u1 + u2*u2;
        } while (r >= 1);
        
        // Factor incorporates the standard deviation.
        float factor = (float) (stddev*Math.sqrt(-2*Math.log(r)/r));
        
        // v1 and v2 are normally-distributed random values.
        float v1 = factor*u1 + mean;
        float v2 = factor*u2 + mean;
        
        // Save v1 for next time.
        nextPolar     = v1;
        haveNextPolar = true;
        
        return v2;
    }
    
    // Constants for the ratio algorithm.
    private static final float C1 = (float) Math.sqrt(8/Math.E);
    private static final float C2 = (float) (4*Math.exp(0.25));
    private static final float C3 = (float) (4*Math.exp(-1.35));
    
    /**
     * Compute the next random value using the ratio algorithm.
     * Requires two uniformly-distributed random values in [0, 1).
     */
    public float nextRatio() {
        float u, v, x, xx;
        do {
            // u and v are two uniformly-distributed random values
            // in [0, 1), and u != 0.
            while ((u = gen.nextFloat()) == 0);  // try again if 0
            v = gen.nextFloat();
            
            float y = C1*(v - 0.5f);    // y coord of point (u, y)
            x = y/u;                    // ratio of point's coords
            
            xx = x*x;
        } while (
                (xx > 5f - C2*u)                  // quick acceptance
                &&
                ( (xx >= C3/u + 1.4f) ||            // quick rejection
                (xx > (float) (-4*Math.log(u))) ) // final test
                );
        return stddev*x + mean;
    }
}