/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.scsrunoff;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.HMConstants.getNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.IDataLoopFunction;
import org.hortonmachine.gears.libs.modules.MultiRasterLoopProcessor;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.Unit;

@Description(OmsScsRunoff.DESCRIPTION)
@Author(name = OmsScsRunoff.AUTHORNAMES, contact = OmsScsRunoff.AUTHORCONTACTS)
@Keywords(OmsScsRunoff.KEYWORDS)
@Label(OmsScsRunoff.LABEL)
@Name(OmsScsRunoff.NAME)
@Status(OmsScsRunoff.STATUS)
@License(OmsScsRunoff.LICENSE)
public class OmsScsRunoff extends HMModel {
    @Description(inRainfall_DESCRIPTION)
    @Unit(pRainfall_UNIT)
    @In
    public GridCoverage2D inRainfall = null;

    @Description(inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(inCN_DESCRIPTION)
    @In
    public GridCoverage2D inCurveNumber = null;

    @Description(inNumberEvents_DESCRIPTION)
    @In
    public GridCoverage2D inNumberOfEvents;

    @Description(outputDischarge_DESCRIPTION)
    @In
    public GridCoverage2D outputDischarge;

    // VARS DOC START
    public static final String DESCRIPTION = "The SCS Runoff model.";
    public static final String DOCUMENTATION = "";
    public static final String KEYWORDS = "SCS, Runoff";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "ScsRunoff";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "The klab team.";
    public static final String AUTHORCONTACTS = "www.integratedmodelling.org";

    public static final String inRainfall_DESCRIPTION = "The rainfall volume.";
    public static final String inNet_DESCRIPTION = "The network map.";
    public static final String inCN_DESCRIPTION = "The map of Curvenumber.";
    public static final String inNumberEvents_DESCRIPTION = "Number of events (if null, defaults to 1)";
    public static final String outputDischarge_DESCRIPTION = "The output runoff.";

    public static final String pRainfall_UNIT = "mm";
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        checkNull(inRainfall, inNet, inCurveNumber);

        double rainNv = getNovalue(inRainfall);
        double netNv = getNovalue(inNet);
        double cnNv = getNovalue(inCurveNumber);
        double _eventsNv = HMConstants.doubleNovalue;
        if (inNumberOfEvents != null) {
            _eventsNv = getNovalue(inNumberOfEvents);
        }
        double eventsNv = _eventsNv;
        double runoffNv = -1.0;
        int defaultEvents = 10; // default num events to 10 if not available

        MultiRasterLoopProcessor processor = new MultiRasterLoopProcessor("Calculating runoff...", pm);
        IDataLoopFunction funct = new IDataLoopFunction(){
            @Override
            public double process( double... values ) {

                double rain = values[0];
                double cn = values[1];
                double net = values[2];
                if (isNovalue(rain, rainNv)) {
                    return runoffNv;
                }
                if (isNovalue(cn, cnNv)) {
                    return 0;
                }
                int eventNum;
                if (inNumberOfEvents != null) {
                    eventNum = (int) values[3];
                    if (isNovalue(eventNum, eventsNv)) {
                        eventNum = defaultEvents;
                    }
                }else {
                    eventNum = defaultEvents;
                }
                boolean isNet = !isNovalue(net, netNv) && net != 0.0;

                double runoff = calculateRunoff(rain, cn, isNet, eventNum);
                return runoff;
            }
        };
        outputDischarge = processor.loop(funct, runoffNv, inRainfall, inCurveNumber, inNet, inNumberOfEvents);

    }

    /**
     * Calculate the runoff.
     *  
     * @param rainfall the rainfall in mm.
     * @param curveNumber the curvenumber value.
     * @param net <code>true</code>, if the pixel is a net pixel.
     * @param eventsNum number of events.
     * @return the runoff value.
     */
    public static double calculateRunoff( double rainfall, double curveNumber, boolean isNet, int eventsNum ) {
        double runoff = 0;
        rainfall = (double) Math.round(rainfall);
        if (rainfall == 0) {
            runoff = 0;
        } else if (isNet) {
            runoff = rainfall;
        } else {
            double sScsCoeff = 1000.0 / curveNumber - 10.0; // TODO check cn unit
            double meanRainDepth = rainfall / eventsNum / 25.4; // convert to inches
            double rainParam = sScsCoeff / meanRainDepth;
            double p1 = -0.2 * rainParam;
            double p2 = 0.8 * rainParam;
            double expP1 = Math.exp(p1);
//                double expP2 = Math.exp(p2);

            double enx = ExponentialIntegrals.enx(rainParam);
            double expResult = Math.exp(p2 + Math.log(enx));

            runoff = eventsNum //
                    * ((meanRainDepth - sScsCoeff) * expP1 + Math.pow(sScsCoeff, 2.0) / meanRainDepth //
                            * expResult)//
//                                * expP2 * enx)//
                    * 25.4;// to mm

            // due to numeric instabilities on lakes pixels which are not considered as
            // water
            if (runoff < 0) {
                runoff = 0;
            }

        }
        return runoff;
    }

