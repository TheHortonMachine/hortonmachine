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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantConfig.ServerKind;
import org.hortonmachine.gears.assistant.Assistants;
import org.hortonmachine.gears.assistant.IAssistantBackend;
import org.junit.Test;

/**
 * Tests the resolution of the configuration and the lookup of the backend.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestLlmConfigs {

    private final Map<String, String> properties = new HashMap<>();
    private final Map<String, String> environment = new HashMap<>();
    private final Map<String, String> preferences = new HashMap<>();

    private AssistantConfig resolve() {
        return LlmConfigs.resolve(properties::get, environment::get, preferences::get);
    }

    @Test
    public void testDefaults() {
        AssistantConfig config = resolve();
        assertEquals(ServerKind.OLLAMA, config.serverKind());
        assertEquals("http://localhost:11434", config.baseUrl());
        assertNull(config.model());
        assertNull(config.apiKey());
        assertEquals(AssistantConfig.DEFAULT_TIMEOUT_SECONDS, config.timeoutSeconds());
        assertEquals(AssistantConfig.DEFAULT_HISTORY_TURNS, config.historyTurns());
        assertEquals(AssistantConfig.DEFAULT_KEEP_ALIVE_MINUTES, config.keepAliveMinutes());
        assertEquals(AssistantConfig.DEFAULT_CONTEXT_LENGTH, config.contextLength());
        assertTrue(config.isLocal());
    }

    @Test
    public void testKeepAliveAndContext() {
        preferences.put(LlmConfigs.KEY_KEEP_ALIVE, "120");
        environment.put("HM_LLM_CONTEXT", "32768");
        assertEquals(120, resolve().keepAliveMinutes());
        assertEquals(32768, resolve().contextLength());
        // a context too small for the instructions falls back to the default
        environment.put("HM_LLM_CONTEXT", "512");
        assertEquals(AssistantConfig.DEFAULT_CONTEXT_LENGTH, resolve().contextLength());
        preferences.put(LlmConfigs.KEY_KEEP_ALIVE, "-1");
        assertEquals(0, resolve().keepAliveMinutes());
    }

    @Test
    public void testHistoryTurns() {
        preferences.put(LlmConfigs.KEY_HISTORY, "2");
        assertEquals(2, resolve().historyTurns());
        preferences.put(LlmConfigs.KEY_HISTORY, "-3");
        assertEquals(0, resolve().historyTurns());
        preferences.put(LlmConfigs.KEY_HISTORY, "many");
        assertEquals(AssistantConfig.DEFAULT_HISTORY_TURNS, resolve().historyTurns());
    }

    @Test
    public void testOrder() {
        preferences.put(LlmConfigs.KEY_URL, "http://pref:1");
        preferences.put(LlmConfigs.KEY_MODEL, "prefmodel");
        preferences.put(LlmConfigs.KEY_TIMEOUT, "20");
        environment.put("HM_LLM_URL", "http://env:2");
        environment.put("HM_LLM_TIMEOUT", "30");
        properties.put(LlmConfigs.KEY_URL, "http://prop:3");

        AssistantConfig config = resolve();
        assertEquals("http://prop:3", config.baseUrl());
        assertEquals(30, config.timeoutSeconds());
        assertEquals("prefmodel", config.model());
        assertFalse(config.isLocal());
    }

    @Test
    public void testOllamaHostComesAfterPreferences() {
        environment.put("OLLAMA_HOST", "0.0.0.0");
        assertEquals("http://localhost:11434", resolve().baseUrl());

        environment.put("OLLAMA_HOST", "gpu.example.org");
        assertEquals("http://gpu.example.org:11434", resolve().baseUrl());

        preferences.put(LlmConfigs.KEY_URL, "https://llm.example.org");
        assertEquals("https://llm.example.org", resolve().baseUrl());
    }

    @Test
    public void testOllamaHostIgnoredForOtherServers() {
        environment.put("OLLAMA_HOST", "gpu.example.org");
        preferences.put(LlmConfigs.KEY_KIND, "openai_compatible");
        AssistantConfig config = resolve();
        assertEquals(ServerKind.OPENAI_COMPATIBLE, config.serverKind());
        assertEquals("http://localhost:11434", config.baseUrl());
    }

    @Test
    public void testBadValuesFallBack() {
        properties.put(LlmConfigs.KEY_KIND, "unknown");
        properties.put(LlmConfigs.KEY_TIMEOUT, "soon");
        properties.put(LlmConfigs.KEY_MODEL, "  ");
        AssistantConfig config = resolve();
        assertEquals(ServerKind.OLLAMA, config.serverKind());
        assertEquals(AssistantConfig.DEFAULT_TIMEOUT_SECONDS, config.timeoutSeconds());
        assertNull(config.model());
    }

    @Test
    public void testNormalizeOllamaHost() {
        assertEquals("http://localhost:11434", LlmConfigs.normalizeOllamaHost("0.0.0.0"));
        assertEquals("http://localhost:8080", LlmConfigs.normalizeOllamaHost("0.0.0.0:8080"));
        assertEquals("https://host.org:11434/ollama", LlmConfigs.normalizeOllamaHost("https://host.org/ollama"));
        assertEquals("http://[::1]:11434", LlmConfigs.normalizeOllamaHost("[::1]"));
        assertEquals("http://[::1]:9", LlmConfigs.normalizeOllamaHost("[::1]:9"));
    }

    @Test
    public void testIsLocal() {
        assertTrue(new AssistantConfig(ServerKind.OLLAMA, "http://127.0.0.1:11434", null, null, 1, 0, 30, 16384).isLocal());
        assertTrue(new AssistantConfig(ServerKind.OLLAMA, "http://[::1]:11434", null, null, 1, 0, 30, 16384).isLocal());
        assertFalse(new AssistantConfig(ServerKind.OLLAMA, "not a url", null, null, 1, 0, 30, 16384).isLocal());
    }

    @Test
    public void testBackendFoundThroughServiceLoader() {
        Optional<IAssistantBackend> backend = Assistants.getBackend();
        assertTrue(backend.isPresent());
        assertTrue(backend.get() instanceof LlmAssistantBackend);
        assertTrue(backend.get().getSqlAssistant().isPresent());
    }
}
