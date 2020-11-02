package org.hortonmachine.hmachine.modules.networktools.trento_p.parameters;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;

public enum CalibrationTimeParameterCodes implements IParametersCode {
    STEP(0, "Hydraulic time step", "min", Constants.DEFAULT_DT, 0.0, null), //
    MAXIMUM_TIME(1, "Maximum simulation time step", "min", Constants.DEFAULT_TPMAX, 0.0, null), //
    MAXIMUM_RAIN_TIME(2, "Maximum Rain Time step", "min", Constants.DEFAULT_TPMAX, 0.0, null); //
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
    private String unit;
    /**
     * The default value of this parameter.
     */
    private final Number defaultValue;
    /**
     * Minimum value that the parameter can be.
     */
    private final Number minRange;
    /**
     * Maximum value that the parameter can be.
     */
    private final Number maxRange;

    /**
     * The name of the WizardPage.
     */
    private final static String CALIBRATION_TIME_PAGE_NAME = "timeCalibrationParameters";//$NON-NLS-1$

    CalibrationTimeParameterCodes( int code, String key, String unit, Number defaultValue, Double minRange,
            Double maxRange ) {
        this.code = code;
        this.key = key;
        this.unit = unit;
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

    public String getUnit() {
        return unit;
    }

    public Number getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Number getMinRange() {
        return minRange;
    }

    @Override
    public Number getMaxRange() {
        return maxRange;
    }
    
    public boolean isInRange( Number value ) {
        if (minRange != null && value.doubleValue() < minRange.doubleValue()) {
            return false;
        } else if (maxRange != null && value.doubleValue() > maxRange.doubleValue()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String getPageName() {
        return CALIBRATION_TIME_PAGE_NAME;
    }

}
