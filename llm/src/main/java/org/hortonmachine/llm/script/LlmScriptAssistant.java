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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantTurn;
import org.hortonmachine.gears.assistant.IScriptAssistant;
import org.hortonmachine.gears.assistant.ScriptAnswer;
import org.hortonmachine.gears.assistant.ScriptAssistOptions;
import org.hortonmachine.gears.assistant.ScriptContext;
import org.hortonmachine.gears.libs.modules.HMModelRegistry;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.llm.AuditLog;
import org.hortonmachine.llm.ChatMessage;
import org.hortonmachine.llm.CodeChats;
import org.hortonmachine.llm.LlmProvider;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The {@link IScriptAssistant} on top of a {@link LlmProvider}.
 *
 * <p>The model gets the rules, the {@link ScriptNotes}, what the console tells in the
 * {@link ScriptContext} and the modules of the {@link ModuleCatalog}: the ones relevant to the
 * request, and those the script being fixed or explained uses, in full, the others by name.
 * Requests are answered with text, whose last groovy code block is the script, if any; fixes with a
 * JSON object <code>{script, explanation}</code> enforced through a JSON schema. The script is then
 * compiled by the console and its use of the modules checked with the {@link ScriptGuard}: the
 * problems go back to the model, with the references of modules it used without having them and
 * the helper methods similar to classes that don't exist, up to {@value #MAX_REPAIR_ROUNDS} times.
 * Every request goes to the {@link AuditLog}.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class LlmScriptAssistant implements IScriptAssistant {

    private static final JSONObject ANSWER_SCHEMA = new JSONObject()//
            .put("type", "object")//
            .put("properties", new JSONObject()//
                    .put("script", new JSONObject().put("type", "string"))//
                    .put("explanation", new JSONObject().put("type", "string")))//
            .put("required", new JSONArray().put("script").put("explanation"))//
            .put("additionalProperties", false);

    /**
     * The rules of the fixes, answered as JSON.
     */
    private static final String SCRIPT_RULES = """
            You write Groovy scripts for the HortonMachine scripting console.
            Rules:
            - Answer only with a JSON object with the fields "script" and "explanation".
            - "script" holds the whole script, with its imports, ready to run.
            - Use only the modules of these instructions, with the exact names of their fields; never invent
              modules, fields or HM methods. Earlier answers can be wrong: take names from these instructions.
            - Prefer a module to writing the algorithm yourself.
            - Feed each input with its kind: the output of the module that makes it, or a file of the user.
              Add the modules that make the inputs the user doesn't have, starting from what the user has.
            - "explanation" says in a few sentences, in the language of the request, what the script does
              and which paths the user must set.
            """;

    private static final String EXPLAIN_RULES = """
            You explain scripts written for the HortonMachine scripting console.
            Explain briefly, step by step, what the script does, in plain words and in the language of the request.
            Point out mistakes, such as modules, fields or methods that don't exist, or modules never run.
            """;

    private static final String ASK_RULES = """
            You help a user of the HortonMachine scripting console, which runs Groovy scripts using the
            HortonMachine processing modules and the HM helper class.
            Rules:
            - Answer briefly and practically, in the language of the request.
            - When a script is asked for, or is the best answer, give one complete script with its imports,
              ready to run, in a single groovy code block; then say in a few sentences what it does and which
              paths the user must set. You can't run anything yourself: the user runs the script.
            - Use only the modules, classes and HM methods of these instructions, with the exact names of their
              fields; never invent them. Earlier answers can be wrong: take names from these instructions.
            - Prefer a module to writing the algorithm yourself.
            - Feed each input with its kind: the output of the module that makes it, or a file of the user.
              Add the modules that make the inputs the user doesn't have, starting from what the user has.
            - When the request names a variable of the earlier runs or of the editor script, use that variable,
              not a placeholder.
            - When the script of the user's editor is given and the request goes on with it, write only the code
              to add to it, which runs after it and uses its imports and variables; never define its variables again.
            """;

    /**
     * How many times a script rejected by the checks is sent back to the model to be corrected.
     */
    public static final int MAX_REPAIR_ROUNDS = 2;

    /**
     * How many example scripts are given, the most similar to the request.
     */
    static final int MAX_EXAMPLES = 2;

    /** The longest error text sent to the model. */
    private static final int MAX_ERROR_LENGTH = 1500;
    /** The most variables listed. */
    private static final int MAX_VARIABLES = 30;
    /** The most lines of the editor script given to the model. */
    static final int MAX_EDITOR_LINES = 300;

    /** The most methods of the helper class suggested for a class that doesn't exist. */
    private static final int MAX_SIMILAR_HELPERS = 5;

    /** The start of a variable definition, up to its name: def, final or a type. */
    private static final String DEFINITION = "(?:def|final|[A-Z][\\w<>,\\[\\] ]*)[ \\t]+";

    private static final Pattern UNRESOLVED_IMPORT = Pattern.compile("unable to resolve class ([\\w.]+\\.\\w+)");

    private static final Pattern UNRESOLVED_CLASS = Pattern.compile("unable to resolve class (?:[\\w.]+\\.)?(\\w+)");

    private final Supplier<AssistantConfig> configSupplier;
    private final Function<AssistantConfig, LlmProvider> providerFactory;
    private final Supplier<ModuleCatalog> catalogSupplier;
    private final AuditLog auditLog;

    /**
     * @param configSupplier gives the configuration in use at each request.
     * @param providerFactory makes the provider for a configuration.
     * @param catalogSupplier gives the modules, called at each request (the catalog is built once).
     * @param auditLog the log of the requests.
     */
    public LlmScriptAssistant( Supplier<AssistantConfig> configSupplier, Function<AssistantConfig, LlmProvider> providerFactory,
            Supplier<ModuleCatalog> catalogSupplier, AuditLog auditLog ) {
        this.configSupplier = configSupplier;
        this.providerFactory = providerFactory;
        this.catalogSupplier = catalogSupplier;
        this.auditLog = auditLog;
    }

    @Override
    public ScriptAnswer ask( ScriptContext context, String request, List<AssistantTurn> history, ScriptAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception {
        ModuleCatalog catalog = catalog();
        // a follow-up is likely about the last script of the discussion or the one in the editor
        String knownCode = String.join("\n", nonNull(lastCode(history), context.editorScript()));
        Set<ModuleDescriptor> references = relevantModules(catalog, request, knownCode.isEmpty() ? null : knownCode, options);
        List<ChatMessage> messages = chat(instructions(ASK_RULES, context, catalog, references, request), history, request,
                historyTurns(), false);
        return askChecked("script-ask", false, request, context, catalog, references, messages, onToken, pm);
    }

    @Override
    public ScriptAnswer fix( ScriptContext context, String script, String error, List<AssistantTurn> history,
            ScriptAssistOptions options, Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception {
        ModuleCatalog catalog = catalog();
        Set<ModuleDescriptor> references = relevantModules(catalog, error, script, options);
        List<ChatMessage> messages = chat(instructions(SCRIPT_RULES, context, catalog, references, script),
                history, fixRequest(script, error), historyTurns(), true);
        return askChecked("script-fix", true, null, context, catalog, references, messages, onToken, pm);
    }

    @Override
    public String explain( ScriptContext context, String script, List<AssistantTurn> history, ScriptAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception {
        ModuleCatalog catalog = catalog();
        Set<ModuleDescriptor> references = relevantModules(catalog, "", script, options);
        List<ChatMessage> messages = chat(instructions(EXPLAIN_RULES, context, catalog, references, script), history,
                explainRequest(script), historyTurns(), false);
        return askText("script-explain", messages, onToken, pm);
    }

    @Override
    public void prepare( ScriptContext context, IHMProgressMonitor pm ) throws Exception {
        // the reference, which every request starts with, and a token of question: not audited,
        // since it holds nothing of the user
        List<ChatMessage> messages = List.of(ChatMessage.system(reference(context, catalog())), ChatMessage.user("Ready?"));
        providerFactory.apply(configSupplier.get()).prepare(messages, pm);
    }

    private static List<String> nonNull( String... texts ) {
        List<String> list = new ArrayList<>();
        for( String text : texts ) {
            if (text != null && !text.isBlank()) {
                list.add(text);
            }
        }
        return list;
    }

    private int historyTurns() {
        return configSupplier.get().historyTurns();
    }

    /**
     * @return the modules.
     * @throws IllegalStateException if there are none: without them the model invents modules.
     */
    private ModuleCatalog catalog() {
        ModuleCatalog catalog = catalogSupplier.get();
        if (catalog.getModules().isEmpty()) {
            throw new IllegalStateException("No processing modules found on the classpath (no "
                    + HMModelRegistry.SERVICE_FILE + " of the modules jar), so the assistant can't tell which modules exist");
        }
        return catalog;
    }

    static List<ChatMessage> chat( String system, List<AssistantTurn> history, String request, int maxTurns,
            boolean jsonAnswers ) {
        return CodeChats.chat(system, history, request, maxTurns, jsonAnswers, "script", "groovy");
    }

    /**
     * @return the code of the most recent turn that has some, given apart or in a code block of
     *          the answer, or <code>null</code>: a follow-up is most likely about it.
     */
    static String lastCode( List<AssistantTurn> history ) {
        if (history != null) {
            for( int i = history.size() - 1; i >= 0; i-- ) {
                AssistantTurn turn = history.get(i);
                if (turn.code() != null) {
                    return turn.code();
                }
                String code = turn.answer() == null ? null : CodeChats.lastCodeBlock(turn.answer(), "groovy");
                if (code != null) {
                    return code;
                }
            }
        }
        return null;
    }

    /**
     * The modules given in full: those the script uses, then the most relevant to the text, up to
     * the number of the options (more if the script uses more).
     *
     * @param catalog the modules.
     * @param text the request, question or error.
     * @param script a script whose modules are needed, or <code>null</code>.
     * @param options the options of the request.
     * @return the modules.
     */
    static Set<ModuleDescriptor> relevantModules( ModuleCatalog catalog, String text, String script, ScriptAssistOptions options ) {
        Set<ModuleDescriptor> modules = new LinkedHashSet<>();
        if (script != null) {
            for( String name : ScriptGuard.check(script, catalog).modules() ) {
                modules.add(catalog.get(name));
            }
        }
        for( ModuleDescriptor module : catalog.select(text, options.moduleReferences()) ) {
            if (modules.size() >= Math.max(options.moduleReferences(), 1)) {
                break;
            }
            modules.add(module);
        }
        return modules;
    }

    /**
     * The instructions: first the reference that is the same for every request (notes, the
     * console, the kinds of data, all the modules), then the rules of the task and what depends on
     * the request (examples, variables, the modules in full).
     *
     * <p>The server reuses its work on the beginning of a prompt that is the same as in the last
     * request: with the same reference first, only the rest is read again, which on a laptop is
     * seconds less per request. Nothing that changes between requests may go into the reference.</p>
     *
     * <p>The modules in full come last, closest to the request: small models use best what they
     * read last.</p>
     *
     * @param rules the rules of the task.
     * @param context the console.
     * @param catalog the modules.
     * @param references the modules given in full.
     * @param text the text the examples are chosen for.
     * @return the instructions.
     */
    static String instructions( String rules, ScriptContext context, ModuleCatalog catalog, Collection<ModuleDescriptor> references,
            String text ) {
        StringBuilder sb = new StringBuilder(reference(context, catalog));
        sb.append("\nYour task:\n").append(rules);
        List<String> examples = chooseExamples(context.examples(), Recipes.getDefault(), text);
        if (!examples.isEmpty()) {
            sb.append("\nWorking example scripts:\n");
            for( String example : examples ) {
                sb.append(example).append("\n");
            }
        }
        if (context.variables() != null && !context.variables().isEmpty()) {
            sb.append("\nVariables left by the earlier runs, usable in the script:\n");
            int count = 0;
            for( Map.Entry<String, String> entry : context.variables().entrySet() ) {
                if (count++ >= MAX_VARIABLES) {
                    break;
                }
                sb.append("- ").append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n");
            }
        }
        if (context.editorScript() != null && !context.editorScript().isBlank()) {
            sb.append("\nThe script in the user's editor, which your code goes on with:\n```groovy\n")
                    .append(editorScriptExcerpt(context.editorScript())).append("\n```\n");
        }
        if (!references.isEmpty()) {
            sb.append("\nThe modules most relevant to the request, with all their fields:\n");
            for( ModuleDescriptor module : references ) {
                sb.append("\n").append(catalog.reference(module));
            }
        }
        return sb.toString();
    }

    /**
     * @param script the script of the editor.
     * @return the script, or for long ones its beginning (imports, variables) and its end (where
     *          the new code goes), at most {@value #MAX_EDITOR_LINES} lines.
     */
    static String editorScriptExcerpt( String script ) {
        List<String> lines = script.strip().lines().toList();
        if (lines.size() <= MAX_EDITOR_LINES) {
            return String.join("\n", lines);
        }
        int half = MAX_EDITOR_LINES / 2;
        return String.join("\n", lines.subList(0, half)) + "\n// ... " + (lines.size() - 2 * half) + " lines left out ...\n"
                + String.join("\n", lines.subList(lines.size() - half, lines.size()));
    }

    /**
     * @return the part of the instructions that is the same for every request of a console.
     */
    static String reference( ScriptContext context, ModuleCatalog catalog ) {
        StringBuilder sb = new StringBuilder(
                "You assist the users of the HortonMachine scripting console. First the reference, then your task.\n\n");
        sb.append(ScriptNotes.TEXT);
        if (context.imports() != null && !context.imports().isBlank()) {
            sb.append("\nThe imports scripts usually start with:\n").append(context.imports().strip()).append("\n");
        }
        if (context.helperReference() != null && !context.helperReference().isBlank()) {
            sb.append("\nThe HM helper class, with static methods called as HM.name(...):\n")
                    .append(context.helperReference().strip()).append("\n");
        }
        String overview = catalog.getKinds().overview();
        if (!overview.isEmpty()) {
            sb.append("\nThe kinds of maps the modules chain with: a field inXxx reads, and outXxx writes, the kind Xxx.\n")
                    .append(overview);
        }
        sb.append("\nAll the modules, by folder (the fields of the most relevant ones follow):\n")
                .append(ModuleCatalog.index(catalog.getModules()));
        return sb.toString();
    }

    /**
     * @return the examples most similar to the text, as a title comment followed by the script;
     *          the first example if none is similar.
     */
    static List<String> chooseExamples( Map<String, String> examples, String text ) {
        return chooseExamples(examples, List.of(), text);
    }

    /**
     * @param examples the examples of the console, by title.
     * @param recipes the recipes, found also by their keywords.
     * @param text the text to find them for.
     * @return the examples and recipes most similar to the text, as a title comment followed by
     *          the script; the first example (or recipe, without examples) if none is similar.
     */
    static List<String> chooseExamples( Map<String, String> examples, List<Recipes.Recipe> recipes, String text ) {
        List<String> chosen = new ArrayList<>();
        List<Map.Entry<String, String>> entries = new ArrayList<>();
        List<String> matchTexts = new ArrayList<>();
        if (examples != null) {
            for( Map.Entry<String, String> entry : examples.entrySet() ) {
                entries.add(entry);
                matchTexts.add(entry.getKey() + " " + entry.getValue());
            }
        }
        for( Recipes.Recipe recipe : recipes ) {
            entries.add(Map.entry(recipe.title(), recipe.script()));
            matchTexts.add(recipe.title() + " " + recipe.keywords() + " " + recipe.script());
        }
        if (entries.isEmpty()) {
            return chosen;
        }
        Set<String> words = ModuleCatalog.words(text == null ? "" : text);
        List<Integer> scores = new ArrayList<>();
        for( String matchText : matchTexts ) {
            Set<String> exampleWords = ModuleCatalog.words(matchText);
            int score = 0;
            for( String word : words ) {
                for( String other : exampleWords ) {
                    if (ModuleCatalog.matches(word, other)) {
                        score++;
                        break;
                    }
                }
            }
            scores.add(score);
        }
        while( chosen.size() < MAX_EXAMPLES ) {
            int best = -1;
            for( int i = 0; i < entries.size(); i++ ) {
                if (scores.get(i) > 0 && (best == -1 || scores.get(i) > scores.get(best))) {
                    best = i;
                }
            }
            if (best == -1) {
                break;
            }
            chosen.add("// " + entries.get(best).getKey() + "\n" + entries.get(best).getValue().strip());
            scores.set(best, 0);
        }
        if (chosen.isEmpty()) {
            chosen.add("// " + entries.get(0).getKey() + "\n" + entries.get(0).getValue().strip());
        }
        return chosen;
    }

    private static String fixRequest( String script, String error ) {
        return "This script failed:\n```groovy\n" + script.strip() + "\n```\n\nThe error was:\n" + shortenError(error)
                + "\n\nWrite the corrected script and explain the fix.";
    }

    private static String explainRequest( String script ) {
        return "Explain this script:\n```groovy\n" + script.strip() + "\n```";
    }

    private static String shortenError( String error ) {
        String text = error.strip();
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) + "..." : text;
    }

    private String askText( String task, List<ChatMessage> messages, Consumer<String> onToken, IHMProgressMonitor pm )
            throws Exception {
        AssistantConfig config = configSupplier.get();
        String response = null;
        try {
            response = providerFactory.apply(config).chat(messages, null, onToken, pm);
            auditLog.append(task, config, messages, response, null, null);
            return CodeChats.toText(response, "script", "groovy");
        } catch (Exception e) {
            auditLog.append(task, config, messages, response, null, CodeChats.errorText(e));
            throw e;
        }
    }

    /**
     * Ask, check the script of the answer and send it back to the model to be corrected while the
     * checks find problems, up to {@value #MAX_REPAIR_ROUNDS} times.
     *
     * @param json <code>true</code> if the answer is the JSON object with the script, which must
     *          have one; <code>false</code> for text, whose last groovy code block is the script, if any.
     * @param request the request of the user, whose variables the script must not define again,
     *          or <code>null</code>.
     */
    private ScriptAnswer askChecked( String task, boolean json, String request, ScriptContext context, ModuleCatalog catalog,
            Set<ModuleDescriptor> references, List<ChatMessage> messages, Consumer<String> onToken, IHMProgressMonitor pm )
            throws Exception {
        AssistantConfig config = configSupplier.get();
        LlmProvider provider = providerFactory.apply(config);
        List<ChatMessage> conversation = new ArrayList<>(messages);
        Set<ModuleDescriptor> given = new LinkedHashSet<>(references);
        String roundTask = task;
        for( int round = 0;; round++ ) {
            String response = null;
            try {
                response = provider.chat(conversation, json ? ANSWER_SCHEMA : null, onToken, pm);
                String script;
                String text;
                if (json) {
                    JSONObject object = CodeChats.toJson(response);
                    script = CodeChats.stripFences(object.optString("script", "")).trim();
                    if (script.isEmpty()) {
                        throw new IllegalStateException("The model gave no script: " + CodeChats.shorten(response));
                    }
                    text = CodeChats.asText(object.optString("explanation", ""), script, "groovy");
                } else {
                    text = CodeChats.toText(response, "script", "groovy");
                    script = CodeChats.lastCodeBlock(text, "groovy");
                    if (script == null) {
                        auditLog.append(roundTask, config, conversation, response, null, null);
                        return new ScriptAnswer(text, null, false, null, List.of());
                    }
                }

                String compileError = context.checker() != null ? context.checker().check(script) : null;
                // small models keep imports of classes that don't exist even when they don't use them:
                // those lines go without asking the model
                String cleaned = dropUnusedUnresolvedImports(script, compileError);
                if (!cleaned.equals(script)) {
                    text = text.replace(script, cleaned);
                    script = cleaned;
                    compileError = context.checker().check(script);
                }
                // code that goes on with the editor script compiles after it
                String editorScript = context.editorScript();
                if (compileError != null && editorScript != null && !editorScript.isBlank()
                        && context.checker().check(editorScript + "\n" + script) == null) {
                    compileError = null;
                }
                ScriptGuard.Result guard = ScriptGuard.check(script, catalog);
                List<String> problems = new ArrayList<>(guard.errors());
                Set<String> knownVariables = new LinkedHashSet<>();
                if (context.variables() != null) {
                    knownVariables.addAll(context.variables().keySet());
                }
                knownVariables.addAll(scriptVariables(editorScript));
                problems.addAll(variableErrors(script, knownVariables, request));
                String checkError = checkError(compileError, problems);
                auditLog.append(roundTask, config, conversation, response, script,
                        checkError == null ? null : "rejected by the checks: " + checkError);
                if (checkError == null || round >= MAX_REPAIR_ROUNDS) {
                    return new ScriptAnswer(text, script, context.checker() != null, checkError, guard.modules());
                }

                // the modules it used without knowing their fields, and the ones it may have meant
                List<ModuleDescriptor> missing = new ArrayList<>();
                for( String name : guard.modules() ) {
                    ModuleDescriptor module = catalog.get(name);
                    if (given.add(module)) {
                        missing.add(module);
                    }
                }
                List<String> similar = similarModules(catalog, compileError);
                List<String> helpers = similarHelperMethods(context.helperReference(), compileError);
                conversation.add(ChatMessage.assistant(response));
                conversation.add(ChatMessage.user(repairRequest(catalog, checkError, missing, similar, helpers, json)));
                roundTask = task + "-repair";
                // the answer that follows replaces the rejected one
                if (pm != null) {
                    pm.beginTask(CodeChats.repairTaskName("script", checkError, round + 2, MAX_REPAIR_ROUNDS + 1), -1);
                }
            } catch (Exception e) {
                auditLog.append(roundTask, config, conversation, response, null, CodeChats.errorText(e));
                throw e;
            }
        }
    }

    /**
     * Remove the imports of classes the compiler can't find that the script never uses: the model
     * would have to be asked again only to drop a line.
     *
     * @param script the script.
     * @param compileError the errors of the compilation, or <code>null</code>.
     * @return the script without those imports, the same if there are none.
     */
    static String dropUnusedUnresolvedImports( String script, String compileError ) {
        if (compileError == null) {
            return script;
        }
        String result = script;
        Matcher matcher = UNRESOLVED_IMPORT.matcher(compileError);
        while( matcher.find() ) {
            String className = matcher.group(1);
            String simpleName = className.substring(className.lastIndexOf('.') + 1);
            Pattern importLine = Pattern.compile("(?m)^[ \\t]*import[ \\t]+" + Pattern.quote(className) + "[ \\t]*;?[ \\t]*\\R?");
            String without = importLine.matcher(result).replaceAll("");
            if (!without.equals(result) && !Pattern.compile("\\b" + Pattern.quote(simpleName) + "\\b").matcher(without).find()) {
                result = without;
            }
        }
        return result;
    }

    /**
     * The variables that already exist (of the earlier runs, of the editor script) that the
     * request names and the script defines again, usually with a placeholder in place of the value
     * the user meant.
     *
     * @param script the script.
     * @param variables the names of the variables that exist.
     * @param request the request, or <code>null</code>.
     * @return the problems, empty if none.
     */
    static List<String> variableErrors( String script, Collection<String> variables, String request ) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            return errors;
        }
        for( String name : variables ) {
            String quoted = Pattern.quote(name);
            if (Pattern.compile("\\b" + quoted + "\\b").matcher(request).find()
                    && Pattern.compile("(?m)^[ \\t]*" + DEFINITION + quoted + "[ \\t]*=").matcher(script).find()) {
                errors.add("The request uses the variable " + name + ", which already exists: use it as it is, don't define it again.");
            }
        }
        return errors;
    }

    /**
     * @param script a script, or <code>null</code>.
     * @return the names of the variables it defines at its top level, not indented.
     */
    static Set<String> scriptVariables( String script ) {
        Set<String> names = new LinkedHashSet<>();
        if (script != null) {
            Matcher matcher = Pattern.compile("(?m)^" + DEFINITION + "(\\w+)[ \\t]*=(?!=)").matcher(script);
            while( matcher.find() ) {
                names.add(matcher.group(1));
            }
        }
        return names;
    }

    /**
     * @return the problems of the compilation and of the module use, or <code>null</code> if none.
     */
    static String checkError( String compileError, List<String> guardErrors ) {
        List<String> problems = new ArrayList<>();
        if (compileError != null && !compileError.isBlank()) {
            problems.add("The script doesn't compile: " + shortenError(compileError));
        }
        problems.addAll(guardErrors);
        return problems.isEmpty() ? null : String.join("\n", problems);
    }

    /**
     * @return for each class the compiler can't find, the modules with a similar name, as "Name: Module1, Module2".
     */
    static List<String> similarModules( ModuleCatalog catalog, String compileError ) {
        List<String> similar = new ArrayList<>();
        if (compileError == null) {
            return similar;
        }
        Set<String> seen = new LinkedHashSet<>();
        Matcher matcher = UNRESOLVED_CLASS.matcher(compileError);
        while( matcher.find() ) {
            String name = matcher.group(1);
            if (!seen.add(name) || catalog.get(name) != null) {
                continue;
            }
            // split the camel case, so that PitFiller finds Pitfiller and FlowDirection FlowDirections
            String words = name.replaceAll("([a-z])([A-Z])", "$1 $2") + " " + name;
            List<String> names = new ArrayList<>();
            for( ModuleDescriptor module : catalog.select(words, 3) ) {
                names.add(module.getName());
            }
            if (!names.isEmpty()) {
                similar.add(name + ": " + String.join(", ", names));
            }
        }
        return similar;
    }

    /**
     * For classes the compiler can't find, the methods of the helper class with a word of their
     * name: a made up <code>HMGeoPackage</code> finds <code>HM.connectGeopackage</code>.
     *
     * @param helperReference the reference of the helper class, a method per line, or <code>null</code>.
     * @param compileError the errors of the compilation, or <code>null</code>.
     * @return the lines of the reference of those methods, at most {@value #MAX_SIMILAR_HELPERS}.
     */
    static List<String> similarHelperMethods( String helperReference, String compileError ) {
        List<String> found = new ArrayList<>();
        if (helperReference == null || compileError == null) {
            return found;
        }
        Set<String> words = new LinkedHashSet<>();
        Matcher matcher = UNRESOLVED_CLASS.matcher(compileError);
        while( matcher.find() ) {
            for( String word : matcher.group(1).split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])") ) {
                // short pieces as HM or Geo would match everything
                if (word.length() >= 4) {
                    words.add(word.toLowerCase());
                }
            }
        }
        for( String line : helperReference.split("\n") ) {
            int open = line.indexOf('(');
            if (open == -1) {
                continue;
            }
            String head = line.substring(0, open).trim();
            String name = head.substring(head.lastIndexOf(' ') + 1).toLowerCase();
            if (words.stream().anyMatch(name::contains) && found.size() < MAX_SIMILAR_HELPERS) {
                found.add(line.strip());
            }
        }
        return found;
    }

    /**
     * The request to correct a rejected script.
     *
     * @param catalog the modules.
     * @param checkError the problems found.
     * @param missing the modules the script uses whose reference the model didn't have yet.
     * @param similar the modules similar to the classes the compiler can't find.
     * @param helpers the methods of the helper class similar to the classes the compiler can't find.
     * @param json <code>true</code> if the answer is the JSON with the script, else text with a code block.
     * @return the request.
     */
    static String repairRequest( ModuleCatalog catalog, String checkError, List<ModuleDescriptor> missing,
            List<String> similar, List<String> helpers, boolean json ) {
        StringBuilder sb = new StringBuilder("The script has these problems:\n").append(checkError).append("\n");
        if (!similar.isEmpty()) {
            sb.append("\nThere are no such classes. Modules with a similar name:\n");
            for( String line : similar ) {
                sb.append("- ").append(line).append("\n");
            }
        }
        if (!helpers.isEmpty()) {
            sb.append("\nThere are no such classes. Methods of the HM helper class with a similar name, called as HM.name(...):\n");
            for( String line : helpers ) {
                sb.append("- ").append(line).append("\n");
            }
        }
        if (!missing.isEmpty()) {
            sb.append("\nThe modules the script uses have exactly these fields:\n");
            for( ModuleDescriptor module : missing ) {
                sb.append("\n").append(catalog.reference(module));
            }
        }
        return sb.append(json ? "\nWrite the corrected script." : "\nAnswer again, with the corrected script in a groovy code block.")
                .toString();
    }
}
