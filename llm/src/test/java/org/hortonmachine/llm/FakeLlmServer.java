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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import org.hortonmachine.gears.utils.files.FileUtilities;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * A local http server for the tests, that answers like Ollama and the OpenAI compatible api.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class FakeLlmServer implements AutoCloseable {

    /**
     * A received request.
     *
     * @param method the http method.
     * @param path the path.
     * @param authorization the authorization header, or <code>null</code>.
     * @param body the body.
     */
    public record Received( String method, String path, String authorization, String body ) {
    }

    private final HttpServer server;
    public final List<Received> received = new CopyOnWriteArrayList<>();

    public FakeLlmServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // handlers that hang (a model loading) must not block the server
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    /**
     * @return the base url of the server.
     */
    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /**
     * Answer a path with a fixed body.
     */
    public void answer( String path, int status, String contentType, String body ) {
        handle(path, exchange -> send(exchange, status, contentType, body));
    }

    /**
     * Answer a path with a handler.
     */
    public void handle( String path, HttpHandler handler ) {
        server.createContext(path, exchange -> {
            String body = FileUtilities.readInputStreamToString(exchange.getRequestBody());
            received.add(new Received(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"), body));
            try {
                handler.handle(exchange);
            } catch (IOException e) {
                // the client went away
            } finally {
                exchange.close();
            }
        });
    }

    /**
     * Send a whole answer.
     */
    public static void send( HttpExchange exchange, int status, String contentType, String body ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * @return the server sent events of a chat completion streamed in the given pieces.
     */
    public static String chatStream( String... pieces ) {
        StringBuilder sb = new StringBuilder();
        for( String piece : pieces ) {
            String chunk = new org.json.JSONObject().put("choices", new org.json.JSONArray()
                    .put(new org.json.JSONObject().put("index", 0).put("delta", new org.json.JSONObject().put("content", piece))))
                    .toString();
            sb.append("data: ").append(chunk).append("\n\n");
        }
        sb.append("data: [DONE]\n\n");
        return sb.toString();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
