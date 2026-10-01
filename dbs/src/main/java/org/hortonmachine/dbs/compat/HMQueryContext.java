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
package org.hortonmachine.dbs.compat;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Opt-in control over the statements a thread creates through
 * {@link org.hortonmachine.dbs.spatialite.hm.HMConnection}: an optional query timeout
 * and the possibility to cancel the running statements from another thread (e.g. a
 * cancel button).
 *
 * <p>Nothing changes for code that does not run inside {@link #run(Callable)}.</p>
 *
 * <p>The timeout is applied only to plain statements, not to prepared statements, since
 * pooled prepared statements are cached and the timeout would leak to later users.
 * Prepared statements can still be canceled.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMQueryContext {

    /**
     * SQLState used by PostgreSQL for canceled statements (user cancel or timeout).
     */
    public static final String PG_QUERY_CANCELED_STATE = "57014";

    private static final ThreadLocal<HMQueryContext> CURRENT = new ThreadLocal<>();

    private final int timeoutSeconds;
    private final List<Statement> statements = new CopyOnWriteArrayList<>();
    private volatile boolean canceled = false;

    /**
     * @param timeoutSeconds the query timeout in seconds. 0 means no timeout.
     */
    public HMQueryContext( int timeoutSeconds ) {
        this.timeoutSeconds = Math.max(0, timeoutSeconds);
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Run some db work on the current thread with this context active.
     */
    public <T> T run( Callable<T> callable ) throws Exception {
        HMQueryContext previous = CURRENT.get();
        CURRENT.set(this);
        try {
            return callable.call();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /**
     * Cancel all statements created inside this context. Can be called from any thread.
     */
    public void cancel() {
        canceled = true;
        for( Statement statement : statements ) {
            try {
                statement.cancel();
            } catch (Exception e) {
                // statement already closed or driver does not support cancel
            }
        }
    }

    /**
     * @return the context active on the current thread or <code>null</code>.
     */
    public static HMQueryContext current() {
        return CURRENT.get();
    }

    /**
     * Register a newly created statement with the context of the current thread, if any.
     *
     * @param statement the statement.
     * @param applyTimeout if <code>true</code> the context timeout is set on the statement.
     */
    public static void register( Statement statement, boolean applyTimeout ) throws SQLException {
        HMQueryContext context = CURRENT.get();
        if (context == null) {
            return;
        }
        if (applyTimeout && context.timeoutSeconds > 0) {
            statement.setQueryTimeout(context.timeoutSeconds);
        }
        context.statements.add(statement);
        if (context.canceled) {
            statement.cancel();
        }
    }

    /**
     * Checks if the exception (or one of its causes) is a PostgreSQL "query canceled" error,
     * which is what both a client cancel and a query timeout produce.
     */
    public static boolean isQueryCanceledException( Throwable t ) {
        while( t != null ) {
            if (t instanceof SQLException && PG_QUERY_CANCELED_STATE.equals(((SQLException) t).getSQLState())) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
