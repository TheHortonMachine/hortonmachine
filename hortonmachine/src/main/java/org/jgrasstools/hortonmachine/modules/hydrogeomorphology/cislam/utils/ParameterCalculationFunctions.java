/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been entirely contributed by Marco Foi (www.mcfoi.it)
 * 
 * "CI-slam module" is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils;

import java.util.ArrayList;

import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

public class ParameterCalculationFunctions {

    public final static double EULER_CONSTANT = 0.5772157;
    // SigmaL: obtained by linear regression of rainfall depth against duration after log
    // transformation
    public static double pSigma1 = 11.82;
    // mF: obtained by linear regression of rainfall depth against duration after log transformation
    public static double pmF = 0.54;
    // Coefficient of Variation: can be obtained as the average of coefficients computed for the
    // different durations
    public static double pCV = 0.23;
    // Return period  in [YEARS]
    private static int pReturnTime = 200;
    // Rainfall DURATION  in [hours]
    private static double pRainfallDuration = 1.0;
    // Integration step : 30 seconds expressed in fraction of [hours]
    public static int DELTA_T_INTEGRATION_STEP_IN_SECONDS = 30;
    // Integration step : 30 seconds expressed in fraction of [hours]
    public static double DELTA_T_INTEGRATION_STEP_IN_HOURS = DELTA_T_INTEGRATION_STEP_IN_SECONDS/3600.0;

    /**
     * Wrapper for running Test Cases without providing any parameter: do not use in actual module workflow!
     * @param pm
     * @return I intensity of rainfall event
     */
    public static double calculateRainfallIntensity( IJGTProgressMonitor pm ) {
        return calculateRainfallIntensity(pSigma1, pmF, pCV, pReturnTime, pRainfallDuration, pm);
    }
    
    /**
     * Wrapper for running with default parameters for Casini Basin: do not use in actual module workflow!
     * Default used basin parameters:
     * pSigma1 = 11.82;
     * pmF = 0.54;
     * pCV = 0.23; Coefficient of Variation
     * @param pReturnTime Return period in [YEARS]
     * @param pRainfallDuration Rainfall DURATION  in [hours]
     * @return I intensity of rainfall event
     */
    public static double calculateRainfallIntensity(int pReturnTime, double pRainfallDuration, IJGTProgressMonitor pm ) {
        return calculateRainfallIntensity(pSigma1, pmF, pCV, pReturnTime, pRainfallDuration, pm);
    }
    
    /**
     * Calculate rainfall Intensity.
     * Logic from Equation 18 of reference paper that is abased on the intensity-duration-frequency (IDF) relationship
     * propose by Koutsoyiannis et al (1998).
     * 
     * @param pSigma1 Sigma1: obtained by linear regression of rainfall depth against duration after log transformation
     * @param pmF obtained by linear regression of rainfall depth against duration after log transformation
     * @param pCV Coefficient of Variation: can be obtained as the average of coefficients computed for the different durations
     * @param pReturnTime Return period in [YEARS]
     * @param pRainfallDuration Rainfall DURATION  in [hours]
     * @param pm IJGTProgressMonitor Progress monitor to return info on progress. Pass null to avoid messages in console. 
     * @return I intensity of rainfall event
     */
    public static double calculateRainfallIntensity( double pSigma1, double pmF, double pCV, int pReturnTime, double pRainfallDuration,
            IJGTProgressMonitor pm) {

        // Logic from Equation 18 of reference paper.

        // Send message to progress monitor
        if(pm !=null){pm.beginTask("Started calculating intensity...", 1);}
        double yTr = Math.log(Math.log(pReturnTime / (pReturnTime - 1.0)));
        double I = (pSigma1 * (1 - ((pCV * Math.pow(6.0, 0.5)) / Math.PI) * (EULER_CONSTANT + yTr)) * Math.pow(pRainfallDuration,
                pmF - 1.0)) / 1000.0;
        if(pm !=null){
        pm.worked(1);
        pm.done();
        }

        return I;
    }

    /**
     * Calculates the soil thickness in a given {@link GridNode}.
     * 
     * @param slopeCurrent the current grid node.
     * @param slope (tan_beta) at the current grid node
     * @param doRound if <code>true</code>, values are round to integer.
     * @return the value of aspect.
     */
    public static double calculateSoilThickness( double slopeCurrent, boolean doRound ) {

        double soilThickness = 1.006 - 0.85 * slopeCurrent;
        return soilThickness;
    }
    
    /**
     * Returns the computed psi value for the dT increment.
     * @param previousPsi_bb
     * @param I
     * @param soil_thickness
     * @param theta_s
     * @param theta_r
     * @param alfaVanGenuchten
     * @param nVanGenuchten
     * @return psi value or JGTConstants.doubleNovalue if psi would be negative
     */
    public static double calculatePsiAtBedrockDuringVerticalInfiltration(double previousPsi_bb, double I, double soil_thickness, double theta_s, double theta_r, double alfaVanGenuchten, double nVanGenuchten){
            
        //psi_bb[i,j]<-psi_bb[i,j]+I/((theta_s[i,j]-theta_r[i,j])*((1+(alfaVanGen[i,j]*(soil_thick[i,j]+psi_bb[i,j]))^n[i,j])^(-1-1/n[i,j])-(1+(alfaVanGen[i,j]*(psi_bb[i,j]))^n[i,j])^(-1-1/n[i,j])))*dt
        
        double psi_bb = 0.0;
        
        double pow1 = Math.pow(
                (
                        1+( Math.pow( alfaVanGenuchten * (soil_thickness + previousPsi_bb), nVanGenuchten) )
                 ),
                (-1-(1/nVanGenuchten))
                );
       double pow2 = Math.pow(
                (
                        1+( Math.pow( alfaVanGenuchten * previousPsi_bb,  nVanGenuchten) )  
                ),
                (-1-(1/nVanGenuchten))
                );
        psi_bb = previousPsi_bb +
                I/
                (
                        (theta_s - theta_r)*
                        (pow1  - pow2)                         
                )*
                DELTA_T_INTEGRATION_STEP_IN_HOURS;
        
        if (psi_bb < 0){
            psi_bb = JGTConstants.doubleNovalue;
        }
        return psi_bb;
    }
    

