/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.utils.optimizers.particleswarm;

import java.util.Arrays;
import java.util.Random;

import org.hortonmachine.gears.utils.math.NumericsUtilities;

/**
 * 
 * Class representing a particle in the swarm.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Particle {
    /**
     * List of locations in parameter space;
     */
    private double[] locations = null;

    /** 
     * Velocity vector 
     */
    private double[] particleVelocities;

    /** 
     * Best local positions found.
     */
    private double[] particleLocalBests;

    /** 
     * Best function value. 
     */
    private double particleBestFunction;

    private double[][] ranges;

    private double[] initialLocations;
    private double[] tmpLocations = null;

    private static Random rand = new Random(2);

    /**
     * Create a new {@link Particle} with a given number of parameters dimension.
     * 
     * @param ranges the parameters spaces ranges.
     */
    public Particle( double[][] ranges ) {
        this.ranges = ranges;

        /*
         * initialize random positions inside the 
         * parameter space
         */
        double[] r = new double[ranges.length];
        for( int i = 0; i < r.length; i++ ) {
            double min = ranges[i][0];
            double max = ranges[i][1];

            double delta = max - min;
            double random = rand.nextDouble() - 1;
            double smallRand = 0.5 * delta * random;
            double value = min + delta / 2.0 + 0.8 * smallRand;

            // System.out.println(min + "/" + max + "/" + value);
            r[i] = value;
        }
        System.out.println("INIT PARTICLE WITH: " + Arrays.toString(r));

        locations = r;
        tmpLocations = new double[locations.length];
        initialLocations = new double[locations.length];

        System.arraycopy(locations, 0, initialLocations, 0, r.length);

        particleLocalBests = new double[locations.length];
        particleVelocities = new double[locations.length];
        for( int i = 0; i < locations.length; i++ ) {
            // store the location
            particleLocalBests[i] = locations[i];
            // clear the velocity vector
            particleVelocities[i] = 0.0;
        }
    }
    /**
     * @return the initial swarm location.
     */
    public double[] getInitialLocations() {
        return initialLocations;
    }

    /**
     * Particle swarming formula to update positions.
     * 
     * @param w inertia weight (controls the impact of the past velocity of the
     *              particle over the current one). 
     * @param c1 constant weighting the influence of local best 
     *              solutions.
     * @param rand1 random factor introduced in search process.
     * @param c2 constant weighting the influence of global best 
     *              solutions.
     * @param rand2 random factor introduced in search process.
     * @param globalBest leader particle (global best) in all dimensions.
     * @return the updated locations or <code>null</code>, if they are outside the ranges.
     */
    public double[] update( double w, double c1, double rand1, double c2, double rand2, double[] globalBest ) {
        for( int i = 0; i < locations.length; i++ ) {
            particleVelocities[i] = w * particleVelocities[i] + //
                    c1 * rand1 * (particleLocalBests[i] - locations[i]) + //
                    c2 * rand2 * (globalBest[i] - locations[i]);

            double tmpLocation = locations[i] + particleVelocities[i];
            /*
             * if the location falls outside the ranges, it should  
             * not be moved.
             */

            tmpLocations[i] = tmpLocation;
        }

        if (!PSEngine.parametersInRange(tmpLocations, ranges)) {
            // System.out.println("PRE-TMPLOCATIONS: " + Arrays.toString(tmpLocations));
            // System.out.println("LOCATIONS: " + Arrays.toString(locations));
            /*
             * mirror the value back
             */
            for( int i = 0; i < tmpLocations.length; i++ ) {
                double min = ranges[i][0];
                double max = ranges[i][1];

                if (tmpLocations[i] > max) {
                    double tmp = max - (tmpLocations[i] - max);
                    if (tmp < min) {
                        tmp = max;
                    }
                    locations[i] = tmp;
                } else if (tmpLocations[i] < min) {
                    double tmp = min + (min - tmpLocations[i]);
                    if (tmp > max) {
                        tmp = min;
                    }
                    locations[i] = tmp;
                } else {
                    locations[i] = tmpLocations[i];
                }
            }

            // System.out.println("POST-LOCATIONS: " + Arrays.toString(locations));
            // System.out.println("VELOCITIES: " + Arrays.toString(particleVelocities));
            return null;
        } else {
            for( int i = 0; i < locations.length; i++ ) {
                locations[i] = tmpLocations[i];
            }
            return locations;
        }
    }

    /**
     * Calculated local best function value for the particle.
     * 
     * @return the local best function value for the particle.
     */
    public double getParticleBestFunction() {
        return particleBestFunction;
    }

    /**
     * Setter for the local best function value of the particle.
     * 
     * @param particleBestFunction the new local best function value to set for the particle.
     */
    public void setParticleBestFunction( double particleBestFunction ) {
        this.particleBestFunction = particleBestFunction;
    }

    /**
     * Setter to set the current positions to be the local best positions.
     */
    public void setParticleLocalBeststoCurrent() {
        for( int i = 0; i < locations.length; i++ ) {
            particleLocalBests[i] = locations[i];
        }
    }
}