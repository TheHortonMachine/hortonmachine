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
package org.hortonmachine.gears.assistant;

import java.util.Map;

/**
 * What a scripting console tells an {@link IScriptAssistant} about itself.
 *
 * @param imports the imports the scripts of the console usually start with, as import lines.
 * @param helperReference the reference of the helper class of the console (methods with their
 *          description), as text.
 * @param examples working example scripts, by title.
 * @param variables the variables a script can use, as left by the earlier runs, from name to
 *          type; only names and types, never values.
 * @param checker compiles a script without running it, or <code>null</code> if the console can't.
 * @param editorScript the script the user is working on, which the answers continue, or
 *          <code>null</code> to not send it: unlike the variables it holds values (paths, hosts),
 *          so it is sent only if the user wants.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record ScriptContext( String imports, String helperReference, Map<String, String> examples,
        Map<String, String> variables, IScriptChecker checker, String editorScript ) {

    /**
     * @param script the script of the editor, or <code>null</code>.
     * @return the same context with that editor script.
     */
    public ScriptContext withEditorScript( String script ) {
        return new ScriptContext(imports, helperReference, examples, variables, checker, script);
    }
}
