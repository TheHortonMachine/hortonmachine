/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.hmachine.models.hm;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYBaseflowRouting;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYQuickflow;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYRechargeRouting;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;

/**
 * Reproduces the D8 regression test of the InVEST Seasonal Water Yield model
 * (test_base_regression_d8 in natcap/invest tests/test_seasonal_water_yield_regression.py).
 *
 * <p>The inputs are synthetic and rebuilt here as in InVEST: a 100x100 grid of 1m cells
 * with a DEM rising eastwards by one per column, two landcovers (north and south half)
 * and four soil groups (cycling by row), monthly precipitation of month+10 mm, monthly
 * et0 of month mm, one rain event per month and a stream threshold of 50 cells.</p>
 *
 * <p>On that DEM all cells drain west and the stream are the 50 westernmost columns,
 * so the flowdirections and network are built directly here. The whole chain runs as in
 * InVEST: monthly quickflow, potential evapotranspiration as Kc times et0, recharge over the
 * year and baseflow. The expected values are the ones of the InVEST test.</p>
 */
public class TestSWYInvestRegression extends HMTestCase {

    private static final int SIZE = 100;
    private static final int MONTHS = 12;

    /**
     * Monthly quickflow volumes of the InVEST test [m3].
     */
    private static final double[] EXPECTED_QUICKFLOW = {55.81547, 61.04926, 66.31405, 71.60957, 76.93555, 82.29161, 87.67738,
            93.09235, 98.53606, 104.00803, 109.50784, 115.03485};
    /**
     * Monthly baseflow volume of the InVEST test [m3], the annual baseflow distributed over the months.
     */
    private static final double EXPECTED_MONTHLY_BASEFLOW = 61.969;
    /**
     * Mean local recharge of the InVEST test [mm].
     */
    private static final double EXPECTED_QB = 52.9128;

    /**
     * The outputs of the SWY chain on the InVEST test data.
     */
    public static class SwyChain {
        public List<GridCoverage2D> quickflows = new ArrayList<>();
        public List<GridCoverage2D> pets = new ArrayList<>();
        public OmsSWYRechargeRouting recharge;
        public OmsSWYBaseflowRouting baseflow;
    }

    public void testInvestD8Regression() throws Exception {
        SwyChain chain = runChain(pm);

        // the InVEST volumes are sum(mm) * 0.001 * pixel area (1m2)
        for( int m = 0; m < MONTHS; m++ ) {
            double volume = sum(chain.quickflows.get(m).getRenderedImage()) * 0.001;
            assertEquals("quickflow month " + (m + 1), EXPECTED_QUICKFLOW[m], volume, EXPECTED_QUICKFLOW[m] * 1E-5);
        }

        // the potential evapotranspiration is Kc * et0 (the month number)
        assertEquals(0.7 * 6, value(chain.pets.get(5), 0, 0), 1E-9);
        assertEquals(0.4 * 6, value(chain.pets.get(5), 99, 0), 1E-9);

        double qb = chain.baseflow.outQb / (SIZE * SIZE);
        assertEquals(EXPECTED_QB, qb, EXPECTED_QB * 1E-5);
        assertEquals(1.0, chain.baseflow.outVriSum, 1E-6);

        double monthlyBaseflow = sum(chain.baseflow.outB.getRenderedImage()) * 0.001 / MONTHS;
        assertEquals(EXPECTED_MONTHLY_BASEFLOW, monthlyBaseflow, EXPECTED_MONTHLY_BASEFLOW * 1E-5);

        /*
         * single pixels, values from the rasters of an InVEST 3.20.2 run of the same test
         * {row, col, L, L_sum_avail, aet, L_sum, B_sum, B}
         */
        double[][] expectedPixels = {//
                {0, 0, -54.6, 4427.25634765625, 54.6, 4372.656631469727, Double.NaN, 0}, //
                {0, 50, 142.05313110351562, 6960.60302734375, 54.6, 7102.656555175781, 7102.65673828125, 142.05313110351562}, //
                {53, 50, 131.59983825683594, 6448.39208984375, 31.2, 6579.991912841797, 6579.99169921875, 131.59983825683594}, //
                {52, 1, -31.2, 6092.32177734375, 31.2, 6061.122295379639, 6061.1220703125, 0}, //
                {99, 99, 166.8, 0, 31.2, 166.8, 166.8, 166.8}//
        };
        for( double[] p : expectedPixels ) {
            int r = (int) p[0];
            int c = (int) p[1];
            String at = " at row " + r + " col " + c;
            assertEquals("L" + at, p[2], value(chain.recharge.outRecharge, r, c), 1E-3);
            assertEquals("L_sum_avail" + at, p[3], value(chain.recharge.outUpslopeSubsidy, r, c), 1E-2);
            assertEquals("aet" + at, p[4], value(chain.recharge.outAet, r, c), 1E-3);
            assertEquals("L_sum" + at, p[5], value(chain.baseflow.outLsum, r, c), 1E-2);
            if (!Double.isNaN(p[6])) {
                // InVEST sets B_sum = 0 on cells draining out of the raster, HM uses L_sum
                assertEquals("B_sum" + at, p[6], value(chain.baseflow.outBaseflow, r, c), 1E-2);
            }
            assertEquals("B" + at, p[7], value(chain.baseflow.outB, r, c), 1E-3);
        }
    }

