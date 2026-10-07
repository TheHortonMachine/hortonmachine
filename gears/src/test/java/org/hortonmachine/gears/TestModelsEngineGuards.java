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
package org.hortonmachine.gears;

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;

/**
 * Test the guards that keep processing from running forever: loops in flow directions and
 * interrupted threads.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestModelsEngineGuards extends HMTestCase {
    private static final double N = -9999.0;

    public void testFlowLoopIsReportedInsteadOfHanging() throws Exception {
        // the middle row: a source flowing east into two cells that drain into each other (east, west)
        double[][] flow = {//
                {N, N, N, N, N}, //
                {1, 1, 1, 5, N}, //
                {N, N, N, N, N}};
        double[][] attributes = {//
                {N, N, N, N, N}, //
                {1, 1, 1, 1, N}, //
                {N, N, N, N, N}};
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 5, 0, 3, 5, 3);
        try (HMRaster flowRaster = raster("flow", region, flow);
                HMRaster attributeRaster = raster("attributes", region, attributes);
                HMRaster markedRaster = raster("marked", region, null)) {
            ModelsEngine.markHillSlopeWithLinkValue(flowRaster, attributeRaster, markedRaster, pm);
            fail("A loop in the flow directions must be reported.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("The flow directions loop"));
        }
    }

    public void testMonitorsStopInterruptedThreads() throws Exception {
        IHMProgressMonitor[] monitors = {new LogProgressMonitor(), new PrintStreamProgressMonitor(System.out, System.err),
                new DummyProgressMonitor()};
        for( IHMProgressMonitor monitor : monitors ) {
            monitor.beginTask("task", 10);
            monitor.worked(1);
            assertFalse(monitor.isCanceled());

            Thread.currentThread().interrupt();
            try {
                assertTrue(monitor.isCanceled());
                monitor.worked(1);
                fail(monitor.getClass().getSimpleName() + " must stop an interrupted thread.");
            } catch (ModelsUserCancelException e) {
                // expected
            } finally {
                // clear the flag, not to disturb other tests
                Thread.interrupted();
            }
        }
    }

    private static HMRaster raster( String name, RegionMap region, double[][] data ) {
        HMRaster.HMRasterWritableBuilder builder = new HMRaster.HMRasterWritableBuilder().setName(name).setRegion(region)
                .setCrs(HMTestMaps.getCrs()).setNoValue(N);
        if (data != null) {
            builder.setData(data);
        }
        return builder.build();
    }
}
