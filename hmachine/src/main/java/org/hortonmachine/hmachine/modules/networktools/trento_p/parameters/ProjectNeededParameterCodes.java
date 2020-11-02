package org.hortonmachine.hmachine.modules.networktools.trento_p.parameters;

/**
 * Needed parameters used to OmsTrentoP in project mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */

public enum ProjectNeededParameterCodes implements IParametersCode {
    A(0, "Coefficient of the pluviometric curve", "-", null, 0.0, null), //
    N(1, "Exponent of the pluviometric curve", "-", null, 0.05, 0.95), //
    TAU(2, "Tangential bottom stress", "N/m2", null, 0.0, null), //
    G(3, "Fill degree", "-", null, 0.0, 0.99), //
    ALIGN(4, "Align mode", "0 (free surface is aligned through a change in pipes depth) or 1 (aligned with bottom step).", null,
            0.0, 1.0);

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
    private final static String PROJECT_NEEDED_PAGE_NAME = "projectNeededParameters";//$NON-NLS-1$

    ProjectNeededParameterCodes( int code, String key, String unit, Number defaultValue, Double minRange, Double maxRange ) {
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

    public static ProjectNeededParameterCodes forCode( int i ) {
        ProjectNeededParameterCodes[] values = values();
        for( ProjectNeededParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static ProjectNeededParameterCodes forKey( String key ) {
        ProjectNeededParameterCodes[] values = values();
        for( ProjectNeededParameterCodes type : values ) {
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
        return PROJECT_NEEDED_PAGE_NAME;
    }

}
