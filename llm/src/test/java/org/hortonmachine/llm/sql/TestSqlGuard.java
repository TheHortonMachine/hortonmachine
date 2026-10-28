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
package org.hortonmachine.llm.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests the {@link SqlGuard}.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestSqlGuard {

    @Test
    public void testReadOnly() {
        assertTrue(SqlGuard.isReadOnly("SELECT * FROM roads"));
        assertTrue(SqlGuard.isReadOnly("  select name, replace(name, 'a', 'b') from roads;  "));
        assertTrue(SqlGuard.isReadOnly("WITH t AS (SELECT 1 AS a) SELECT a FROM t"));
        assertTrue(SqlGuard.isReadOnly("EXPLAIN QUERY PLAN SELECT * FROM roads"));
        assertTrue(SqlGuard.isReadOnly("PRAGMA table_info(roads)"));
        assertTrue(SqlGuard.isReadOnly("SELECT 1; SELECT 2"));
        // keywords inside literals, identifiers and comments don't count
        assertTrue(SqlGuard.isReadOnly("SELECT 'DROP TABLE x' AS \"delete\" FROM roads -- insert\n"));
        assertTrue(SqlGuard.isReadOnly("SELECT /* update */ 'it''s; DELETE' FROM roads"));
    }

    @Test
    public void testNotReadOnly() {
        assertFalse(SqlGuard.isReadOnly("DELETE FROM roads"));
        assertFalse(SqlGuard.isReadOnly("update roads set name = 'a'"));
        assertFalse(SqlGuard.isReadOnly("SELECT 1; DROP TABLE roads"));
        assertFalse(SqlGuard.isReadOnly("WITH d AS (DELETE FROM roads RETURNING *) SELECT * FROM d"));
        assertFalse(SqlGuard.isReadOnly("SELECT * INTO copy FROM roads"));
        assertFalse(SqlGuard.isReadOnly("PRAGMA journal_mode = WAL"));
        assertFalse(SqlGuard.isReadOnly("SELECT CreateSpatialIndex('roads', 'geom')"));
        assertFalse(SqlGuard.isReadOnly("REPLACE INTO roads VALUES (1)"));
        assertFalse(SqlGuard.isReadOnly(""));
        assertFalse(SqlGuard.isReadOnly("-- only a comment"));
    }

    @Test
    public void testApplyLimit() {
        assertEquals("SELECT * FROM roads\nLIMIT 100", SqlGuard.applyLimit("SELECT * FROM roads", 100));
        assertEquals("SELECT * FROM roads\nLIMIT 100", SqlGuard.applyLimit("SELECT * FROM roads ;  ", 100));
        assertEquals("SELECT * FROM roads\nLIMIT 5", SqlGuard.applyLimit("SELECT * FROM roads -- all of them", 5));
        assertEquals("SELECT * FROM roads WHERE id IN (SELECT id FROM t LIMIT 3)\nLIMIT 5",
                SqlGuard.applyLimit("SELECT * FROM roads WHERE id IN (SELECT id FROM t LIMIT 3)", 5));
        assertEquals("SELECT 'limit' FROM roads\nLIMIT 5", SqlGuard.applyLimit("SELECT 'limit' FROM roads", 5));
    }

    @Test
    public void testApplyLimitLeavesAlone() {
        assertEquals("SELECT * FROM roads LIMIT 10", SqlGuard.applyLimit("SELECT * FROM roads LIMIT 10", 100));
        assertEquals("SELECT * FROM roads FETCH FIRST 3 ROWS ONLY",
                SqlGuard.applyLimit("SELECT * FROM roads FETCH FIRST 3 ROWS ONLY", 100));
        assertEquals("SELECT 1; SELECT 2", SqlGuard.applyLimit("SELECT 1; SELECT 2", 100));
        assertEquals("PRAGMA table_info(roads)", SqlGuard.applyLimit("PRAGMA table_info(roads)", 100));
        assertEquals("SELECT * FROM roads", SqlGuard.applyLimit("SELECT * FROM roads", 0));
    }

    @Test
    public void testMaskKeepsPositions() {
        String sql = "SELECT 'a;b', \"c\"\"d\" /* x\ny */ FROM t -- z";
        String masked = SqlGuard.mask(sql);
        assertEquals(sql.length(), masked.length());
        assertFalse(masked.contains(";"));
        assertFalse(masked.contains("x"));
        assertTrue(masked.contains("\n"));
        assertTrue(masked.startsWith("SELECT "));
    }
}