    /**
     * @author wilx
     */
    private static class ExponentialIntegrals {

        // Internally Defined Constants //
        static final double DBL_EPSILON = Math.ulp(1.0);
        static final double epsilon = 10.0 * DBL_EPSILON;
        static final double DBL_MAX = Double.MAX_VALUE;

        // //////////////////////////////////////////////////////////////////////////////
        // double xExponential_Integral_Ei( double x ) //
        // //
        // Description: //
        // The exponential integral Ei(x) is the integral with integrand //
        // exp(t) / t //
        // where the integral extends from -inf to x. //
        // Note that there is a singularity at t = 0. Therefore for x > 0, the //
        // integral is defined to be the Cauchy principal value: //
        // lim { I[-inf, -eta] exp(-t) dt / t + I[eta, x] exp(-t) dt / t } //
        // in which the limit is taken as eta > 0 approaches 0 and I[a,b] //
        // denotes the integral from a to b. //
        // //
        // Arguments: //
        // double x The argument of the exponential integral Ei(). //
        // //
        // Return Value: //
        // The value of the exponential integral Ei evaluated at x. //
        // If x = 0.0, then Ei is -inf and -DBL_MAX is returned. //
        // //
        // Example: //
        // double y, x; //
        // //
        // ( code to initialize x ) //
        // //
        // y = xExponential_Integral_Ei( x ); //
        // //////////////////////////////////////////////////////////////////////////////

        @SuppressWarnings("unused")
        public static double exponentialIntegralEi( final double x ) {
            if (x < -5.0) {
                return continuedFractionEi(x);
            }
            if (x == 0.0) {
                return -DBL_MAX;
            }
            if (x < 6.8) {
                return powerSeriesEi(x);
            }
            if (x < 50.0) {
                return argumentAdditionSeriesEi(x);
            }
            return continuedFractionEi(x);
        }

        // //////////////////////////////////////////////////////////////////////////////
        // static double Continued_Fraction_Ei( double x ) //
        // //
        // Description: //
        // For x < -5 or x > 50, the continued fraction representation of Ei //
        // converges fairly rapidly. //
        // //
        // The continued fraction expansion of Ei(x) is: //
        // Ei(x) = -exp(x) { 1/(-x+1-) 1/(-x+3-) 4/(-x+5-) 9/(-x+7-) ... }. //
        // //
        // //
        // Arguments: //
        // double x //
        // The argument of the exponential integral Ei(). //
        // //
        // Return Value: //
        // The value of the exponential integral Ei evaluated at x. //
        // //////////////////////////////////////////////////////////////////////////////

        private static double continuedFractionEi( final double x ) {
            double Am1 = 1.0;
            double A0 = 0.0;
            double Bm1 = 0.0;
            double B0 = 1.0;
            double a = expl(x);
            double b = -x + 1.0;
            double Ap1 = b * A0 + a * Am1;
            double Bp1 = b * B0 + a * Bm1;
            int j = 1;

            a = 1.0;
            while( fabsl(Ap1 * B0 - A0 * Bp1) > epsilon * fabsl(A0 * Bp1) ) {
                if (fabsl(Bp1) > 1.0) {
                    Am1 = A0 / Bp1;
                    A0 = Ap1 / Bp1;
                    Bm1 = B0 / Bp1;
                    B0 = 1.0;
                } else {
                    Am1 = A0;
                    A0 = Ap1;
                    Bm1 = B0;
                    B0 = Bp1;
                }
                a = -j * j;
                b += 2.0;
                Ap1 = b * A0 + a * Am1;
                Bp1 = b * B0 + a * Bm1;
                j += 1;
            }
            return (-Ap1 / Bp1);
        }

