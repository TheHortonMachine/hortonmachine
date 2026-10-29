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

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.function.Consumer;

import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.ModelInfo;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A {@link LlmProvider} for the native chat api of Ollama (<code>/api/chat</code>), which, unlike
 * its OpenAI compatible api, sets the context of the model and how long it stays loaded.
 *
 * <p>Answers are streamed (one JSON object per line) and asked with temperature 0. A JSON schema
 * is sent as <code>format</code>. Every request sends the same context length: a different one
 * would make the server load the model again.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class OllamaChatProvider implements LlmProvider {

    /** The characters of text per token, about, for the estimate of the length of a request. */
    static final int CHARS_PER_TOKEN = 4;
    /** The tokens of the context left for the answer. */
    static final int ANSWER_TOKENS = 1024;

    private final AssistantConfig config;
    private final HttpClient client;

    /**
     * @param config the server and model to use.
     * @param client the http client.
     */
    public OllamaChatProvider( AssistantConfig config, HttpClient client ) {
        this.config = config;
        this.client = client;
    }

    @Override
    public String chat( List<ChatMessage> messages, JSONObject jsonSchema, Consumer<String> onToken, IHMProgressMonitor pm )
            throws Exception {
        JSONObject body = body(messages);
        if (jsonSchema != null) {
            body.put("format", jsonSchema);
        }
        StringBuilder answer = new StringBuilder();
        stream(body, pm, token -> {
            answer.append(token);
            if (onToken != null) {
                onToken.accept(token);
            }
        });
        if (answer.length() == 0) {
            throw new IOException("The model gave an empty answer.");
        }
        return answer.toString();
    }

    @Override
    public void prepare( List<ChatMessage> messages, IHMProgressMonitor pm ) throws Exception {
        JSONObject body = body(messages);
        // one token is enough to make the server read the whole request
        body.getJSONObject("options").put("num_predict", 1);
        stream(body, pm, token -> {
        });
    }

    /**
     * @return the body of a chat request, with the options of the configuration.
     * @throws IllegalStateException if the request doesn't fit in the context of the model.
     */
    JSONObject body( List<ChatMessage> messages ) {
        String model = config.model();
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("No model is chosen in the assistant settings.");
        }
        checkContext(messages, config.contextLength());
        JSONObject body = new JSONObject();
        body.put("model", model);
        JSONArray messagesArray = new JSONArray();
        for( ChatMessage message : messages ) {
            messagesArray.put(new JSONObject().put("role", message.role()).put("content", message.content()));
        }
        body.put("messages", messagesArray);
        body.put("stream", true);
        body.put("keep_alive", config.keepAliveMinutes() + "m");
        body.put("options", new JSONObject().put("temperature", 0).put("num_ctx", config.contextLength()));
        return body;
    }

    /**
     * Refuse a request longer than the context of the model: Ollama doesn't fail on it, it silently
     * drops the beginning (the instructions, the first tables of a schema) and the model answers
     * without knowing what it lost.
     *
     * <p>The length is estimated at {@value #CHARS_PER_TOKEN} characters per token, with
     * {@value #ANSWER_TOKENS} tokens left for the answer.</p>
     *
     * @param messages the messages of the request.
     * @param contextLength the context of the model, in tokens.
     * @throws IllegalStateException if the request doesn't fit.
     */
    static void checkContext( List<ChatMessage> messages, int contextLength ) {
        long chars = 0;
        for( ChatMessage message : messages ) {
            chars += message.content().length();
        }
        long tokens = chars / CHARS_PER_TOKEN;
        if (tokens + ANSWER_TOKENS > contextLength) {
            throw new IllegalStateException("The request needs about " + tokens + " tokens, more than the context of the model ("
                    + contextLength + " tokens, " + ANSWER_TOKENS + " of them kept for the answer): the model would read only "
                    + "its end. Raise \"Context (tokens)\" in the settings, which takes more memory, or use a server with a "
                    + "larger context");
        }
    }

    private void stream( JSONObject body, IHMProgressMonitor pm, Consumer<String> onToken ) throws Exception {
        HttpRequest request = HttpSupport.request(config, "/api/chat").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        HttpSupport.streamLines(client, request, pm, line -> {
            if (line.isBlank()) {
                return;
            }
            JSONObject chunk = new JSONObject(line);
            if (chunk.has("error")) {
                throw new IllegalStateException(HttpSupport.errorMessage(200, line));
            }
            JSONObject message = chunk.optJSONObject("message");
            String token = message == null ? "" : message.optString("content", "");
            if (!token.isEmpty()) {
                onToken.accept(token);
            }
        });
    }

    @Override
    public List<ModelInfo> listModels() throws Exception {
        return new OllamaManager(config, client).listModels();
    }
}
