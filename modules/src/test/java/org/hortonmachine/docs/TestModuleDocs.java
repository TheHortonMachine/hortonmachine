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
package org.hortonmachine.docs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.junit.Test;

/**
 * Checks that the module reference fragments of the manual are in sync with the module annotations.
 *
 * <p>If this fails, regenerate the fragments as described in docs/manual/README.md and commit them.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestModuleDocs {

    private static final File MANUAL_MODULES = new File("../docs/manual/modules");

    @Test
    public void testGeneratedFragmentsAreUpToDate() throws Exception {
        File listFile = new File(MANUAL_MODULES, "modules.txt");
        assertTrue("Missing modules list: " + listFile.getAbsolutePath(), listFile.exists());
        File generatedFolder = new File(MANUAL_MODULES, "generated");

        Map<String, String> expected = ModuleDocsGenerator.generate(ModuleDocsGenerator.readModulesList(listFile));
        for( Map.Entry<String, String> entry : expected.entrySet() ) {
            File fragment = new File(generatedFolder, entry.getKey());
            assertTrue("Missing generated fragment, regenerate the module docs: " + fragment, fragment.exists());
            String committed = Files.readString(fragment.toPath(), StandardCharsets.UTF_8);
            assertEquals("Outdated fragment, regenerate the module docs: " + fragment, entry.getValue(), committed);
        }
    }
}
