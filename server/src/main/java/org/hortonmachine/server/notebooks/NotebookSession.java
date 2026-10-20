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

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hortonmachine.server.notebooks.NotebookDocument.Cell;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * An open notebook: its document, with the latest outputs of the cells, and its kernel, started at the
 * first run of a cell.
 *
 * <p>The pages that show the notebook listen to its events, as JSON:
 * <ul>
 * <li><code>{"type":"status","state":"off|starting|idle|busy"}</code>;</li>
 * <li><code>{"type":"queued","cell":..}</code>, <code>{"type":"start","cell":..,"count":..}</code>,
 *     <code>{"type":"output","cell":..,"output":{..}}</code> and
 *     <code>{"type":"done","cell":..,"status":"ok|error|interrupted"}</code> for the runs of the cells.</li>
 * </ul>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NotebookSession {
    private static final Logger LOGGER = Logger.getLogger(NotebookSession.class.getName());

    public static final String OFF = "off";
    public static final String STARTING = "starting";
    public static final String IDLE = "idle";
    public static final String BUSY = "busy";

    private final String name;
    private final File file;
    private final File workFolder;
    private final Supplier<List<String>> javaCommand;
    private final NotebookDocument document;
    private final List<Consumer<JsonNode>> listeners = new CopyOnWriteArrayList<>();
    /** The cells of the runs sent to the kernel and not done yet, by id of the run. */
    private final Map<String, String> pending = new LinkedHashMap<>();

    private KernelProcess kernel;
    private String state = OFF;
    private volatile long lastActivity = System.currentTimeMillis();

    /**
     * @param name the name of the notebook.
     * @param file the notebook file.
     * @param workFolder the working folder of the kernel.
     * @param javaCommand gives the start of the java command of the kernel.
     */
    NotebookSession( String name, File file, File workFolder, Supplier<List<String>> javaCommand ) throws Exception {
        this.name = name;
        this.file = file;
        this.workFolder = workFolder;
        this.javaCommand = javaCommand;
        this.document = NotebookDocument.read(file);
    }

    public String getName() {
        return name;
    }

    public synchronized String getState() {
        return state;
    }

    long getLastActivity() {
        return lastActivity;
    }

    boolean hasListeners() {
        return !listeners.isEmpty();
    }

    /**
     * Add a listener of the events; it gets the state at once.
     */
    public synchronized void addListener( Consumer<JsonNode> listener ) {
        listeners.add(listener);
        listener.accept(statusEvent());
    }

    public void removeListener( Consumer<JsonNode> listener ) {
        listeners.remove(listener);
    }

    /**
     * @return the notebook as the page needs it: the name, the state of the kernel and the cells with their outputs.
     */
    public synchronized ObjectNode snapshot() {
        ObjectNode node = NotebookDocument.MAPPER.createObjectNode();
        node.put("name", name);
        node.put("state", state);
        ArrayNode cells = node.putArray("cells");
        for( Cell cell : document.getCells() ) {
            ObjectNode cellNode = cells.addObject();
            cellNode.put("id", cell.id);
            cellNode.put("type", cell.type);
            cellNode.put("source", cell.source);
            if (cell.executionCount != null) {
                cellNode.put("executionCount", cell.executionCount);
            }
            ArrayNode outputs = cellNode.putArray("outputs");
            cell.outputs.forEach(outputs::add);
        }
        return node;
    }

    /**
     * @return the content of the saved notebook file.
     */
    public synchronized byte[] savedBytes() throws Exception {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Save the cells sent by the page, with the outputs of their last runs.
     */
    public synchronized void save( List<Cell> cells ) throws Exception {
        document.setCells(cells);
        document.write(file);
    }

    /**
     * Run a code cell, starting the kernel if needed. The cell gets the code and loses its outputs.
     *
     * @param cellId the id of the cell, a new code cell at the end if not yet in the notebook.
     * @param code the code to run.
     */
    public synchronized void run( String cellId, String code ) throws Exception {
        lastActivity = System.currentTimeMillis();
        Cell cell = document.getCell(cellId);
        if (cell == null) {
            cell = new Cell(cellId, NotebookDocument.CODE, code);
            document.getCells().add(cell);
        }
        if (!cell.isCode()) {
            throw new IllegalArgumentException("Only code cells can run.");
        }
        cell.source = code;
        cell.outputs.clear();
        cell.executionCount = null;
        if (kernel == null) {
            startKernel();
        }
        String runId = UUID.randomUUID().toString();
        pending.put(runId, cellId);
        broadcast(event("queued", cellId));
        kernel.execute(runId, code);
    }

    /**
     * Stop the running cell and drop the queued ones.
     */
    public synchronized void interrupt() {
        if (kernel != null) {
            kernel.interrupt();
        }
    }

    /**
     * Kill the kernel and start a new one: the variables are lost, the outputs stay.
     */
    public synchronized void restart() throws Exception {
        stopKernel(true);
        startKernel();
    }

    /**
     * Stop the kernel, if running.
     */
    public synchronized void shutdownKernel() {
        stopKernel(false);
    }

    private void stopKernel( boolean kill ) {
        if (kernel == null) {
            return;
        }
        KernelProcess old = kernel;
        kernel = null;
        dropPending("interrupted");
        if (kill) {
            old.kill();
        } else {
            old.shutdown();
        }
        setState(OFF);
    }

    private void startKernel() throws Exception {
        setState(STARTING);
        KernelProcess[] started = new KernelProcess[1];
        started[0] = new KernelProcess(javaCommand.get(), workFolder, message -> onKernelMessage(started[0], message));
        kernel = started[0];
    }

    private synchronized void onKernelMessage( KernelProcess from, JsonNode message ) {
        if (from != kernel) {
            // a killed kernel
            return;
        }
        lastActivity = System.currentTimeMillis();
        String type = message.path("type").asText();
        String cellId = pending.get(message.path("id").asText());
        Cell cell = cellId != null ? document.getCell(cellId) : null;
        switch( type ) {
        case "ready":
            setState(pending.isEmpty() ? IDLE : BUSY);
            break;
        case "start":
            if (cell != null) {
                cell.executionCount = message.path("count").asInt();
                ObjectNode event = event("start", cellId);
                event.put("count", cell.executionCount);
                broadcast(event);
            }
            setState(BUSY);
            break;
        case "output":
            if (cell != null) {
                JsonNode output = message.path("output");
                cell.addOutput(output);
                ObjectNode event = event("output", cellId);
                event.set("output", output);
                broadcast(event);
            }
            break;
        case "done":
            pending.remove(message.path("id").asText());
            if (cellId != null) {
                ObjectNode event = event("done", cellId);
                event.put("status", message.path("status").asText());
                broadcast(event);
            }
            if (pending.isEmpty()) {
                setState(IDLE);
            }
            break;
        case "exit":
            kernel = null;
            dropPending("error");
            setState(OFF);
            LOGGER.info("The kernel of the notebook " + name + " stopped.");
            break;
        default:
            break;
        }
    }

    /**
     * Tell the pages that the pending runs are over.
     */
    private void dropPending( String status ) {
        for( String cellId : new ArrayList<>(pending.values()) ) {
            ObjectNode event = event("done", cellId);
            event.put("status", status);
            broadcast(event);
        }
        pending.clear();
    }

    private void setState( String newState ) {
        if (!newState.equals(state)) {
            state = newState;
            broadcast(statusEvent());
        }
    }

    private ObjectNode statusEvent() {
        ObjectNode event = NotebookDocument.MAPPER.createObjectNode();
        event.put("type", "status");
        event.put("state", state);
        return event;
    }

    private static ObjectNode event( String type, String cellId ) {
        ObjectNode event = NotebookDocument.MAPPER.createObjectNode();
        event.put("type", type);
        event.put("cell", cellId);
        return event;
    }

    private void broadcast( JsonNode event ) {
        for( Consumer<JsonNode> listener : listeners ) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "A page of the notebook can't get an event.", e);
                listeners.remove(listener);
            }
        }
    }
}
