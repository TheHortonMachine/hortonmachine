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

/**
 * The options of a request to an {@link IScriptAssistant}.
 *
 * @param moduleReferences how many modules, the most relevant to the request, are described
 *          to the model with all their parameters; the others are only listed by name.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record ScriptAssistOptions( int moduleReferences ) {

    /**
     * The modules described in full by {@link #defaults()}.
     */
    public static final int DEFAULT_MODULE_REFERENCES = 6;

    /**
     * @return the {@link #DEFAULT_MODULE_REFERENCES}.
     */
    public static ScriptAssistOptions defaults() {
        return new ScriptAssistOptions(DEFAULT_MODULE_REFERENCES);
    }
}
