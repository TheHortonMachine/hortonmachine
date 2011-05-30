package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.*;

public enum CalibrationOptionalParameterCodes implements IParametersCode{
    JMAX(0, "Max bisection number", "[-]", Double.toString(Constants.DEFAULT_J_MAX),JMAX_RANGE[0],JMAX_RANGE[1]), //
    EPS(1, "Accuracy", "[-]", Double.toString(Constants.DEFAULT_ACCURACY),EPS_RANGE[0],EPS_RANGE[1]), //
    MAX_FILL_DEGREE(2, "Maximum fill degree", "[-]", Double.toString(Constants.DEFAULT_MAX_THETA),THETA_RANGE[0],THETA_RANGE[1]), //
    CELERITY_FACTOR(3, "Celerity factor", "[-]", Double.toString(DEFAULT_CELERITY_FACTOR),CELERITY_RANGE[0],CELERITY_RANGE[1]),// //$NON-NLS-1$
    TOLERANCE(4, "tollerance", "[-]", Double.toString(Constants.DEFAULT_TOLERANCE),TOLERANCE_RANGE[0],TOLERANCE_RANGE[1]), //
    GAMMA(5, "Exponent of the average ponderal slope", "[-]", Double.toString(Constants.DEFAULT_GAMMA),GAMMA_RANGE[0],GAMMA_RANGE[1]), //
    INFLUX_EXP(6, "Exponent of the influx coefficent", "[-]", Double.toString(Constants.DEFAULT_ESP1),INFLUX_EXPONENT_RANGE[0],INFLUX_EXPONENT_RANGE[1]), //
    EXPONENT(7, "Exponent of the basin extension", "[-]", Double.toString(Constants.DEFAULT_EXPONENT),EXPONENT_RANGE[0],EXPONENT_RANGE[1]); //
  
    private final static String  CALIBRATION_OPTIONAL_PAGE_NAME = "Calibration optional parameters";//$NON-NLS-1$
    private final static String  CALIBRATION_OPTIONAL_PAGE_TITLE = "Optional parameters in calibration mode";//$NON-NLS-1$
    private final static String  CALIBRATION_OPTIONAL_PAGE_DESCRIPTION = "This field could not be setted to calibrate";//$NON-NLS-1$
    private int code;
    private String key;
    private String description;
    private final String defaultValue;
    private final Double minRange;
    private final Double maxRange;
    
    CalibrationOptionalParameterCodes( int code, String key, String description, String defaultValue, Double minRange, Double maxRange ) {
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

    public static CalibrationOptionalParameterCodes forCode( int i ) {
        CalibrationOptionalParameterCodes[] values = values();
        for( CalibrationOptionalParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static CalibrationOptionalParameterCodes forKey( String key ) {
        CalibrationOptionalParameterCodes[] values = values();
        for( CalibrationOptionalParameterCodes type : values ) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given key: " + key);
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
        return CALIBRATION_OPTIONAL_PAGE_NAME;
    }

    @Override
    public String getPageTitle() {
        // TODO Auto-generated method stub
        return CALIBRATION_OPTIONAL_PAGE_TITLE;
    }

    @Override
    public String getPageDescription() {
        // TODO Auto-generated method stub
        return CALIBRATION_OPTIONAL_PAGE_DESCRIPTION;
    }
}
