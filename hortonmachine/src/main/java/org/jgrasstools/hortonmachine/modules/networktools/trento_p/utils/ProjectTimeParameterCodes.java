package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

public enum ProjectTimeParameterCodes implements IParametersCode {
    STEP(0, "Time step", "Simulation duration [min]", Double.toString(Constants.DEFAULT_TDTP), 0.015, null), //
    MINIMUM_TIME(1, "Minimum amount Rain Time step", "Hydraulic time step [min]", Double.toString(Constants.DEFAULT_TPMIN), 5.0,
            null), //
    MAXIMUM_TIME(2, "Maximum amount Rain Time step", "Hydraulic time step [min]", Double.toString(Constants.DEFAULT_TPMAX), 30.0,
            null); //

    private final static String PROJECT_TIME_PAGE_NAME = "Time project  parameters";//$NON-NLS-1$
    private final static String PROJECT_TIME_PAGE_TITLE = "time parameters in calibration mode";//$NON-NLS-1$
    private final static String PROJECT_TIME_PAGE_DESCRIPTION = "This field could not be setted to calibrate";//$NON-NLS-1$

    private int code;
    private String key;
    private String description;
    private final String defaultValue;
    private final Double minRange;
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

    @Override
    public String getPageTitle() {
        // TODO Auto-generated method stub
        return PROJECT_TIME_PAGE_TITLE;
    }

    @Override
    public String getPageDescription() {
        // TODO Auto-generated method stub
        return PROJECT_TIME_PAGE_DESCRIPTION;
    }

}
