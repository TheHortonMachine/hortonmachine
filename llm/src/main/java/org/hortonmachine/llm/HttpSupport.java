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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.json.JSONObject;

/**
 * The http calls shared by the clients, with the api key and cancellation.
 *
 * <p>Requests are cancelable while waiting for the answer to start (a model being loaded can take
 * a while) and while it streams: the monitor is checked every {@value #CANCEL_CHECK_MILLIS} ms.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
final class HttpSupport {

    static final int CANCEL_CHECK_MILLIS = 200;

    private HttpSupport() {
    }

    /**
     * @return a client shared by the requests of a backend.
     */
    static HttpClient newClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /**
     * Start a request to the server of a configuration.
     *
     * @param config the configuration.
     * @param path the path of the endpoint, starting with a slash.
     * @return the request builder with timeout and api key set.
     */
    static HttpRequest.Builder request( AssistantConfig config, String path ) {
        String base = config.baseUrl().trim();
        while( base.endsWith("/") ) {
            base = base.substring(0, base.length() - 1);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()));
        String apiKey = config.apiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey.trim());
        }
        return builder;
    }

    /**
     * Get the path of an endpoint of the OpenAI compatible api, which lives under <code>/v1</code>:
     * base urls are accepted both with and without it.
     *
     * @param config the configuration.
     * @param path the path after <code>/v1</code>, e.g. <code>/chat/completions</code>.
     * @return the path to use with {@link #request(AssistantConfig, String)}.
     */
    static String openAiPath( AssistantConfig config, String path ) {
        String base = config.baseUrl().trim();
        while( base.endsWith("/") ) {
            base = base.substring(0, base.length() - 1);
        }
        return base.endsWith("/v1") ? path : "/v1" + path;
    }

    /**
     * Send a request and get the body of a successful answer.
     *
     * @param client the client.
     * @param request the request.
     * @return the body.
     * @throws IOException with the error of the server if it doesn't answer with success.
     */
    static String send( HttpClient client, HttpRequest request ) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException(errorMessage(response.statusCode(), response.body()));
        }
        return response.body();
    }

    /**
     * Send a request and pass each line of the streamed answer to a consumer.
     *
     * @param client the client.
     * @param request the request.
     * @param pm the monitor used to cancel, or <code>null</code>.
     * @param onLine receives each line.
     * @throws ModelsUserCancelException if canceled.
     * @throws IOException with the error of the server if it doesn't answer with success.
     */
    static void streamLines( HttpClient client, HttpRequest request, IHMProgressMonitor pm, Consumer<String> onLine )
            throws Exception {
        CompletableFuture<HttpResponse<InputStream>> future = client.sendAsync(request,
                HttpResponse.BodyHandlers.ofInputStream());
        HttpResponse<InputStream> response = waitFor(future, pm);
        InputStream body = response.body();
        if (response.statusCode() >= 400) {
            try (body) {
                throw new IOException(errorMessage(response.statusCode(), FileUtilities.readInputStreamToString(body)));
            }
        }

        // a blocked read can't check the monitor: a watcher closes the stream to wake it up
        Thread watcher = null;
        if (pm != null) {
            watcher = new Thread(() -> {
                try {
                    while( !pm.isCanceled() ) {
                        Thread.sleep(CANCEL_CHECK_MILLIS);
                    }
                    body.close();
                } catch (InterruptedException | IOException e) {
                    // done
                }
            }, "hm-llm-cancel-watcher");
            watcher.setDaemon(true);
            watcher.start();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while( (line = reader.readLine()) != null ) {
                checkCanceled(pm);
                onLine.accept(line);
            }
            checkCanceled(pm);
        } catch (IOException e) {
            checkCanceled(pm);
            throw e;
        } finally {
            if (watcher != null) {
                watcher.interrupt();
            }
        }
    }

    /**
     * Wait for an answer, giving up if the monitor is canceled.
     */
    private static <T> T waitFor( CompletableFuture<T> future, IHMProgressMonitor pm ) throws Exception {
        while( true ) {
            if (pm != null && pm.isCanceled()) {
                future.cancel(true);
                throw new ModelsUserCancelException();
            }
            try {
                return future.get(CANCEL_CHECK_MILLIS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // check the monitor again
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                throw e;
            }
        }
    }

    private static void checkCanceled( IHMProgressMonitor pm ) {
        if (pm != null && pm.isCanceled()) {
            throw new ModelsUserCancelException();
        }
    }

    /**
     * Get a readable error from the answer of a server: Ollama sends <code>{"error":"..."}</code>,
     * the OpenAI compatible servers <code>{"error":{"message":"..."}}</code>.
     *
     * @param status the http status.
     * @param body the body of the answer.
     * @return the message.
     */
    static String errorMessage( int status, String body ) {
        String message = body == null ? "" : body.trim();
        try {
            JSONObject json = new JSONObject(message);
            Object error = json.opt("error");
            if (error instanceof JSONObject) {
                message = ((JSONObject) error).optString("message", message);
            } else if (error != null) {
                message = error.toString();
            }
        } catch (Exception e) {
            // not json, use the text
        }
        if (message.length() > 300) {
            message = message.substring(0, 300) + "...";
        }
        return "The server answered with status " + status + (message.isEmpty() ? "" : ": " + message);
    }
}
