package org.jgrasstools.hortonmachine.modules.networkmanagement.utils;

public enum TrentoPConstants {
    WSPECIFICWEIGHT(9800), /* [N/m^3] */
    MINUTE2SEC(60), //
    HOUR2MIN(60), //
    METER2CM(100), //
    METER2CMSQUARED(10000), //
    CUBICMETER2LITER(1000), //
    ONEOVERSIX(0.166667), // /* 1/6 */
    TWOOVERTHREE(0.666667), // /* 2/3 */
    EIGHTOVERTHREE(2.666667), // /* 8/3 */
    THREEOVEREIGHT(0.375), // /* 3/8 */
    TWO_THIRTEENOVEREIGHT(308.442165), // /* 2^(13/8)*100 */
    TWO_THIRTEENOVERTHREE(20.158737), // /* 2^(13/3) */
    TWO_TWENTYOVERTHIRTEEN(290.484571), // /* 2^(20/13)*100 */
    TWO_TENOVERTHREE(10.079368), // /* 2^(10/3) */
    THIRTHEENOVERSIX(2.166667), // /* 13/6 */
    SIXOVERTHIRTEEN(0.461538), // /* 6/13 */
    ONEOVERTHIRTEEN(0.076923), // /* 1/13 */
    ONEOVERTHERTEEN(0.0769231), // /* 1/13 */
    SEVENOVERTHIRTEEN(0.5384615), // /* 7/13 */
    FIVEOVEREIGHT(0.625), /* 5/8 */
    TWO_SQUAREROOT_TWO(2.828427), // /* 2 * sqrt(2) */
    ONEOVERFOUR(0.25); // /* 1/4 */

    private final double value;
    TrentoPConstants( double value ) {
        this.value = value;
    }

    public double value() {
        return value;
    }
}
