package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;
/**
 * Time parameters used to TrentoP in project mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis
 *
 */
public enum ProjectTimeParameterCodes implements IParametersCode {
    STEP(0, "Time step", "Simulation duration", String.valueOf(Constants.DEFAULT_TDTP), 0.015, null), //
    MINIMUM_TIME(1, "Minimum amount Rain Time step", "Hydraulic time step [min]", String.valueOf(Constants.DEFAULT_TPMIN), 5.0,
            null), //
    MAXIMUM_TIME(2, "Maximum amount Rain Time step", "Hydraulic time step [min]", String.valueOf(Constants.DEFAULT_TPMAX), 30.0,
            null); //
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
    
    ProjectTimeParameterCodes( int code, String key, String description, String defaultValue, Double minRange, Double maxRange ) {
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
        return PROJECT_TIME_PAGE_NAME;
    }



}
