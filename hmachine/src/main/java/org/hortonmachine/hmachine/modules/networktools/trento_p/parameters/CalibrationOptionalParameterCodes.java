package org.hortonmachine.hmachine.modules.networktools.trento_p.parameters;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.CELERITY_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.EPS_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.EXPONENT_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.GAMMA_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.INFLUX_EXPONENT_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.JMAX_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.THETA_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.TOLERANCE_RANGE;

import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;

/**
 * Optional parameters used to OmsTrentoP in calibration mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */

public enum CalibrationOptionalParameterCodes implements IParametersCode {
    JMAX(0, "Max bisection number", "-", Constants.DEFAULT_J_MAX, JMAX_RANGE[0], JMAX_RANGE[1]), //
    EPS(1, "Precision", "-", Constants.DEFAULT_EPSILON, EPS_RANGE[0], EPS_RANGE[1]), //
    MAX_FILL_DEGREE(2, "Maximum fill degree", "-", Constants.DEFAULT_MAX_THETA, THETA_RANGE[0], THETA_RANGE[1]), //
    CELERITY_FACTOR(3, "Celerity factor", "-", Constants.DEFAULT_CELERITY_FACTOR, CELERITY_RANGE[0], CELERITY_RANGE[1]), //
    TOLERANCE(4, "Tolerance", "-", Constants.DEFAULT_TOLERANCE, TOLERANCE_RANGE[0], TOLERANCE_RANGE[1]), //
    GAMMA(5, "Exponent of the average ponderal slope", "-", Constants.DEFAULT_GAMMA, GAMMA_RANGE[0], GAMMA_RANGE[1]), //
    INFLUX_EXP(6, "Exponent of the influx coefficent", "-", Constants.DEFAULT_ESP1, INFLUX_EXPONENT_RANGE[0],
            INFLUX_EXPONENT_RANGE[1]), //
    EXPONENT(7, "Exponent of the basin extension", "-", Constants.DEFAULT_EXPONENT, EXPONENT_RANGE[0], EXPONENT_RANGE[1]), //
    ACCURACY(8, "Accuracy (bisection mode)", "-", Constants.DEFAULT_ACCURACY, 0.0, null), //
    A(9, "Coefficient of the pluviometric curve", "-", null, 0.0, null), //
    N(10, "Exponent of the pluviometric curve", "-", null, 0.05, 0.95); //

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
    private String unit;
    /**
     * The default value of this parameter.
     */
    private final Number defaultValue;
    /**
     * Minimum value that the parameter can be.
     */
    private final Number minRange;
    /**
    * Maximum value that the parameter can be.
    */
    private final Number maxRange;

    CalibrationOptionalParameterCodes( int code, String key, String unit, Number defaultValue, Number minRange,
            Number maxRange ) {
        this.code = code;
        this.key = key;
        this.unit = unit;
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

    public String getUnit() {
        return unit;
    }

    public Number getDefaultValue() {
        return defaultValue;
    }

    public boolean isInRange( Number value ) {
        if (minRange != null && value.doubleValue() < minRange.doubleValue()) {
            return false;
        } else if (maxRange != null && value.doubleValue() > maxRange.doubleValue()) {
            return false;
        } else {
            return true;
        }
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
