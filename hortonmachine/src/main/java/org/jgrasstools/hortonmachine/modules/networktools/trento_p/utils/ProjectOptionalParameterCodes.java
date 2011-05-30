package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.*;

public enum ProjectOptionalParameterCodes implements IParametersCode {
    MIN_DEPTH(0, "Minimum excavation depth", "[m]", Double.toString(DEFAULT_MINIMUM_DEPTH), MIN_DEPTH_RANGE[0],
            MIN_DEPTH_RANGE[1]), //
    MAX_JUNCTION(1, "Maz number of junction", "[-]", Integer.toString(Constants.DEFAULT_MAX_JUNCTION), MAX_JUNCTIONS_RANGE[0],
            MAX_JUNCTIONS_RANGE[1]), //
    JMAX(2, "Max bisection number", "[-]", Integer.toString(Constants.DEFAULT_J_MAX), JMAX_RANGE[0], JMAX_RANGE[1]), //
    EPS(3, "Accuracy", "[-]", Double.toString(Constants.DEFAULT_ACCURACY), EPS_RANGE[0], EPS_RANGE[1]), //
    MIN_FILL_DEGREE(4, "Minimum fill degree", "[-]", Double.toString(Constants.DEFAULT_MING), MIN_FILL_DEGREE_RANGE[0],
            MIN_FILL_DEGREE_RANGE[1]), //
    MIN_DISCHARGE(5, "Minimum discharge in a pipe", "[-]", Double.toString(Constants.DEFAULT_MIN_DISCHARGE),
            MIN_DISCHARGE_RANGE[0], MIN_DISCHARGE_RANGE[1]), //
    MAX_FILL_DEGREE(6, "Maximum fill degree", "[-]", Double.toString(Constants.DEFAULT_MAX_THETA), THETA_RANGE[0], THETA_RANGE[1]), //
    CELERITY_FACTOR(7, "Celerity factor", "[-]", Double.toString(Constants.DEFAULT_CELERITY_FACTOR), CELERITY_RANGE[0],
            CELERITY_RANGE[1]), //
    EXPONENT(8, "Exponent of the basin extension", "[-]", Double.toString(Constants.DEFAULT_EXPONENT), EXPONENT_RANGE[0],
            EXPONENT_RANGE[1]), //
    TOLERANCE(9, "tollerance", "[-]", Double.toString(Constants.DEFAULT_TOLERANCE), TOLERANCE_RANGE[0], TOLERANCE_RANGE[1]), //
    C(10, "base to height", "[-]", Double.toString(Constants.DEFAULT_C), C_RANGE[0], C_RANGE[1]), //
    GAMMA(11, "Exponent of the average ponderal slope", "[-]", Double.toString(Constants.DEFAULT_GAMMA), GAMMA_RANGE[0],
            GAMMA_RANGE[1]), //
    INFLUX_EXP(12, "Exponent of the influx coefficent", "[-]", Double.toString(Constants.DEFAULT_ESP1), INFLUX_EXPONENT_RANGE[0],
            INFLUX_EXPONENT_RANGE[1]); //

    private int code;
    private String key;
    private String description;
    private final String defaultValue;
    private final Number minRange;
    private final Number maxRange;

    private final static String PROJECT_OPTIONAL_PAGE_NAME = "project optional parameters";//$NON-NLS-1$
    private final static String PROJECT_OPTIONAL_PAGE_TITLE = "optional parameters in project mode";//$NON-NLS-1$
    private final static String PROJECT_OPTIONAL_PAGE_DESCRIPTION = "This field could not  be setted  to project";//$NON-NLS-1$

    ProjectOptionalParameterCodes( int code, String key, String description, String defaultValue, Number minRange, Number maxRange ) {
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

    public static ProjectOptionalParameterCodes forCode( int i ) {
        ProjectOptionalParameterCodes[] values = values();
        for( ProjectOptionalParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static ProjectOptionalParameterCodes forKey( String key ) {
        ProjectOptionalParameterCodes[] values = values();
        for( ProjectOptionalParameterCodes type : values ) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given key: " + key);
    }

    @Override
    public Number getMinRange() {
        // TODO Auto-generated method stub
        return minRange;
    }

    @Override
    public Number getMaxRange() {
        // TODO Auto-generated method stub
        return maxRange;
    }

    @Override
    public String getPageName() {
        // TODO Auto-generated method stub
        return PROJECT_OPTIONAL_PAGE_NAME;
    }

    @Override
    public String getPageTitle() {
        // TODO Auto-generated method stub
        return PROJECT_OPTIONAL_PAGE_TITLE;
    }

    @Override
    public String getPageDescription() {
        // TODO Auto-generated method stub
        return PROJECT_OPTIONAL_PAGE_DESCRIPTION;
    }
}
