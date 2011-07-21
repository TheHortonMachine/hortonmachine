package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.*;
/**
 * Optional parameters used to TrentoP in project mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis
 *
 */
public enum ProjectOptionalParameterCodes implements IParametersCode {
    MIN_DEPTH(0, "Minimum excavation depth", "It's the minimum depth of a pipe [m]", String.valueOf(DEFAULT_MINIMUM_DEPTH),
            MIN_DEPTH_RANGE[0], MIN_DEPTH_RANGE[1]), //
    MAX_JUNCTION(1, "Max number of junction", "It's the maximum number of junction in a node[-]", String
            .valueOf(Constants.DEFAULT_MAX_JUNCTION), MAX_JUNCTIONS_RANGE[0], MAX_JUNCTIONS_RANGE[1]), //
    JMAX(2, "Max bisection number", "used to find the diameter of pipes. [-]", String.valueOf(Constants.DEFAULT_J_MAX),
            JMAX_RANGE[0], JMAX_RANGE[1]), //
    EPS(3, "Precision", "It's related to the discharge evalutation [-]", String.valueOf(Constants.DEFAULT_ACCURACY),
            EPS_RANGE[0], EPS_RANGE[1]), //
    MIN_FILL_DEGREE(4, "Minimum fill degree", "[-]", String.valueOf(Constants.DEFAULT_MING), MIN_FILL_DEGREE_RANGE[0],
            MIN_FILL_DEGREE_RANGE[1]), //
    MIN_DISCHARGE(5, "Minimum discharge in a pipe", "[l/s]", String.valueOf(Constants.DEFAULT_MIN_DISCHARGE),
            MIN_DISCHARGE_RANGE[0], MIN_DISCHARGE_RANGE[1]), //
    MAX_FILL_DEGREE(6, "Maximum fill degree", "[-]", String.valueOf(Constants.DEFAULT_MAX_THETA), THETA_RANGE[0], THETA_RANGE[1]), //
    CELERITY_FACTOR(7, "Celerity factor", "[-]", String.valueOf(Constants.DEFAULT_CELERITY_FACTOR), CELERITY_RANGE[0],
            CELERITY_RANGE[1]), //
    EXPONENT(8, "Exponent of the basin extension", "usually is 0.3[-]", String.valueOf(Constants.DEFAULT_EXPONENT),
            EXPONENT_RANGE[0], EXPONENT_RANGE[1]), //
    TOLERANCE(9, "tollerance", "used to find the pipes diameter[-]", String.valueOf(Constants.DEFAULT_TOLERANCE),
            TOLERANCE_RANGE[0], TOLERANCE_RANGE[1]), //
    C(10, "base to height", "It's used only in rectangular or trapezium section, value between o.5 and 2 [-]", String
            .valueOf(Constants.DEFAULT_C), C_RANGE[0], C_RANGE[1]), //
    GAMMA(11, "Exponent of the average ponderal slope",
            "It's used to evaluate the mean time to avvess to the network,, value between 0.2 and 0.5  [-]", String
                    .valueOf(Constants.DEFAULT_GAMMA), GAMMA_RANGE[0], GAMMA_RANGE[1]), //
    INFLUX_EXP(12, "Exponent of the influx coefficent", "Used to evaluate the mean residence time[-]", String
            .valueOf(Constants.DEFAULT_ESP1), INFLUX_EXPONENT_RANGE[0], INFLUX_EXPONENT_RANGE[1]), //
    ACCURACY(13, "Accuracy", "Used to evaluate the result with bisection mode  ", String.valueOf(Constants.DEFAULT_ACCURACY),
            new Double(0), null), //
    ; //
    /**
     * The name of the WizardPage.
     */
    private final static String PROJECT_OPTIONAL_PAGE_NAME = "projectOptionalParameters";//$NON-NLS-1$
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
    private final Number minRange;
    /**
     * Maximum value that the parameter can be.
     */
    private final Number maxRange;

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

}
