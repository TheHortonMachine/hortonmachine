package org.jgrasstools.gears.io.disktree.jtstmp;

import java.io.Serializable;

/**
 * Provides a test for whether an interval is
 * so small it should be considered as zero for the purposes of
 * inserting it into a binary tree.
 * The reason this check is necessary is that round-off error can
 * cause the algorithm used to subdivide an interval to fail, by
 * computing a midpoint value which does not lie strictly between the
 * endpoints.
 *
 * @version 1.7
 */
public class IntervalSize implements Serializable {

    private static final long serialVersionUID = 2027763845052457104L;
    /**
       * This value is chosen to be a few powers of 2 less than the
       * number of bits available in the double representation (i.e. 53).
       * This should allow enough extra precision for simple computations to be correct,
       * at least for comparison purposes.
       */
    public static final int MIN_BINARY_EXPONENT = -50;

    /**
     * Computes whether the interval [min, max] is effectively zero width.
     * I.e. the width of the interval is so much less than the
     * location of the interval that the midpoint of the interval cannot be
     * represented precisely.
     */
    public static boolean isZeroWidth( double min, double max ) {
        double width = max - min;
        if (width == 0.0)
            return true;

        double maxAbs = Math.max(Math.abs(min), Math.abs(max));
        double scaledInterval = width / maxAbs;
        int level = DoubleBits.exponent(scaledInterval);
        return level <= MIN_BINARY_EXPONENT;
    }
}
