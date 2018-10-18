package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Main tests for normal dbs
 */
@Ignore 
public class TestPostgresDbsMain {

    private static final String TABLE1 = "table1";
    private static final String TABLE2 = "table2";
    private static final EDb DB_TYPE = EDb.POSTGRES;

    private static ADb db;

    @BeforeClass
    public static void createDb() throws Exception {

        ADatabaseSyntaxHelper dt = DB_TYPE.getDatabaseSyntaxHelper();
        db = DB_TYPE.getDb();
        db.setCredentials("test", "test");
        db.open("localhost:5432/test");

        db.createTable(TABLE1, "id " + dt.INTEGER() + " PRIMARY KEY", "name " + dt.TEXT(), "temperature " + dt.REAL());
        String[] inserts = {//
                "INSERT INTO " + TABLE1 + " VALUES(1, 'Tscherms', 36.0);", //
                "INSERT INTO " + TABLE1 + " VALUES(2, 'Meran', 34.0);", //
                "INSERT INTO " + TABLE1 + " VALUES(3, 'Bozen', 42.0);", //
        };
        for( String insert : inserts ) {
            db.executeInsertUpdateDeleteSql(insert);
        }
        db.createTable(TABLE2, "id " + dt.INTEGER() + " PRIMARY KEY", "table1id " + dt.INTEGER(),
                "FOREIGN KEY (table1id) REFERENCES " + TABLE1 + "(id)");
        inserts = new String[]{//
                "INSERT INTO " + TABLE2 + " VALUES(1, 1);", //
                "INSERT INTO " + TABLE2 + " VALUES(2, 2);", //
                "INSERT INTO " + TABLE2 + " VALUES(3, 3);", //
        };
        for( String insert : inserts ) {
            db.executeInsertUpdateDeleteSql(insert);
        }

    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (db.hasTable(TABLE1)) {
            db.executeInsertUpdateDeleteSql("drop table " + TABLE1 + " cascade");
        }
        if (db.hasTable(TABLE2)) {
            db.executeInsertUpdateDeleteSql("drop table " + TABLE2);
        }
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void testTableOps() throws Exception {
        assertTrue(db.hasTable(TABLE1));

        List<String[]> tableColumns = db.getTableColumns(TABLE1);
        assertTrue(tableColumns.size() == 3);
        assertEquals("id", tableColumns.get(0)[0].toLowerCase());
        assertEquals("name", tableColumns.get(1)[0].toLowerCase());
        assertEquals("temperature", tableColumns.get(2)[0].toLowerCase());

        assertEquals("1", tableColumns.get(0)[2]);
        assertEquals("0", tableColumns.get(1)[2]);

        List<String> tables = db.getTables(false);
        assertTrue(tables.size() == 2);

        List<ForeignKey> foreignKeys = db.getForeignKeys(TABLE1);
        assertEquals(0, foreignKeys.size());
        foreignKeys = db.getForeignKeys(TABLE2);
        assertEquals(1, foreignKeys.size());

        List<Index> indexes = db.getIndexes(TABLE1);
        assertEquals(1, indexes.size());
        indexes = db.getIndexes(TABLE2);
        assertEquals(1, indexes.size());

    }

    @Test
    public void testContents() throws Exception {
        assertEquals(3, db.getCount(TABLE1));

        String sql = "select id, name, temperature from " + TABLE1 + " order by temperature";
        QueryResult result = db.getTableRecordsMapFromRawSql(sql, 2);
        assertEquals(2, result.data.size());

        result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertEquals(3, result.data.size());

        assertEquals(-1, result.geometryIndex);
        double temperature = ((Number) result.data.get(0)[2]).doubleValue();
        assertEquals(34.0, temperature, 0.00001);
    }

}
