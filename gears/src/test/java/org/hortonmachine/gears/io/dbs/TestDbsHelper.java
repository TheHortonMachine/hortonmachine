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
}
