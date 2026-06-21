package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.curvefitter;

import java.util.Objects;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator;
import org.apache.commons.math3.linear.RealVector;

public class KrigingParamValidator implements ParameterValidator {

    private final double[] lowerBounds_;
    private final double[] upperBounds_;

    public KrigingParamValidator(double[] lowerBounds, double[] upperBounds) {
        Objects.requireNonNull(lowerBounds, "lowerBounds");
        Objects.requireNonNull(upperBounds, "upperBounds");

        if (lowerBounds.length != 3) {
            throw new DimensionMismatchException(lowerBounds.length, 3);
        }
        if (upperBounds.length != 3) {
            throw new DimensionMismatchException(upperBounds.length, 3);
        }

        this.lowerBounds_ = lowerBounds.clone();
        this.upperBounds_ = upperBounds.clone();

        for (int i = 0; i < 3; i++) {
            if (this.lowerBounds_[i] > this.upperBounds_[i]) {
                throw new IllegalArgumentException("lowerBounds[" + i + "] > upperBounds[" + i + "]");
            }
        }
    }

    @Override
    public RealVector validate(RealVector params) {
        if (params.getDimension() != 3) {
            throw new DimensionMismatchException(params.getDimension(), 3);
        }

        RealVector n = params.copy();
        for (int i = 0; i < 3; i++) {
            double v = n.getEntry(i);

            if (Double.isNaN(v)) {
                v = lowerBounds_[i]; // fallback safe
            }

            if (v < lowerBounds_[i]) v = lowerBounds_[i];
            if (v > upperBounds_[i]) v = upperBounds_[i];

            n.setEntry(i, v);
        }
        return n;
    }
}
