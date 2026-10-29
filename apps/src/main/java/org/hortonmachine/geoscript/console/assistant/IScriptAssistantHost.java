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
package org.hortonmachine.geoscript.console.assistant;

import org.hortonmachine.gears.assistant.ScriptContext;

/**
 * What the {@link ScriptAssistantPanel} needs from the scripting console.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public interface IScriptAssistantHost {

    /**
     * @return the console as the assistant needs to know it, with the variables of now.
     */
    ScriptContext getContext();

    /**
     * @return the script of the editor.
     */
    String getEditorScript();

    /**
     * @return the selected text of the editor, or <code>null</code> if nothing is selected.
     */
    String getSelectedScript();

    /**
     * Put a script in the editor, without running it.
     *
     * @param script the script.
     * @param replace <code>true</code> to replace the whole editor text, <code>false</code> to
     *          insert it at the caret, replacing the selection if any.
     */
    void addScriptToEditor( String script, boolean replace );

    /**
     * @param script the script to check.
     * @return the error of the last run of this script, with the relevant part of the stack trace,
     *          or <code>null</code> if it didn't fail or wasn't run.
     */
    String getLastError( String script );
}
