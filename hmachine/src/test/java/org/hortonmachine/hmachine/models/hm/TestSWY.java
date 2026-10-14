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

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYBaseflowRouting;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYQuickflow;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYRechargeRouting;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;

/**
 * Tests the Seasonal Water Yield modules chain.
 *
 * <p>The routing tests use a single row of cells all draining east, the last
 * two cells being stream. Expected values are calculated by hand from the InVEST equations.</p>
 */
public class TestSWY extends HMTestCase {

    private static final double NV = HMConstants.doubleNovalue;
    private static final double EAST = 1;

    /**
     * Reference values from the InVEST quickflow equation (with scipy's exp1).
     */
    public void testQuickflowEquation() throws Exception {
        double[][] pCnNQf = {//
                {200, 50, 1, 84.55587423121537}, //
                {200, 85, 5, 91.44824565088271}, //
                {80, 95, 5, 42.88798160548313}, //
                {500, 70, 1, 407.02342468365407}, //
                {30, 85, 1, 11.335441189117642}, //
                {20, 99, 10, 8.400043766838646}, //
                {100, 70, 10, 1.6560353125867628}, //
                {1.4, 99, 1, 0.4507345046449977}//
        };
        for( double[] v : pCnNQf ) {
            double qf = OmsSWYQuickflow.calculateRunoff(v[0], v[1], false, (int) v[2]);
            assertEquals("P=" + v[0] + " CN=" + v[1] + " n=" + v[2], v[3], qf, v[3] * 1E-9);
        }
    }

    public void testQuickflowEdgeCases() throws Exception {
        // no rain
        assertEquals(0.0, OmsSWYQuickflow.calculateRunoff(0, 70, false, 5), 0);
        // stream: all rain becomes runoff, no rounding
        assertEquals(0.6, OmsSWYQuickflow.calculateRunoff(0.6, 70, true, 5), 0);
        // CN == 100: no retention
        assertEquals(0.6, OmsSWYQuickflow.calculateRunoff(0.6, 100, false, 5), 0);
        // CN == 0: infinite retention
        assertEquals(0.0, OmsSWYQuickflow.calculateRunoff(30, 0, false, 5), 0);
        // very large S/a ratio
        assertEquals(0.0, OmsSWYQuickflow.calculateRunoff(0.3, 30, false, 20), 0);
    }

    public void testQuickflowRasters() throws Exception {
        OmsSWYQuickflow quickflow = new OmsSWYQuickflow();
        quickflow.pm = pm;
        quickflow.inRainfall = line(100, 100, 100, 100, NV);
        quickflow.inCurveNumber = line(70, 70, 70, 70, 70);
        quickflow.inNet = line(0, 0, 0, 1, 1);
        quickflow.inNumberOfEvents = line(10, NV, 1, 1, 1);
        quickflow.process();

        double qf10 = OmsSWYQuickflow.calculateRunoff(100, 70, false, 10);
        double qf1 = OmsSWYQuickflow.calculateRunoff(100, 70, false, 1);
        // novalue events fall back to the default of 10 events
        checkMatrixEqual(quickflow.outQuickflow.getRenderedImage(), new double[][]{{qf10, qf10, qf1, 100, -1}}, 1E-9);
    }

    public void testQuickflowCurveNumberNovalues() throws Exception {
        OmsSWYQuickflow quickflow = new OmsSWYQuickflow();
        quickflow.pm = pm;
        quickflow.inRainfall = line(100, 0, 100, 100, 100);
        quickflow.inCurveNumber = line(70, NV, NV, NV, 70);
        quickflow.inNet = line(0, 0, 0, 1, 1);
        quickflow.process();

        // as in InVEST, without curve number no rain gives 0, streams take all the rain, the rest is novalue
        double qf10 = OmsSWYQuickflow.calculateRunoff(100, 70, false, 10);
        checkMatrixEqual(quickflow.outQuickflow.getRenderedImage(), new double[][]{{qf10, 0, -1, 100, 100}}, 1E-9);
    }

