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
package org.hortonmachine.server.notebooks;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A running {@link NotebookKernel} process, seen from the server.
 *
 * <p>The messages of the kernel go to the listener, from a reading thread; when the process ends the
 * listener gets <code>{"type":"exit"}</code>.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class KernelProcess {
    private static final Logger LOGGER = Logger.getLogger(KernelProcess.class.getName());

    /** The seconds the kernel has to end after the shutdown message before it is killed. */
    private static final int SHUTDOWN_GRACE_SECONDS = 3;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Process process;
    private final PrintStream input;

    /**
     * Start a kernel.
     *
     * @param javaCommand the start of the java command, as for the jobs.
     * @param folder the working folder of the kernel, against which the relative paths of the cells resolve.
     * @param listener gets the messages of the kernel.
     */
    public KernelProcess( List<String> javaCommand, File folder, Consumer<JsonNode> listener ) throws Exception {
        List<String> command = new ArrayList<>(javaCommand);
        command.add(NotebookKernel.class.getName());
        process = new ProcessBuilder(command).directory(folder).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        input = new PrintStream(process.getOutputStream(), true, StandardCharsets.UTF_8);
        Thread reader = new Thread(() -> read(listener), "notebook-kernel-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void read( Consumer<JsonNode> listener ) {
        try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while( (line = output.readLine()) != null ) {
                JsonNode message;
                try {
                    message = mapper.readTree(line);
                } catch (Exception e) {
                    // not of the protocol, as the jvm warnings
                    LOGGER.fine("Kernel: " + line);
                    continue;
                }
                try {
                    listener.accept(message);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "A message of the kernel can't be handled.", e);
                }
            }
        } catch (Exception e) {
            // the process ended
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ObjectNode exit = mapper.createObjectNode();
        exit.put("type", "exit");
        listener.accept(exit);
    }

    /**
     * Send a message to the kernel.
     */
    public void send( Map<String, ? > message ) {
        try {
            String json = mapper.writeValueAsString(message);
            synchronized (input) {
                input.println(json);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "A message can't be sent to the kernel.", e);
        }
    }

    public void execute( String id, String code ) {
        send(Map.of("op", "execute", "id", id, "code", code));
    }

    public void interrupt() {
        send(Map.of("op", "interrupt"));
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    /**
     * Ask the kernel to end, killing it if it doesn't.
     */
    public void shutdown() {
        if (!process.isAlive()) {
            return;
        }
        send(Map.of("op", "shutdown"));
        try {
            if (!process.waitFor(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS)) {
                kill();
            }
        } catch (InterruptedException e) {
            kill();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Kill the kernel at once.
     */
    public void kill() {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }
}
