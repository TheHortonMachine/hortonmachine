/*
 * ObjectiveFunction.java
 *
 * Created on October 13, 2004, 4:33 PM
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
     * If measuredValue[i] <= missingValue (where 0<=i<measuredValue.length), then
     * measuredValue[i] and simulatedValue[i] are not included for objective
     * function calculation.
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
