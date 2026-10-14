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
package org.hortonmachine.modules;

import java.awt.image.Raster;
import java.io.File;
import java.nio.file.Files;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * Runs the Seasonal Water Yield chain through the toolbox modules, on files, with the
 * synthetic data of the D8 regression test of InVEST (see TestSWYInvestRegression in hmachine).
 *
 * <p>The monthly rasters are given to the recharge routing as folders and name prefixes.</p>
 */
public class TestSWYChain extends HMTestCase {

    private static final int SIZE = 100;
    private static final RegionMap REGION = RegionMap.fromBoundsAndGrid(1180000, 1180000 + SIZE, 690000 - SIZE, 690000, SIZE,
            SIZE);

    public void testChainWithFolders() throws Exception {
        File folder = Files.createTempDirectory("hm-swy").toFile();
        try {
            File climate = new File(folder, "climate");
            File quickflows = new File(folder, "quickflow");
            File pets = new File(folder, "pet");
            climate.mkdirs();
            quickflows.mkdirs();
            pets.mkdirs();

            // all cells drain west, the stream are the 50 western columns
            double[][] flow = new double[SIZE][SIZE];
            double[][] net = new double[SIZE][SIZE];
            double[][] curveNumber = new double[SIZE][SIZE];
            double[][] kc = new double[SIZE][SIZE];
            for( int r = 0; r < SIZE; r++ ) {
                boolean north = r < SIZE / 2;
                int soilGroup = r % 4 + 1;
                for( int c = 0; c < SIZE; c++ ) {
                    flow[r][c] = Direction.W.getFlow();
                    net[r][c] = c < 50 ? 1 : 0;
                    curveNumber[r][c] = soilGroup > 2 ? 1 : north ? 50 : soilGroup == 1 ? 72 : 82;
                    kc[r][c] = north ? 0.7 : 0.4;
                }
            }
            flow[0][0] = Direction.N.getFlow();
            String flowPath = write(new File(folder, "flow.tif"), flow);
            String netPath = write(new File(folder, "net.tif"), net);
            String cnPath = write(new File(folder, "cn.tif"), curveNumber);
            String kcPath = write(new File(folder, "kc.tif"), kc);
            String eventsPath = write(new File(folder, "events.tif"), constant(1));

            for( int month = 1; month <= 12; month++ ) {
                String precipitation = write(new File(climate, "prcp_" + month + ".tif"), constant(month + 10));
                String et0 = write(new File(climate, "et0_" + month + ".tif"), constant(month));

                SWYQuickflow quickflow = new SWYQuickflow();
                quickflow.inRainfall = precipitation;
                quickflow.inCurveNumber = cnPath;
                quickflow.inNet = netPath;
                quickflow.inNumberOfEvents = eventsPath;
                quickflow.outQuickflow = new File(quickflows, "qf_" + month + ".tif").getAbsolutePath();
                quickflow.process();

                PotentialEvapotranspiredWaterVolume pet = new PotentialEvapotranspiredWaterVolume();
                pet.inCropCoefficient = kcPath;
                pet.inReferenceEtp = et0;
                pet.outputPet = new File(pets, "pet_" + month + ".tif").getAbsolutePath();
                pet.process();
            }

            SWYRechargeRouting recharge = new SWYRechargeRouting();
            recharge.inFlowdirections = flowPath;
            recharge.inPrecipitationFolder = climate.getAbsolutePath();
            recharge.pPrecipitationPrefix = "prcp_";
            recharge.inQuickflowFolder = quickflows.getAbsolutePath();
            recharge.pQuickflowPrefix = "qf_";
            recharge.inPetFolder = pets.getAbsolutePath();
            recharge.pPetPrefix = "pet_";
            recharge.outRecharge = new File(folder, "l.tif").getAbsolutePath();
            recharge.outAvailableRecharge = new File(folder, "l_avail.tif").getAbsolutePath();
            recharge.outAet = new File(folder, "aet.tif").getAbsolutePath();
            recharge.outUpslopeSubsidy = new File(folder, "l_sum_avail.tif").getAbsolutePath();
            recharge.process();

            SWYBaseflowRouting baseflow = new SWYBaseflowRouting();
            baseflow.inFlowdirections = flowPath;
            baseflow.inNet = netPath;
            baseflow.inRecharge = recharge.outRecharge;
            baseflow.inAvailableRecharge = recharge.outAvailableRecharge;
            baseflow.outB = new File(folder, "b.tif").getAbsolutePath();
            baseflow.outBaseflow = new File(folder, "b_sum.tif").getAbsolutePath();
            baseflow.outLsum = new File(folder, "l_sum.tif").getAbsolutePath();
            baseflow.outVri = new File(folder, "vri.tif").getAbsolutePath();
            baseflow.process();

            // the values of the InVEST test: mean recharge, vri sum and monthly baseflow volume
            assertEquals(52.9128, baseflow.outQb / (SIZE * SIZE), 52.9128 * 1E-5);
            assertEquals(1.0, baseflow.outVriSum, 1E-6);
            double monthlyBaseflow = sum(OmsRasterReader.readRaster(baseflow.outB)) * 0.001 / 12;
            assertEquals(61.969, monthlyBaseflow, 61.969 * 1E-5);
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }

    public void testMissingInterval() throws Exception {
        File folder = Files.createTempDirectory("hm-swy").toFile();
        try {
            String flowPath = write(new File(folder, "flow.tif"), constant(Direction.W.getFlow()));
            for( int month = 1; month <= 3; month++ ) {
                write(new File(folder, "prcp_" + month + ".tif"), constant(10));
                write(new File(folder, "pet_" + month + ".tif"), constant(10));
                if (month != 2) {
                    write(new File(folder, "qf_" + month + ".tif"), constant(1));
                }
            }
            SWYRechargeRouting recharge = new SWYRechargeRouting();
            recharge.inFlowdirections = flowPath;
            recharge.inPrecipitationFolder = folder.getAbsolutePath();
            recharge.pPrecipitationPrefix = "prcp_";
            recharge.inQuickflowFolder = folder.getAbsolutePath();
            recharge.pQuickflowPrefix = "qf_";
            recharge.inPetFolder = folder.getAbsolutePath();
            recharge.pPetPrefix = "pet_";
            try {
                recharge.process();
                fail("The quickflow of month 2 is missing.");
            } catch (Exception e) {
                assertTrue(e.getMessage(), e.getMessage().contains("quickflow [1, 3]"));
            }
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }

    private static double[][] constant( double value ) {
        double[][] data = new double[SIZE][SIZE];
        for( double[] row : data ) {
            java.util.Arrays.fill(row, value);
        }
        return data;
    }

    private static String write( File file, double[][] data ) throws Exception {
        GridCoverage2D raster = CoverageUtilities.buildCoverageWithNovalue(file.getName(), data, REGION, HMTestMaps.crs, true,
                HMConstants.doubleNovalue);
        OmsRasterWriter.writeRaster(file.getAbsolutePath(), raster);
        return file.getAbsolutePath();
    }

    private static double sum( GridCoverage2D raster ) {
        Raster data = raster.getRenderedImage().getData();
        double sum = 0;
        for( int r = 0; r < data.getHeight(); r++ ) {
            for( int c = 0; c < data.getWidth(); c++ ) {
                double value = data.getSampleDouble(c, r, 0);
                if (!HMConstants.isNovalue(value, HMConstants.doubleNovalue)) {
                    sum += value;
                }
            }
        }
        return sum;
    }
}
