package org.hortonmachine.hmachine.modules.networktools.trento_p.parameters;

import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.CELERITY_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.C_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MINIMUM_DEPTH;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.EPS_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.EXPONENT_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.GAMMA_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.INFLUX_EXPONENT_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.JMAX_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.MAX_JUNCTIONS_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.MIN_DEPTH_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.MIN_DISCHARGE_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.MIN_FILL_DEGREE_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.THETA_RANGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.TOLERANCE_RANGE;

import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;
/**
 * Optional parameters used to OmsTrentoP in project mode.
 * <p>
 * It specify a key and a description, that can be used to build a GUI, and the default value,if it exist, and the range.
 * </p>
 * 
 * @author Daniele Andreis (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */

public enum ProjectOptionalParameterCodes implements IParametersCode {
    MIN_DEPTH(0, "Minimum excavation depth", "m", DEFAULT_MINIMUM_DEPTH, MIN_DEPTH_RANGE[0], MIN_DEPTH_RANGE[1]), //
    MAX_JUNCTION(1, "Max number of junction", "-", Constants.DEFAULT_MAX_JUNCTION, MAX_JUNCTIONS_RANGE[0],
            MAX_JUNCTIONS_RANGE[1]), //
    JMAX(2, "Max bisection number", "-", Constants.DEFAULT_J_MAX, JMAX_RANGE[0], JMAX_RANGE[1]), //
    EPS(3, "Precision", "-", Constants.DEFAULT_EPSILON, EPS_RANGE[0], EPS_RANGE[1]), //
    MIN_FILL_DEGREE(4, "Minimum fill degree", "-", Constants.DEFAULT_MING, MIN_FILL_DEGREE_RANGE[0], MIN_FILL_DEGREE_RANGE[1]), //
    MIN_DISCHARGE(5, "Minimum discharge in a pipe", "l/s", Constants.DEFAULT_MIN_DISCHARGE, MIN_DISCHARGE_RANGE[0],
            MIN_DISCHARGE_RANGE[1]), //
    MAX_FILL_DEGREE(6, "Maximum fill degree", "-", Constants.DEFAULT_MAX_THETA, THETA_RANGE[0], THETA_RANGE[1]), //
    CELERITY_FACTOR(7, "Celerity factor", "-", Constants.DEFAULT_CELERITY_FACTOR, CELERITY_RANGE[0], CELERITY_RANGE[1]), //
    EXPONENT(8, "Exponent of the basin extension", "-", Constants.DEFAULT_EXPONENT, EXPONENT_RANGE[0], EXPONENT_RANGE[1]), //
    TOLERANCE(9, "Tolerance", "-", Constants.DEFAULT_TOLERANCE, TOLERANCE_RANGE[0], TOLERANCE_RANGE[1]), //
    C(10, "Base to height", "-", Constants.DEFAULT_C, C_RANGE[0], C_RANGE[1]), //
    GAMMA(11, "Exponent of the average ponderal slope", "-", Constants.DEFAULT_GAMMA, GAMMA_RANGE[0], GAMMA_RANGE[1]), //
    INFLUX_EXP(12, "Exponent of the influx coefficent", "-", Constants.DEFAULT_ESP1, INFLUX_EXPONENT_RANGE[0],
            INFLUX_EXPONENT_RANGE[1]), //
    ACCURACY(13, "Accuracy (bisection mode)", "-", Constants.DEFAULT_ACCURACY, 0.0, null), //
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
     * The unit
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

    ProjectOptionalParameterCodes( int code, String key, String unit, Number defaultValue, Number minRange, Number maxRange ) {
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
