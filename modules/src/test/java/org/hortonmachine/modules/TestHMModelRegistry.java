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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMModelRegistry;
import org.hortonmachine.gears.modules.r.cutout.OmsCutOut;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.hortonmachine.lesto.modules.filter.LasHeightDistribution;
import org.junit.Test;

import oms3.annotations.Execute;

/**
 * Tests the SPI based discovery of the modules.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestHMModelRegistry {

    /**
     * A module that is registered only through the service file created in the test,
     * as an external jar would do.
     */
    public static class ExternalModule extends HMModel {
        @Execute
        public void process() {
        }
    }

    @Test
    public void testModulesOfAllJarsAreRegistered() {
        List<Class< ? extends HMModel>> classes = HMModelRegistry.getModelClasses();
        // gears, hmachine, lesto and modules
        assertTrue(classes.contains(OmsCutOut.class));
        assertTrue(classes.contains(OmsPitfiller.class));
        assertTrue(classes.contains(LasHeightDistribution.class));
        assertTrue(classes.contains(Pitfiller.class));

        assertEquals(Pitfiller.class, HMModelRegistry.getModelClass("Pitfiller"));
        assertEquals(OmsPitfiller.class, HMModelRegistry.getModelClass("OmsPitfiller"));
        assertNull(HMModelRegistry.getModelClass("NotExistingModule"));

        // test classes are not processed
        assertFalse(classes.contains(ExternalModule.class));
    }

    @Test
    public void testExternalServiceFileIsRegistered() throws Exception {
        Path tmp = Files.createTempDirectory("hm-registry-test");
        File serviceFile = tmp.resolve(HMModelRegistry.SERVICE_FILE).toFile();
        try {
            serviceFile.getParentFile().mkdirs();
            List<String> lines = Arrays.asList(//
                    "# external modules", //
                    ExternalModule.class.getName(), //
                    "org.hortonmachine.notexisting.MissingModule", //
                    String.class.getName() //
            );
            Files.write(serviceFile.toPath(), lines, StandardCharsets.UTF_8);

            try (URLClassLoader classLoader = new URLClassLoader(new URL[]{tmp.toUri().toURL()},
                    getClass().getClassLoader())) {
                List<Class< ? extends HMModel>> classes = HMModelRegistry.loadModelClasses(classLoader);
                assertTrue(classes.contains(ExternalModule.class));
                // the modules of the HortonMachine jars are still there
                assertTrue(classes.contains(Pitfiller.class));
                // broken entries are skipped
                for( Class< ? extends HMModel> clazz : classes ) {
                    assertFalse(clazz.getName().contains("notexisting"));
                }
            }
        } finally {
            serviceFile.delete();
            serviceFile.getParentFile().delete();
            serviceFile.getParentFile().getParentFile().delete();
            tmp.toFile().delete();
        }
    }
}
