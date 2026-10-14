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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.swy;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.HMConstants.getNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.IDataLoopFunction;
import org.hortonmachine.gears.libs.modules.MultiRasterLoopProcessor;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

@Description(OmsSWYQuickflow.DESCRIPTION)
@Documentation(OmsSWYQuickflow.DOCUMENTATION)
@Bibliography({OmsSWYQuickflow.BIBLIOGRAPHY_INVEST_SWY, OmsSWYQuickflow.BIBLIOGRAPHY_HAMEL_ET_AL_2020})
@Author(name = OmsSWYQuickflow.AUTHORNAMES, contact = OmsSWYQuickflow.AUTHORCONTACTS)
@Keywords(OmsSWYQuickflow.KEYWORDS)
@Label(OmsSWYQuickflow.LABEL)
@Name(OmsSWYQuickflow.NAME)
@Status(OmsSWYQuickflow.STATUS)
@License(OmsSWYQuickflow.LICENSE)
public class OmsSWYQuickflow extends HMModel {

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

    @Description(outQuickflow_DESCRIPTION)
    @Unit(quickflowUnit)
    @Out
    public GridCoverage2D outQuickflow;

    // VARS DOC START
    public static final String DESCRIPTION = "The quickflow of an interval (ex. a month) of the InVEST Seasonal Water Yield model, from the precipitation, the curve number and the number of rain events.";
    public static final String DOCUMENTATION = """
            The quickflow is the expected value of the SCS curve number runoff of the rain events of the interval, \
            with the depth of the events exponentially distributed around their mean (InVEST equation 1). The curve \
            number gives the potential retention S = 1000/CN - 10, in inches.

            The special cases are those of InVEST: without precipitation or rain events the quickflow is 0; on the \
            stream cells, and where the curve number is 100, all the precipitation becomes quickflow; where the \
            retention is more than 100 times the mean depth of an event (also with curve number 0) the quickflow is 0. \
            Where the curve number is novalue, the stream cells still get all the precipitation and the other cells \
            are novalue. Where the number of events is not given, 10 events are used.
            """;
    public static final String BIBLIOGRAPHY_INVEST_SWY = "Natural Capital Project. InVEST User's Guide, Seasonal Water Yield. Stanford University, University of Minnesota, Chinese Academy of Sciences, The Nature Conservancy, World Wildlife Fund, Stockholm Resilience Centre and the Royal Swedish Academy of Sciences. https://releases.naturalcapitalproject.org/invest-userguide/latest/";
    public static final String BIBLIOGRAPHY_HAMEL_ET_AL_2020 = "Hamel, P., Valencia, J., Schmitt, R., Shrestha, M., Piman, T., Sharp, R. P., Francesconi, W., Guswa, A. J. (2020). Modeling seasonal water yield for landscape management: Applications in Peru and Myanmar. Journal of Environmental Management, 270, 110792.";
    public static final String KEYWORDS = "SCS, Runoff, CN, Quickflow, Hydrology, Hydromorphology";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "SWYQuickflow";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "The klab team.";
    public static final String AUTHORCONTACTS = "www.integratedmodelling.org";

    public static final String inRainfall_DESCRIPTION = "The map of the precipitation of the interval.";
    public static final String inNet_DESCRIPTION = "The map of the stream network.";
    public static final String inCN_DESCRIPTION = "The map of the curve number, which encapsulates soil, land cover and antecedent moisture.";
    public static final String inNumberEvents_DESCRIPTION = "The map of the number of rain events of the interval (if not set, or novalue, 10).";
    public static final String outQuickflow_DESCRIPTION = "The map of the quickflow of the interval.";

    public static final String quickflowUnit = "mm";
    public static final String pRainfall_UNIT = "mm";
    // VARS DOC END

