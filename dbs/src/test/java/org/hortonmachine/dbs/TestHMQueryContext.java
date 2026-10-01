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
package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.Statement;

import org.hortonmachine.dbs.compat.HMQueryContext;
import org.junit.Test;

public class TestHMQueryContext {

    private static class FakeStatement {
        int timeout = -1;
        int cancelCount = 0;

        Statement proxy() {
            return (Statement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Statement.class},
                    ( p, method, args ) -> {
                        switch( method.getName() ) {
                        case "setQueryTimeout":
                            timeout = (Integer) args[0];
                            return null;
                        case "cancel":
                            cancelCount++;
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }
    }

    @Test
    public void testNoContextLeavesStatementsUntouched() throws Exception {
        FakeStatement statement = new FakeStatement();
        HMQueryContext.register(statement.proxy(), true);
        assertEquals(-1, statement.timeout);
        assertNull(HMQueryContext.current());
    }

    @Test
    public void testTimeoutAndCancel() throws Exception {
        HMQueryContext context = new HMQueryContext(30);
        FakeStatement plain = new FakeStatement();
        FakeStatement prepared = new FakeStatement();
        context.run(() -> {
            assertSame(context, HMQueryContext.current());
            HMQueryContext.register(plain.proxy(), true);
            HMQueryContext.register(prepared.proxy(), false);
            return null;
        });
        assertNull(HMQueryContext.current());
        assertEquals(30, plain.timeout);
        // prepared statements are pooled, the timeout must not be applied
        assertEquals(-1, prepared.timeout);

        assertFalse(context.isCanceled());
        context.cancel();
        assertTrue(context.isCanceled());
        assertEquals(1, plain.cancelCount);
        assertEquals(1, prepared.cancelCount);
    }

    @Test
    public void testZeroTimeoutIsNotApplied() throws Exception {
        HMQueryContext context = new HMQueryContext(0);
        FakeStatement plain = new FakeStatement();
        context.run(() -> {
            HMQueryContext.register(plain.proxy(), true);
            return null;
        });
        assertEquals(-1, plain.timeout);
    }

    @Test
    public void testNestedContextsRestorePrevious() throws Exception {
        HMQueryContext outer = new HMQueryContext(10);
        HMQueryContext inner = new HMQueryContext(0);
        outer.run(() -> {
            inner.run(() -> {
                assertSame(inner, HMQueryContext.current());
                return null;
            });
            assertSame(outer, HMQueryContext.current());
            return null;
        });
        assertNull(HMQueryContext.current());
    }

    @Test
    public void testCanceledExceptionDetection() {
        SQLException canceled = new SQLException("canceling statement due to user request", "57014");
        assertTrue(HMQueryContext.isQueryCanceledException(canceled));
        assertTrue(HMQueryContext.isQueryCanceledException(new RuntimeException(canceled)));
        assertFalse(HMQueryContext.isQueryCanceledException(new SQLException("syntax error", "42601")));
        assertFalse(HMQueryContext.isQueryCanceledException(null));
    }
}
