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

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.cli.ModuleDescriptor;
import org.junit.Test;

/**
 * Tests the {@link ModuleCatalog} on the modules of the classpath.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestModuleCatalog {

    @Test
    public void testModules() throws Exception {
        ModuleCatalog catalog = ModuleCatalog.getDefault();

        assertTrue(String.valueOf(catalog.getModules().size()), catalog.getModules().size() > 150);
        assertNotNull(catalog.get("Pitfiller"));
        assertNotNull(catalog.get("ExtractNetwork"));
        // hidden in the toolbox, but useful in scripts
        assertNotNull(catalog.get("Mapcalc"));
        for( ModuleDescriptor module : catalog.getModules() ) {
            assertEquals(ModuleCatalog.MODULES_PACKAGE, module.getModuleClass().getPackageName());
            assertFalse(module.getName(), module.getName().startsWith("Oms"));
        }
        // the readers and writers used only inside other modules
        assertNull(catalog.get("RasterReader"));
    }

    @Test
    public void testSelect() throws Exception {
        ModuleCatalog catalog = ModuleCatalog.getDefault();

        assertEquals("Pitfiller", catalog.select("fill the pits of my dtm", 5).get(0).getName());
        assertEquals("ExtractNetwork", catalog.select("extract the stream network from the dtm", 5).get(0).getName());
        // a module named in the request comes first
        assertEquals("Tca", catalog.select("extract the network from the tca", 5).get(0).getName());
        List<String> diff = names(catalog.select("calculate the difference between two rasters", 3));
        assertTrue(diff.toString(), diff.contains("RasterDiff"));
        // by name, also in another case
        assertEquals("FlowDirections", catalog.select("run flowdirections on it", 5).get(0).getName());
        List<String> none = names(catalog.select("qqqq wwww", 5));
        assertTrue(none.toString(), none.isEmpty());
        assertEquals(2, catalog.select("raster", 2).size());
    }

    @Test
    public void testWordMatches() throws Exception {
        assertTrue(ModuleCatalog.matches("pits", "pits"));
        assertTrue(ModuleCatalog.matches("contour", "contours"));
        assertTrue(ModuleCatalog.matches("fill", "fills"));
        assertFalse(ModuleCatalog.matches("flow", "fluid"));
        assertFalse(ModuleCatalog.matches("network", "net"));
        assertFalse(ModuleCatalog.words("the file of the map").contains("the"));
    }

    @Test
    public void testReference() throws Exception {
        String reference = ModuleCatalog.getDefault().reference(ModuleCatalog.getDefault().get("Pitfiller"));

        assertTrue(reference, reference.startsWith("Module Pitfiller (Dem Manipulation): Fills the depressions"));
        assertTrue(reference, reference.contains("Notes: All the depressions are filled"));
        // the kinds of the data: the dem is the user's, or a depitted one
        assertTrue(reference, reference.contains("- inElev (input raster path, String): The digital elevation model. "
                + "[kind Elev, a file of the user, or from Pitfiller.outPit, DePitter.outPit, PitfillerBarnes.outPit]\n"));
        assertTrue(reference, reference.contains("- outPit (output raster path, String): The depitted elevation model. [kind Pit]\n"));
        assertTrue(reference, reference.contains("Usage:\ndef pitfiller = new Pitfiller()\npitfiller.inElev = \"...\"\n"
                + "pitfiller.outPit = \"...\"\npitfiller.process()\n"));

        String basin = ModuleCatalog.getDefault().reference(ModuleCatalog.getDefault().get("ExtractBasin"));
        assertTrue(basin, basin.contains("- outArea (result, double, read it after process())"));
        // what makes the drainage directions, the preferred modules first
        assertTrue(basin, basin.contains("The map of the drainage directions. [kind Flow, from FlowDirections.outFlow, "
                + "DrainDir.outFlow, DePitter.outFlow, PitfillerBarnes.outFlow, LeastCostFlowDirections.outFlow, ...]\n"));
        // the vector network has no producers and no description: no hint
        assertTrue(basin, basin.contains("before extracting the basin.\n"));
        // results and the optional fields are not set in the usage
        assertTrue(basin, basin.contains("extractBasin.inFlow = \"...\"\n"));
        assertTrue(basin, basin.contains("extractBasin.outBasin = \"...\"\n"));
        assertFalse(basin, basin.contains("extractBasin.outArea ="));
        assertFalse(basin, basin.contains("extractBasin.outOutlet ="));
    }

    @Test
    public void testIndex() throws Exception {
        ModuleCatalog catalog = ModuleCatalog.getDefault();
        String index = ModuleCatalog.index(List.of(catalog.get("Pitfiller"), catalog.get("FlowDirections")));

        assertTrue(index, index.startsWith("Dem Manipulation:\n- Pitfiller: Fills the depressions"));
        assertTrue(index, index.contains("- FlowDirections: "));
        for( String line : index.split("\n") ) {
            assertTrue(line, line.length() < 130);
        }
    }

    private static List<String> names( List<ModuleDescriptor> modules ) {
        List<String> names = new ArrayList<>();
        for( ModuleDescriptor module : modules ) {
            names.add(module.getName());
        }
        return names;
    }
}
