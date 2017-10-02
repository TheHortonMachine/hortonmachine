package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.*;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Utility.*;

/**
 * Optional parameters used to OmsTrentoP in calibration mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis
 *
 */

public enum CalibrationOptionalParameterCodes implements IParametersCode {
    JMAX(0, "Max bisection number", "It's used inside the program to solve some root equation [-]", F.format(DEFAULT_J_MAX),
            JMAX_RANGE[0], JMAX_RANGE[1]), //
    EPS(1, "Precision", "It's related to the discharge evalutation [-]", F.format(DEFAULT_ACCURACY), EPS_RANGE[0], EPS_RANGE[1]), //
    MAX_FILL_DEGREE(2, "Maximum fill degree", "[-]", F.format(DEFAULT_MAX_THETA), THETA_RANGE[0], THETA_RANGE[1]), //
    CELERITY_FACTOR(3, "Celerity factor", "[-]", F.format(DEFAULT_CELERITY_FACTOR), CELERITY_RANGE[0], CELERITY_RANGE[1]), // //$NON-NLS-1$
    TOLERANCE(4, "tollerance", "used to find the delay.[-]", F.format(DEFAULT_TOLERANCE), TOLERANCE_RANGE[0], TOLERANCE_RANGE[1]), //
    GAMMA(5, "Exponent of the average ponderal slope",
            "It's used to evaluate the mean time to avvess to the network, value between 0.2 and 0.5  [-]", F
                    .format(DEFAULT_GAMMA), GAMMA_RANGE[0], GAMMA_RANGE[1]), //
    INFLUX_EXP(6, "Exponent of the influx coefficent", "Used to evaluate the mean residence time[-]", F.format(DEFAULT_ESP1),
            INFLUX_EXPONENT_RANGE[0], INFLUX_EXPONENT_RANGE[1]), //
    EXPONENT(7, "Exponent of the basin extension",
            "It's used to evaluate the mean time to avvess to the network, usually is 0.3 [-]", F.format(DEFAULT_EXPONENT),
            EXPONENT_RANGE[0], EXPONENT_RANGE[1]), //
    ACCURACY(8, "Accuracy", "Used to evaluate the result with bisection mode ", null, new Double(0), null); //$NON-NLS-1$

    /**
     * The name of the WizardPage.
     */
    private final static String CALIBRATION_OPTIONAL_PAGE_NAME = "calibrationOptionalParameters";//$NON-NLS-1$
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

    CalibrationOptionalParameterCodes( int code, String key, String description, String defaultValue, Number minRange,
            Number maxRange ) {
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

    public static CalibrationOptionalParameterCodes forCode( int i ) {
        CalibrationOptionalParameterCodes[] values = values();
        for( CalibrationOptionalParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static CalibrationOptionalParameterCodes forKey( String key ) {
        CalibrationOptionalParameterCodes[] values = values();
        for( CalibrationOptionalParameterCodes type : values ) {
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
        return CALIBRATION_OPTIONAL_PAGE_NAME;
    }

}
