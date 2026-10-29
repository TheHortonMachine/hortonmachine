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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantConfig.ServerKind;
import org.hortonmachine.gears.assistant.AssistantTurn;
import org.hortonmachine.gears.assistant.ModelInfo;
import org.hortonmachine.gears.assistant.SqlAnswer;
import org.hortonmachine.gears.assistant.SqlAssistOptions;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.llm.AuditLog;
import org.hortonmachine.llm.ChatMessage;
import org.hortonmachine.llm.LlmProvider;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the schema description and the {@link LlmSqlAssistant} on a GeoPackage, with a scripted model.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestSqlAssistant {

    private static File gpkgFile;
    private static ASpatialDb db;

    /**
     * A model that gives scripted answers (the queued ones first, then the fixed one) and keeps
     * the requests.
     */
    private static class ScriptedProvider implements LlmProvider {
        final List<List<ChatMessage>> requests = new ArrayList<>();
        final List<JSONObject> schemas = new ArrayList<>();
        final List<String> queuedAnswers = new ArrayList<>();
        String answer;
        RuntimeException failure;

        @Override
        public String chat( List<ChatMessage> messages, JSONObject jsonSchema, Consumer<String> onToken,
                IHMProgressMonitor pm ) {
            requests.add(List.copyOf(messages));
            schemas.add(jsonSchema);
            if (failure != null) {
                throw failure;
            }
            String text = queuedAnswers.isEmpty() ? answer : queuedAnswers.remove(0);
            if (onToken != null) {
                onToken.accept(text);
            }
            return text;
        }

        @Override
        public List<ModelInfo> listModels() {
            return List.of();
        }
    }

    @BeforeClass
    public static void createDb() throws Exception {
        gpkgFile = File.createTempFile("hm-llm-", ".gpkg");
        gpkgFile.delete();
        db = EDb.GEOPACKAGE.getSpatialDb();
        db.open(gpkgFile.getAbsolutePath());
        db.initSpatialMetadata(null);

        db.createTable(SqlName.m("owners"), "id INTEGER PRIMARY KEY", "name TEXT");
        // 4326 is in every new GeoPackage, other srids need to be added first
        db.createSpatialTable(SqlName.m("parcels"), 4326, "geom POLYGON",
                new String[]{"fid INTEGER PRIMARY KEY AUTOINCREMENT", "code TEXT", "owner_id INTEGER"},
                new String[]{"FOREIGN KEY (owner_id) REFERENCES owners(id)"}, false);
        // no geometry: placed through the parcel
        db.createTable(SqlName.m("inspections"), "id INTEGER PRIMARY KEY", "parcel_id INTEGER", "notes TEXT",
                "FOREIGN KEY (parcel_id) REFERENCES parcels(fid)");
        db.executeInsertUpdateDeleteSql("INSERT INTO owners VALUES (1, 'Rossi')");
    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (db != null) {
            db.close();
        }
        if (gpkgFile != null) {
            gpkgFile.delete();
        }
    }

    private static AssistantConfig config() {
        return new AssistantConfig(ServerKind.OLLAMA, "http://localhost:11434", "test-model", null, 10, 2, 30, 16384);
    }

    @Test
    public void testSchemaContext() throws Exception {
        String schema = SchemaContextBuilder.build(db, false);

        assertTrue(schema, schema.contains("TABLE owners\n"));
        assertTrue(schema, schema.contains("  - id INTEGER PRIMARY KEY\n"));
        assertTrue(schema, schema.contains("  - name TEXT\n"));
        assertTrue(schema, schema.contains("TABLE parcels\n"));
        assertTrue(schema, schema.contains("  - geom GEOMETRY POLYGON SRID 4326, spatial index\n"));
        assertTrue(schema, schema.contains("  foreign key owner_id -> owners(id)\n"));
        assertFalse(schema, schema.contains("gpkg_"));
        assertFalse(schema, schema.contains("rtree_"));
        assertTrue(schema, schema.contains("- inspections has no geometry; its location: inspections.parcel_id -> parcels.fid, "
                + "geometry parcels.geom (POLYGON, SRID 4326)\n"));
        // no foreign key to a table with geometry: nothing to say
        assertFalse(schema, schema.contains("owners has no geometry"));
        assertFalse(schema, schema.contains("sample rows"));

        String withRows = SchemaContextBuilder.build(db, true);
        assertTrue(withRows, withRows.contains("  sample rows (id, name):\n    1 | Rossi\n"));
    }

    @Test
    public void testAsk() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = "The parcel codes.\n\n```sql\nSELECT code FROM parcels;\n```";
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));

        List<String> tokens = new ArrayList<>();
        SqlAnswer answer = assistant.ask(db, "Which parcel codes are there?", List.of(), SqlAssistOptions.defaults(),
                tokens::add,
                null);

        assertEquals("SELECT code FROM parcels\nLIMIT 100", answer.sql());
        // the limit shows in the text too
        assertEquals("The parcel codes.\n\n```sql\nSELECT code FROM parcels\nLIMIT 100\n```", answer.text());
        assertTrue(answer.readOnly());
        assertTrue(answer.limitApplied());
        assertTrue(answer.checked());
        assertNull(answer.checkError());
        assertEquals(1, tokens.size());

        List<ChatMessage> messages = provider.requests.get(0);
        assertEquals("system", messages.get(0).role());
        assertTrue(messages.get(0).content().contains("Dialect: GeoPackage (SQLite)"));
        assertTrue(messages.get(0).content().contains("TABLE parcels"));
        assertEquals("Which parcel codes are there?", messages.get(1).content());
        assertNull("free text, no schema", provider.schemas.get(0));

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals(1, lines.size());
        JSONObject entry = new JSONObject(lines.get(0));
        assertEquals("sql-ask", entry.getString("task"));
        assertEquals("test-model", entry.getString("model"));
        assertEquals("SELECT code FROM parcels\nLIMIT 100", entry.getString("code"));
        assertTrue(entry.isNull("error"));
        logFile.delete();
    }

    @Test
    public void testWriteSqlGetsNoLimit() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = "Removes all owners:\n```sql\nDELETE FROM owners\n```";
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));

        SqlAnswer answer = assistant.ask(db, "Remove all owners", List.of(), SqlAssistOptions.defaults(), null, null);
        assertEquals("DELETE FROM owners", answer.sql());
        assertFalse(answer.readOnly());
        assertFalse(answer.limitApplied());
        // only read-only SQL is given to the database, even just to plan it
        assertFalse(answer.checked());
        assertNull(answer.checkError());
        logFile.delete();
    }

    @Test
    public void testRejectedSqlIsRepaired() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.queuedAnswers.add("The names.\n```sql\nSELECT nome FROM owners\n```");
        provider.answer = "The names.\n```sql\nSELECT name FROM owners\n```";
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));

        SqlAnswer answer = assistant.ask(db, "The owner names", List.of(), new SqlAssistOptions(false, 0), null, null);
        assertEquals("SELECT name FROM owners", answer.sql());
        assertTrue(answer.checked());
        assertNull(answer.checkError());

        assertEquals(2, provider.requests.size());
        List<ChatMessage> repair = provider.requests.get(1);
        assertEquals(4, repair.size());
        assertEquals("assistant", repair.get(2).role());
        assertTrue(repair.get(2).content().contains("SELECT nome FROM owners"));
        assertTrue(repair.get(3).content(), repair.get(3).content().contains("no such column: nome"));
        // the right columns come with the error
        assertTrue(repair.get(3).content(), repair.get(3).content().contains("owners: id (primary key), name\n"));
        assertTrue(repair.get(3).content(), repair.get(3).content().contains("Answer again, with the corrected SQL in a sql code block."));

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals(2, lines.size());
        JSONObject first = new JSONObject(lines.get(0));
        assertEquals("sql-ask", first.getString("task"));
        assertTrue(first.getString("error").startsWith("rejected by the database"));
        JSONObject second = new JSONObject(lines.get(1));
        assertEquals("sql-ask-repair", second.getString("task"));
        assertTrue(second.isNull("error"));
        logFile.delete();
    }

    @Test
    public void testTablesUsedIn() throws Exception {
        String tables = SchemaContextBuilder.describeTablesUsedIn(db,
                "SELECT i.notes, p.code FROM inspections i JOIN parcels p ON p.fid = i.parcel_id");
        assertTrue(tables, tables.contains("inspections: id (primary key), parcel_id, notes\n"));
        assertTrue(tables, tables.contains("  inspections has no geometry; its location: inspections.parcel_id -> parcels.fid, "
                + "geometry parcels.geom (POLYGON, SRID 4326)\n"));
        assertTrue(tables, tables.contains("parcels: fid (primary key)"));
        assertTrue(tables, tables.contains("geom (geometry POLYGON, SRID 4326)"));
        assertFalse(tables, tables.contains("owners"));

        tables = SchemaContextBuilder.describeTablesUsedIn(db, "SELECT nome FROM \"owners\"");
        assertEquals("owners: id (primary key), name\n  owners has no geometry\n", tables);
        assertEquals("", SchemaContextBuilder.describeTablesUsedIn(db, "SELECT 1"));
    }

    @Test
    public void testRepairRequestKeepsOnlyTheError() {
        String error = "ERROR: column p.id does not exist\n  Hint: Perhaps you meant to reference the column \"e.id\".\n"
                + "  Position: 147";
        String request = LlmSqlAssistant.repairRequest(error, "pipe: idsap (primary key)\n", true);
        assertTrue(request, request.contains("ERROR: column p.id does not exist\n"));
        assertFalse(request, request.contains("Hint"));
        assertFalse(request, request.contains("Position"));
        assertTrue(request, request.contains("pipe: idsap (primary key)\n"));
        assertTrue(request, request.contains("Write the corrected SQL."));

        request = LlmSqlAssistant.repairRequest("no such column: x", "", false);
        assertFalse(request, request.contains("tables used"));
        assertTrue(request, request.contains("in a sql code block"));
    }

    @Test
    public void testRepairGivesUp() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        // the GeoPackage connection has no ST_Area: planning the SQL finds it
        provider.answer = "The areas.\n```sql\nSELECT ST_Area(geom) FROM parcels\n```";
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));

        SqlAnswer answer = assistant.ask(db, "The areas", List.of(), new SqlAssistOptions(false, 0), null, null);
        assertEquals("SELECT ST_Area(geom) FROM parcels", answer.sql());
        assertTrue(answer.checked());
        assertTrue(answer.checkError(), answer.checkError().contains("no such function: ST_Area"));
        assertEquals(1 + LlmSqlAssistant.MAX_REPAIR_ROUNDS, provider.requests.size());
        logFile.delete();
    }

    @Test
    public void testFixAndExplain() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));
        SqlAssistOptions options = new SqlAssistOptions(false, 0);

        provider.answer = "{\"sql\":\"SELECT name FROM owners\",\"explanation\":\"The column is name.\"}";
        SqlAnswer answer = assistant.fix(db, "SELECT nome FROM owners", "no such column: nome", List.of(), options, null,
                null);
        assertEquals("SELECT name FROM owners", answer.sql());
        // the JSON of the fix, shown as text like the other answers
        assertEquals("The column is name.\n\n```sql\nSELECT name FROM owners\n```", answer.text());
        assertFalse(answer.limitApplied());
        assertEquals("object", provider.schemas.get(0).getString("type"));
        String request = provider.requests.get(0).get(1).content();
        assertTrue(request.contains("SELECT nome FROM owners"));
        assertTrue(request.contains("no such column: nome"));

        provider.answer = "  It lists the owner names.\n";
        assertEquals("It lists the owner names.",
                assistant.explain(db, "SELECT name FROM owners", List.of(), options, null, null));
        assertNull(provider.schemas.get(1));

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals("sql-fix", new JSONObject(lines.get(0)).getString("task"));
        assertEquals("sql-explain", new JSONObject(lines.get(1)).getString("task"));
        logFile.delete();
    }

    @Test
    public void testAskWithAndWithoutDatabase() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));
        SqlAssistOptions options = SqlAssistOptions.defaults();

        // a code block that isn't SQL is no SQL
        provider.answer = "Use the remote button with:\n```\njdbc:postgresql://host:5432/name\n```\n";
        SqlAnswer answer = assistant.ask(null, "How do I open our PostGIS?", List.of(), options, null, null);
        assertEquals("Use the remote button with:\n```\njdbc:postgresql://host:5432/name\n```", answer.text());
        assertNull(answer.sql());
        String system = provider.requests.get(0).get(0).content();
        assertTrue(system.contains("jdbc:postgresql://<host>"));
        assertTrue(system.contains("No database is connected right now."));
        assertFalse(system.contains("Schema:"));
        assertNull(provider.schemas.get(0));

        // SQL without a database: nothing can check it
        provider.answer = "```sql\nSELECT * FROM roads\n```";
        answer = assistant.ask(null, "All the roads", List.of(), options, null, null);
        assertEquals("SELECT * FROM roads\nLIMIT 100", answer.sql());
        assertFalse(answer.checked());
        assertNull(answer.checkError());

        provider.answer = "The code of a reference system.";
        assistant.ask(db, "What is an SRID?", List.of(), options, null, null);
        system = provider.requests.get(2).get(0).content();
        assertTrue(system.contains("Dialect: GeoPackage (SQLite)"));
        assertTrue(system.contains("TABLE parcels"));

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals("sql-ask", new JSONObject(lines.get(0)).getString("task"));
        logFile.delete();
    }

    @Test
    public void testLooksLikeSql() {
        assertTrue(LlmSqlAssistant.looksLikeSql("SELECT 1"));
        assertTrue(LlmSqlAssistant.looksLikeSql("-- the names\n  with x as (select 1) select * from x"));
        assertTrue(LlmSqlAssistant.looksLikeSql("PRAGMA table_info(roads)"));
        assertFalse(LlmSqlAssistant.looksLikeSql("jdbc:postgresql://host:5432/name"));
        assertFalse(LlmSqlAssistant.looksLikeSql("psql -h host -d name"));
        assertFalse(LlmSqlAssistant.looksLikeSql("-- only a comment"));
    }

    @Test
    public void testDiscussionHistory() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        // the test configuration gives back the last 2 turns
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));
        List<AssistantTurn> history = List.of(//
                new AssistantTurn("first", "dropped, too old", null), //
                new AssistantTurn("Which parcels are there?", "All the parcel codes.", "SELECT code FROM parcels"), //
                new AssistantTurn("What is an SRID?", "The code of a reference system.", null));

        // a fix, answered as JSON, gets the earlier SQL as JSON, or the model would answer in text
        provider.answer = "{\"sql\":\"SELECT code FROM parcels WHERE owner_id = 1\",\"explanation\":\"Only Rossi.\"}";
        assistant.fix(db, "SELECT code FROM parcels WHERE owner = 1", "no such column: owner", history,
                SqlAssistOptions.defaults(), null, null);

        List<ChatMessage> messages = provider.requests.get(0);
        assertEquals(6, messages.size());
        assertEquals("system", messages.get(0).role());
        assertEquals("Which parcels are there?", messages.get(1).content());
        JSONObject sqlTurn = new JSONObject(messages.get(2).content());
        assertEquals("assistant", messages.get(2).role());
        assertEquals("SELECT code FROM parcels", sqlTurn.getString("sql"));
        assertEquals("All the parcel codes.", sqlTurn.getString("explanation"));
        assertEquals("What is an SRID?", messages.get(3).content());
        assertEquals("The code of a reference system.", messages.get(4).content());

        List<ChatMessage> none = LlmSqlAssistant.chat("rules", history, "alone", 0, true);
        assertEquals(2, none.size());

        // a question, answered as text, gets the earlier SQL as text
        provider.answer = "They all use 4326.";
        assistant.ask(db, "Do the tables use different SRIDs?", history, SqlAssistOptions.defaults(), null, null);
        List<ChatMessage> textMessages = provider.requests.get(1);
        assertEquals("All the parcel codes.\n\n```sql\nSELECT code FROM parcels\n```", textMessages.get(2).content());
        assertEquals("Do the tables use different SRIDs?", textMessages.get(5).content());
        logFile.delete();
    }

    @Test
    public void testJsonTextAnswersBecomeText() {
        assertEquals("Plain answer.", LlmSqlAssistant.toText("  Plain answer.\n"));
        assertEquals("Yes, all 4326.\n\n```sql\nSELECT 1\n```",
                LlmSqlAssistant.toText("{\"explanation\":\"Yes, all 4326.\",\"sql\":\"SELECT 1\"}"));
        assertEquals("Only words.", LlmSqlAssistant.toText("```json\n{\"explanation\":\"Only words.\",\"sql\":\"\"}\n```"));
        // other JSON stays as the model wrote it
        assertEquals("{\"type\":\"Point\"}", LlmSqlAssistant.toText("{\"type\":\"Point\"}"));
    }

    @Test
    public void testFailuresAreLogged() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));

        provider.answer = "I can't help with that.";
        try {
            assistant.fix(db, "SELECT x", "no such column: x", List.of(), SqlAssistOptions.defaults(), null, null);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("The model didn't answer with JSON"));
        }

        provider.failure = new IllegalStateException("server down");
        try {
            assistant.ask(db, "?", List.of(), SqlAssistOptions.defaults(), null, null);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("server down", e.getMessage());
        }

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals(2, lines.size());
        JSONObject first = new JSONObject(lines.get(0));
        assertEquals("I can't help with that.", first.getString("response"));
        assertTrue(first.getString("error").startsWith("The model didn't answer with JSON"));
        JSONObject second = new JSONObject(lines.get(1));
        assertTrue(second.isNull("response"));
        assertEquals("server down", second.getString("error"));
        logFile.delete();
    }

    @Test
    public void testAnswerInFences() {
        String response = "```json\n{\"sql\":\"```sql\\nSELECT 1\\n```\",\"explanation\":\"One.\"}\n```";
        SqlAnswer answer = LlmSqlAssistant.toAnswer(response, new SqlAssistOptions(false, 0));
        assertEquals("SELECT 1", answer.sql());
        assertEquals("One.\n\n```sql\nSELECT 1\n```", answer.text());
    }
}
