/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.dbs.spatialite.jgt;

import java.sql.Savepoint;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.IJGTConnection;
import org.jgrasstools.dbs.compat.ITransactionExecuter;

/**
 * The transaction executor for the jgrasstools part.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class JGTTransactionExecuter implements ITransactionExecuter {

    private ASpatialDb db;
    private Savepoint savepoint;
    private boolean autoCommitEnabled;

    private IJGTConnection conn;

    public JGTTransactionExecuter( ASpatialDb db ) {
        this.db = db;
    }

    public void execute() throws Exception {
        conn = db.getConnection();
        savepoint = conn.setSavepoint();
        autoCommitEnabled = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            executeInTransaction();

            conn.commit();
        } catch (Exception e) {
            conn.rollback(savepoint);
            throw new Exception(e);
        } finally {
            conn.setAutoCommit(autoCommitEnabled);
        }
    }

}
