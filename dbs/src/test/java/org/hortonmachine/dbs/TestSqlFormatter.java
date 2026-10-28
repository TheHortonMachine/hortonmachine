package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hortonmachine.dbs.utils.SqlFormatter;
import org.junit.Test;

/**
 * Tests the {@link SqlFormatter}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestSqlFormatter {

    @Test
    public void testClausesListsAndConditions() {
        String sql = "SELECT e.id, p.idsap FROM event e JOIN gpspoint g ON g.id = e.gpspoint_id "
                + "WHERE e.value > 0 AND p.status = 1 ORDER BY e.id LIMIT 5";
        String expected = """
                SELECT e.id,
                    p.idsap
                FROM event e
                JOIN gpspoint g ON g.id = e.gpspoint_id
                WHERE e.value > 0
                    AND p.status = 1
                ORDER BY e.id
                LIMIT 5""";
        assertEquals(expected, SqlFormatter.format(sql));
    }

    @Test
    public void testSubqueriesAndFunctions() {
        String sql = "SELECT e.id, ST_Distance(g.the_geom, p.the_geom) AS d FROM event e CROSS JOIN LATERAL "
                + "(SELECT p.idsap, p.the_geom FROM pipe p ORDER BY ST_Distance(g.the_geom, p.the_geom) LIMIT 1) p";
        String expected = """
                SELECT e.id,
                    ST_Distance(g.the_geom, p.the_geom) AS d
                FROM event e
                CROSS JOIN LATERAL (
                    SELECT p.idsap,
                        p.the_geom
                    FROM pipe p
                    ORDER BY ST_Distance(g.the_geom, p.the_geom)
                    LIMIT 1
                ) p""";
        String formatted = SqlFormatter.format(sql);
        assertEquals(expected, formatted);
        // formatting again changes nothing
        assertEquals(expected, SqlFormatter.format(formatted));
    }

    @Test
    public void testLiteralsCommentsAndBetween() {
        String sql = "select a from t where x between 1 and 2 and name = 'a, from (b)' -- note\n and y = 1";
        String expected = """
                select a
                from t
                where x between 1 and 2
                    and name = 'a, from (b)' -- note
                    and y = 1""";
        assertEquals(expected, SqlFormatter.format(sql));
    }

    @Test
    public void testGroupByAndStatements() {
        String sql = "SELECT a, count(*) FROM t GROUP BY a, b HAVING count(*) > 1; SELECT 2";
        String expected = """
                SELECT a,
                    count(*)
                FROM t
                GROUP BY a,
                    b
                HAVING count(*) > 1;

                SELECT 2""";
        assertEquals(expected, SqlFormatter.format(sql));
        assertNull(SqlFormatter.format(null));
        assertEquals(" ", SqlFormatter.format(" "));
    }
}
