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

import java.util.List;

/**
 * The answer of an {@link IScriptAssistant} to a request or a fix.
 *
 * @param text the whole answer as markdown, with the script, if any, in a groovy code block.
 * @param script the script of the answer that was checked, or <code>null</code> if the answer has none.
 * @param checked <code>true</code> if the script was compiled and its use of the modules checked.
 * @param checkError the errors of the last check, or <code>null</code> if it passed or none ran.
 * @param modules the names of the processing modules the script uses.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record ScriptAnswer( String text, String script, boolean checked, String checkError, List<String> modules ) {
}
