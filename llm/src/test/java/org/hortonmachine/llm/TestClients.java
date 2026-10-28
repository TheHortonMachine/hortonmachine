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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantConfig.ServerKind;
import org.hortonmachine.gears.assistant.BackendStatus;
import org.hortonmachine.gears.assistant.ModelInfo;
import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the http clients against a {@link FakeLlmServer}.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestClients {

    private static final String TAGS = """
            {"models":[{"name":"qwen2.5-coder:7b","size":4683087332,
              "details":{"parameter_size":"7.6B","quantization_level":"Q4_K_M"}},
             {"name":"llama3:latest","size":100}]}
            """;

    private FakeLlmServer server;
    private HttpClient client;

    @Before
    public void setUp() throws Exception {
        server = new FakeLlmServer();
        client = HttpSupport.newClient();
    }

    @After
    public void tearDown() {
        server.close();
    }

    private AssistantConfig config( ServerKind kind, String model, String apiKey ) {
        return new AssistantConfig(kind, server.url(), model, apiKey, 10, 0);
    }

    @Test
    public void testChatStreamsTokensAndSendsSchemaAndKey() throws Exception {
        server.answer("/v1/chat/completions", 200, "text/event-stream",
                FakeLlmServer.chatStream("{\"sql\":", "\"SELECT 1\"", "}"));
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(config(ServerKind.OLLAMA, "m1", "secret"), client);

        List<String> tokens = new ArrayList<>();
        JSONObject schema = new JSONObject().put("type", "object");
        String answer = provider.chat(List.of(ChatMessage.system("rules"), ChatMessage.user("question")), schema,
                tokens::add, new TestMonitor());

        assertEquals("{\"sql\":\"SELECT 1\"}", answer);
        assertEquals(List.of("{\"sql\":", "\"SELECT 1\"", "}"), tokens);

        FakeLlmServer.Received request = server.received.get(0);
        assertEquals("Bearer secret", request.authorization());
        JSONObject body = new JSONObject(request.body());
        assertEquals("m1", body.getString("model"));
        assertTrue(body.getBoolean("stream"));
        assertEquals("system", body.getJSONArray("messages").getJSONObject(0).getString("role"));
        assertEquals("question", body.getJSONArray("messages").getJSONObject(1).getString("content"));
        JSONObject format = body.getJSONObject("response_format");
        assertEquals("json_schema", format.getString("type"));
        assertEquals("object", format.getJSONObject("json_schema").getJSONObject("schema").getString("type"));
    }

    @Test
    public void testChatWithoutSchemaAndKey() throws Exception {
        server.answer("/v1/chat/completions", 200, "text/event-stream", FakeLlmServer.chatStream("Hello"));
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(config(ServerKind.OLLAMA, "m1", null), client);

        assertEquals("Hello", provider.chat(List.of(ChatMessage.user("hi")), null, null, null));
        FakeLlmServer.Received request = server.received.get(0);
        assertNull(request.authorization());
        assertFalse(new JSONObject(request.body()).has("response_format"));
    }

    @Test
    public void testBaseUrlWithV1() throws Exception {
        server.answer("/v1/models", 200, "application/json", "{\"data\":[{\"id\":\"a\"},{\"id\":\"b\"}]}");
        AssistantConfig config = new AssistantConfig(ServerKind.OPENAI_COMPATIBLE, server.url() + "/v1/", null, null, 10, 0);

        List<ModelInfo> models = new OpenAiCompatibleProvider(config, client).listModels();
        assertEquals(2, models.size());
        assertEquals("a", models.get(0).name());
        assertEquals("/v1/models", server.received.get(0).path());
    }

    @Test
    public void testServerErrorIsReadable() throws Exception {
        server.answer("/v1/chat/completions", 404, "application/json", "{\"error\":{\"message\":\"model 'x' not found\"}}");
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(config(ServerKind.OLLAMA, "x", null), client);
        try {
            provider.chat(List.of(ChatMessage.user("hi")), null, null, null);
            fail();
        } catch (IOException e) {
            assertEquals("The server answered with status 404: model 'x' not found", e.getMessage());
        }
    }

    @Test
    public void testChatWithoutModelFails() throws Exception {
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(config(ServerKind.OLLAMA, " ", null), client);
        try {
            provider.chat(List.of(ChatMessage.user("hi")), null, null, null);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(server.received.isEmpty());
        }
    }

    @Test
    public void testCancelWhileWaitingForTheModel() throws Exception {
        // a model being loaded: nothing comes for a long time
        server.handle("/v1/chat/completions", exchange -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // stop
            }
        });
        TestMonitor pm = new TestMonitor();
        cancelLater(pm, 300);
        long start = System.currentTimeMillis();
        try {
            new OpenAiCompatibleProvider(config(ServerKind.OLLAMA, "m", null), client)
                    .chat(List.of(ChatMessage.user("hi")), null, null, pm);
            fail();
        } catch (ModelsUserCancelException e) {
            assertTrue(System.currentTimeMillis() - start < 3000);
        }
    }

    @Test
    public void testCancelWhileStreaming() throws Exception {
        server.handle("/v1/chat/completions", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(FakeLlmServer.chatStream("first").replace("data: [DONE]\n\n", "").getBytes(StandardCharsets.UTF_8));
            os.flush();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // stop
            }
        });
        TestMonitor pm = new TestMonitor();
        List<String> tokens = new ArrayList<>();
        cancelLater(pm, 500);
        long start = System.currentTimeMillis();
        try {
            new OpenAiCompatibleProvider(config(ServerKind.OLLAMA, "m", null), client)
                    .chat(List.of(ChatMessage.user("hi")), null, tokens::add, pm);
            fail();
        } catch (ModelsUserCancelException e) {
            assertTrue(System.currentTimeMillis() - start < 3000);
            assertEquals(List.of("first"), tokens);
        }
    }

    private static void cancelLater( TestMonitor pm, long millis ) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                // cancel now
            }
            pm.setCanceled(true);
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Test
    public void testOllamaVersionAndModels() throws Exception {
        server.answer("/api/version", 200, "application/json", "{\"version\":\"0.12.3\"}");
        server.answer("/api/tags", 200, "application/json", TAGS);
        OllamaManager manager = new OllamaManager(config(ServerKind.OLLAMA, null, null), client);

        assertEquals("0.12.3", manager.getVersion());
        List<ModelInfo> models = manager.listModels();
        assertEquals(2, models.size());
        assertEquals(new ModelInfo("qwen2.5-coder:7b", 4683087332L, "7.6B Q4_K_M"), models.get(0));
        assertEquals(new ModelInfo("llama3:latest", 100, null), models.get(1));
    }

    @Test
    public void testOllamaPullProgress() throws Exception {
        String lines = """
                {"status":"pulling manifest"}
                {"status":"pulling abc","digest":"abc","total":200,"completed":50}
                {"status":"pulling abc","digest":"abc","total":200,"completed":200}
                {"status":"pulling def","digest":"def","total":10,"completed":10}
                {"status":"verifying sha256 digest"}
                {"status":"success"}
                """;
        server.answer("/api/pull", 200, "application/x-ndjson", lines);
        TestMonitor pm = new TestMonitor();
        new OllamaManager(config(ServerKind.OLLAMA, null, null), client).pull("qwen2.5-coder:7b", pm);

        assertEquals(List.of("pulling manifest", "pulling abc", "pulling def", "verifying sha256 digest", "success"), pm.tasks);
        assertEquals(200, pm.worked);
        assertTrue(pm.done);
        assertEquals("qwen2.5-coder:7b", new JSONObject(server.received.get(0).body()).getString("model"));
    }

    @Test
    public void testOllamaPullError() throws Exception {
        server.answer("/api/pull", 200, "application/x-ndjson",
                "{\"status\":\"pulling manifest\"}\n{\"error\":\"pull model manifest: file does not exist\"}\n");
        try {
            new OllamaManager(config(ServerKind.OLLAMA, null, null), client).pull("nope", null);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("pull model manifest: file does not exist", e.getMessage());
        }
    }

    @Test
    public void testBackendStatus() throws Exception {
        server.answer("/api/version", 200, "application/json", "{\"version\":\"0.12.3\"}");
        server.answer("/api/tags", 200, "application/json", TAGS);
        AuditLog log = new AuditLog(java.io.File.createTempFile("hm-llm-", ".jsonl"));

        BackendStatus status = new LlmAssistantBackend(config(ServerKind.OLLAMA, "llama3", null), log).checkStatus();
        assertTrue(status.reachable());
        assertTrue(status.local());
        assertEquals("0.12.3", status.version());
        assertEquals("Ready.", status.message());

        status = new LlmAssistantBackend(config(ServerKind.OLLAMA, "other", null), log).checkStatus();
        assertTrue(status.message().contains("not on the server"));

        status = new LlmAssistantBackend(config(ServerKind.OLLAMA, null, null), log).checkStatus();
        assertEquals("Choose a model.", status.message());
    }

    @Test
    public void testBackendStatusUnreachable() throws Exception {
        String url = server.url();
        server.close();
        AuditLog log = new AuditLog(java.io.File.createTempFile("hm-llm-", ".jsonl"));
        AssistantConfig config = new AssistantConfig(ServerKind.OLLAMA, url, null, null, 5, 0);

        BackendStatus status = new LlmAssistantBackend(config, log).checkStatus();
        assertFalse(status.reachable());
        assertTrue(status.message().contains("ollama.com"));
        assertTrue(status.message(), status.message().contains("connection refused"));

        AssistantConfig openAi = new AssistantConfig(ServerKind.OPENAI_COMPATIBLE, url, null, null, 5, 0);
        LlmAssistantBackend backend = new LlmAssistantBackend(openAi, log);
        status = backend.checkStatus();
        assertFalse(status.reachable());
        assertFalse(status.message().contains("ollama.com"));
        assertFalse(backend.canPullModels());
    }
}