        // //////////////////////////////////////////////////////////////////////////////
        // static double Power_Series_Ei( double x ) //
        // //
        // Description: //
        // For -5 < x < 6.8, the power series representation for //
        // (Ei(x) - gamma - ln|x|)/exp(x) is used, where gamma is Euler's gamma //
        // constant. //
        // Note that for x = 0.0, Ei is -inf. In which case -DBL_MAX is //
        // returned. //
        // //
        // The power series expansion of (Ei(x) - gamma - ln|x|) / exp(x) is //
        // - Sum(1 + 1/2 + ... + 1/j) (-x)^j / j!, where the Sum extends //
        // from j = 1 to inf. //
        // //
        // Arguments: //
        // double x //
        // The argument of the exponential integral Ei(). //
        // //
        // Return Value: //
        // The value of the exponential integral Ei evaluated at x. //
        // //////////////////////////////////////////////////////////////////////////////

        private static double powerSeriesEi( final double x ) {
            double xn = -x;
            double Sn = -x;
            double Sm1 = 0.0;
            double hsum = 1.0;
            final double g = 0.5772156649015328606065121;
            double y = 1.0;
            double factorial = 1.0;

            if (x == 0.0) {
                return -DBL_MAX;
            }

            while( fabsl(Sn - Sm1) > epsilon * fabsl(Sm1) ) {
                Sm1 = Sn;
                y += 1.0;
                xn *= (-x);
                factorial *= y;
                hsum += (1.0 / y);
                Sn += hsum * xn / factorial;
            }
            return (g + logl(fabsl(x)) - expl(x) * Sn);
        }

        static final double ei[] = {1.915047433355013959531e2, 4.403798995348382689974e2, 1.037878290717089587658e3,
                2.492228976241877759138e3, 6.071406374098611507965e3, 1.495953266639752885229e4, 3.719768849068903560439e4,
                9.319251363396537129882e4, 2.349558524907683035782e5, 5.955609986708370018502e5, 1.516637894042516884433e6,
                3.877904330597443502996e6, 9.950907251046844760026e6, 2.561565266405658882048e7, 6.612718635548492136250e7,
                1.711446713003636684975e8, 4.439663698302712208698e8, 1.154115391849182948287e9, 3.005950906525548689841e9,
                7.842940991898186370453e9, 2.049649711988081236484e10, 5.364511859231469415605e10, 1.405991957584069047340e11,
                3.689732094072741970640e11, 9.694555759683939661662e11, 2.550043566357786926147e12, 6.714640184076497558707e12,
                1.769803724411626854310e13, 4.669055014466159544500e13, 1.232852079912097685431e14, 3.257988998672263996790e14,
                8.616388199965786544948e14, 2.280446200301902595341e15, 6.039718263611241578359e15, 1.600664914324504111070e16,
                4.244796092136850759368e16, 1.126348290166966760275e17, 2.990444718632336675058e17, 7.943916035704453771510e17,
                2.111342388647824195000e18, 5.614329680810343111535e18, 1.493630213112993142255e19, 3.975442747903744836007e19,
                1.058563689713169096306e20};

        private static double expl( final double x ) {
            return Math.exp(x);
        }

        private static double fabsl( final double x ) {
            return Math.abs(x);
        }

        private static double logl( final double x ) {
            return Math.log(x);
        }

        @SuppressWarnings("unused")
        public static void eialpha( double x, int n, double alpha[] ) {
            int k;
            double a, b, c;
            c = 1.0 / x;
            a = Math.exp(-x);
            b = alpha[0] = a * c;
            for( k = 1; k <= n; k++ )
                alpha[k] = b = (a + k * b) * c;
        }

        public static double jfrac( int n, double a[], double b[] ) {
            int i;
            double d;
            d = 0.0;
            for( i = n; i >= 1; i-- )
                d = a[i] / (b[i] + d);
            return (d + b[0]);
        }

        public static double chepolsum( int n, double x, double a[] ) {
            int k;
            double h, r, s, tx;
            if (n == 0)
                return (a[0]);
            if (n == 1)
                return (a[0] + a[1] * x);
            tx = x + x;
            r = a[n];
            h = a[n - 1] + r * tx;
            for( k = n - 2; k >= 1; k-- ) {
                s = r;
                r = h;
                h = a[k] + r * tx - s;
            }
            return (a[0] - r + h * x);
        }

