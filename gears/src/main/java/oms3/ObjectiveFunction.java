/*
 * $Id: ObjectiveFunction.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3;

/**
 * Objective Function.
 * 
 * Interface for providing a objective function implementation.
 * 
 * @author  makiko, od
 */
public interface ObjectiveFunction {

    /** 
     * Calculates the objective function value based on the given simulated and observed values.
     * {@literal If measuredValue[i] <= missingValue (where 0<=i<measuredValue.length), then
     * measuredValue[i] and simulatedValue[i] are not included for objective
     * function calculation.}
     * 
     * @param obs  the observed values
     * @param sim the simulated values
     * @param missingValue   the missing value that indicates a observed or/and simulated value is missing  
     * @return               the objective function value.
     */
    public double calculate(double[] obs, double[] sim, double missingValue);

    /** This method tells you if it is good for the objective function value to be large.
     *  @return true if a greater objective function value is considered better.
     *          false is returned otherwise. 
     */
    public boolean positiveDirection();

}
