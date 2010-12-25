package org.jgrasstools.hortonmachine.modules.networkmanagement.core;

import java.util.ArrayList;
import java.util.List;

/**  
 * Data contained in the following matrix. 
 * <p>
 * Number is column number,
 * Basin is Mulinu as in Sistemi di fognatura, manuale di progettazione,
 * Artina et al., Milano, 1997.
 * <p>
 * Warning: It assumed that topography curvature is neglible !
 */
public class SewerData {
    /**
     * state number
     */
    public int stateNum;
    /**
     * state into which the state drains
     */
    public int stateDrain;
    /**
     * area relative to the state (ha = 10^{-2} km^2)
     */
    public double stateDrainArea;
    /**
     * length of the pipe to be design (m)
     */
    public double pipeLength;
    /**
     * initial elevation of the terrain (m above the sea level)
     */
    public double initElev;
    /**
     * final elevation of the terrain (m above the sea level)
     */
    public double finalElev;
    /**
     * urban runoff coefficient
     */
    public double runoffCoeff;
    /**
     * The average residence time of water outside the 
     * network, per unit area, for a given run-off coefficient 
     * and average state slope.
     */
    public double alpha;
    /**
     * Gauckler-Strickler coefficient
     */
    public double gsCoeff;
    /**
     * Minimum pipe slope (%)
     */
    public double minSope;
    /**
     * Pipe section type: 1=circular, 2=rectangular, 3=trapezoidal 
     */
    public int pipeType;
    /**
     * Average state slope computed as the weighted mean of the state elevation. 
     * The weights are the areas at the same height within the state.
     */
    public double avgStateSlope;

    public SewerData( int stateNum, int stateDrain, double stateDrainArea, double pipeLength, double initElev, double finalElev,
            double runoffCoeff, double alpha, double gsCoeff, double minSope, int pipeType, double avgStateSlope ) {
        this.stateNum = stateNum;
        this.stateDrain = stateDrain;
        this.stateDrainArea = stateDrainArea;
        this.pipeLength = pipeLength;
        this.initElev = initElev;
        this.finalElev = finalElev;
        this.runoffCoeff = runoffCoeff;
        this.alpha = alpha;
        this.gsCoeff = gsCoeff;
        this.minSope = minSope;
        this.pipeType = pipeType;
        this.avgStateSlope = avgStateSlope;
    }

    public static List<SewerData> demoData() {
        List<SewerData> data = new ArrayList<SewerData>();
        SewerData tmp = new SewerData(1, 4, 0.4493, 103, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(2, 3, 0.2682, 103, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(3, 4, 0.4980, 35, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(4, 9, 0.6202, 67, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(5, 6, 0.6219, 102, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(6, 7, 0.6121, 150, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(7, 9, 1.0577, 33, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(8, 9, 1.4793, 118, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(9, 10, 0.3423, 210, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(10, 11, 2.1491, 164, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(11, 12, 1.2346, 70, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(12, 16, 0.8947, 90, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(13, 14, 0.5502, 78, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(14, 15, 1.0411, 201, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(15, 16, 1.5212, 18, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);
        tmp = new SewerData(16, 0, 0.0010, 199, 0, 0, 0.315, 0.7, 70, 0.1, 1, 1);
        data.add(tmp);

        return data;
    }
}
