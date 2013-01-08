package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;
/**
 * Needed parameters used to OmsTrentoP in project mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis
 *
 */

public enum ProjectNeededParameterCodes implements IParametersCode {
    A(0, "Coefficient of the pluviometric curve", " ", null, new Double(0), null), //
    N(1, "Exponent of the pluviometric curve", " ", null, new Double(0), null), //
    TAU(2, "Tangential bottom stress", " ", null, new Double(0), null), //
    G(3, "Fill degree", " ", null, new Double(0), null), //
    ALIGN(4, "Align mode", " ", null, null, null);

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
    private final static String PROJECT_NEEDED_PAGE_NAME = "projectNeededParameters";//$NON-NLS-1$

    ProjectNeededParameterCodes( int code, String key, String description, String defaultValue, Double minRange, Double maxRange ) {
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
        return PROJECT_NEEDED_PAGE_NAME;
    }

}
