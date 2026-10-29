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
package org.hortonmachine.llm.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.hortonmachine.cli.ModuleDescriptor;
import org.junit.Test;

/**
 * Tests the {@link DataKinds} and its curated file on the modules of the classpath.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestDataKinds {

    private static DataKinds kinds() {
        return ModuleCatalog.getDefault().getKinds();
    }

    @Test
    public void testKindOf() throws Exception {
        DataKinds kinds = kinds();
        assertEquals("Flow", kinds.kindOf("inFlow"));
        assertEquals("Flow", kinds.kindOf("outFlow"));
        // aliases
        assertEquals("Flow", kinds.kindOf("inDrain"));
        assertEquals("Elev", kinds.kindOf("inDtm"));
        // generic and numbered kinds say nothing, as names that are not in/out fields
        assertNull(kinds.kindOf("inRaster"));
        assertNull(kinds.kindOf("inRaster1"));
        assertNull(kinds.kindOf("inMap2"));
        assertNull(kinds.kindOf("pThres"));
        assertNull(kinds.kindOf("index"));
    }

    @Test
    public void testAccepts() throws Exception {
        DataKinds kinds = kinds();
        assertTrue(kinds.accepts("Elev", "Pit"));
        assertTrue(kinds.accepts("Flow", "Flow"));
        assertFalse(kinds.accepts("Pit", "Elev"));
        assertFalse(kinds.accepts("Flow", "Elev"));
    }

    @Test
    public void testWords() throws Exception {
        DataKinds kinds = kinds();
        assertEquals(Set.of("Elev"), kinds.kindsInWords("demPath"));
        assertEquals(Set.of("Elev"), kinds.kindsInWords("\"/path/to/your/dem.tif\""));
        assertEquals(Set.of("Elev", "Pit"), kinds.kindsInWords("dem_pit"));
        // whole words only
        assertEquals(Set.of(), kinds.kindsInWords("demo"));
    }

    @Test
    public void testProducers() throws Exception {
        ModuleCatalog catalog = ModuleCatalog.getDefault();
        DataKinds kinds = catalog.getKinds();
        ModuleDescriptor.Parameter inFlow = catalog.get("ExtractBasin").getParameter("inFlow");
        List<String> producers = kinds.producers(inFlow);
        // the preferred ones of the file first
        assertEquals(List.of("FlowDirections.outFlow", "DrainDir.outFlow", "DePitter.outFlow", "PitfillerBarnes.outFlow"),
                producers.subList(0, 4));
        // only of the same data type: the raster network, not the vector one
        assertTrue(kinds.producers(catalog.get("ExtractBasin").getParameter("inNetwork")).isEmpty());
    }

    @Test
    public void testOverview() throws Exception {
        String overview = kinds().overview();
        assertTrue(overview, overview.contains("- Flow: the D8 drainage directions, computed from the depitted elevation model (Pit). "
                + "Made by FlowDirections.outFlow, DrainDir.outFlow"));
        assertTrue(overview, overview.contains("- Elev: an elevation model"));
        // only the described kinds
        assertFalse(overview, overview.contains("- Aspect"));
    }

    /**
     * The curated file must only name kinds, modules and fields that exist, or its entries would
     * silently do nothing.
     */
    @Test
    public void testCuratedFile() throws Exception {
        Properties properties = new Properties();
        try (InputStream stream = DataKinds.class.getResourceAsStream(DataKinds.RESOURCE)) {
            assertNotNull(DataKinds.RESOURCE, stream);
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        ModuleCatalog catalog = ModuleCatalog.getDefault();
        for( String key : properties.stringPropertyNames() ) {
            if (key.equals("generic")) {
                continue;
            }
            String kind = key.substring(0, key.indexOf('.'));
            String property = key.substring(key.indexOf('.') + 1);
            assertTrue(key, Set.of("description", "aliases", "accepts", "producers", "words").contains(property));
            String value = properties.getProperty(key);
            if (property.equals("producers")) {
                for( String module : value.split(",") ) {
                    ModuleDescriptor descriptor = catalog.get(module.trim());
                    assertNotNull(key + ": no module " + module, descriptor);
                    assertTrue(key + ": " + module + " has no out" + kind, descriptor.getParameters().stream()
                            .anyMatch(p -> p.name.startsWith("out") && kind.equals(catalog.getKinds().kindOf(p.name))));
                }
            } else if (property.equals("accepts")) {
                for( String accepted : value.split(",") ) {
                    assertNotNull(key + ": " + accepted + " is not described", catalog.getKinds().getDescription(accepted.trim()));
                }
            }
        }
    }
}
