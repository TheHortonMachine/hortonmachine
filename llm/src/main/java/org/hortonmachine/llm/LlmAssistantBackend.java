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

import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Optional;

import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantConfig.ServerKind;
import org.hortonmachine.gears.assistant.BackendStatus;
import org.hortonmachine.gears.assistant.IAssistantBackend;
import org.hortonmachine.gears.assistant.ISqlAssistant;
import org.hortonmachine.gears.assistant.ModelInfo;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.llm.sql.LlmSqlAssistant;

/**
 * The {@link IAssistantBackend} of this module, registered for the {@link java.util.ServiceLoader}.
 *
 * <p>Chats go through the OpenAI compatible api for both kinds of server; with an Ollama server the
 * native api is used in addition to check the version and to list and download models.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class LlmAssistantBackend implements IAssistantBackend {

    /**
     * The model suggested to new users: small enough for a 16 GB laptop, trained on code.
     */
    public static final String RECOMMENDED_MODEL = "qwen2.5-coder:7b";

    private static final String INSTALL_HINT = "Install Ollama from https://ollama.com/download "
            + "(on Linux: curl -fsSL https://ollama.com/install.sh | sh) and start it.";

    private final HttpClient client = HttpSupport.newClient();
    private final AuditLog auditLog;
    private volatile AssistantConfig config;

    /**
     * The constructor used by the {@link java.util.ServiceLoader}: configuration from
     * {@link LlmConfigs#resolve()} and the {@link AuditLog#defaultLog() default log}.
     */
    public LlmAssistantBackend() {
        this(LlmConfigs.resolve(), AuditLog.defaultLog());
    }

    /**
     * @param config the configuration to start with.
     * @param auditLog the log of the requests.
     */
    public LlmAssistantBackend( AssistantConfig config, AuditLog auditLog ) {
        this.config = config;
        this.auditLog = auditLog;
    }

    @Override
    public String getName() {
        return config.serverKind() == ServerKind.OLLAMA ? "Ollama" : "OpenAI compatible server";
    }

    @Override
    public AssistantConfig getConfig() {
        return config;
    }

    @Override
    public void setConfig( AssistantConfig config ) {
        this.config = config;
        LlmConfigs.save(config);
    }

    @Override
    public BackendStatus checkStatus() {
        AssistantConfig current = config;
        boolean local = current.isLocal();
        boolean ollama = current.serverKind() == ServerKind.OLLAMA;
        try {
            String version = ollama ? new OllamaManager(current, client).getVersion() : null;
            List<ModelInfo> models = listModels(current);
            String message;
            if (models.isEmpty()) {
                message = "The server has no model yet" + (ollama ? ": download one, e.g. " + RECOMMENDED_MODEL + "." : ".");
            } else if (current.model() == null || current.model().isBlank()) {
                message = "Choose a model.";
            } else if (!hasModel(models, current.model())) {
                message = "The model " + current.model() + " is not on the server: choose or download another one.";
            } else {
                message = "Ready.";
            }
            return new BackendStatus(true, local, version, message);
        } catch (Exception e) {
            String message = "The server at " + current.baseUrl() + " can't be reached: " + describe(e);
            if (ollama && local) {
                message += "\n" + INSTALL_HINT;
            }
            return new BackendStatus(false, local, null, message);
        }
    }

    /**
     * The http client reports a refused connection with no message: say what happened.
     */
    static String describe( Exception e ) {
        if (e instanceof ConnectException) {
            return "connection refused, the server is not running or not listening there.";
        }
        if (e instanceof HttpTimeoutException) {
            return "no answer in time.";
        }
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    @Override
    public List<ModelInfo> listModels() throws Exception {
        return listModels(config);
    }

    private List<ModelInfo> listModels( AssistantConfig current ) throws Exception {
        if (current.serverKind() == ServerKind.OLLAMA) {
            return new OllamaManager(current, client).listModels();
        }
        return new OpenAiCompatibleProvider(current, client).listModels();
    }

    /**
     * Ollama names models with a tag and gives <code>latest</code> to those asked without one.
     */
    static boolean hasModel( List<ModelInfo> models, String model ) {
        String wanted = model.trim();
        for( ModelInfo info : models ) {
            if (info.name().equals(wanted) || info.name().equals(wanted + ":latest")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getRecommendedModel() {
        return RECOMMENDED_MODEL;
    }

    @Override
    public boolean canPullModels() {
        return config.serverKind() == ServerKind.OLLAMA;
    }

    @Override
    public void pullModel( String model, IHMProgressMonitor pm ) throws Exception {
        if (!canPullModels()) {
            throw new UnsupportedOperationException("Only Ollama servers can download models.");
        }
        new OllamaManager(config, client).pull(model, pm);
    }

    @Override
    public Optional<ISqlAssistant> getSqlAssistant() {
        return Optional.of(new LlmSqlAssistant(this::getConfig, c -> new OpenAiCompatibleProvider(c, client), auditLog));
    }
}
