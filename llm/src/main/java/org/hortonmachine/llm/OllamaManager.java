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

import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.ModelInfo;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The management calls of the native Ollama api: version, models, download of models.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class OllamaManager {

    private final AssistantConfig config;
    private final HttpClient client;

    /**
     * @param config the server to use.
     * @param client the http client.
     */
    public OllamaManager( AssistantConfig config, HttpClient client ) {
        this.config = config;
        this.client = client;
    }

    /**
     * @return the version of the server.
     * @throws Exception if the server can't be reached.
     */
    public String getVersion() throws Exception {
        HttpRequest request = HttpSupport.request(config, "/api/version").GET().build();
        return new JSONObject(HttpSupport.send(client, request)).optString("version", null);
    }

    /**
     * @return the models downloaded on the server.
     * @throws Exception if the server can't be reached.
     */
    public List<ModelInfo> listModels() throws Exception {
        HttpRequest request = HttpSupport.request(config, "/api/tags").GET().build();
        JSONArray models = new JSONObject(HttpSupport.send(client, request)).optJSONArray("models");
        List<ModelInfo> infos = new ArrayList<>();
        if (models != null) {
            for( int i = 0; i < models.length(); i++ ) {
                JSONObject model = models.getJSONObject(i);
                String details = null;
                JSONObject detailsObj = model.optJSONObject("details");
                if (detailsObj != null) {
                    String parameterSize = detailsObj.optString("parameter_size", "");
                    String quantization = detailsObj.optString("quantization_level", "");
                    details = (parameterSize + " " + quantization).trim();
                    if (details.isEmpty()) {
                        details = null;
                    }
                }
                infos.add(new ModelInfo(model.getString("name"), model.optLong("size", -1), details));
            }
        }
        return infos;
    }

    /**
     * Download a model on the server.
     *
     * <p>Ollama downloads a model in parts, each one with its own progress: the monitor
     * gets a new task for each of them.</p>
     *
     * @param model the name of the model.
     * @param pm the monitor for progress and cancellation, or <code>null</code>.
     * @throws Exception if the download fails or is refused.
     */
    public void pull( String model, IHMProgressMonitor pm ) throws Exception {
        JSONObject body = new JSONObject().put("model", model).put("stream", true);
        HttpRequest request = HttpSupport.request(config, "/api/pull").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

        String[] currentStatus = {null};
        int[] lastPercent = {0};
        boolean[] success = {false};
        HttpSupport.streamLines(client, request, pm, line -> {
            if (line.isBlank()) {
                return;
            }
            JSONObject progress = new JSONObject(line);
            if (progress.has("error")) {
                throw new IllegalStateException(progress.getString("error"));
            }
            String status = progress.optString("status", "");
            if (status.equals("success")) {
                success[0] = true;
            }
            if (pm == null) {
                return;
            }
            if (!status.equals(currentStatus[0])) {
                currentStatus[0] = status;
                lastPercent[0] = 0;
                pm.beginTask(status, 100);
            }
            long total = progress.optLong("total", 0);
            long completed = progress.optLong("completed", 0);
            if (total > 0) {
                int percent = (int) Math.min(100, completed * 100 / total);
                if (percent > lastPercent[0]) {
                    pm.worked(percent - lastPercent[0]);
                    lastPercent[0] = percent;
                }
            }
        });
        if (pm != null) {
            pm.done();
        }
        if (!success[0]) {
            throw new IOException("The download of " + model + " ended without success.");
        }
    }
}
