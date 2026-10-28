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
        return new AssistantConfig(ServerKind.OLLAMA, "http://localhost:11434", "test-model", null, 10, 2);
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
    public void testGenerate() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = "{\"sql\":\"SELECT code FROM parcels;\",\"explanation\":\"The parcel codes.\"}";
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));

        List<String> tokens = new ArrayList<>();
        SqlAnswer answer = assistant.generate(db, "Which parcel codes are there?", List.of(), SqlAssistOptions.defaults(),
                tokens::add,
                null);

        assertEquals("SELECT code FROM parcels\nLIMIT 100", answer.sql());
        assertEquals("The parcel codes.", answer.explanation());
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
        assertEquals("object", provider.schemas.get(0).getString("type"));

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals(1, lines.size());
        JSONObject entry = new JSONObject(lines.get(0));
        assertEquals("sql-generate", entry.getString("task"));
        assertEquals("test-model", entry.getString("model"));
        assertEquals("SELECT code FROM parcels\nLIMIT 100", entry.getString("code"));
        assertTrue(entry.isNull("error"));
        logFile.delete();
    }

    @Test
    public void testWriteSqlGetsNoLimit() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = "{\"sql\":\"DELETE FROM owners\",\"explanation\":\"Removes all owners.\"}";
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));

        SqlAnswer answer = assistant.generate(db, "Remove all owners", List.of(), SqlAssistOptions.defaults(), null, null);
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
        provider.queuedAnswers.add("{\"sql\":\"SELECT nome FROM owners\",\"explanation\":\"The names.\"}");
        provider.answer = "{\"sql\":\"SELECT name FROM owners\",\"explanation\":\"The names.\"}";
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));

        SqlAnswer answer = assistant.generate(db, "The owner names", List.of(), new SqlAssistOptions(false, 0), null, null);
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

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals(2, lines.size());
        JSONObject first = new JSONObject(lines.get(0));
        assertEquals("sql-generate", first.getString("task"));
        assertTrue(first.getString("error").startsWith("rejected by the database"));
        JSONObject second = new JSONObject(lines.get(1));
        assertEquals("sql-generate-repair", second.getString("task"));
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
        String request = LlmSqlAssistant.repairRequest(error, "pipe: idsap (primary key)\n");
        assertTrue(request, request.contains("ERROR: column p.id does not exist\n"));
        assertFalse(request, request.contains("Hint"));
        assertFalse(request, request.contains("Position"));
        assertTrue(request, request.contains("pipe: idsap (primary key)\n"));

        request = LlmSqlAssistant.repairRequest("no such column: x", "");
        assertFalse(request, request.contains("tables used"));
    }

    @Test
    public void testRepairGivesUp() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        // the GeoPackage connection has no ST_Area: planning the SQL finds it
        provider.answer = "{\"sql\":\"SELECT ST_Area(geom) FROM parcels\",\"explanation\":\"The areas.\"}";
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));

        SqlAnswer answer = assistant.generate(db, "The areas", List.of(), new SqlAssistOptions(false, 0), null, null);
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
        assertFalse(answer.limitApplied());
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
    public void testHelpWithAndWithoutDatabase() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        File logFile = File.createTempFile("hm-llm-", ".jsonl");
        LlmSqlAssistant assistant = new LlmSqlAssistant(TestSqlAssistant::config, c -> provider, new AuditLog(logFile));
        SqlAssistOptions options = SqlAssistOptions.defaults();

        provider.answer = "Use the remote button with jdbc:postgresql://host:5432/name.\n";
        String answer = assistant.help(null, "How do I open our PostGIS?", List.of(), options, null, null);
        assertEquals("Use the remote button with jdbc:postgresql://host:5432/name.", answer);
        String system = provider.requests.get(0).get(0).content();
        assertTrue(system.contains("jdbc:postgresql://<host>"));
        assertTrue(system.contains("No database is connected right now."));
        assertFalse(system.contains("Schema:"));
        assertNull(provider.schemas.get(0));

        assistant.help(db, "What is an SRID?", List.of(), options, null, null);
        system = provider.requests.get(1).get(0).content();
        assertTrue(system.contains("Dialect: GeoPackage (SQLite)"));
        assertTrue(system.contains("TABLE parcels"));

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals("sql-help", new JSONObject(lines.get(0)).getString("task"));
        logFile.delete();
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

        provider.answer = "{\"sql\":\"SELECT code FROM parcels WHERE owner_id = 1\",\"explanation\":\"Only Rossi.\"}";
        assistant.generate(db, "now only the ones of Rossi", history, SqlAssistOptions.defaults(), null, null);

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
        assertEquals("now only the ones of Rossi", messages.get(5).content());

        List<ChatMessage> none = LlmSqlAssistant.chat("rules", history, "alone", 0, true);
        assertEquals(2, none.size());

        // a text request gets the earlier SQL as text, or the model would answer in JSON
        provider.answer = "They all use 4326.";
        assistant.help(db, "Do the tables use different SRIDs?", history, SqlAssistOptions.defaults(), null, null);
        List<ChatMessage> textMessages = provider.requests.get(1);
        assertEquals("All the parcel codes.\n\n```sql\nSELECT code FROM parcels\n```", textMessages.get(2).content());
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
            assistant.generate(db, "?", List.of(), SqlAssistOptions.defaults(), null, null);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("The model didn't answer with JSON"));
        }

        provider.failure = new IllegalStateException("server down");
        try {
            assistant.generate(db, "?", List.of(), SqlAssistOptions.defaults(), null, null);
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
        assertEquals("One.", answer.explanation());
    }
}