        public static double pol( int n, double x, double a[] ) {
            double r;
            r = 0.0;
            for( ; n >= 0; n-- )
                r = r * x + a[n];
            return (r);
        }

        public static double ei( double x ) {
            double p[] = new double[8];
            double q[] = new double[8];
            if (x > 24.0) {
                p[0] = 1.00000000000058;
                q[1] = 1.99999999924131;
                p[1] = x - 3.00000016782085;
                q[2] = -2.99996432944446;
                p[2] = x - 5.00140345515924;
                q[3] = -7.90404992298926;
                p[3] = x - 7.49289167792884;
                q[4] = -4.31325836146628;
                p[4] = x - 3.08336269051763e1;
                q[5] = 2.95999399486831e2;
                p[5] = x - 1.39381360364405;
                q[6] = -6.74704580465832;
                p[6] = x + 8.91263822573708;
                q[7] = 1.04745362652468e3;
                p[7] = x - 5.31686623494482e1;
                return Math.exp(x) * (1.0 + jfrac(7, q, p) / x) / x;
            } else if (x > 12.0) {
                p[0] = 9.99994296074708e-1;
                q[1] = 1.00083867402639;
                p[1] = x - 1.95022321289660;
                q[2] = -3.43942266899870;
                p[2] = x + 1.75656315469614;
                q[3] = 2.89516727925135e1;
                p[3] = x + 1.79601688769252e1;
                q[4] = 7.60761148007735e2;
                p[4] = x - 3.23467330305403e1;
                q[5] = 2.57776384238440e1;
                p[5] = x - 8.28561994140641;
                q[6] = 5.72837193837324e1;
                p[6] = x - 1.86545454883399e1;
                q[7] = 6.95000655887434e1;
                p[7] = x - 3.48334653602853;
                return Math.exp(x) * jfrac(7, q, p) / x;
            } else if (x > 6.0) {
                p[0] = 1.00443109228078;
                q[1] = 5.27468851962908e-1;
                p[1] = x - 4.32531132878135e1;
                q[2] = 2.73624119889328e3;
                p[2] = x + 6.01217990830080e1;
                q[3] = 1.43256738121938e1;
                p[3] = x - 3.31842531997221e1;
                q[4] = 1.00367439516726e3;
                p[4] = x + 2.50762811293561e1;
                q[5] = -6.25041161671876;
                p[5] = x + 9.30816385662165;
                q[6] = 3.00892648372915e2;
                p[6] = x - 2.19010233854880e1;
                q[7] = 3.93707701852715;
                p[7] = x - 2.18086381520724;
                return Math.exp(x) * jfrac(7, q, p) / x;
            } else if (x > 0.0) {
                double t, r, x0, xmx0;
                p[0] = -1.95773036904548e8;
                q[0] = -8.26271498626055e7;
                p[1] = 3.89280421311201e6;
                q[1] = 8.91925767575612e7;
                p[2] = -2.21744627758845e7;
                q[2] = -2.49033375740540e7;
                p[3] = -1.19623669349247e5;
                q[3] = 4.28559624611749e6;
                p[4] = -2.49301393458648e5;
                q[4] = -4.83547436162164e5;
                p[5] = -4.21001615357070e3;
                q[5] = 3.57300298058508e4;
                p[6] = -5.49142265521085e2;
                q[6] = -1.60708926587221e3;
                p[7] = -8.66937339951070;
                q[7] = 3.41718750000000e1;
                x0 = 0.372507410781367;
                t = x / 3.0 - 1.0;
                r = chepolsum(7, t, p) / chepolsum(7, t, q);
                xmx0 = (x - 409576229586.0 / 1099511627776.0) - 0.767177250199394e-12;
                if (Math.abs(xmx0) > 0.037)
                    t = Math.log(x / x0);
                else {
                    double z, z2;
                    p[0] = 0.837207933976075e1;
                    p[1] = -0.652268740837103e1;
                    p[2] = 0.569955700306720;
                    q[0] = 0.418603966988037e1;
                    q[1] = -0.465669026080814e1;
                    q[2] = 0.1e1;
                    z = xmx0 / (x + x0);
                    z2 = z * z;
                    t = z * pol(2, z2, p) / pol(2, z2, q);
                }
                return t + xmx0 * r;
            } else if (x > -1.0) {
                double y;
                p[0] = -4.41785471728217e4;
                q[0] = 7.65373323337614e4;
                p[1] = 5.77217247139444e4;
                q[1] = 3.25971881290275e4;
                p[2] = 9.93831388962037e3;
                q[2] = 6.10610794245759e3;
                p[3] = 1.84211088668000e3;
                q[3] = 6.35419418378382e2;
                p[4] = 1.01093806161906e2;
                q[4] = 3.72298352833327e1;
                p[5] = 5.03416184097568;
                q[5] = 1.0;
                y = -x;
                return Math.log(y) - pol(5, y, p) / pol(5, y, q);
            } else if (x > -4.0) {
                double y;
                p[0] = 8.67745954838444e-8;
                q[0] = 1.0;
                p[1] = 9.99995519301390e-1;
                q[1] = 1.28481935379157e1;
                p[2] = 1.18483105554946e1;
                q[2] = 5.64433569561803e1;
                p[3] = 4.55930644253390e1;
                q[3] = 1.06645183769914e2;
                p[4] = 6.99279451291003e1;
                q[4] = 8.97311097125290e1;
                p[5] = 4.25202034768841e1;
                q[5] = 3.14971849170441e1;
                p[6] = 8.83671808803844;
                q[6] = 3.79559003762122;
                p[7] = 4.01377664940665e-1;
                q[7] = 9.08804569188869e-2;
                y = -1.0 / x;
                return -Math.exp(x) * pol(7, y, p) / pol(5, y, q);
            } else {
                double y;
                p[0] = -9.99999999998447e-1;
                q[0] = 1.0;
                p[1] = -2.66271060431811e1;
                q[1] = 2.86271060422192e1;
                p[2] = -2.41055827097015e2;
                q[2] = 2.92310039388533e2;
                p[3] = -8.95927957772937e2;
                q[3] = 1.33278537748257e3;
                p[4] = -1.29885688756484e3;
                q[4] = 2.77761949509163e3;
                p[5] = -5.45374158883133e2;
                q[5] = 2.40401713225909e3;
                p[6] = -5.66575206533869;
                q[6] = 6.31657483280800e2;
                y = -1.0 / x;
                return -Math.exp(x) * y * (1.0 + y * pol(6, y, p) / pol(5, y, q));
            }
        }

