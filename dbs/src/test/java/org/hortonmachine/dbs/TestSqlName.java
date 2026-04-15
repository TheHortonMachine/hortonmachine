package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;

import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TableName;
import org.junit.Test;

public class TestSqlName {

    @Test
    public void testQuotedIdentifierRendering() {
        SqlName name = SqlName.m("123 table");
        assertEquals("\"123 table\"", name.fixedDoubleName);
        assertEquals("\"123 table\"", name.fixedName);
        assertEquals("[123 table]", name.bracketName);
    }

    @Test
    public void testSchemaQualifiedRendering() {
        SqlName name = SqlName.qualified("my schema", "my.table");
        assertEquals("my schema.my.table", name.getFullname());
        assertEquals("\"my schema\".\"my.table\"", name.fixedDoubleName);
        assertEquals("[my schema].[my.table]", name.bracketName);
        assertEquals("my schema.my.table", name.toString());
    }

    @Test
    public void testMultiDotNameIsNotCorrupted() {
        SqlName name = SqlName.m("a.b.c");
        assertEquals("a.b.c", name.name);
        assertEquals(null, name.schema);
        assertEquals("\"a.b.c\"", name.fixedDoubleName);
    }

    @Test
    public void testQuotedInputIsNormalized() {
        SqlName name = SqlName.m("\"123 table\"");
        assertEquals("123 table", name.name);
        assertEquals(null, name.schema);
        assertEquals("\"123 table\"", name.fixedDoubleName);

        SqlName qualified = SqlName.m("\"my schema\".\"123 table\"");
        assertEquals("my schema", qualified.schema);
        assertEquals("123 table", qualified.name);
        assertEquals("\"my schema\".\"123 table\"", qualified.fixedDoubleName);
    }

    @Test
    public void testTableNameReusesSqlIdentifierLogic() {
        TableName tableName = new TableName("mixed Case", "public", ETableType.TABLE);
        assertEquals("public.mixed Case", tableName.getFullName());
        assertEquals("public.\"mixed Case\"", tableName.fixedDoubleName);
        assertEquals("public_mixed_Case", tableName.nameForIndex());
        assertEquals(tableName.fixedDoubleName, tableName.toSqlName().fixedDoubleName);
    }
}
