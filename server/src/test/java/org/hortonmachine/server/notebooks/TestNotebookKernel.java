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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

/**
 * Tests of the {@link NotebookKernel}, run in the test process.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestNotebookKernel extends TestCase {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LinkedBlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
    private PrintStream toKernel;
    private Thread kernelThread;
    private NotebookKernel kernel;
    private int nextId = 0;

    @Override
    protected void setUp() throws Exception {
        PrintStream protocol = new PrintStream(new OutputStream(){
            private final ByteArrayOutputStream line = new ByteArrayOutputStream();
            @Override
            public synchronized void write( int b ) {
                if (b == '\n') {
                    try {
                        messages.add(mapper.readTree(line.toString(StandardCharsets.UTF_8)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    line.reset();
                } else {
                    line.write(b);
                }
            }
        }, true, StandardCharsets.UTF_8);
        kernel = new NotebookKernel(protocol);
        PipedOutputStream pipe = new PipedOutputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(new PipedInputStream(pipe, 1 << 20), StandardCharsets.UTF_8));
        toKernel = new PrintStream(pipe, true, StandardCharsets.UTF_8);
        kernelThread = new Thread(() -> {
            try {
                kernel.run(input);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        kernelThread.start();
        assertEquals("ready", next().path("type").asText());
    }

    @Override
    protected void tearDown() throws Exception {
        toKernel.println("{\"op\":\"shutdown\"}");
        kernelThread.join(5000);
    }

    private JsonNode next() throws Exception {
        JsonNode message = messages.poll(30, TimeUnit.SECONDS);
        assertNotNull("no message from the kernel", message);
        return message;
    }

    private String execute( String code ) throws Exception {
        String id = "e" + (nextId++);
        toKernel.println(mapper.writeValueAsString(Map.of("op", "execute", "id", id, "code", code)));
        return id;
    }

    /**
     * @return the outputs of an execution, the last element is the status.
     */
    private List<JsonNode> run( String code ) throws Exception {
        String id = execute(code);
        return collect(id);
    }

    private List<JsonNode> collect( String id ) throws Exception {
        List<JsonNode> outputs = new ArrayList<>();
        while( true ) {
            JsonNode message = next();
            String type = message.path("type").asText();
            if (!id.equals(message.path("id").asText())) {
                continue;
            }
            if (type.equals("output")) {
                outputs.add(message.path("output"));
            } else if (type.equals("done")) {
                outputs.add(message.path("status"));
                return outputs;
            }
        }
    }

    private static String status( List<JsonNode> outputs ) {
        return outputs.get(outputs.size() - 1).asText();
    }

    private static String plainText( JsonNode output ) {
        return output.path("data").path("text/plain").asText();
    }

    public void testVariablesAndImportsAreKept() throws Exception {
        List<JsonNode> outputs = run("import java.util.concurrent.atomic.AtomicInteger\ndef counter = new AtomicInteger(40)\nint step = 2\nnull");
        assertEquals("ok", status(outputs));
        assertEquals(1, outputs.size());

        // the def and typed variables and the import are seen by the next cells
        outputs = run("counter.addAndGet(step)\nnew AtomicInteger(counter.get())");
        assertEquals("ok", status(outputs));
        assertEquals("execute_result", outputs.get(0).path("output_type").asText());
        assertEquals("42", plainText(outputs.get(0)));

        // closures too
        run("def twice = { it * 2 }");
        outputs = run("twice(21)");
        assertEquals("42", plainText(outputs.get(0)));
    }

    public void testDisplayAndTables() throws Exception {
        List<JsonNode> outputs = run("display('first')\n[a: 1, b: 2]");
        assertEquals("ok", status(outputs));
        assertEquals("display_data", outputs.get(0).path("output_type").asText());
        assertEquals("first", plainText(outputs.get(0)));

        JsonNode table = outputs.get(1).path("data").path(NotebookOutputs.TABLE_MIME);
        assertEquals("Key", table.path("header").get(0).asText());
        assertEquals("b", table.path("rows").get(1).get(0).asText());
        assertTrue(outputs.get(1).path("data").path("text/html").asText().startsWith("<table>"));
    }

    public void testImages() throws Exception {
        List<JsonNode> outputs = run("new org.locationtech.jts.io.WKTReader().read('POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0))')");
        assertEquals("ok", status(outputs));
        // the wkt and the image
        assertEquals(2, outputs.size() - 1);
        String png = outputs.get(1).path("data").path("image/png").asText();
        assertTrue(png.startsWith("iVBORw0KGgo"));
    }

    public void testErrors() throws Exception {
        List<JsonNode> outputs = run("def x = 1\nthrow new IllegalStateException('broken')");
        assertEquals("error", status(outputs));
        JsonNode error = outputs.get(0);
        assertEquals("error", error.path("output_type").asText());
        assertEquals("java.lang.IllegalStateException", error.path("ename").asText());
        assertEquals("broken", error.path("evalue").asText());
        assertTrue(error.path("traceback").toString(), error.path("traceback").toString().contains("at line 2 of the cell"));

        outputs = run("def y = ");
        assertEquals("error", status(outputs));
        assertEquals("CompilationError", outputs.get(0).path("ename").asText());

        // the kernel goes on
        assertEquals("ok", status(run("1")));
    }

    public void testPrintedOutput() throws Exception {
        PrintStream original = System.out;
        System.setOut(kernel.cellStream("stdout"));
        try {
            List<JsonNode> outputs = run("println 'hello'\nprintln 'Native library load failed.'\nprint 'no newline'\nnull");
            assertEquals("ok", status(outputs));
            StringBuilder text = new StringBuilder();
            for( JsonNode output : outputs.subList(0, outputs.size() - 1) ) {
                assertEquals("stream", output.path("output_type").asText());
                text.append(output.path("text").asText());
            }
            // the gdal noise is hidden
            assertEquals("hello\nno newline", text.toString());
        } finally {
            System.setOut(original);
        }
    }

    public void testInterrupt() throws Exception {
        String running = execute("while( true ) { }");
        String queued = execute("42");
        // let the loop start
        JsonNode message;
        do {
            message = next();
        } while( !message.path("type").asText().equals("start") );
        toKernel.println("{\"op\":\"interrupt\"}");

        List<JsonNode> outputs = collect(queued);
        assertEquals("interrupted", status(outputs));
        outputs = collect(running);
        assertEquals("interrupted", status(outputs));

        // the variables are still there and the kernel works
        run("def kept = 7");
        assertEquals("7", plainText(run("kept").get(0)));
    }
}
