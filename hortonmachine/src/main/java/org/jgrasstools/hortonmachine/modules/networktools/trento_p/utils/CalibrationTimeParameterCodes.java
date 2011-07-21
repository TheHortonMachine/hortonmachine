package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;
/**
 * Optional parameters used to TrentoP in calibration mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis
 *
 */
public enum CalibrationTimeParameterCodes implements IParametersCode {
    STEP(0, "Time step", "Hydraulic time step [min]", String.valueOf(Constants.DEFAULT_DT), 0.0, null), //
    MAXIMUM_TIME(1, "Maximum amount of simulation time step","Simulation duration [min]" , String.valueOf(Constants.DEFAULT_TPMAX), 0.0,
            null), //
    MAXIMUM_RAIN_TIME(2, "Maximum amount Rain Time step","Simulation duration [min]" , String.valueOf(Constants.DEFAULT_TPMAX), 0.0,
            null); //
    /**
     * An id associate to the value. 
     */
    private int code;
    /**
     * The key (used as label in a GUI).
     */
    private String key;
    /**
     * The description of the parameter (used as a tip in a gui)
     */
    private String description;
    /**
     * The default value of this parameter.
     */
    private final String defaultValue;
    /**
     * Minimum value that the parameter can be.
     */
    private final Double minRange;
    /**
     * Maximum value that the parameter can be.
     */
    private final Double maxRange;  
    
    /**
     * The name of the WizardPage.
     */
    private final static String CALIBRATION_TIME_PAGE_NAME = "timeCalibrationParameters";//$NON-NLS-1$

    CalibrationTimeParameterCodes( int code, String key, String description, String defaultValue, Double minRange, Double maxRange ) {
        this.code = code;
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.minRange = minRange;
        this.maxRange = maxRange;
    }

    public int getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Double getMinRange() {
        // TODO Auto-generated method stub
        return minRange;
    }

    @Override
    public Double getMaxRange() {
        // TODO Auto-generated method stub
        return maxRange;
    }

    @Override
    public String getPageName() {
        // TODO Auto-generated method stub
        return CALIBRATION_TIME_PAGE_NAME;
    }



}
