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

/**
 * Interface for a particle swarm fitting function that is supposed to return a value to minimize. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IPSFunction {

    /**
     * Evaluates the value of the fitting function and returns a value representing the cost, to be minimized.
     * 
     * @param iterationStep the iterationstep to monitor the process.
     * @param particleNum the particle number to monitor the process. For 
     * 			every iteration step, all particles are evaluated.
     * @param parameters the parameters to be used in the function.
     * @param ranges the valid ranges for the given parameters.
     * @return the calculated cost value.
     * @throws Exception 
     */
    public double evaluateCost( int iterationStep, int particleNum, double[] parameters, double[]... ranges ) throws Exception;
    
    /**
     * @return a description for the applied optimization function.
     */
    public String optimizationDescription();

    /**
     * Evaluates if the supplied value is better than the supplied best.
     * 
     * <p>In this case we are minimizing cost, so a lower value is better.
     * 
     * @param evaluatedValue the value to check.
     * @param consideredBest the best to check against.
     * @return <code>true</code> if the evaluatedValue is considered to be better than the 
     *      supplied best.
     */
	public default boolean isBetter(double evaluatedValue, double consideredBest) {
		return evaluatedValue < consideredBest; 
	}


    /**
     * Evaluates if the solution has converged.
     * 
     * @param globalBest the current global best.
     * @param globalBestLocations the locations resulting from the current global best.
     * @param previousBestLocations the locations of the previous iteration. 
     * @return <code>true</code> if the solution can be considered as converged.
     */
    public boolean hasConverged( double globalBest, double[] globalBestLocations, double[] previousBestLocations );

    /**
     * Gives the initial global best to use (ex. for initial swarm creation).
     * 
     * <p>Since we are minimizing cost, this should be a high value.
     * 
     * @return the initial global best to use.
     */
	public default double getInitialGlobalBest() {
		return Double.POSITIVE_INFINITY;
	}

}