    private static double value( GridCoverage2D raster, int row, int col ) {
        return raster.getRenderedImage().getData().getSampleDouble(col, row, 0);
    }

    /**
     * Run quickflow, recharge and baseflow routing on the InVEST test data.
     *
     * @param pm the progress monitor.
     * @return the chain outputs.
     * @throws Exception
     */
    public static SwyChain runChain( IHMProgressMonitor pm ) throws Exception {
        double[][] flow = new double[SIZE][SIZE];
        double[][] net = new double[SIZE][SIZE];
        double[][] curveNumber = new double[SIZE][SIZE];
        double[][] kc = new double[SIZE][SIZE];
        double[][] events = new double[SIZE][SIZE];
        for( int r = 0; r < SIZE; r++ ) {
            int landcover = r < SIZE / 2 ? 0 : 1;
            int soilGroup = r % 4 + 1;
            for( int c = 0; c < SIZE; c++ ) {
                flow[r][c] = Direction.W.getFlow();
                net[r][c] = c < 50 ? 1 : 0;
                curveNumber[r][c] = curveNumber(landcover, soilGroup);
                kc[r][c] = landcover == 0 ? 0.7 : 0.4;
                events[r][c] = 1;
            }
        }
        // the north-west corner drains north in InVEST, out of the raster as west does
        flow[0][0] = Direction.N.getFlow();

        GridCoverage2D flowGC = raster(flow);
        GridCoverage2D netGC = raster(net);
        GridCoverage2D cnGC = raster(curveNumber);
        GridCoverage2D kcGC = raster(kc);
        GridCoverage2D eventsGC = raster(events);

        SwyChain chain = new SwyChain();
        List<GridCoverage2D> precipitations = new ArrayList<>();
        for( int month = 1; month <= MONTHS; month++ ) {
            double[][] precipitation = new double[SIZE][SIZE];
            double[][] et0 = new double[SIZE][SIZE];
            for( int r = 0; r < SIZE; r++ ) {
                for( int c = 0; c < SIZE; c++ ) {
                    precipitation[r][c] = month + 10;
                    et0[r][c] = month;
                }
            }
            GridCoverage2D precipitationGC = raster(precipitation);
            precipitations.add(precipitationGC);

            OmsPotentialEvapotranspiredWaterVolume pet = new OmsPotentialEvapotranspiredWaterVolume();
            pet.pm = pm;
            pet.inCropCoefficient = kcGC;
            pet.inReferenceEtp = raster(et0);
            pet.process();
            chain.pets.add(pet.outputPet);

            OmsSWYQuickflow quickflow = new OmsSWYQuickflow();
            quickflow.pm = pm;
            quickflow.inRainfall = precipitationGC;
            quickflow.inCurveNumber = cnGC;
            quickflow.inNet = netGC;
            quickflow.inNumberOfEvents = eventsGC;
            quickflow.process();
            chain.quickflows.add(quickflow.outQuickflow);
        }

        OmsSWYRechargeRouting recharge = new OmsSWYRechargeRouting();
        recharge.pm = pm;
        recharge.inFlowdirections = flowGC;
        recharge.inPrecipitationList = precipitations;
        recharge.inQuickflowList = chain.quickflows;
        recharge.inPetList = chain.pets;
        recharge.pBeta = 1.0;
        recharge.pGamma = 1.0;
        recharge.process();
        chain.recharge = recharge;

        OmsSWYBaseflowRouting baseflow = new OmsSWYBaseflowRouting();
        baseflow.pm = pm;
        baseflow.inFlowdirections = flowGC;
        baseflow.inNet = netGC;
        baseflow.inRecharge = recharge.outRecharge;
        baseflow.inAvailableRecharge = recharge.outAvailableRecharge;
        baseflow.process();
        chain.baseflow = baseflow;
        return chain;
    }

    /**
     * The curve number table of the InVEST test (soil groups C and D have CN 1).
     */
    private static double curveNumber( int landcover, int soilGroup ) {
        switch( soilGroup ) {
        case 1:
            return landcover == 0 ? 50 : 72;
        case 2:
            return landcover == 0 ? 50 : 82;
        default:
            return 1;
        }
    }

    private static GridCoverage2D raster( double[][] data ) {
        RegionMap region = RegionMap.fromBoundsAndGrid(1180000, 1180000 + SIZE, 690000 - SIZE, 690000, SIZE, SIZE);
        return CoverageUtilities.buildCoverageWithNovalue("swy", data, region, HMTestMaps.getCrs(), true,
                HMConstants.doubleNovalue);
    }

    private static double sum( RenderedImage image ) {
        java.awt.image.Raster data = image.getData();
        double sum = 0;
        for( int r = 0; r < data.getHeight(); r++ ) {
            for( int c = 0; c < data.getWidth(); c++ ) {
                sum += data.getSampleDouble(c, r, 0);
            }
        }
        return sum;
    }
}
