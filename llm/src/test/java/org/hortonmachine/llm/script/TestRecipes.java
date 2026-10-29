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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.Phases;
import org.junit.Test;

import groovy.lang.GroovyClassLoader;

/**
 * Tests the {@link Recipes}: every recipe of the index is read, compiles, and uses the modules as
 * the {@link ScriptGuard} wants (fields, kinds of the inputs, process()).
 *
 * <p>The HM helper class lives in the applications, so its calls are not checked here.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestRecipes {

    @Test
    public void testAllRecipesAreRead() throws Exception {
        List<String> files = Recipes.listFiles();
        assertFalse(files.isEmpty());
        assertEquals("a recipe of the index could not be read", files.size(), Recipes.getDefault().size());
        for( Recipes.Recipe recipe : Recipes.getDefault() ) {
            assertFalse(recipe.file(), recipe.title().isBlank());
            assertFalse(recipe.file(), recipe.keywords().isBlank());
            assertFalse(recipe.file(), recipe.script().startsWith("// keywords"));
        }
    }

    @Test
    public void testRecipesCompile() throws Exception {
        for( Recipes.Recipe recipe : Recipes.getDefault() ) {
            CompilerConfiguration config = new CompilerConfiguration();
            try (GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader(), config)) {
                CompilationUnit unit = new CompilationUnit(config, null, loader);
                unit.addSource(recipe.file(), recipe.script());
                unit.compile(Phases.CLASS_GENERATION);
            } catch (MultipleCompilationErrorsException e) {
                fail(recipe.file() + ": " + e.getMessage());
            }
        }
    }

    @Test
    public void testRecipesPassTheGuard() throws Exception {
        for( Recipes.Recipe recipe : Recipes.getDefault() ) {
            ScriptGuard.Result result = ScriptGuard.check(recipe.script(), ModuleCatalog.getDefault());
            assertEquals(recipe.file(), List.of(), result.errors());
        }
    }

    @Test
    public void testRecipesArePlaceholders() throws Exception {
        // no real paths, hosts or keys of anybody's projects: recipes are published
        for( Recipes.Recipe recipe : Recipes.getDefault() ) {
            String script = recipe.script();
            assertFalse(recipe.file(), script.matches("(?s).*\"(/home|/Users|/storage|/media|[A-Z]:[/\\\\]).*"));
            assertFalse(recipe.file(), script.matches("(?s).*\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b.*"));
            assertFalse(recipe.file(), script.matches("(?s).*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*"));
        }
    }

    @Test
    public void testRecipesAreChosen() throws Exception {
        List<String> chosen = LlmScriptAssistant.chooseExamples(Map.of(), Recipes.getDefault(),
                "extract the watershed at a coordinate from my dem");
        assertTrue(chosen.toString(), chosen.get(0).startsWith("// Basin (watershed) draining to an outlet point"));
        // the title is shown, the keywords line is not
        assertFalse(chosen.get(0).contains("// keywords:"));

        chosen = LlmScriptAssistant.chooseExamples(Map.of(), Recipes.getDefault(), "mosaic the tiles of a folder");
        assertTrue(chosen.toString(), chosen.get(0).startsWith("// Mosaic all the rasters"));
    }
}
