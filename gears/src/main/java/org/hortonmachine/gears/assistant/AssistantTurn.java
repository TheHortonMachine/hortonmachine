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
 * An earlier exchange of a discussion with an assistant, passed back with the next request so
 * that follow-up questions are understood.
 *
 * <p>The assistants are stateless: the caller keeps the turns and decides when a discussion
 * starts over.</p>
 *
 * @param question what the user asked.
 * @param answer the text of the answer (the explanation, for answers with code).
 * @param code the code of the answer (SQL or a script), or <code>null</code> for answers that are only text.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record AssistantTurn( String question, String answer, String code ) {
}
