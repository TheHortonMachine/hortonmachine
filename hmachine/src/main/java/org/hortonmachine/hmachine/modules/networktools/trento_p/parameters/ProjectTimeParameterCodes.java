package org.hortonmachine.hmachine.modules.networktools.trento_p.parameters;

import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;

/**
 * Time parameters used to OmsTrentoP in project mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public enum ProjectTimeParameterCodes implements IParametersCode {
    STEP(0, "Time step for simulation", "min", Constants.DEFAULT_TDTP, 0.015, null), //
    MINIMUM_TIME(1, "Minimum Rain Time step", "min", Constants.DEFAULT_TPMIN, 5.0, null), //
    MAXIMUM_TIME(2, "Maximum Rain Time step", "min", Constants.DEFAULT_TPMAX, 30.0, null); //
    /**
     * The name of the WizardPage.
     */
    private final static String PROJECT_TIME_PAGE_NAME = "timeProjectParameters";//$NON-NLS-1$
    /**
     * An id associate to the value. 
     */
    private int code;
    /**
     * The key (used as label in a GUI).
     */
    private String key;
    /**
     * The unit of the parameter 
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

    ProjectTimeParameterCodes( int code, String key, String unit, Number defaultValue, Number minRange, Number maxRange ) {
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
    
    public boolean isInRange( Number value ) {
        if (minRange != null && value.doubleValue() < minRange.doubleValue()) {
            return false;
        } else if (maxRange != null && value.doubleValue() > maxRange.doubleValue()) {
            return false;
        } else {
            return true;
        }
    }

    public static ProjectTimeParameterCodes forCode( int i ) {
        ProjectTimeParameterCodes[] values = values();
        for( ProjectTimeParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static ProjectTimeParameterCodes forKey( String key ) {
        ProjectTimeParameterCodes[] values = values();
        for( ProjectTimeParameterCodes type : values ) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given key: " + key);
    }

    @Override
    public Number getMinRange() {
        return minRange;
    }

    @Override
    public Number getMaxRange() {
        return maxRange;
    }

    @Override
    public String getPageName() {
        return PROJECT_TIME_PAGE_NAME;
    }

}