    public static double calculateDinamicLinearTopographicIndexValue(int t, double Ab, double tempo_soglia, double th_time, double ratio, double slope){
        
        int Dt = 3600; // Required to express result  
        double IT_din_lin_Value = Ab*
                Math.max(
                        0,
                        Math.min( 
                            (
                                (
                                // This could be tau1_c : "corrivation time" as in Equation (11). The exp at following line raises from 0 to 1 once system is at steady-state.
                                t - Math.max(tempo_soglia,th_time) 
                                // K = RATIO : a matrix of cumulated d_l along flow path. 'd_l' is defined on line 555 of R script
                                )*Dt/ratio
                            ),
                            1
                            )
                        )/
                    slope;
        return IT_din_lin_Value;
        
    }
    

    /**
     * Calculates the time required by a basing node to develop a perched water table,
     * given the rainfall intensity (assumed uniform in space and time),
     * the initial soil moisture content V0 [L] and the soil moisture requires to develop the water table.
     * The calculation is based on Equation (1) of the reference paper.
     * @param pVwt double [L]
     * @param pV0 double [L]
     * @param pRainfallIntensity [L/T]
     * @return Twt double [h] Hours required to develop a perched water table at soil-bedrock interface
     */
    public static double calculateTimeForWaterTableDevelopmentTwt( double pVwt, double pV0, double pRainfallIntensity ) {

        double Twt = (pVwt - pV0) / pRainfallIntensity;

        return Twt;

    }
    
    /**
     * Computes Psi at bedrock as in Equation (12) of reference paper
     * Note that IT_din_lin is used as  equivalent to Area/(perimeter*sin(beta))
     * @param soil_thickness
     * @param I
     * @param K
     * @param IT_din_lin_Val
     * @param pm
     * @return
     */
    public static double calculatePsiAtBedrockInPositivePressureZonesVAlue(double soil_thickness, double I, double K,
            double IT_din_lin_Val, IJGTProgressMonitor pm ) {
        
        double psi_b = - (Math.min( soil_thickness, (I/(K*3600))*(IT_din_lin_Val)) );
        return psi_b;
    }
    
    

    public static ArrayList<Integer> parseRainfallDurationsString(String pRainfallDurations, IJGTProgressMonitor pm){

        String[] rainfallDurationsSplit = pRainfallDurations.trim().split(",");
        ArrayList<Integer> rainfallDurationsArrayList = new ArrayList<Integer>();
        String durations = "";
        for( int i = 0; i < rainfallDurationsSplit.length; i++ ) {
            try {
                if (Integer.parseInt(rainfallDurationsSplit[i].trim()) > 0) {
                    durations += rainfallDurationsSplit[i].trim() + " ";
                    rainfallDurationsArrayList.add(i, (Integer) Integer.parseInt(rainfallDurationsSplit[i].trim()));
                } else {
                    if(pm!=null){pm.errorMessage("One of the provided Rainfall Durations was interpreted as zero: please check your input!");}
                    throw new ModelsIllegalargumentException(
                            "One of the provided Rainfall Durations was interpreted as zero: please check your input!", ParameterCalculationFunctions.class.getSimpleName());
                }
            } catch (NumberFormatException e) {
                if(pm!=null){pm.errorMessage("One of the provided Rainfall Durations could not be interpreted as a valid hourly duration: check your input!");}
                throw new ModelsIllegalargumentException(
                        "One of the provided Rainfall Durations could not be interpreted as a valid hourly duration: check your input!", ParameterCalculationFunctions.class.getSimpleName());
            }
        }
        if(pm!=null){pm.message("The simulations will be carried on using the following Rainfall Durationss [hours]: " + durations);}
        
        return rainfallDurationsArrayList;
    }
    
    public static ArrayList<Integer> parseReturnTimesString(String pReturnTimes, IJGTProgressMonitor pm){
        
        String[] returnTimesSplit = pReturnTimes.trim().split(",");
        ArrayList<Integer> returnTimesArrayList = new ArrayList<Integer>();
        String years = "";
        for( int i = 0; i < returnTimesSplit.length; i++ ) {
            try {
                if (Integer.parseInt(returnTimesSplit[i].trim()) > 0) {
                    years += returnTimesSplit[i].trim() + " ";
                    returnTimesArrayList.add(i, (Integer) Integer.parseInt(returnTimesSplit[i].trim()));
                } else {
                    if(pm!=null){pm.errorMessage("One of the provided Return Times was interpreted as zero: please check your input!");}
                    throw new ModelsIllegalargumentException(
                            "One of the provided Return Times was interpreted as zero: please check your input!", ParameterCalculationFunctions.class.getSimpleName());
                }
            } catch (NumberFormatException e) {
                if(pm!=null){pm.errorMessage("One of the provided Return Times could not be interpreted as a valid year: check your input!");}
                throw new ModelsIllegalargumentException(
                        "One of the provided Return Times could not be interpreted as a valid year: check your input!", ParameterCalculationFunctions.class.getSimpleName());
            }
        }
        if(pm!=null){pm.message("The calculation will be carried on for the following Retrun Times [years]: " + years);}
        
        return returnTimesArrayList;
    }

    public static void calculateFS_Initial() {
        // TODO Auto-generated method stub
        
    }
    
}