    public void testPetFromReferenceEtp() throws Exception {
        OmsPotentialEvapotranspiredWaterVolume pet = new OmsPotentialEvapotranspiredWaterVolume();
        pet.pm = pm;
        pet.inCropCoefficient = line(0.5, 1, NV, 1, 1.2);
        pet.inReferenceEtp = line(100, 100, 100, NV, 50);
        pet.process();

        checkMatrixEqual(pet.outputPet.getRenderedImage(), new double[][]{{50, 100, NV, NV, 60}}, 1E-9);
    }

    public void testPetHargreaves() throws Exception {
        OmsPotentialEvapotranspiredWaterVolume pet = new OmsPotentialEvapotranspiredWaterVolume();
        pet.pm = pm;
        pet.inCropCoefficient = line(1, 0.5, 1, 1, NV);
        pet.inMaxTemp = line(25, 25, 20, 25, 25);
        pet.inMinTemp = line(10, 10, 18, 10, 10);
        pet.inAtmosphericTemp = line(17.5, 17.5, 19, 17.5, 17.5);
        pet.inRainfall = line(100, 100, 300, NV, 100);
        pet.inSolarRadiation = line(30, 30, 30, 30, 30);
        pet.pDaysInTimestep = 30.0;
        pet.process();

        // 0.0013 * 0.408 * 30 * (17.5 + 17) * (25 - 10 - 0.0123 * 100)^0.76 * 30 days
        double et0 = 120.85229844411951;
        // the third cell is wet with a small temperature range: no evapotranspiration instead of NaN
        checkMatrixEqual(pet.outputPet.getRenderedImage(), new double[][]{{et0, et0 * 0.5, 0, NV, NV}}, 1E-9);
    }