        public static double enx( double x ) {
            double a[] = new double[2];
            enx(x, 0, 1, a);
            return a[1];
        }

        public static void enx( double x, int n1, int n2, double a[] ) {
            if (x <= 1.5) {
                int i;
                double w, e;
                e = 0.0;
                w = -ei(-x);
                if (n1 == 1)
                    a[1] = w;
                if (n2 > 1)
                    e = Math.exp(-x);
                for( i = 2; i <= n2; i++ ) {
                    w = (e - x * w) / (i - 1);
                    if (i >= n1)
                        a[i] = w;
                }
            } else {
                int i, n;
                double w, e, an;
                n = (int) Math.ceil(x);
                if (n <= 10) {
                    double f, w1, t, h;
                    double p[] = new double[20];
                    p[2] = 0.37534261820491e-1;
                    p[11] = 0.135335283236613;
                    p[3] = 0.89306465560228e-2;
                    p[12] = 0.497870683678639e-1;
                    p[4] = 0.24233983686581e-2;
                    p[13] = 0.183156388887342e-1;
                    p[5] = 0.70576069342458e-3;
                    p[14] = 0.673794699908547e-2;
                    p[6] = 0.21480277819013e-3;
                    p[15] = 0.247875217666636e-2;
                    p[7] = 0.67375807781018e-4;
                    p[16] = 0.911881965554516e-3;
                    p[8] = 0.21600730159975e-4;
                    p[17] = 0.335462627902512e-3;
                    p[9] = 0.70411579854292e-5;
                    p[18] = 0.123409804086680e-3;
                    p[10] = 0.23253026570282e-5;
                    p[19] = 0.453999297624848e-4;
                    f = w = p[n];
                    e = p[n + 9];
                    w1 = t = 1.0;
                    h = x - n;
                    i = n - 1;
                    do {
                        f = (e - i * f) / n;
                        t = -h * t / (n - i);
                        w1 = t * f;
                        w += w1;
                        i--;
                    } while( Math.abs(w1) > 1.0e-15 * w );
                } else {
                    double b[] = new double[n + 1];
                    nonexpenx(x, n, n, b);
                    w = b[n] * Math.exp(-x);
                }
                if (n1 == n2 && n1 == n)
                    a[n] = w;
                else {
                    e = Math.exp(-x);
                    an = w;
                    if (n <= n2 && n >= n1)
                        a[n] = w;
                    for( i = n - 1; i >= n1; i-- ) {
                        w = (e - i * w) / x;
                        if (i <= n2)
                            a[i] = w;
                    }
                    w = an;
                    for( i = n + 1; i <= n2; i++ ) {
                        w = (e - x * w) / (i - 1);
                        if (i >= n1)
                            a[i] = w;
                    }
                }
            }
        }

