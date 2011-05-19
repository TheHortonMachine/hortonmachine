package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

public enum CalibrationTimeParameterCodes {
    STEP(0, "Time step", "Simulation duration [min]",  Double.toString(Constants.DEFAULT_DT)), //
    MAXIMUM_TIME(2, "Maximum amount Rain Time step", "Hydraulic time step [min]", Double.toString(Constants.DEFAULT_TPMAX)); //

    private int code;
    private String key;
    private String description;
    private final String defaultValue;
    CalibrationTimeParameterCodes( int code, String key, String description, String defaultValue ) {
        this.code = code;
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
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

    public static CalibrationTimeParameterCodes forCode( int i ) {
        CalibrationTimeParameterCodes[] values = values();
        for( CalibrationTimeParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static CalibrationTimeParameterCodes forKey( String key ) {
        CalibrationTimeParameterCodes[] values = values();
        for( CalibrationTimeParameterCodes type : values ) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given key: " + key);
    }
}
