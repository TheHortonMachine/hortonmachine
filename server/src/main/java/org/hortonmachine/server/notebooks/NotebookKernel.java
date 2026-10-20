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
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.display.DisplayOptions;
import org.hortonmachine.gears.utils.display.DisplayRenderers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.transform.ThreadInterrupt;

/**
 * The kernel of a notebook: a separate java process that runs the code cells in Groovy, one at a time,
 * keeping the variables between them.
 *
 * <p>It talks with the server through its standard input and output, one JSON message per line:
 * <ul>
 * <li>in: <code>{"op":"execute","id":..,"code":..}</code>, <code>{"op":"interrupt"}</code>,
 *     <code>{"op":"shutdown"}</code>;</li>
 * <li>out: <code>{"type":"ready"}</code>, <code>{"type":"start","id":..,"count":..}</code>,
 *     <code>{"type":"output","id":..,"output":{..}}</code> with the outputs in the nbformat 4 form, and
 *     <code>{"type":"done","id":..,"status":"ok|error|interrupted"}</code>.</li>
 * </ul>
 *
 * <p>What the cells print is sent as stream outputs, the value of the last expression and the values
 * given to <code>display(value)</code> as rendered by the {@link DisplayRenderers}. The variables declared
 * with <code>def</code> at the top level of a cell and the imports are kept for the next cells.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NotebookKernel {

    /** The sizes of the rendered values. */
    public static final DisplayOptions DISPLAY_OPTIONS = new DisplayOptions(600, 600, 100);

    /**
     * Output lines that are not shown, since they would only hide the output of the cells, as in the
     * scripting console. Each pattern must match the whole line, without the line ending.
     */
    private static final List<Pattern> HIDDEN_OUTPUT_LINES = List.of(//
            Pattern.compile("Native library load failed\\."), //
            Pattern.compile(".*UnsatisfiedLinkError.*(gdal|kdu_jni).*"), //
            Pattern.compile(".*(GDALUtilities loadGDAL|KakaduUtilities loadKakadu)"), //
            Pattern.compile(".*Failed to load the (GDAL|Kakadu) native libs.*"), //
            // printed by JAI/ImageN when two jars register the same operations
            Pattern.compile("Error while parsing JAI registry file .*"), //
            Pattern.compile("Error in registry file at line number #\\d+"), //
            Pattern.compile("A descriptor is already registered against the name .* under registry mode .*") //
    );

    private static final String CELL_CLASS_PREFIX = "Cell";

    private final ObjectMapper mapper = new ObjectMapper();
    private final PrintStream protocol;
    private final Binding binding = new Binding();
    private final ImportCustomizer imports = new ImportCustomizer();
    private final GroovyShell shell;
    private final LinkedBlockingQueue<JsonNode> queue = new LinkedBlockingQueue<>();

    private volatile String currentId;
    private volatile Thread executor;
    private int executionCount = 0;

    NotebookKernel( PrintStream protocol ) {
        this.protocol = protocol;
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class), imports,
                new CellCustomizer(imports));
        shell = new GroovyShell(NotebookKernel.class.getClassLoader(), binding, config);
        binding.setVariable("display", new Closure<Object>(this){
            private static final long serialVersionUID = 1L;
            @SuppressWarnings("unused")
            public Object doCall( Object value ) {
                sendValue(value, false);
                return null;
            }
        });
    }

    public static void main( String[] args ) throws Exception {
        // the protocol uses the real output, what the cells print goes into stream messages
        PrintStream protocol = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        NotebookKernel kernel = new NotebookKernel(protocol);
        System.setOut(kernel.cellStream("stdout"));
        System.setErr(kernel.cellStream("stderr"));
        kernel.warmUp();
        kernel.run(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
        System.exit(0);
    }

    /**
     * Do once, before the first cell, what the libraries print about at their first use, as the
     * registries of JAI: out of a cell, what is printed is dropped. The first cell is faster too.
     */
    void warmUp() {
        try {
            shell.evaluate("1", "WarmUp");
            RegionMap region = RegionMap.fromBoundsAndGrid(0, 2, 0, 2, 2, 2);
            GridCoverage2D coverage = CoverageUtilities.buildCoverage("warmup", new double[][]{{1, 2}, {3, 4}}, region, null,
                    true);
            DisplayRenderers.render(coverage, new DisplayOptions(10, 10, 1));
        } catch (Throwable e) {
            // only a speed up
        }
        System.out.flush();
        System.err.flush();
    }

    /**
     * Read the messages until the shutdown or the end of the input, running the cells in another thread.
     */
    void run( BufferedReader input ) throws Exception {
        executor = new Thread(this::executeLoop, "notebook-executor");
        executor.setDaemon(true);
        executor.start();
        send(Map.of("type", "ready"));
        String line;
        while( (line = input.readLine()) != null ) {
            JsonNode message;
            try {
                message = mapper.readTree(line);
            } catch (Exception e) {
                continue;
            }
            String op = message.path("op").asText();
            if (op.equals("execute")) {
                queue.add(message);
            } else if (op.equals("interrupt")) {
                interrupt();
            } else if (op.equals("shutdown")) {
                break;
            }
        }
    }

    /**
     * Stop the running cell and drop the queued ones.
     */
    private void interrupt() {
        List<JsonNode> dropped = new ArrayList<>();
        queue.drainTo(dropped);
        for( JsonNode message : dropped ) {
            sendDone(message.path("id").asText(), "interrupted");
        }
        if (currentId != null) {
            executor.interrupt();
        }
    }

    private void executeLoop() {
        while( true ) {
            JsonNode message;
            try {
                message = queue.take();
            } catch (InterruptedException e) {
                // an interrupt that came after the end of the cell
                continue;
            }
            execute(message.path("id").asText(), message.path("code").asText());
            // clear an interrupt that came too late for the cell
            Thread.interrupted();
        }
    }

    private void execute( String id, String code ) {
        executionCount++;
        currentId = id;
        String scriptName = CELL_CLASS_PREFIX + executionCount;
        send(Map.of("type", "start", "id", id, "count", executionCount));
        String status = "ok";
        try {
            Object result = shell.evaluate(code, scriptName);
            System.out.flush();
            System.err.flush();
            if (result != null) {
                sendValue(result, true);
            }
        } catch (CompilationFailedException e) {
            status = "error";
            sendError("CompilationError", e.getMessage().replace(scriptName + ": ", "").trim(), List.of());
        } catch (Throwable e) {
            System.out.flush();
            System.err.flush();
            if (isInterruption(e)) {
                status = "interrupted";
                sendError("Interrupted", "The cell was interrupted.", List.of());
            } else {
                status = "error";
                sendError(e.getClass().getName(), String.valueOf(e.getMessage()), traceback(e, scriptName));
            }
        } finally {
            currentId = null;
        }
        sendDone(id, status);
    }

    private static boolean isInterruption( Throwable e ) {
        for( Throwable current = e; current != null; current = current.getCause() ) {
            if (current instanceof InterruptedException) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the lines of the error: the exceptions and their causes, with the lines of the cell.
     */
    private static List<String> traceback( Throwable e, String scriptName ) {
        List<String> lines = new ArrayList<>();
        int depth = 0;
        for( Throwable current = e; current != null && depth < 10; current = current.getCause(), depth++ ) {
            lines.add((depth == 0 ? "" : "Caused by: ") + current);
            for( StackTraceElement element : current.getStackTrace() ) {
                if (scriptName.equals(element.getFileName()) || element.getClassName().startsWith(scriptName)) {
                    lines.add("    at line " + element.getLineNumber() + " of the cell");
                }
            }
        }
        return lines;
    }

    /**
     * Send a value rendered by the {@link DisplayRenderers}.
     *
     * @param isResult <code>true</code> for the value of the cell, <code>false</code> for <code>display()</code>.
     */
    private void sendValue( Object value, boolean isResult ) {
        for( Map<String, Object> output : NotebookOutputs.fromParts(DisplayRenderers.render(value, DISPLAY_OPTIONS),
                isResult ? executionCount : null) ) {
            sendOutput(output);
        }
    }

    private void sendError( String name, String value, List<String> traceback ) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("output_type", "error");
        output.put("ename", name);
        output.put("evalue", value);
        output.put("traceback", traceback);
        sendOutput(output);
    }

    private void sendOutput( Map<String, Object> output ) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "output");
        message.put("id", currentId);
        message.put("output", output);
        send(message);
    }

    private void sendDone( String id, String status ) {
        send(Map.of("type", "done", "id", id, "status", status));
    }

    private void send( Map<String, ? > message ) {
        try {
            String json = mapper.writeValueAsString(message);
            synchronized (protocol) {
                protocol.println(json);
            }
        } catch (Exception e) {
            // nothing to tell it to
        }
    }

    /**
     * @return a stream that sends the complete lines printed by the cells as stream outputs.
     */
    PrintStream cellStream( String name ) {
        OutputStream out = new OutputStream(){
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            @Override
            public synchronized void write( int b ) {
                buffer.write(b);
                if (b == '\n') {
                    sendLines(false);
                }
            }
            @Override
            public synchronized void write( byte[] b, int off, int len ) {
                buffer.write(b, off, len);
                for( int i = off; i < off + len; i++ ) {
                    if (b[i] == '\n') {
                        sendLines(false);
                        break;
                    }
                }
            }
            @Override
            public synchronized void flush() {
                sendLines(true);
            }
            /** The newline byte never occurs inside a multibyte utf-8 character, so lines can be cut on bytes. */
            private void sendLines( boolean all ) {
                byte[] bytes = buffer.toByteArray();
                int end = bytes.length;
                if (!all) {
                    while( end > 0 && bytes[end - 1] != '\n' ) {
                        end--;
                    }
                }
                if (end == 0) {
                    return;
                }
                String text = new String(bytes, 0, end, StandardCharsets.UTF_8);
                buffer.reset();
                buffer.write(bytes, end, bytes.length - end);
                StringBuilder shown = new StringBuilder();
                for( String line : text.split("(?<=\n)") ) {
                    if (!isHidden(line.stripTrailing())) {
                        shown.append(line);
                    }
                }
                if (shown.length() > 0 && currentId != null) {
                    Map<String, Object> output = new LinkedHashMap<>();
                    output.put("output_type", "stream");
                    output.put("name", name);
                    output.put("text", shown.toString());
                    sendOutput(output);
                }
            }
        };
        return new PrintStream(out, true, StandardCharsets.UTF_8);
    }

    private static boolean isHidden( String line ) {
        for( Pattern pattern : HIDDEN_OUTPUT_LINES ) {
            if (pattern.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }
}