    public void testIntervalRasters() throws Exception {
        File folder = Files.createTempDirectory("hm-swy").toFile();
        try {
            for( String name : new String[]{"prcp_1.tif", "prcp_2.tif", "prcp_10.tif", "prcp_1.tif.aux.xml", "prcp_a3.tif",
                    "prcp_4.qml", "prcp_.tif", "notes.txt"} ) {
                new File(folder, name).createNewFile();
            }
            TreeMap<Integer, File> rasters = OmsSWYRechargeRouting.getIntervalRasters(folder, "prcp_");
            assertEquals(Arrays.asList(1, 2, 10), new ArrayList<>(rasters.keySet()));
            assertEquals("prcp_10.tif", rasters.get(10).getName());

            // the same interval twice
            new File(folder, "prcp_01.tif").createNewFile();
            try {
                OmsSWYRechargeRouting.getIntervalRasters(folder, "prcp_");
                fail("Two rasters for the same interval are not allowed.");
            } catch (ModelsIllegalargumentException e) {
                // expected
            }
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }

    public void testRechargeRouting() throws Exception {
        OmsSWYRechargeRouting recharge = rechargeRouting(line(100, 100, 100, 100, 100), line(10, 10, 10, 100, 100));

        // the stream cells evapotranspire the upslope subsidy, so their recharge is negative
        checkMatrixEqual(recharge.outUpslopeSubsidy.getRenderedImage(), new double[][]{{0, 40, 80, 120, 70}}, 1E-9);
        checkMatrixEqual(recharge.outAet.getRenderedImage(), new double[][]{{50, 50, 50, 50, 50}}, 1E-9);
        checkMatrixEqual(recharge.outRecharge.getRenderedImage(), new double[][]{{40, 40, 40, -50, -50}}, 1E-9);
        checkMatrixEqual(recharge.outAvailableRecharge.getRenderedImage(), new double[][]{{40, 40, 40, -50, -50}}, 1E-9);
    }

    public void testRechargeRoutingNovalues() throws Exception {
        // novalues in precipitation and quickflow are considered 0 and do not stop the routing
        OmsSWYRechargeRouting recharge = rechargeRouting(line(100, NV, 100, 100, 100), line(10, NV, 10, 100, 100));

        checkMatrixEqual(recharge.outUpslopeSubsidy.getRenderedImage(), new double[][]{{0, 40, 0, 40, 0}}, 1E-9);
        checkMatrixEqual(recharge.outAet.getRenderedImage(), new double[][]{{50, 40, 50, 40, 0}}, 1E-9);
        checkMatrixEqual(recharge.outRecharge.getRenderedImage(), new double[][]{{40, -40, 40, -40, 0}}, 1E-9);
    }

    public void testBaseflowRouting() throws Exception {
        OmsSWYBaseflowRouting baseflow = baseflowRouting(line(40, 40, 40, -50, -50), line(0, 0, 0, 1, 1));

        checkMatrixEqual(baseflow.outLsum.getRenderedImage(), new double[][]{{40, 80, 120, 70, 20}}, 1E-9);
        checkMatrixEqual(baseflow.outBaseflow.getRenderedImage(), new double[][]{{40, 80, 120, 70, 20}}, 1E-9);
        checkMatrixEqual(baseflow.outB.getRenderedImage(), new double[][]{{40, 40, 40, 0, 0}}, 1E-9);
        checkMatrixEqual(baseflow.outVri.getRenderedImage(), new double[][]{{2, 2, 2, -2.5, -2.5}}, 1E-9);
        assertEquals(20.0, baseflow.outQb, 1E-9);
        assertEquals(1.0, baseflow.outVriSum, 1E-9);
    }

    public void testBaseflowRoutingNoRecharge() throws Exception {
        // dry month, all water evapotranspires: no NaN
        OmsSWYBaseflowRouting baseflow = baseflowRouting(line(0, 0, 0, 0, 0), line(0, 0, 0, 1, 1));

        checkMatrixEqual(baseflow.outB.getRenderedImage(), new double[][]{{0, 0, 0, 0, 0}}, 0);
        // vri is not defined if there is no recharge
        checkMatrixEqual(baseflow.outVri.getRenderedImage(), new double[][]{{NV, NV, NV, NV, NV}}, 0);
    }

    public void testBaseflowRoutingLongFlowpath() throws Exception {
        // the walk upstream must not depend on the stack size
        int n = 20000;
        double[] ones = new double[n];
        Arrays.fill(ones, 1);
        OmsSWYBaseflowRouting baseflow = baseflowRouting(line(ones), line(new double[n]));

        // with uniform recharge the cumulative baseflow equals the cumulative recharge
        double[] expected = new double[n];
        for( int i = 0; i < n; i++ ) {
            expected[i] = i + 1;
        }
        checkMatrixEqual(baseflow.outBaseflow.getRenderedImage(), new double[][]{expected}, 1E-6);
    }

    private OmsSWYRechargeRouting rechargeRouting( GridCoverage2D precipitation, GridCoverage2D quickflow ) throws Exception {
        OmsSWYRechargeRouting recharge = new OmsSWYRechargeRouting();
        recharge.pm = pm;
        recharge.inFlowdirections = line(EAST, EAST, EAST, EAST, EAST);
        recharge.inPrecipitation = precipitation;
        recharge.inQuickflow = quickflow;
        recharge.inPet = line(50, 50, 50, 50, 50);
        recharge.process();
        return recharge;
    }

    private OmsSWYBaseflowRouting baseflowRouting( GridCoverage2D recharge, GridCoverage2D net ) throws Exception {
        int cols = recharge.getRenderedImage().getWidth();
        double[] flow = new double[cols];
        Arrays.fill(flow, EAST);

        OmsSWYBaseflowRouting baseflow = new OmsSWYBaseflowRouting();
        baseflow.pm = pm;
        baseflow.inFlowdirections = line(flow);
        baseflow.inNet = net;
        baseflow.inRecharge = recharge;
        // gamma = 1, so the available recharge equals the recharge
        baseflow.inAvailableRecharge = recharge;
        baseflow.process();
        return baseflow;
    }

    private static GridCoverage2D line( double... values ) {
        RegionMap region = RegionMap.fromBoundsAndGrid(0, values.length, 0, 1, values.length, 1);
        return CoverageUtilities.buildCoverageWithNovalue("line", new double[][]{values}, region, HMTestMaps.getCrs(), true,
                NV);
    }
}
