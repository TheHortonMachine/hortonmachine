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

import java.util.LinkedHashMap;
import java.util.Map;

import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.groovy.ClassIndex;
import org.hortonmachine.gears.utils.groovy.GroovyCompleter;
import org.hortonmachine.gears.utils.groovy.GroovyCompleter.Item;
import org.hortonmachine.gears.utils.groovy.GroovyCompleter.Kind;
import org.hortonmachine.gears.utils.groovy.GroovyCompleter.Result;
import org.hortonmachine.gears.utils.groovy.GroovyTypeResolver;

/**
 * Test the {@link GroovyCompleter}, shared by the scripting console and the notebooks.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestGroovyCompleter extends HMTestCase {

    private static ClassIndex index;
    private final Map<String, Object> variables = new LinkedHashMap<>();
    private GroovyCompleter completer;

    @Override
    protected void setUp() throws Exception {
        synchronized (TestGroovyCompleter.class) {
            if (index == null) {
                index = new ClassIndex();
                index.build();
            }
        }
        GroovyTypeResolver resolver = new GroovyTypeResolver(index, () -> getClass().getClassLoader(), () -> variables);
        completer = new GroovyCompleter(resolver);
    }

    private Result complete( String script ) {
        return completer.complete(script, script.length(), true);
    }

    private static Item find( Result result, String name ) {
        for( Item item : result.items() ) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public void testMembersOfBindingVariables() throws Exception {
        // the live values, as in the notebooks after a cell ran
        variables.put("text", "hello");
        Result result = complete("text.toU");
        assertTrue(result.scoped());
        assertEquals("toU", result.prefix());
        assertEquals(5, result.from());
        Item method = find(result, "toUpperCase");
        assertNotNull(method);
        assertEquals(Kind.METHOD, method.kind());
        assertEquals("String", method.type());
        assertNull(find(result, "return"));
    }

    public void testMembersOfDeclaredVariables() throws Exception {
        Result result = complete("def list = new ArrayList()\nlist.si");
        Item size = find(result, "size");
        assertNotNull(size);
        assertTrue(size.isMethod());
        assertTrue(size.params().isEmpty());
    }

    public void testModuleParameters() throws Exception {
        Result result = complete("def summary = new org.hortonmachine.gears.modules.r.summary.OmsRasterSummary()\nsummary.inR");
        Item inRaster = find(result, "inRaster");
        assertNotNull(inRaster);
        assertEquals(Kind.PARAMETER, inRaster.kind());
        assertEquals("input", inRaster.role());
        assertNotNull(inRaster.description());
    }

    public void testClassesAndImports() throws Exception {
        Result result = complete("OmsRasterSumm");
        Item clazz = find(result, "OmsRasterSummary");
        assertNotNull(clazz);
        assertEquals(Kind.CLASS, clazz.kind());
        assertEquals("org.hortonmachine.gears.modules.r.summary.OmsRasterSummary", clazz.fqn());
        assertTrue(clazz.needsImport());
        assertNotNull(completer.classDescription(clazz.fqn()));

        result = complete("import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary\nOmsRasterSumm");
        assertFalse(find(result, "OmsRasterSummary").needsImport());

        // packages in the imports
        result = complete("import org.hortonm");
        Item pkg = find(result, "hortonmachine");
        assertNotNull(pkg);
        assertEquals(Kind.PACKAGE, pkg.kind());
    }

    public void testKeywordsAndVariables() throws Exception {
        variables.put("raster", 1);
        Result result = complete("def rows = 3\nr");
        assertFalse(result.scoped());
        assertEquals(Kind.KEYWORD, find(result, "return").kind());
        assertEquals("binding", find(result, "raster").definedIn());
        assertEquals("script", find(result, "rows").definedIn());

        // without keywords, as the console that has its own
        result = completer.complete("r", 1, false);
        assertNull(find(result, "return"));
    }

    public void testEnteredText() throws Exception {
        assertEquals("abc_1", GroovyCompleter.enteredText("x.abc_1"));
        assertEquals("", GroovyCompleter.enteredText("x."));
    }
}
