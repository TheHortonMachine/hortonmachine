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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.ModelInfo;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A {@link LlmProvider} for the OpenAI compatible chat api, offered by Ollama, LM Studio, vLLM
 * and by hosted services: changing base url, model and key is enough to switch among them.
 *
 * <p>Answers are streamed (server sent events) and asked with temperature 0, since the
 * assistants want reproducible code rather than creative text. A JSON schema is sent as
 * <code>response_format</code> of type <code>json_schema</code>.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class OpenAiCompatibleProvider implements LlmProvider {

    private final AssistantConfig config;
    private final HttpClient client;

    /**
     * @param config the server and model to use.
     * @param client the http client.
     */
    public OpenAiCompatibleProvider( AssistantConfig config, HttpClient client ) {
        this.config = config;
        this.client = client;
    }

    @Override
    public String chat( List<ChatMessage> messages, JSONObject jsonSchema, Consumer<String> onToken, IHMProgressMonitor pm )
            throws Exception {
        String model = config.model();
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("No model is chosen in the assistant settings.");
        }

        JSONObject body = new JSONObject();
        body.put("model", model);
        JSONArray messagesArray = new JSONArray();
        for( ChatMessage message : messages ) {
            messagesArray.put(new JSONObject().put("role", message.role()).put("content", message.content()));
        }
        body.put("messages", messagesArray);
        body.put("stream", true);
        body.put("temperature", 0);
        if (jsonSchema != null) {
            JSONObject schemaFormat = new JSONObject().put("name", "answer").put("strict", true).put("schema", jsonSchema);
            body.put("response_format", new JSONObject().put("type", "json_schema").put("json_schema", schemaFormat));
        }

        HttpRequest request = HttpSupport.request(config, HttpSupport.openAiPath(config, "/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

        StringBuilder answer = new StringBuilder();
        HttpSupport.streamLines(client, request, pm, line -> {
            if (!line.startsWith("data:")) {
                return;
            }
            String data = line.substring(5).trim();
            if (data.isEmpty() || data.equals("[DONE]")) {
                return;
            }
            JSONObject chunk = new JSONObject(data);
            if (chunk.has("error")) {
                throw new IllegalStateException(HttpSupport.errorMessage(200, data));
            }
            JSONArray choices = chunk.optJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return;
            }
            JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
            if (delta == null) {
                return;
            }
            String token = delta.optString("content", "");
            if (!token.isEmpty()) {
                answer.append(token);
                if (onToken != null) {
                    onToken.accept(token);
                }
            }
        });
        if (answer.length() == 0) {
            throw new IOException("The model gave an empty answer.");
        }
        return answer.toString();
    }

    @Override
    public List<ModelInfo> listModels() throws Exception {
        HttpRequest request = HttpSupport.request(config, HttpSupport.openAiPath(config, "/models")).GET().build();
        JSONArray data = new JSONObject(HttpSupport.send(client, request)).optJSONArray("data");
        List<ModelInfo> models = new ArrayList<>();
        if (data != null) {
            for( int i = 0; i < data.length(); i++ ) {
                models.add(new ModelInfo(data.getJSONObject(i).getString("id"), -1, null));
            }
        }
        return models;
    }
}
