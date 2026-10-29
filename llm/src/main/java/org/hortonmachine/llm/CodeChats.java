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
package org.hortonmachine.llm;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.assistant.AssistantTurn;
import org.json.JSONObject;

/**
 * The parts shared by the assistants that answer either with text or with code, the code given as
 * a JSON object <code>{&lt;code key&gt;, explanation}</code>: building the chat from the discussion
 * and reading the answers.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class CodeChats {

    private CodeChats() {
    }

    /**
     * Build the messages of a request: the instructions, the last turns of the discussion and the
     * new request.
     *
     * <p>Earlier answers with code are given back in the form the new answer must have: the JSON the
     * model answers with when code is asked, text with a code block when text is asked.
     * Small models copy the form of the earlier answers, so a text answer would otherwise come
     * as JSON.</p>
     *
     * @param system the instructions.
     * @param history the earlier turns, oldest first, can be <code>null</code>.
     * @param request the new request.
     * @param maxTurns the most recent turns to give back, 0 for none.
     * @param jsonAnswers <code>true</code> if the new answer is the JSON with code, <code>false</code> for text.
     * @param codeKey the key of the code in the JSON, as sql or script.
     * @param language the language of the code blocks, as sql or groovy.
     * @return the messages.
     */
    public static List<ChatMessage> chat( String system, List<AssistantTurn> history, String request, int maxTurns,
            boolean jsonAnswers, String codeKey, String language ) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(system));
        if (history != null && maxTurns > 0) {
            int from = Math.max(0, history.size() - maxTurns);
            for( AssistantTurn turn : history.subList(from, history.size()) ) {
                messages.add(ChatMessage.user(turn.question()));
                String answer = turn.answer() == null ? "" : turn.answer();
                if (turn.code() != null) {
                    answer = jsonAnswers ? new JSONObject().put(codeKey, turn.code()).put("explanation", answer).toString()
                            : asText(answer, turn.code(), language);
                }
                messages.add(ChatMessage.assistant(answer));
            }
        }
        messages.add(ChatMessage.user(request));
        return messages;
    }

    /**
     * @return the explanation followed by the code in a fenced block.
     */
    public static String asText( String explanation, String code, String language ) {
        return (explanation.isBlank() ? "" : explanation.strip() + "\n\n") + "```" + language + "\n" + code.strip() + "\n```";
    }

    /**
     * Take a text answer as it is, unless the model wrote the JSON of the code answers anyways: that
     * is turned into text with a code block.
     *
     * @param response the answer of the model.
     * @param codeKey the key of the code in the JSON.
     * @param language the language of the code block.
     * @return the text to show.
     */
    public static String toText( String response, String codeKey, String language ) {
        String text = response.trim();
        String json = stripFences(text).trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            try {
                JSONObject object = new JSONObject(json);
                if (object.has("explanation") || object.has(codeKey)) {
                    String code = stripFences(object.optString(codeKey, "")).trim();
                    String explanation = object.optString("explanation", "");
                    return code.isEmpty() ? explanation.strip() : asText(explanation, code, language);
                }
            } catch (Exception e) {
                // not that JSON: keep the text
            }
        }
        return text;
    }

    /**
     * Read the JSON object of an answer with code.
     *
     * <p>Small models sometimes wrap the JSON in markdown fences anyways: they are removed.</p>
     *
     * @param response the answer of the model.
     * @return the object.
     * @throws IllegalStateException if the answer holds no JSON object.
     */
    public static JSONObject toJson( String response ) {
        String json = stripFences(response);
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start == -1 || end <= start) {
            throw new IllegalStateException("The model didn't answer with JSON: " + shorten(response));
        }
        return new JSONObject(json.substring(start, end + 1));
    }

    /**
     * Find the code of a text answer: its last fenced code block in the language, or without
     * one. A block still open at the end (an answer cut short) counts too.
     *
     * @param text the answer, as markdown.
     * @param language the language of the blocks, as sql or groovy; java is taken as groovy.
     * @return the code of the last such block, or <code>null</code> if there is none.
     */
    public static String lastCodeBlock( String text, String language ) {
        String last = null;
        StringBuilder block = null;
        boolean wanted = false;
        for( String line : text.replace("\r", "").split("\n", -1) ) {
            String trimmed = line.trim();
            if (block == null) {
                if (trimmed.startsWith("```")) {
                    String tag = trimmed.substring(3).trim().toLowerCase();
                    wanted = tag.isEmpty() || tag.equals(language) || (language.equals("groovy") && tag.equals("java"));
                    block = new StringBuilder();
                }
            } else if (trimmed.startsWith("```")) {
                if (wanted && !block.toString().isBlank()) {
                    last = block.toString();
                }
                block = null;
            } else {
                block.append(line).append("\n");
            }
        }
        if (block != null && wanted && !block.toString().isBlank()) {
            last = block.toString();
        }
        return last == null ? null : last.strip();
    }

    /**
     * Remove the markdown fences around a text, if any.
     */
    public static String stripFences( String text ) {
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

    /**
     * @return the text, cut to 200 characters, for error messages.
     */
    public static String shorten( String text ) {
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    /**
     * @return the message of the exception, or its class if it has none.
     */
    public static String errorText( Exception e ) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    /**
     * The name of the task of a repair round, given to the monitor when a rejected answer is sent
     * back to the model: the text streamed after it is a new answer.
     *
     * @param what what is corrected, as script or SQL.
     * @param checkError the problems found.
     * @param attempt the number of the answer about to come, from 2.
     * @param attempts the most answers there can be.
     * @return the name, with the first line of the problems.
     */
    public static String repairTaskName( String what, String checkError, int attempt, int attempts ) {
        String firstLine = checkError.strip().lines().findFirst().orElse("");
        return "The checks rejected the " + what + " (" + shorten(firstLine) + "): the model corrects it, attempt " + attempt
                + " of " + attempts + ".";
    }
}
