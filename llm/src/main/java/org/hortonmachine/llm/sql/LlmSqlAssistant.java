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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantTurn;
import org.hortonmachine.gears.assistant.ISqlAssistant;
import org.hortonmachine.gears.assistant.SqlAnswer;
import org.hortonmachine.gears.assistant.SqlAssistOptions;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.llm.AuditLog;
import org.hortonmachine.llm.ChatMessage;
import org.hortonmachine.llm.LlmProvider;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The {@link ISqlAssistant} on top of a {@link LlmProvider}.
 *
 * <p>The model gets the rules, the notes on the dialect and the schema as instructions, and
 * answers with a JSON object <code>{sql, explanation}</code> enforced through a JSON schema. The
 * SQL is then checked with the {@link SqlGuard}: the read-only flag is set and queries without
 * a limit get the one of the options. Read-only SQL is also planned by the database
 * (<code>EXPLAIN</code>, never run): if the database rejects it, its error goes back to the model,
 * up to {@value #MAX_REPAIR_ROUNDS} times. Every request goes to the {@link AuditLog}.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class LlmSqlAssistant implements ISqlAssistant {

    private static final JSONObject ANSWER_SCHEMA = new JSONObject()//
            .put("type", "object")//
            .put("properties", new JSONObject()//
                    .put("sql", new JSONObject().put("type", "string"))//
                    .put("explanation", new JSONObject().put("type", "string")))//
            .put("required", new JSONArray().put("sql").put("explanation"))//
            .put("additionalProperties", false);

    private static final String SQL_RULES = """
            You write SQL for the database described below.
            Rules:
            - Answer only with a JSON object with the fields "sql" and "explanation".
            - "sql" holds exactly one SQL statement for this database, without comments.
            - Use only the tables and columns listed below, with their exact names; never invent names.
              Primary keys are not always called id: check the schema.
            - A table without a GEOMETRY column has no geometry: never use other columns as geometries.
              To place it, join the table holding its location, as listed after the schema.
            - Earlier answers in the discussion can be wrong: take names from the schema, never from earlier answers.
            - Write a query that only reads data, unless the request explicitly asks to change data or tables.
            - "explanation" says in one or two sentences, in the language of the request, what the SQL does.
            """;

    private static final String EXPLAIN_RULES = """
            You explain SQL written for the database described below.
            Explain briefly, step by step, what the SQL does, in plain words and in the language of the request.
            Point out mistakes, such as tables or columns that don't exist or functions the dialect lacks.
            """;

    private static final String HELP_RULES = """
            You help a user of the HortonMachine database viewer with databases and spatial data.
            Answer briefly and practically, in the language of the question. If SQL helps, show it, but
            remember that the user runs it: you can't connect, run or change anything yourself.
            When writing SQL, take table and column names from the schema, never from earlier answers,
            which can be wrong.
            """;

    /**
     * How many times SQL rejected by the database is sent back to the model to be corrected.
     */
    public static final int MAX_REPAIR_ROUNDS = 2;

    private final Supplier<AssistantConfig> configSupplier;
    private final Function<AssistantConfig, LlmProvider> providerFactory;
    private final AuditLog auditLog;

    /**
     * @param configSupplier gives the configuration in use at each request.
     * @param providerFactory makes the provider for a configuration.
     * @param auditLog the log of the requests.
     */
    public LlmSqlAssistant( Supplier<AssistantConfig> configSupplier, Function<AssistantConfig, LlmProvider> providerFactory,
            AuditLog auditLog ) {
        this.configSupplier = configSupplier;
        this.providerFactory = providerFactory;
        this.auditLog = auditLog;
    }

    @Override
    public SqlAnswer generate( ADb db, String question, List<AssistantTurn> history, SqlAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception {
        List<ChatMessage> messages = chat(instructions(SQL_RULES, db, options), history, question, historyTurns(), true);
        return askSql("sql-generate", db, messages, options, onToken, pm);
    }

    @Override
    public SqlAnswer fix( ADb db, String sql, String dbError, List<AssistantTurn> history, SqlAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception {
        List<ChatMessage> messages = chat(instructions(SQL_RULES, db, options), history, fixRequest(sql, dbError),
                historyTurns(), true);
        return askSql("sql-fix", db, messages, options, onToken, pm);
    }

    @Override
    public String explain( ADb db, String sql, List<AssistantTurn> history, SqlAssistOptions options, Consumer<String> onToken,
            IHMProgressMonitor pm ) throws Exception {
        List<ChatMessage> messages = chat(instructions(EXPLAIN_RULES, db, options), history, explainRequest(sql),
                historyTurns(), false);
        return askText("sql-explain", messages, onToken, pm);
    }

    @Override
    public String help( ADb db, String question, List<AssistantTurn> history, SqlAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception {
        String system = HELP_RULES + "\n" + ConnectionGuide.TEXT;
        if (db == null) {
            system += "\nNo database is connected right now.\n";
        } else {
            system += "\nThe connected database:\n" + instructions("", db, options);
        }
        return askText("sql-help", chat(system, history, question, historyTurns(), false), onToken, pm);
    }

    private int historyTurns() {
        return configSupplier.get().historyTurns();
    }

    private static String fixRequest( String sql, String dbError ) {
        return "This SQL failed:\n" + sql + "\n\nThe database gave this error:\n" + dbError
                + "\n\nWrite the corrected SQL and explain the fix.";
    }

    private static String explainRequest( String sql ) {
        return "Explain this SQL:\n" + sql;
    }

    /**
     * Build the messages of a request: the instructions, the last turns of the discussion and the
     * new request.
     *
     * <p>Earlier answers with SQL are given back in the form the new answer must have: the JSON the
     * model answers with when SQL is asked, text with an SQL code block when text is asked.
     * Small models copy the form of the earlier answers, so a text answer would otherwise come
     * as JSON.</p>
     *
     * @param system the instructions.
     * @param history the earlier turns, oldest first, can be <code>null</code>.
     * @param request the new request.
     * @param maxTurns the most recent turns to give back, 0 for none.
     * @param jsonAnswers <code>true</code> if the new answer is the JSON with SQL, <code>false</code> for text.
     * @return the messages.
     */
    static List<ChatMessage> chat( String system, List<AssistantTurn> history, String request, int maxTurns,
            boolean jsonAnswers ) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(system));
        if (history != null && maxTurns > 0) {
            int from = Math.max(0, history.size() - maxTurns);
            for( AssistantTurn turn : history.subList(from, history.size()) ) {
                messages.add(ChatMessage.user(turn.question()));
                String answer = turn.answer() == null ? "" : turn.answer();
                if (turn.sql() != null) {
                    answer = jsonAnswers ? new JSONObject().put("sql", turn.sql()).put("explanation", answer).toString()
                            : asText(answer, turn.sql());
                }
                messages.add(ChatMessage.assistant(answer));
            }
        }
        messages.add(ChatMessage.user(request));
        return messages;
    }

    private static String asText( String explanation, String sql ) {
        return (explanation.isBlank() ? "" : explanation.strip() + "\n\n") + "```sql\n" + sql.strip() + "\n```";
    }

    private String askText( String task, List<ChatMessage> messages, Consumer<String> onToken, IHMProgressMonitor pm )
            throws Exception {
        AssistantConfig config = configSupplier.get();
        String response = null;
        try {
            response = providerFactory.apply(config).chat(messages, null, onToken, pm);
            auditLog.append(task, config, messages, response, null, null);
            return toText(response);
        } catch (Exception e) {
            auditLog.append(task, config, messages, response, null, errorText(e));
            throw e;
        }
    }

    /**
     * Take a text answer as it is, unless the model wrote the JSON of the SQL answers anyways: that
     * is turned into text with an SQL code block.
     *
     * @param response the answer of the model.
     * @return the text to show.
     */
    static String toText( String response ) {
        String text = response.trim();
        String json = stripFences(text).trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            try {
                JSONObject object = new JSONObject(json);
                if (object.has("explanation") || object.has("sql")) {
                    String sql = stripFences(object.optString("sql", "")).trim();
                    String explanation = object.optString("explanation", "");
                    return sql.isEmpty() ? explanation.strip() : asText(explanation, sql);
                }
            } catch (Exception e) {
                // not that JSON: keep the text
            }
        }
        return text;
    }

    /**
     * Ask for SQL, have the database check it and send it back to the model to be corrected while
     * the database rejects it, up to {@value #MAX_REPAIR_ROUNDS} times.
     */
    private SqlAnswer askSql( String task, ADb db, List<ChatMessage> messages, SqlAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception {
        AssistantConfig config = configSupplier.get();
        LlmProvider provider = providerFactory.apply(config);
        List<ChatMessage> conversation = new ArrayList<>(messages);
        String roundTask = task;
        for( int round = 0;; round++ ) {
            String response = null;
            try {
                response = provider.chat(conversation, ANSWER_SCHEMA, onToken, pm);
                SqlAnswer answer = toAnswer(response, options);
                boolean checked = answer.readOnly() && SqlGuard.isSingleStatement(answer.sql());
                String checkError = checked ? check(db, answer.sql()) : null;
                auditLog.append(roundTask, config, conversation, response, answer.sql(),
                        checkError == null ? null : "rejected by the database: " + checkError);
                if (checkError == null || round >= MAX_REPAIR_ROUNDS) {
                    return new SqlAnswer(answer.sql(), answer.explanation(), answer.readOnly(), answer.limitApplied(), checked,
                            checkError);
                }
                conversation.add(ChatMessage.assistant(response));
                conversation.add(ChatMessage.user(repairRequest(checkError, tablesUsedIn(db, answer.sql()))));
                roundTask = task + "-repair";
                if (onToken != null) {
                    onToken.accept("\n");
                }
            } catch (Exception e) {
                auditLog.append(roundTask, config, conversation, response, null, errorText(e));
                throw e;
            }
        }
    }

    /**
     * The request to correct rejected SQL.
     *
     * <p>Only the first line of the error is given: the rest (positions, PostgreSQL's "Perhaps you
     * meant" hints) tends to mislead small models. The columns of the tables the SQL uses are given
     * right with it, since small models use them much better there than somewhere in the schema.</p>
     *
     * @param dbError the error of the database.
     * @param tablesUsed the description of the tables the SQL uses, can be empty.
     * @return the request.
     */
    static String repairRequest( String dbError, String tablesUsed ) {
        String firstLine = dbError.strip().lines().findFirst().orElse(dbError).strip();
        String request = "The database rejected this SQL with the error:\n" + firstLine + "\n";
        if (!tablesUsed.isBlank()) {
            request += "\nThe tables used in the SQL have exactly these columns:\n" + tablesUsed;
        }
        return request + "\nWrite the corrected SQL. Use only these tables and columns, with their exact names.";
    }

    private static String tablesUsedIn( ADb db, String sql ) {
        try {
            return SchemaContextBuilder.describeTablesUsedIn(db, sql);
        } catch (Exception e) {
            // the error alone then
            return "";
        }
    }

    /**
     * Have the database plan some SQL without running it.
     *
     * @param db the database.
     * @param sql a single read-only statement.
     * @return the error of the database, or <code>null</code> if it accepts the SQL.
     */
    static String check( ADb db, String sql ) {
        String explain = switch( db.getType() ) {
        case SQLITE, MBTILES, SPATIALITE, SPATIALITE4ANDROID, GEOPACKAGE, GEOPACKAGE4ANDROID -> "EXPLAIN QUERY PLAN ";
        default -> "EXPLAIN ";
        };
        try {
            db.getTableRecordsMapFromRawSql(explain + sql, 1);
            return null;
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().trim();
            return message.length() > 500 ? message.substring(0, 500) + "..." : message;
        }
    }

    private static String instructions( String rules, ADb db, SqlAssistOptions options ) throws Exception {
        return rules + "\nDialect: " + DialectNotes.getName(db.getType()) + "\n" + DialectNotes.getNotes(db.getType())
                + "\nSchema:\n" + SchemaContextBuilder.build(db, options.includeSampleRows());
    }

    /**
     * Turn the answer of the model into a {@link SqlAnswer}, with the checks of the {@link SqlGuard}.
     *
     * <p>Small models sometimes wrap the JSON or the SQL in markdown fences anyways: they are removed.</p>
     *
     * @param response the answer of the model.
     * @param options the options of the request.
     * @return the answer.
     * @throws IllegalStateException if the answer holds no SQL.
     */
    static SqlAnswer toAnswer( String response, SqlAssistOptions options ) {
        String json = stripFences(response);
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start == -1 || end <= start) {
            throw new IllegalStateException("The model didn't answer with JSON: " + shorten(response));
        }
        JSONObject object = new JSONObject(json.substring(start, end + 1));
        String sql = stripFences(object.optString("sql", "")).trim();
        if (sql.isEmpty()) {
            throw new IllegalStateException("The model gave no SQL: " + shorten(response));
        }
        String explanation = object.optString("explanation", "").trim();

        boolean readOnly = SqlGuard.isReadOnly(sql);
        String finalSql = sql;
        if (readOnly) {
            finalSql = SqlGuard.applyLimit(sql, options.autoLimit());
        }
        return new SqlAnswer(finalSql, explanation, readOnly, !finalSql.equals(sql), false, null);
    }

    private static String stripFences( String text ) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            trimmed = firstNewline == -1 ? "" : trimmed.substring(firstNewline + 1);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed;
    }

    private static String shorten( String text ) {
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    private static String errorText( Exception e ) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
