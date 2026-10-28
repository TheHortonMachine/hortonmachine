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
package org.hortonmachine.database.assistant;

import org.hortonmachine.dbs.compat.ADb;

/**
 * What the {@link SqlAssistantPanel} needs from the database viewer.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public interface ISqlAssistantHost {

    /**
     * @return the connected database, or <code>null</code> if none.
     */
    ADb getDatabase();

    /**
     * @return the SQL in the current editor.
     */
    String getEditorSql();

    /**
     * Add SQL to the current editor, without running it.
     *
     * @param sql the SQL to add.
     */
    void addSqlToEditor( String sql );

    /**
     * @param sql the SQL to check.
     * @return the error the database gave the last time this SQL was run, or <code>null</code>
     *          if it didn't fail or wasn't run.
     */
    String getLastError( String sql );
}
