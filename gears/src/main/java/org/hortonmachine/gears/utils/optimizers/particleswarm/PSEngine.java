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
    private double globalBestCost;
    private double[] globalBestLocations;
    private IPSFunction function;
    private int iterationStep;
    private Random rand;
    private double[][] ranges;
    private String prefix;
    
    private final Object globalLock = new Object(); // lock for global best updates
	private Integer numOfThreads;


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
            double initDecelerationFactor, double decayFactor, IPSFunction function, Integer numOfThreads, String prefix ) {
        this.particlesNum = particlesNum;
        this.accelerationFactorLocal = accelerationFactorLocal;
        this.accelerationFactorGlobal = accelerationFactorGlobal;
        this.initDecelerationFactor = initDecelerationFactor;
        this.decayFactor = decayFactor;
        this.maxIterations = maxIterations;
        this.function = function;
		this.numOfThreads = numOfThreads;
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

        if (numOfThreads != null && numOfThreads > 1) {
			createSwarmMultiThreaded();
		} else {
			createSwarm();
		}
        
        System.out.println(prefix + " INITIAL BEST COST = " + globalBestCost);
        System.out.println(prefix + " INITIAL BEST PARAMS = " + Arrays.toString(globalBestLocations));

        
        double[] previous = null;
        while( iterationStep <= maxIterations ) {
        	if (numOfThreads != null && numOfThreads > 1) {
        		updateSwarmMultiThreaded();
        	} else {
        		updateSwarm();
			}

            if (function.hasConverged(globalBestCost, globalBestLocations, previous)) {
                break;
            }
            previous = globalBestLocations.clone();
            

            System.out.println(prefix + " CURRENT BEST COST = " + globalBestCost);
            System.out.println(prefix + " CURRENT BEST PARAMS = " + Arrays.toString(globalBestLocations));
        }

        System.out.println(prefix + " FINAL BEST COST = " + globalBestCost);
        System.out.println(prefix + " FINAL BEST PARAMS = " + Arrays.toString(globalBestLocations));
        
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
        return globalBestCost;
    }

    private void createSwarm() throws Exception {
        rand = new Random();
        iterationStep = 0;
        globalBestCost = function.getInitialGlobalBest();
        swarm = new Particle[particlesNum];
        for( int j = 0; j < swarm.length; j++ ) {
            swarm[j] = new Particle(ranges);
            double[] currentLocations = swarm[j].getInitialLocations();
            double evaluatedCost = function.evaluateCost(iterationStep, j, currentLocations, ranges);
            swarm[j].setParticleBestFunction(evaluatedCost);
            /* find globally best function value */
            if (function.isBetter(evaluatedCost, globalBestCost)) {
                globalBestCost = evaluatedCost;
                if (globalBestLocations == null) {
                    globalBestLocations = new double[currentLocations.length];
                }
                for( int k = 0; k < currentLocations.length; k++ ) {
                    globalBestLocations[k] = currentLocations[k];
                }
            } else if (globalBestLocations == null) {
                throw new RuntimeException("No evaluated value found better than the initial global best: " + evaluatedCost + " vs. "
                        + globalBestCost);
            }
        }
    }
    
	private void createSwarmMultiThreaded() throws Exception {
		rand = new Random();
		iterationStep = 0;
		globalBestCost = function.getInitialGlobalBest();
		swarm = new Particle[particlesNum];

		// globalBestLocations will be initialized lazily once we know the dimension
		globalBestLocations = null;

		int nThreads = Runtime.getRuntime().availableProcessors();
		java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(nThreads);

		java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();

		for (int j = 0; j < swarm.length; j++) {
			final int particleIndex = j;

			futures.add(executor.submit(() -> {
				// create particle and evaluate it
				Particle p = new Particle(ranges);
				swarm[particleIndex] = p;

				double[] currentLocations = p.getInitialLocations();
				double evaluatedCost = function.evaluateCost(iterationStep, particleIndex, currentLocations, ranges);
				p.setParticleBestFunction(evaluatedCost);

				// update global best (thread-safe)
				synchronized (globalLock) {
					if (function.isBetter(evaluatedCost, globalBestCost)) {
						globalBestCost = evaluatedCost;

						if (globalBestLocations == null) {
							globalBestLocations = new double[currentLocations.length];
						}
						System.arraycopy(currentLocations, 0, globalBestLocations, 0, currentLocations.length);
					}
				}

				return null;
			}));
		}

		// wait for all particles to finish initialization
		for (java.util.concurrent.Future<?> f : futures) {
			f.get();
		}
		executor.shutdown();

		// Safety check (should not happen in practice)
		if (globalBestLocations == null) {
			throw new RuntimeException("No evaluated value found better than the initial global best: " + globalBestCost);
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
            double evaluatedCost;
            if (currentLocations != null) {
                evaluatedCost = function.evaluateCost(iterationStep, i, currentLocations, ranges);
            } else {
                // parameters were outside, ignore and try next round with new position
                continue;
            }
            /* update best local function value */
            if (function.isBetter(evaluatedCost, particle.getParticleBestFunction())) {
                particle.setParticleBestFunction(evaluatedCost);
                particle.setParticleLocalBeststoCurrent();
            }
            /* update best global function value */
            if (function.isBetter(evaluatedCost, globalBestCost)) {
                globalBestCost = evaluatedCost;
                for( int j = 0; j < currentLocations.length; j++ ) {
                    globalBestLocations[j] = currentLocations[j];
                }
            }
        }
    }
    
    private void updateSwarmMultiThreaded() throws Exception {
    	iterationStep++;

        double w = initDecelerationFactor * Math.pow(iterationStep, -decayFactor);

        int nThreads = Runtime.getRuntime().availableProcessors();
        if (numOfThreads != null && numOfThreads > 0) {
        	nThreads = numOfThreads;
        }
        var executor = java.util.concurrent.Executors.newFixedThreadPool(nThreads);

        var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        
        for (int i = 0; i < swarm.length; i++) {
            final int particleIndex = i;

            futures.add(executor.submit(() -> {
                Particle particle = swarm[particleIndex];

                double[] currentLocations = particle.update(
                        w,
                        accelerationFactorLocal, rand.nextDouble(),
                        accelerationFactorGlobal, rand.nextDouble(),
                        globalBestLocations);

                if (currentLocations == null) {
                    return null; // skip, outside bounds
                }

                double evaluatedCost = function.evaluateCost(iterationStep, particleIndex, currentLocations, ranges);

                // update particle-local best
                if (function.isBetter(evaluatedCost, particle.getParticleBestFunction())) {
                    particle.setParticleBestFunction(evaluatedCost);
                    particle.setParticleLocalBeststoCurrent();
                }

                // update global best (thread-safe)
                synchronized (globalLock) {
                    if (function.isBetter(evaluatedCost, globalBestCost)) {
                        globalBestCost = evaluatedCost;
                        System.arraycopy(currentLocations, 0, globalBestLocations, 0, currentLocations.length);
                    }
                }

                return null;
            }));
        }

        // wait for all threads to finish
        for (var f : futures) f.get();

        executor.shutdown();
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
