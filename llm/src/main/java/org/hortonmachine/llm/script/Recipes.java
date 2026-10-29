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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.log.Logger;

/**
 * Working scripts for common tasks, given to the model as examples when they are similar to the
 * request, together with the examples of the console.
 *
 * <p>They live next to this class, in the folder {@value #FOLDER}: one script per file, listed in
 * {@value #INDEX}. A recipe starts with two comment lines, the title and the keywords used to find
 * it, which are not shown to the model as part of the script:</p>
 *
 * <pre>
 * // Hydrological chain from a DEM: depitting, flow directions, contributing areas
 * // keywords: dem, pitfiller, flowdirections, tca, network
 * import org.hortonmachine.modules.*
 * ...
 * </pre>
 *
 * <p>Paths are placeholders as <code>/path/to/dem.tif</code>, declared at the top. TestRecipes
 * compiles every recipe and checks its use of the modules.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Recipes {

    /** The folder of the recipes, next to this class. */
    public static final String FOLDER = "recipes/";
    /** The file listing the recipes, one file name per line; # starts a comment. */
    public static final String INDEX = "recipes.txt";

    private static final String KEYWORDS_PREFIX = "// keywords:";

    /**
     * A recipe.
     *
     * @param file the name of its file.
     * @param title what it does.
     * @param keywords the words to find it by, comma separated.
     * @param script the script, without the title and keywords lines.
     */
    public record Recipe( String file, String title, String keywords, String script ) {
    }

    private static List<Recipe> defaultRecipes;

    private Recipes() {
    }

    /**
     * @return the recipes of {@value #INDEX}, read once; the broken ones are logged and skipped.
     */
    public static synchronized List<Recipe> getDefault() {
        if (defaultRecipes == null) {
            List<Recipe> recipes = new ArrayList<>();
            try {
                for( String file : listFiles() ) {
                    try {
                        recipes.add(parse(file, read(FOLDER + file)));
                    } catch (Exception e) {
                        Logger.INSTANCE.insertError("Recipes", "Can't read the recipe " + file, e);
                    }
                }
            } catch (Exception e) {
                Logger.INSTANCE.insertError("Recipes", "Can't read " + FOLDER + INDEX, e);
            }
            defaultRecipes = List.copyOf(recipes);
        }
        return defaultRecipes;
    }

    /**
     * @return the file names of {@value #INDEX}, in order.
     * @throws Exception if the index can't be read.
     */
    static List<String> listFiles() throws Exception {
        List<String> files = new ArrayList<>();
        for( String line : read(FOLDER + INDEX).split("\n") ) {
            String file = line.replaceAll("#.*", "").trim();
            if (!file.isEmpty()) {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * @param file the name of the file.
     * @param text the text of the recipe.
     * @return the recipe.
     * @throws IllegalArgumentException if the title or keywords line is missing.
     */
    static Recipe parse( String file, String text ) {
        String[] lines = text.replace("\r", "").split("\n", 3);
        if (lines.length < 3 || !lines[0].startsWith("//") || !lines[1].startsWith(KEYWORDS_PREFIX)) {
            throw new IllegalArgumentException(file + " must start with a title comment line and a " + KEYWORDS_PREFIX + " line.");
        }
        String title = lines[0].substring(2).trim();
        String keywords = lines[1].substring(KEYWORDS_PREFIX.length()).trim();
        return new Recipe(file, title, keywords, lines[2].strip() + "\n");
    }

    private static String read( String resource ) throws Exception {
        try (InputStream stream = Recipes.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
