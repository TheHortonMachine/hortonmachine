package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

public enum OptionalParameterCodes {
    MIN_DEPTH(0, "Minimum excavation depth", "[m]", Double.toString(Constants.DEFAULT_MINIMUM_DEPTH)), //
    MAX_JUNCTION(1, "Maz number of junction", "[-]", Double.toString(Constants.DEFAULT_MAX_JUNCTION)), //
    JMAX(2, "Max bisection number", "[-]", Double.toString(Constants.DEFAULT_J_MAX)), //
    EPS(3, "Accuracy", "[-]", Double.toString(Constants.DEFAULT_ACCURACY)), //
    MIN_FILL_DEGREE(4, "Minimum fill degree", "[-]", Double.toString(Constants.DEFAULT_MING)), //
    MIN_DISCHARGE(5, "Minimum discharge in a pipe", "[-]", Double.toString(Constants.DEFAULT_MIN_DISCHARGE)), //
    MAX_FILL_DEGREE(6, "Maximum fill degree", "[-]", Double.toString(Constants.DEFAULT_MAX_THETA)), //
    CELERITY_FACTOR(7, "Celerity factor", "[-]", Double.toString(Constants.DEFAULT_CELERITY_FACTOR)), //
    EXPONENT(8, "Exponent of the basin extension", "[-]", Double.toString(Constants.DEFAULT_EXPONENT)), //
    TOLERANCE(9, "tollerance", "[-]", Double.toString(Constants.DEFAULT_TOLERANCE)), //
    C(10, "base to height", "[-]", Double.toString(Constants.DEFAULT_C)), //
    GAMMA(11, "Exponent of the average ponderal slope", "[-]", Double.toString(Constants.DEFAULT_GAMMA)), //
    INFLUX_EXP(12, "Exponent of the influx coefficent", "[-]", Double.toString(Constants.DEFAULT_ESP1)), //
    FRANCO(13, "Minimum dig depth", "[-]", Double.toString(Constants.DEFAULT_FRANCO)); //

    private int code;
    private String key;
    private String description;
    private final String defaultValue;
    OptionalParameterCodes( int code, String key, String description, String defaultValue ) {
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

    public static OptionalParameterCodes forCode( int i ) {
        OptionalParameterCodes[] values = values();
        for( OptionalParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static OptionalParameterCodes forKey( String key ) {
        OptionalParameterCodes[] values = values();
        for( OptionalParameterCodes type : values ) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given key: " + key);
    }
}
