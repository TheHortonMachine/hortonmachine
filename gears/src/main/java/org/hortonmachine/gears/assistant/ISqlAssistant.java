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
import java.util.function.Consumer;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * An assistant that writes and explains SQL for a database.
 *
 * <p>The schema of the database (tables, columns, primary keys, geometry columns with
 * type and SRID) and its dialect are read from the connection and given to the model,
 * the data only if asked through {@link SqlAssistOptions#includeSampleRows()}.</p>
 *
 * <p>The SQL is only generated, never executed: running it is up to the caller.</p>
 *
 * <p>The assistant keeps no state: to continue a discussion, pass the earlier exchanges as
 * <code>history</code>, oldest first (an empty list starts a new one). Only the most recent
 * turns may be sent to the model, to stay within its context.</p>
 *
 * <p>In all methods <code>onToken</code> receives the raw text of the answer while it
 * arrives (can be <code>null</code>), and <code>pm</code> is used to cancel the request. When
 * SQL is rejected by the database and sent back to the model, <code>pm.beginTask</code> is
 * called with what happens: the text <code>onToken</code> receives after it is a new answer,
 * which replaces the rejected one.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public interface ISqlAssistant {

    /**
     * Answer a question: write the SQL that answers it, or answer about databases (how to connect
     * to one, what the viewer can do, concepts like SRIDs or spatial indexes).
     *
     * <p>It works also without a connected database, for example to help connecting to one. With
     * one, the SQL of the answer, if any, is checked like the one of {@link #fix}.</p>
     *
     * @param db the connected database, whose schema is given to the model, or <code>null</code> if none.
     * @param question the question, in natural language.
     * @param history the earlier turns of the discussion, oldest first.
     * @param options the options of the request.
     * @param onToken receives the answer while it arrives, or <code>null</code>.
     * @param pm the monitor used to cancel the request.
     * @return the answer, with its SQL if it has some.
     * @throws Exception if the request fails or is canceled.
     */
    SqlAnswer ask( ADb db, String question, List<AssistantTurn> history, SqlAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception;

    /**
     * Fix SQL that failed on the database.
     *
     * @param db the database the SQL was run on.
     * @param sql the SQL that failed.
     * @param dbError the error message given by the database.
     * @param history the earlier turns of the discussion, oldest first.
     * @param options the options of the request.
     * @param onToken receives the answer while it arrives, or <code>null</code>.
     * @param pm the monitor used to cancel the request.
     * @return the fixed SQL with an explanation of the fix.
     * @throws Exception if the request fails or is canceled.
     */
    SqlAnswer fix( ADb db, String sql, String dbError, List<AssistantTurn> history, SqlAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception;

    /**
     * Explain what some SQL does.
     *
     * @param db the database the SQL is for.
     * @param sql the SQL to explain.
     * @param history the earlier turns of the discussion, oldest first.
     * @param options the options of the request.
     * @param onToken receives the answer while it arrives, or <code>null</code>.
     * @param pm the monitor used to cancel the request.
     * @return the explanation.
     * @throws Exception if the request fails or is canceled.
     */
    String explain( ADb db, String sql, List<AssistantTurn> history, SqlAssistOptions options, Consumer<String> onToken,
            IHMProgressMonitor pm ) throws Exception;
}
