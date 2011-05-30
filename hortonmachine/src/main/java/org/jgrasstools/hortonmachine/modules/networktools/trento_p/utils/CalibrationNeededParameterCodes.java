package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

public enum CalibrationNeededParameterCodes implements IParametersCode {
    ACCURACY(0, "Accuracy to use to calculate a solution", " ", null, null, null);  //$NON-NLS-1$

    private int code;
    private String key;
    private String description;
    private final String defaultValue;
    private final Double minRange;
    private final Double maxRange;
    private final static String  CALIBRATION_NEEDED_PAGE_NAME = "Calibration parameters";//$NON-NLS-1$
    private final static String  CALIBRATION_NEEDED_PAGE_TITLE = "Mandatory parameters in calibration mode";//$NON-NLS-1$
    private final static String  CALIBRATION_NEEDED_PAGE_DESCRIPTION = "This fields have to be setted to calibrate";//$NON-NLS-1$

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
    public  String getPageName() {
        // TODO Auto-generated method stub
        return CALIBRATION_NEEDED_PAGE_NAME;
    }

    @Override
    public String getPageTitle() {
        // TODO Auto-generated method stub
        return CALIBRATION_NEEDED_PAGE_TITLE;
    }

    @Override
    public String getPageDescription() {
        // TODO Auto-generated method stub
        return CALIBRATION_NEEDED_PAGE_DESCRIPTION;
    }
}
