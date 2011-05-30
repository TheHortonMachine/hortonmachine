package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

public enum CalibrationTimeParameterCodes implements IParametersCode {
    STEP(0, "Time step", "Simulation duration [min]", Double.toString(Constants.DEFAULT_DT), 0.0, null), //
    MAXIMUM_TIME(1, "Maximum amount Rain Time step", "Hydraulic time step [min]", Double.toString(Constants.DEFAULT_TPMAX), 0.0,
            null); //

    private int code;
    private String key;
    private String description;
    private final String defaultValue;
    private final Double minRange;
    private final Double maxRange;
    private final static String CALIBRATION_TIME_PAGE_NAME = "Calibration Time parameters";//$NON-NLS-1$
    private final static String CALIBRATION_TIME_PAGE_TITLE = "Time parameters in calibration mode";//$NON-NLS-1$
    private final static String CALIBRATION_TIME_PAGE_DESCRIPTION = "This field could not be setted to calibrate";//$NON-NLS-1$

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

    @Override
    public String getPageTitle() {
        // TODO Auto-generated method stub
        return CALIBRATION_TIME_PAGE_TITLE;
    }

    @Override
    public String getPageDescription() {
        // TODO Auto-generated method stub
        return CALIBRATION_TIME_PAGE_DESCRIPTION;
    }

}
