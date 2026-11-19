package org.hortonmachine.gears.io.dbs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestDbsHelper {

    @Test
    public void testExtractFirstTableTokenHandlesQuotedNames() {
        assertEquals("\"123 multipoly\"", DbsHelper.extractFirstTableToken("\"123 multipoly\" m"));
        assertEquals("\"my schema\".\"123 multipoly\"", DbsHelper.extractFirstTableToken("\"my schema\".\"123 multipoly\" m"));
        assertEquals("[123 multipoly]", DbsHelper.extractFirstTableToken("[123 multipoly] m"));
        assertEquals("plain_table", DbsHelper.extractFirstTableToken("plain_table t"));
    }

    @Test
    public void testIndexOfMainFromSkipsCtesSubqueriesAndQuotes() {
        assertEquals("MUPOLYGON", mainTable("WITH ranked AS (\n SELECT c.mukey FROM component c\n"
                + " LEFT JOIN chorizon h ON h.cokey = c.cokey WHERE c.x = 'Yes')\n"
                + "SELECT p.MUKEY, p.Shape\nFROM MUPOLYGON p JOIN ranked r ON r.mukey = p.MUKEY;"));
        assertEquals("roads", mainTable("select (select count(*) from other) as n, geom from roads"));
        assertEquals("roads", mainTable("select 'from x' as a, \"from\" , fromdate from roads"));
        assertEquals("roads", mainTable("select a -- from x\n, /* from y */ geom FROM\n\troads r"));
        assertEquals("plain", mainTable("select * from plain"));
        assertEquals(-1, DbsHelper.indexOfMainFrom("select 1"));
    }

    private static String mainTable( String sql ) {
        int index = DbsHelper.indexOfMainFrom(sql);
        return DbsHelper.extractFirstTableToken(sql.substring(index + 4).trim());
    }
}