        public static void nonexpenx( double x, int n1, int n2, double a[] ) {
            int i, n;
            double w, an;
            n = (x <= 1.5) ? 1 : (int) Math.ceil(x);
            if (n <= 10) {
                double b[] = new double[n + 1];
                enx(x, n, n, b);
                w = b[n] * Math.exp(x);
            } else {
                int k, k1;
                double ue, ve, we, we1, uo, vo, wo, wo1, r, s;
                ue = 1.0;
                ve = we = 1.0 / (x + n);
                we1 = 0.0;
                uo = 1.0;
                vo = -n / (x * (x + n + 1.0));
                wo1 = 1.0 / x;
                wo = vo + wo1;
                w = (we + wo) / 2.0;
                k1 = 1;
                k = k1;
                while( wo - we > 1.0e-15 * w && we > we1 && wo < wo1 ) {
                    we1 = we;
                    wo1 = wo;
                    r = n + k;
                    s = r + x + k;
                    ue = 1.0 / (1.0 - k * (r - 1.0) * ue / ((s - 2.0) * s));
                    uo = 1.0 / (1.0 - k * r * uo / (s * s - 1.0));
                    ve *= (ue - 1.0);
                    vo *= (uo - 1.0);
                    we += ve;
                    wo += vo;
                    w = (we + wo) / 2.0;
                    k1++;
                    k = k1;
                }
            }
            an = w;
            if (n <= n2 && n >= n1)
                a[n] = w;
            for( i = n - 1; i >= n1; i-- ) {
                w = (1.0 - i * w) / x;
                if (i <= n2)
                    a[i] = w;
            }
            w = an;
            for( i = n + 1; i <= n2; i++ ) {
                w = (1.0 - x * w) / (i - 1);
                if (i >= n1)
                    a[i] = w;
            }
        }

        // //////////////////////////////////////////////////////////////////////////////
        // static double Argument_Addition_Series_Ei(double x) //
        // //
        // Description: //
        // For 6.8 < x < 50.0, the argument addition series is used to calculate //
        // Ei. //
        // //
        // The argument addition series for Ei(x) is: //
        // Ei(x+dx) = Ei(x) + exp(x) Sum j! [exp(j) expj(-dx) - 1] / x^(j+1), //
        // where the Sum extends from j = 0 to inf, |x| > |dx| and expj(y) is //
        // the exponential polynomial expj(y) = Sum y^k / k!, the Sum extending //
        // from k = 0 to k = j. //
        // //
        // Arguments: //
        // double x //
        // The argument of the exponential integral Ei(). //
        // //
        // Return Value: //
        // The value of the exponential integral Ei evaluated at x. //
        // //////////////////////////////////////////////////////////////////////////////
        private static double argumentAdditionSeriesEi( final double x ) {
            final int k = (int) (x + 0.5);
            int j = 0;
            final double xx = k;
            final double dx = x - xx;
            double xxj = xx;

            final double edx = expl(dx);
            double Sm = 1.0;
            double Sn = (edx - 1.0) / xxj;
            double term = DBL_MAX;
            double factorial = 1.0;
            double dxj = 1.0;

            while( fabsl(term) > epsilon * fabsl(Sn) ) {
                j++;
                factorial *= j;
                xxj *= xx;
                dxj *= (-dx);
                Sm += (dxj / factorial);
                term = (factorial * (edx * Sm - 1.0)) / xxj;
                Sn += term;
            }

            return ei[k - 7] + Sn * expl(xx);
        }
    }

}