    /**
     * Above this value of the S/a ratio the quickflow is set to 0, as in InVEST.
     */
    private static final double MAX_S_A_RATIO = 100.0;

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
                boolean isNet = !isNovalue(net, netNv) && net != 0.0;
                if (isNovalue(cn, cnNv)) {
                    // as in InVEST, without curve number only no rain and stream cells are defined
                    if (rain <= 0) {
                        return 0;
                    }
                    if (isNet) {
                        return rain;
                    }
                    return runoffNv;
                }
                int eventNum = defaultEvents;
                if (inNumberOfEvents != null && !isNovalue(values[3], eventsNv)) {
                    eventNum = (int) values[3];
                }
                if(eventNum == 0 && rain != 0) {
                    eventNum = 1; // we have at least one event
                }

                double runoff = calculateRunoff(rain, cn, isNet, eventNum);
                return runoff;
            }
        };
        outQuickflow = processor.loop(funct, runoffNv, inRainfall, inCurveNumber, inNet, inNumberOfEvents);

    }

    /**
     * Calculate the runoff using the curve number approach, assuming
     * exponential rainfall distribution and applying the expectation fomula.
     *
     * <p>Edge cases are handled as in InVEST:</p>
     * <ul>
     * <li>no rain or no events: 0</li>
     * <li>stream pixel or S == 0 (CN == 100): all rain becomes runoff</li>
     * <li>S/a &gt; 100 (this includes CN == 0): 0</li>
     * <li>negative results: 0</li>
     * </ul>
     *
     * @param rainfall the rainfall in mm.
     * @param curveNumber the curvenumber value.
     * @param net <code>true</code>, if the pixel is a net pixel.
     * @param eventsNum number of events in the considered time unit (for InVEST always the month).
     * @return the runoff value.
     */
    public static double calculateRunoff( double rainfall, double curveNumber, boolean isNet, int eventsNum ) {
        if (rainfall <= 0 || eventsNum <= 0) {
            return 0;
        }
        if (isNet) {
            return rainfall;
        }

        double sScsCoeff = 1000.0 / curveNumber - 10.0; // SCS coefficient in inches
        if (sScsCoeff == 0) {
            return rainfall;
        }

        // calculate the mean rain depth per event
        double meanRainDepth = rainfall / eventsNum / 25.4; // convert to inches

        // compute the quickflow expectation
        double rainParam = sScsCoeff / meanRainDepth;
        if (rainParam > MAX_S_A_RATIO) {
            return 0;
        }

        double runoff = eventsNum //
                * ((meanRainDepth - sScsCoeff) * Math.exp(-0.2 * rainParam) //
                        + sScsCoeff * sScsCoeff / meanRainDepth * Math.exp(0.8 * rainParam) * exponentialIntegralE1(rainParam)) //
                * 25.4;// to mm

        // the formula can give small negative values for some combinations of inputs
        if (runoff < 0) {
            runoff = 0;
        }
        return runoff;
    }

    /**
     * The exponential integral E1(x) for x &gt; 0.
     *
     * <p>Power series for x &lt;= 1, continued fraction (modified Lentz) otherwise,
     * as in Numerical Recipes (expint with n = 1).</p>
     *
     * @param x the argument, must be positive.
     * @return the value of E1(x).
     */
    static double exponentialIntegralE1( double x ) {
        final double euler = 0.5772156649015329;
        final double eps = 1E-15;
        final double fpmin = Double.MIN_NORMAL / eps;
        final int maxIterations = 1000;

        if (x <= 0) {
            throw new IllegalArgumentException("E1 is only defined for positive arguments: " + x);
        }
        if (x > 1.0) {
            double b = x + 1.0;
            double c = 1.0 / fpmin;
            double d = 1.0 / b;
            double h = d;
            for( int i = 1; i <= maxIterations; i++ ) {
                double an = -(double) i * i;
                b += 2.0;
                d = 1.0 / (an * d + b);
                c = b + an / c;
                double delta = c * d;
                h *= delta;
                if (Math.abs(delta - 1.0) < eps) {
                    return h * Math.exp(-x);
                }
            }
        } else {
            double result = -Math.log(x) - euler;
            double factor = 1.0;
            for( int i = 1; i <= maxIterations; i++ ) {
                factor *= -x / i;
                double delta = -factor / i;
                result += delta;
                if (Math.abs(delta) < Math.abs(result) * eps) {
                    return result;
                }
            }
        }
        throw new IllegalStateException("E1 did not converge for x = " + x);
    }

}
