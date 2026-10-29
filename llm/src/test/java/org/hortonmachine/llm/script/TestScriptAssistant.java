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
package org.hortonmachine.llm.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantConfig.ServerKind;
import org.hortonmachine.gears.assistant.AssistantTurn;
import org.hortonmachine.gears.assistant.ModelInfo;
import org.hortonmachine.gears.assistant.ScriptAnswer;
import org.hortonmachine.gears.assistant.ScriptAssistOptions;
import org.hortonmachine.gears.assistant.ScriptContext;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.llm.AuditLog;
import org.hortonmachine.llm.ChatMessage;
import org.hortonmachine.llm.CodeChats;
import org.hortonmachine.llm.LlmProvider;
import org.hortonmachine.llm.TestMonitor;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link LlmScriptAssistant} with a scripted model and the modules of the classpath.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestScriptAssistant {

    private static final String GOOD_SCRIPT = """
            import org.hortonmachine.modules.*
            def pitfiller = new Pitfiller()
            pitfiller.inElev = "/data/dtm.tif"
            pitfiller.outPit = "/data/pit.tif"
            pitfiller.process()
            """;

    private File logFile;

    /**
     * A model that gives scripted answers (the queued ones first, then the fixed one) and keeps
     * the requests.
     */
    private static class ScriptedProvider implements LlmProvider {
        final List<List<ChatMessage>> requests = new ArrayList<>();
        final List<JSONObject> schemas = new ArrayList<>();
        final List<String> queuedAnswers = new ArrayList<>();
        final List<List<ChatMessage>> prepared = new ArrayList<>();
        String answer;

        @Override
        public void prepare( List<ChatMessage> messages, IHMProgressMonitor pm ) {
            prepared.add(List.copyOf(messages));
        }

        @Override
        public String chat( List<ChatMessage> messages, JSONObject jsonSchema, Consumer<String> onToken,
                IHMProgressMonitor pm ) {
            requests.add(List.copyOf(messages));
            schemas.add(jsonSchema);
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

    @Before
    public void createLog() throws Exception {
        logFile = File.createTempFile("hm-llm-", ".jsonl");
    }

    @After
    public void deleteLog() {
        logFile.delete();
    }

    private static AssistantConfig config() {
        return new AssistantConfig(ServerKind.OLLAMA, "http://localhost:11434", "test-model", null, 10, 2, 30, 16384);
    }

    private LlmScriptAssistant assistant( ScriptedProvider provider ) {
        return new LlmScriptAssistant(TestScriptAssistant::config, c -> provider, ModuleCatalog::getDefault,
                new AuditLog(logFile));
    }

    private static ScriptContext context( List<String> checkerErrors ) {
        Map<String, String> examples = new LinkedHashMap<>();
        examples.put("Work with a database", "def db = HM.connectSpatialite(\"...\")\nprintln db.getTables()");
        examples.put("Extract stream network from dtm", "def pitfiller = new Pitfiller()\npitfiller.process()");
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("dtm", "String");
        return new ScriptContext("import org.hortonmachine.modules.*", "readRaster( String source )", examples, variables,
                checkerErrors == null ? null : script -> checkerErrors.isEmpty() ? null : checkerErrors.remove(0), null);
    }

    private static String answer( String script, String explanation ) {
        return new JSONObject().put("script", script).put("explanation", explanation).toString();
    }

    /**
     * @return a text answer with the script in a groovy code block.
     */
    private static String textAnswer( String text, String script ) {
        return text + "\n\n```groovy\n" + script.strip() + "\n```\n\nSet the paths first.";
    }

    @Test
    public void testAsk() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = textAnswer("Fills the pits.", GOOD_SCRIPT);

        ScriptAnswer answer = assistant(provider).ask(context(new ArrayList<>()), "Fill the pits of my dtm", List.of(),
                ScriptAssistOptions.defaults(), null, null);

        // the whole text is the answer, its code block the checked script
        assertEquals(provider.answer, answer.text());
        assertEquals(GOOD_SCRIPT.strip(), answer.script());
        assertTrue(answer.checked());
        assertNull(answer.checkError());
        assertEquals(List.of("Pitfiller"), answer.modules());
        assertEquals(1, provider.requests.size());
        assertNull("free text, no schema", provider.schemas.get(0));

        String system = provider.requests.get(0).get(0).content();
        assertTrue(system.contains("You help a user"));
        assertTrue(system.contains("How HortonMachine scripts work:"));
        assertTrue(system.contains("The imports scripts usually start with:\nimport org.hortonmachine.modules.*\n"));
        assertTrue(system.contains("HM.name(...):\nreadRaster( String source )\n"));
        assertTrue(system.contains("// Extract stream network from dtm\ndef pitfiller"));
        assertFalse("not similar to the request", system.contains("// Work with a database"));
        assertTrue(system.contains("- dtm (String)\n"));
        // the reference first, the same for every request, so the server reuses its work on it:
        // then the task, the examples, the variables and the relevant module in full
        String reference = LlmScriptAssistant.reference(context(new ArrayList<>()), ModuleCatalog.getDefault());
        assertTrue(system.startsWith(reference));
        assertTrue(reference.contains("- Pitfiller: "));
        assertTrue(reference.contains("- Flow: the D8 drainage directions"));
        assertFalse("nothing of the request in the reference", reference.contains("// Extract stream network"));
        int task = system.indexOf("\nYour task:\nYou help a user");
        int full = system.indexOf("\nModule Pitfiller (Dem Manipulation)");
        assertTrue(task == reference.length() && full > task);

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals(1, lines.size());
        JSONObject entry = new JSONObject(lines.get(0));
        assertEquals("script-ask", entry.getString("task"));
        assertEquals(GOOD_SCRIPT.strip(), entry.getString("code"));
    }

    @Test
    public void testAskWithoutScript() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = "Pitfiller fills the pits, see [the manual](https://www.hortonmachine.org).";

        ScriptAnswer answer = assistant(provider).ask(context(new ArrayList<>()), "Which module fills pits?", List.of(),
                ScriptAssistOptions.defaults(), null, null);

        // nothing to check, nothing to repair
        assertEquals(provider.answer, answer.text());
        assertNull(answer.script());
        assertFalse(answer.checked());
        assertNull(answer.checkError());
        assertEquals(1, provider.requests.size());
    }

    @Test
    public void testPrepare() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = answer(GOOD_SCRIPT, "Fills the pits.");
        LlmScriptAssistant assistant = assistant(provider);
        assistant.prepare(context(new ArrayList<>()), null);

        // the server reads exactly the beginning of every later request, and nothing of the user
        String prepared = provider.prepared.get(0).get(0).content();
        assertEquals(LlmScriptAssistant.reference(context(new ArrayList<>()), ModuleCatalog.getDefault()), prepared);
        assertFalse(prepared.contains("- dtm (String)"));
        assistant.ask(context(new ArrayList<>()), "Fill the pits of my dtm", List.of(), ScriptAssistOptions.defaults(), null, null);
        assertTrue(provider.requests.get(0).get(0).content().startsWith(prepared));
        // not in the audit log, which only has the request
        assertEquals(1, FileUtilities.readFileToLinesList(logFile).size());
    }

    @Test
    public void testNoModules() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = "Use FlowAccumulation.";
        LlmScriptAssistant assistant = new LlmScriptAssistant(TestScriptAssistant::config, c -> provider,
                () -> new ModuleCatalog(List.of()), new AuditLog(logFile));

        // without the modules the model would invent them: the request is refused before reaching it
        try {
            assistant.ask(context(null), "Extract a watershed", List.of(), ScriptAssistOptions.defaults(), null, null);
            fail("no modules, no request");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No processing modules"));
        }
        assertTrue(provider.requests.isEmpty());
    }

    @Test
    public void testRepairs() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        // a class that doesn't exist, then a module whose fields it didn't have, then good
        provider.queuedAnswers.add(textAnswer("first", "def p = new PitFiller()\np.process()"));
        provider.queuedAnswers.add(textAnswer("second", "def b = new ExtractBasin()\nb.inDem = \"x\"\nb.process()"));
        provider.answer = textAnswer("third", GOOD_SCRIPT);
        List<String> checkerErrors = new ArrayList<>();
        checkerErrors.add("ConsoleScript.groovy: 1: unable to resolve class PitFiller");

        TestMonitor pm = new TestMonitor();
        ScriptAnswer answer = assistant(provider).ask(context(checkerErrors), "Fill the pits", List.of(),
                ScriptAssistOptions.defaults(), null, pm);

        assertTrue(answer.text().startsWith("third"));
        assertEquals(GOOD_SCRIPT.strip(), answer.script());
        assertNull(answer.checkError());
        assertEquals(3, provider.requests.size());
        // each repair round is announced, so that the panel replaces the rejected answer
        assertEquals(2, pm.tasks.size());
        assertTrue(pm.tasks.get(0), pm.tasks.get(0).startsWith("The checks rejected the script (The script doesn't compile: "));
        assertTrue(pm.tasks.get(0), pm.tasks.get(0).endsWith("attempt 2 of 3."));
        assertTrue(pm.tasks.get(1), pm.tasks.get(1).endsWith("attempt 3 of 3."));

        List<ChatMessage> second = provider.requests.get(1);
        String repair = second.get(second.size() - 1).content();
        assertTrue(repair, repair.startsWith("The script has these problems:\nThe script doesn't compile: "));
        assertTrue(repair, repair.contains("Modules with a similar name:\n- PitFiller: Pitfiller"));
        assertTrue(repair, repair.endsWith("Answer again, with the corrected script in a groovy code block."));

        List<ChatMessage> third = provider.requests.get(2);
        repair = third.get(third.size() - 1).content();
        assertTrue(repair, repair.contains("The module ExtractBasin has no field inDem."));
        // ExtractBasin was not among the modules given in full: its reference comes with the repair
        assertTrue(repair, repair.contains("exactly these fields:\n\nModule ExtractBasin"));

        List<String> lines = FileUtilities.readFileToLinesList(logFile);
        assertEquals(3, lines.size());
        assertEquals("script-ask-repair", new JSONObject(lines.get(2)).getString("task"));
    }

    @Test
    public void testSimilarHelperMethods() throws Exception {
        String reference = """
                Database
                --------
                \tASpatialDb connectGeopackage( String databasePath )
                \tASpatialDb connectPostgis( String host, int port, String database, String user, String pwd )
                \tvoid packageInfo()
                """;
        String error = "2: unable to resolve class org.hortonmachine.gears.io.geopackage.HMGeoPackage @ line 2, column 1.";

        // a made up class points to the helper method of the same thing
        assertEquals(List.of("ASpatialDb connectGeopackage( String databasePath )", "void packageInfo()"),
                LlmScriptAssistant.similarHelperMethods(reference, error));
        assertTrue(LlmScriptAssistant.similarHelperMethods(reference, "1: unexpected token: }").isEmpty());
        assertTrue(LlmScriptAssistant.similarHelperMethods(null, error).isEmpty());

        String repair = LlmScriptAssistant.repairRequest(ModuleCatalog.getDefault(), "The script doesn't compile: " + error,
                List.of(), List.of(), LlmScriptAssistant.similarHelperMethods(reference, error), false);
        assertTrue(repair, repair.contains("called as HM.name(...):\n- ASpatialDb connectGeopackage( String databasePath )\n"));
    }

    @Test
    public void testDropUnusedUnresolvedImports() throws Exception {
        String script = "import org.hortonmachine.*\nimport org.hortonmachine.gears.io.geopackage.HMGeopackageManager\n"
                + "def db = HM.connectGeopackage(path)\n";
        String error = "2: unable to resolve class org.hortonmachine.gears.io.geopackage.HMGeopackageManager @ line 2, column 1.";

        // never used: the line goes
        assertEquals("import org.hortonmachine.*\ndef db = HM.connectGeopackage(path)\n",
                LlmScriptAssistant.dropUnusedUnresolvedImports(script, error));
        // used: the model must replace it, the import stays to show the error
        String used = script + "def manager = new HMGeopackageManager()\n";
        assertEquals(used, LlmScriptAssistant.dropUnusedUnresolvedImports(used, error));
        assertEquals(script, LlmScriptAssistant.dropUnusedUnresolvedImports(script, null));
        assertEquals(script, LlmScriptAssistant.dropUnusedUnresolvedImports(script, "1: unexpected token: }"));
    }

    @Test
    public void testVariableErrors() throws Exception {
        Set<String> variables = Set.of("path", "dtm");
        String script = "def path = \"/path/to/data.gpkg\"\nprintln dtm\n";

        List<String> errors = LlmScriptAssistant.variableErrors(script, variables, "open the geopackage of the variable path");
        assertEquals(List.of("The request uses the variable path, which already exists: use it as it is, don't define it again."),
                errors);
        // a typed definition too
        assertEquals(1, LlmScriptAssistant.variableErrors("String path = 'x'", variables, "use path").size());
        // not named in the request, or only used: fine
        assertTrue(LlmScriptAssistant.variableErrors(script, variables, "open my geopackage").isEmpty());
        assertTrue(LlmScriptAssistant.variableErrors("println path", variables, "print path").isEmpty());
        assertTrue(LlmScriptAssistant.variableErrors(script, Set.of(), "path").isEmpty());
    }

    @Test
    public void testScriptVariables() throws Exception {
        String script = "import org.hortonmachine.*\ndef path = \"/data/a.gpkg\"\nString name = 'x'\nfinal n = 3\n"
                + "List<String> list = []\nif (n == 3) {\n    def inner = 1\n}\npath == null\n";
        // the top level only, comparisons aren't definitions
        assertEquals(Set.of("path", "name", "n", "list"), LlmScriptAssistant.scriptVariables(script));
        assertTrue(LlmScriptAssistant.scriptVariables(null).isEmpty());
    }

    @Test
    public void testEditorScript() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        String editor = "import org.hortonmachine.*\ndef path = \"/data/roads.gpkg\"\ndef db = HM.connectGeopackage(path)";
        // code going on with the editor script: alone it misses db, after the editor script it compiles
        String continuation = "db.getTables().each { println it.name }";
        provider.answer = textAnswer("Lists the tables.", continuation);
        List<String> checkerErrors = new ArrayList<>();
        checkerErrors.add("1: the variable db is undeclared");

        ScriptAnswer answer = assistant(provider).ask(context(checkerErrors).withEditorScript(editor),
                "list the tables of db", List.of(), ScriptAssistOptions.defaults(), null, null);

        assertEquals(1, provider.requests.size());
        assertNull(answer.checkError());
        assertEquals(continuation, answer.script());
        String system = provider.requests.get(0).get(0).content();
        assertTrue(system, system.contains("The script in the user's editor, which your code goes on with:\n```groovy\n" + editor
                + "\n```\n"));
        // after the task, as it changes at each request
        assertTrue(system.indexOf("The script in the user's editor") > system.indexOf("\nYour task:\n"));

        // a variable of the editor script named in the request, defined again
        provider.queuedAnswers.add(textAnswer("Lists them.", "def path = \"/path/to/data.gpkg\"\nprintln path"));
        provider.answer = textAnswer("Lists them.", "println path");
        answer = assistant(provider).ask(context(new ArrayList<>()).withEditorScript(editor), "print the path", List.of(),
                ScriptAssistOptions.defaults(), null, null);
        assertEquals("println path", answer.script());
        List<ChatMessage> second = provider.requests.get(2);
        assertTrue(second.get(second.size() - 1).content().contains("The request uses the variable path, which already exists"));
    }

    @Test
    public void testEditorScriptExcerpt() throws Exception {
        assertEquals("a\nb", LlmScriptAssistant.editorScriptExcerpt("a\nb\n"));
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < LlmScriptAssistant.MAX_EDITOR_LINES + 50; i++ ) {
            sb.append("line").append(i).append("\n");
        }
        String excerpt = LlmScriptAssistant.editorScriptExcerpt(sb.toString());
        // the beginning and the end, where the new code goes
        assertTrue(excerpt.startsWith("line0\n"));
        assertTrue(excerpt.endsWith("line" + (LlmScriptAssistant.MAX_EDITOR_LINES + 49)));
        assertTrue(excerpt.contains("\n// ... 50 lines left out ...\n"));
        assertEquals(LlmScriptAssistant.MAX_EDITOR_LINES + 1, excerpt.lines().count());
    }

    @Test
    public void testUnusedImportDroppedWithoutRepair() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        String withImport = "import org.hortonmachine.gears.io.Missing\n" + GOOD_SCRIPT.strip();
        provider.answer = textAnswer("Fills the pits.", withImport);
        List<String> checkerErrors = new ArrayList<>();
        checkerErrors.add("2: unable to resolve class org.hortonmachine.gears.io.Missing @ line 1, column 1.");

        ScriptAnswer answer = assistant(provider).ask(context(checkerErrors), "Fill the pits", List.of(),
                ScriptAssistOptions.defaults(), null, null);

        assertEquals(1, provider.requests.size());
        assertNull(answer.checkError());
        assertEquals(GOOD_SCRIPT.strip(), answer.script());
        // the text shows the script without the line
        assertFalse(answer.text().contains("Missing"));
        assertTrue(answer.text().contains(GOOD_SCRIPT.strip()));
    }

    @Test
    public void testGivesUpAfterRepairs() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = textAnswer("never run", "def p = new Pitfiller()\np.inElev = \"x\"");

        ScriptAnswer answer = assistant(provider).ask(context(null), "Fill the pits", List.of(),
                ScriptAssistOptions.defaults(), null, null);

        assertEquals(1 + LlmScriptAssistant.MAX_REPAIR_ROUNDS, provider.requests.size());
        assertFalse("no compiler", answer.checked());
        assertTrue(answer.checkError(), answer.checkError().contains("is never run"));
    }

    @Test
    public void testFixAndExplainGetTheModulesOfTheScript() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = answer(GOOD_SCRIPT, "Fixed.");
        String broken = "def basin = new ExtractBasin()\nbasin.process()";

        ScriptAnswer fixed = assistant(provider).fix(context(new ArrayList<>()), broken, "java.lang.NullPointerException: inFlow",
                List.of(), ScriptAssistOptions.defaults(), null, null);
        // the JSON of the fix, shown as text like the other answers
        assertEquals("Fixed.\n\n```groovy\n" + GOOD_SCRIPT.strip() + "\n```", fixed.text());
        assertEquals(GOOD_SCRIPT.strip(), fixed.script());
        assertEquals("object", provider.schemas.get(0).getString("type"));
        List<ChatMessage> messages = provider.requests.get(0);
        assertTrue(messages.get(0).content().contains("\nModule ExtractBasin"));
        String request = messages.get(messages.size() - 1).content();
        assertTrue(request, request.startsWith("This script failed:\n```groovy\ndef basin"));
        assertTrue(request, request.contains("The error was:\njava.lang.NullPointerException: inFlow"));

        provider.answer = "It runs ExtractBasin.";
        String text = assistant(provider).explain(context(null), broken, List.of(), ScriptAssistOptions.defaults(), null, null);
        assertEquals("It runs ExtractBasin.", text);
        assertTrue(provider.requests.get(1).get(0).content().contains("\nModule ExtractBasin"));
        assertEquals(null, provider.schemas.get(1));
    }

    @Test
    public void testHistory() throws Exception {
        ScriptedProvider provider = new ScriptedProvider();
        provider.answer = "```json\n" + answer("println 1", "Prints one.") + "\n```";
        List<AssistantTurn> history = List.of(//
                new AssistantTurn("too old", "dropped", null), //
                new AssistantTurn("Fill the pits", "Fills them.", GOOD_SCRIPT), //
                new AssistantTurn("What is a pit?", "A cell with no lower neighbour.", null));

        // a text answer given as JSON anyways becomes text with a code block
        ScriptAnswer answer = assistant(provider).ask(context(null), "And then?", history, ScriptAssistOptions.defaults(), null,
                null);
        assertEquals("Prints one.\n\n```groovy\nprintln 1\n```", answer.text());
        assertEquals("println 1", answer.script());

        List<ChatMessage> messages = provider.requests.get(0);
        // system, 2 turns of 2 messages, the question
        assertEquals(6, messages.size());
        assertEquals("Fill the pits", messages.get(1).content());
        assertTrue(messages.get(2).content(), messages.get(2).content().startsWith("Fills them.\n\n```groovy\nimport"));
        // the follow-up is about the last script: its module is given in full
        assertTrue(messages.get(0).content().contains("\nModule Pitfiller"));

        // also when the script is only in a code block of the answer
        List<AssistantTurn> textHistory = List.of(new AssistantTurn("Fill the pits", textAnswer("Fills them.", GOOD_SCRIPT), null));
        assertEquals(GOOD_SCRIPT.strip(), LlmScriptAssistant.lastCode(textHistory));
        assertNull(LlmScriptAssistant.lastCode(List.of(new AssistantTurn("What is a pit?", "A cell.", null))));
    }

    @Test
    public void testLastCodeBlock() throws Exception {
        String text = "Read it:\n```groovy\nprintln 1\n```\nThen:\n```sql\nSELECT 1\n```\nand\n```\nprintln 2\n```\nDone.";
        assertEquals("println 2", CodeChats.lastCodeBlock(text, "groovy"));
        assertEquals("SELECT 1", CodeChats.lastCodeBlock("```groovy\nprintln 1\n```\n```sql\nSELECT 1\n```", "sql"));
        assertEquals("println 3", CodeChats.lastCodeBlock("```java\nprintln 3\n```", "groovy"));
        // an answer cut short still has its code
        assertEquals("println 4", CodeChats.lastCodeBlock("Here:\n```groovy\nprintln 4\n", "groovy"));
        assertNull(CodeChats.lastCodeBlock("No code here.", "groovy"));
        assertNull(CodeChats.lastCodeBlock("```groovy\n\n```", "groovy"));
    }

    @Test
    public void testChooseExamples() throws Exception {
        Map<String, String> examples = new LinkedHashMap<>();
        examples.put("Add prj to files in folder", "OmsFileIterator.addPrj(folder, epsg)");
        examples.put("Work with a database", "HM.connectPostgis()");
        examples.put("Print raster cell info", "HM.getCellInfo()");

        assertEquals(List.of("// Work with a database\nHM.connectPostgis()"),
                LlmScriptAssistant.chooseExamples(examples, "list the tables of my database"));
        assertEquals(List.of("// Add prj to files in folder\nOmsFileIterator.addPrj(folder, epsg)"),
                LlmScriptAssistant.chooseExamples(examples, "xyzzy"));
        assertEquals(2, LlmScriptAssistant.chooseExamples(examples, "raster info of a database").size());
    }

    /**
     * How big the instructions get with the real catalog, to keep them in the context of small
     * models (about 4 characters per token).
     */
    @Test
    public void testInstructionsSize() throws Exception {
        ModuleCatalog catalog = ModuleCatalog.getDefault();
        String request = "extract the stream network from my dtm";
        String system = LlmScriptAssistant.instructions("rules", context(null), catalog,
                LlmScriptAssistant.relevantModules(catalog, request, null, ScriptAssistOptions.defaults()), request);
        System.out.println("Script assistant instructions without the HM reference: " + system.length() + " characters, about "
                + system.length() / 4 + " tokens.");
        assertTrue(String.valueOf(system.length()), system.length() < 60000);
    }
}
