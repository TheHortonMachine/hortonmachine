package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;
/**
 * Needed parameters used to TrentoP in calibration mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis
 *
 */
public enum CalibrationNeededParameterCodes implements IParametersCode {
    ACCURACY(0, "Accuracy", "Used to evaluate the result with bisection mode ", null, new Double(0), null); //$NON-NLS-1$
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
    private final static String CALIBRATION_NEEDED_PAGE_NAME = "calibrationNeededParameters";//$NON-NLS-1$

    CalibrationNeededParameterCodes( int code, String key, String description, String defaultValue, Double minRange,
            Double maxRange ) {
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

    public static CalibrationNeededParameterCodes forCode( int i ) {
        CalibrationNeededParameterCodes[] values = values();
        for( CalibrationNeededParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static CalibrationNeededParameterCodes forKey( String key ) {
        CalibrationNeededParameterCodes[] values = values();
        for( CalibrationNeededParameterCodes type : values ) {
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
        return CALIBRATION_NEEDED_PAGE_NAME;
    }

}
