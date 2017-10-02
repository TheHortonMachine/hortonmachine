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

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

/**
 * Particle swarm main engine.
 *
 * <p>http://www.borgelt.net/psopt.html
 * <p>Biblio: http://ncra.ucd.ie/COMP30290/crc2006/Olapeju_Ayoola_03304281.pdf ?</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PSEngine {

    private double accelerationFactorLocal;
    private double accelerationFactorGlobal;
    private double initDecelerationFactor;
    private int maxIterations;
    private double decayFactor;
    private int particlesNum;
    private Particle[] swarm;
    private double globalBest;
    private double[] globalBestLocations;
    private IPSFunction function;
    private int iterationStep;
    private Random rand;
    private double[][] ranges;
    private String prefix;

    /**
     * Constructor.
     * 
     * @param particlesNum number of particles involved.
     * @param maxIterations maximum iterations.
     * @param accelerationFactorLocal local acceleration factor for particles.
     * @param accelerationFactorGlobal global acceleration factor for particles.
     * @param initDecelerationFactor initial deceleration factor.
     * @param decayFactor decay factor.
     * @param function the fitting {@link IPSFunction function} to use.
     * @param prefix TODO
     */
    public PSEngine( int particlesNum, int maxIterations, double accelerationFactorLocal, double accelerationFactorGlobal,
            double initDecelerationFactor, double decayFactor, IPSFunction function, String prefix ) {
        this.particlesNum = particlesNum;
        this.accelerationFactorLocal = accelerationFactorLocal;
        this.accelerationFactorGlobal = accelerationFactorGlobal;
        this.initDecelerationFactor = initDecelerationFactor;
        this.decayFactor = decayFactor;
        this.maxIterations = maxIterations;
        this.function = function;
        this.prefix = prefix;
    }

    /**
     * Set ranges for the parameter space.
     * 
     * <p>The order of the ranges needs to be the same 
     * that will be used be the particles and fitting function.
     * 
     * @param ranges the [min, max] ranges to use.
     */
    public void initializeRanges( double[]... ranges ) {
        this.ranges = ranges;
    }

    /**
     * Run the particle swarm engine.
     * @throws Exception 
     */
    public void run() throws Exception {
        if (ranges == null) {
            throw new ModelsIllegalargumentException("No ranges have been defined for the parameter space.", this);
        }

        createSwarm();
        double[] previous = null;
        while( iterationStep <= maxIterations ) {
            updateSwarm();

            if (printStep()) {
                System.out.println(prefix + " - ITER: " + iterationStep + " global best: " + globalBest + " - for positions: "
                        + Arrays.toString(globalBestLocations));
            }

            if (function.hasConverged(globalBest, globalBestLocations, previous)) {
                break;
            }
            previous = globalBestLocations.clone();
        }
    }

    private boolean printStep() {
        if (maxIterations > 10000) {
            if (iterationStep % 1000 == 0) {
                return true;
            }
        } else if (maxIterations > 1000) {
            if (iterationStep % 100 == 0) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }
    /**
     * Getter for the found solution. 
     * 
     * @return the solution.
     */
    public double[] getSolution() {
        return globalBestLocations.clone();
    }

    public double getSolutionFittingValue() {
        return globalBest;
    }

    private void createSwarm() throws Exception {
        rand = new Random();
        iterationStep = 0;
        globalBest = function.getInitialGlobalBest();
        swarm = new Particle[particlesNum];
        for( int j = 0; j < swarm.length; j++ ) {
            swarm[j] = new Particle(ranges);
            double[] currentLocations = swarm[j].getInitialLocations();
            double evaluated = function.evaluate(iterationStep, j, currentLocations, ranges);
            swarm[j].setParticleBestFunction(evaluated);
            /* find globally best function value */
            if (function.isBetter(evaluated, globalBest)) {
                globalBest = evaluated;
                if (globalBestLocations == null) {
                    globalBestLocations = new double[currentLocations.length];
                }
                for( int k = 0; k < currentLocations.length; k++ ) {
                    globalBestLocations[k] = currentLocations[k];
                }
            } else if (globalBestLocations == null) {
                throw new RuntimeException("No evaluated value found better than the initial global best: " + evaluated + " vs. "
                        + globalBest);
            }
        }
    }

    private void updateSwarm() throws Exception {
        // System.out.println("UPDATE SWARM");
        iterationStep++;
        /* 
         * velocity decay factor:
         * 
         *  - it decreases when the iteration number increases
         *  - it decreases when the decay factor increases 
         */
        double w = initDecelerationFactor * Math.pow(iterationStep, -decayFactor);
        // System.out.println("W = " + w);
        /* traverse the particles */
        for( int i = 0; i < swarm.length; i++ ) {
            Particle particle = this.swarm[i];
            double[] currentLocations = particle.update(w, accelerationFactorLocal, rand.nextDouble(), accelerationFactorGlobal,
                    rand.nextDouble(), globalBestLocations);
            double evaluated;
            if (currentLocations != null) {
                evaluated = function.evaluate(iterationStep, i, currentLocations, ranges);
            } else {
                // parameters were outside, ignore and try next round with new position
                continue;
            }
            /* update best local function value */
            if (function.isBetter(evaluated, particle.getParticleBestFunction())) {
                particle.setParticleBestFunction(evaluated);
                particle.setParticleLocalBeststoCurrent();
            }
            /* update best global function value */
            if (function.isBetter(evaluated, globalBest)) {
                globalBest = evaluated;
                for( int j = 0; j < currentLocations.length; j++ ) {
                    globalBestLocations[j] = currentLocations[j];
                }
            }
        }
    }

    /**
     * Checks if the parameters are in the ranges.
     * 
     * @param parameters the params.
     * @param ranges the ranges.
     * @return <code>true</code>, if they are inside the given ranges.
     */
    public static boolean parametersInRange( double[] parameters, double[]... ranges ) {
        for( int i = 0; i < ranges.length; i++ ) {
            if (!NumericsUtilities.isBetween(parameters[i], ranges[i])) {
                return false;
            }
        }
        return true;
    }

}
