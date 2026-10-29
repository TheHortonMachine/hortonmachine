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
 * The answer of an {@link ISqlAssistant}.
 *
 * <p>When <code>readOnly</code> is <code>false</code> the SQL changes the database (or
 * couldn't be recognized as a pure query), and the user has to confirm before running it.</p>
 *
 * <p>Read-only SQL is checked by the database before it is given back: the database only plans
 * it (<code>EXPLAIN</code>), without running it, which finds wrong table or column names and
 * functions the dialect lacks. Rejected SQL is sent back to the model to be corrected a few times;
 * if the database still rejects it, the error is in <code>checkError</code>.</p>
 *
 * @param text the whole answer as markdown, with the SQL, if any, in a sql code block.
 * @param sql the SQL of the answer that was checked, or <code>null</code> if the answer has none.
 * @param readOnly <code>true</code> if the SQL only reads data.
 * @param limitApplied <code>true</code> if a limit on the rows was added to the SQL of the model.
 * @param checked <code>true</code> if the database checked the SQL (only read-only SQL is checked).
 * @param checkError the error of the database for the final SQL, or <code>null</code> if it was
 *          accepted or not checked.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record SqlAnswer( String text, String sql, boolean readOnly, boolean limitApplied, boolean checked,
        String checkError ) {
}
