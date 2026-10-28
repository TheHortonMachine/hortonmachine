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
package org.hortonmachine.gears.assistant;

import java.net.InetAddress;
import java.net.URI;

/**
 * The configuration of the server used by an {@link IAssistantBackend}.
 *
 * @param serverKind the kind of server.
 * @param baseUrl the base url of the server, e.g. <code>http://localhost:11434</code>.
 * @param model the name of the model to use, or <code>null</code> if none is chosen yet.
 * @param apiKey the key sent as bearer token, or <code>null</code> for servers that need none.
 * @param timeoutSeconds the time to wait for an answer.
 * @param historyTurns how many earlier turns of a discussion are sent again with each request, so
 *          that follow-up questions are understood; 0 makes every request stand alone. More turns
 *          make longer and slower requests and use more of the context of the model.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record AssistantConfig( ServerKind serverKind, String baseUrl, String model, String apiKey, int timeoutSeconds,
        int historyTurns ) {

    /**
     * The kinds of server an assistant can talk to.
     */
    public enum ServerKind {
        /**
         * An Ollama server, which can also list and download models.
         */
        OLLAMA,
        /**
         * Any server with an OpenAI compatible chat endpoint (LM Studio, vLLM, hosted services).
         */
        OPENAI_COMPATIBLE
    }

    /**
     * The url of a local Ollama.
     */
    public static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    /**
     * The default time to wait for an answer, long since the first call also loads the model.
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 300;

    /**
     * The default turns of a discussion sent again with each request: enough for follow-ups, small
     * enough for the context of the small local models.
     */
    public static final int DEFAULT_HISTORY_TURNS = 6;

    /**
     * @return <code>true</code> if the server runs on this machine, so that no data leaves it.
     */
    public boolean isLocal() {
        try {
            String host = URI.create(baseUrl).getHost();
            if (host == null) {
                return false;
            }
            return host.equalsIgnoreCase("localhost") || InetAddress.getByName(host).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
