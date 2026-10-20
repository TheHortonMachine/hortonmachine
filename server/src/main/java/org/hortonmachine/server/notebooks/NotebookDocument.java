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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hortonmachine.gears.utils.files.FileUtilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A notebook in the nbformat 4 format of Jupyter (<code>.ipynb</code>), with Groovy code cells.
 *
 * <p>The parts of the file that are not cells, as its metadata, are kept as they are.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NotebookDocument {

    public static final String CODE = "code";
    public static final String MARKDOWN = "markdown";

    static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A cell of the notebook.
     */
    public static class Cell {
        public final String id;
        public final String type;
        public String source;
        public Integer executionCount;
        /** The outputs of a code cell, in the nbformat form. */
        public final List<JsonNode> outputs = new ArrayList<>();

        public Cell( String id, String type, String source ) {
            this.id = id;
            this.type = type;
            this.source = source;
        }

        public boolean isCode() {
            return CODE.equals(type);
        }

        /**
         * Add an output, joining the text of consecutive streams of the same name, as Jupyter does.
         */
        public void addOutput( JsonNode output ) {
            if ("stream".equals(output.path("output_type").asText()) && !outputs.isEmpty()) {
                JsonNode last = outputs.get(outputs.size() - 1);
                if ("stream".equals(last.path("output_type").asText())
                        && last.path("name").asText().equals(output.path("name").asText())) {
                    ((ObjectNode) last).put("text", last.path("text").asText() + output.path("text").asText());
                    return;
                }
            }
            outputs.add(output.deepCopy());
        }
    }

    private final ObjectNode root;
    private final List<Cell> cells = new ArrayList<>();

    private NotebookDocument( ObjectNode root ) {
        this.root = root;
    }

    /**
     * @return a new notebook with an empty code cell.
     */
    public static NotebookDocument create() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("nbformat", 4);
        root.put("nbformat_minor", 5);
        ObjectNode metadata = root.putObject("metadata");
        ObjectNode kernelspec = metadata.putObject("kernelspec");
        kernelspec.put("name", "hortonmachine-groovy");
        kernelspec.put("display_name", "HortonMachine Groovy");
        kernelspec.put("language", "groovy");
        ObjectNode languageInfo = metadata.putObject("language_info");
        languageInfo.put("name", "groovy");
        languageInfo.put("file_extension", ".groovy");
        NotebookDocument document = new NotebookDocument(root);
        document.cells.add(new Cell(newId(), CODE, ""));
        return document;
    }

    /**
     * Read a notebook file. Cells without id get one, cells of other types than code and markdown
     * become markdown cells.
     */
    public static NotebookDocument read( File file ) throws Exception {
        JsonNode tree = MAPPER.readTree(FileUtilities.readFile(file));
        if (!(tree instanceof ObjectNode) || tree.path("nbformat").asInt() != 4) {
            throw new IllegalArgumentException("Not a notebook in the nbformat 4 format: " + file.getName());
        }
        NotebookDocument document = new NotebookDocument((ObjectNode) tree);
        for( JsonNode node : tree.path("cells") ) {
            String id = node.path("id").asText("");
            if (id.isEmpty() || document.getCell(id) != null) {
                id = newId();
            }
            String type = CODE.equals(node.path("cell_type").asText()) ? CODE : MARKDOWN;
            Cell cell = new Cell(id, type, joinText(node.path("source")));
            if (cell.isCode()) {
                JsonNode count = node.path("execution_count");
                cell.executionCount = count.isInt() ? count.asInt() : null;
                for( JsonNode output : node.path("outputs") ) {
                    cell.outputs.add(output);
                }
            }
            document.cells.add(cell);
        }
        return document;
    }

    /**
     * Write the notebook, the sources as lists of lines, as Jupyter does.
     */
    public void write( File file ) throws Exception {
        ArrayNode cellsNode = MAPPER.createArrayNode();
        for( Cell cell : cells ) {
            ObjectNode node = cellsNode.addObject();
            node.put("id", cell.id);
            node.put("cell_type", cell.type);
            node.putObject("metadata");
            node.set("source", splitLines(cell.source));
            if (cell.isCode()) {
                if (cell.executionCount != null) {
                    node.put("execution_count", cell.executionCount);
                } else {
                    node.putNull("execution_count");
                }
                ArrayNode outputs = node.putArray("outputs");
                for( JsonNode output : cell.outputs ) {
                    outputs.add(output);
                }
            }
        }
        root.set("cells", cellsNode);
        root.put("nbformat", 4);
        if (root.path("nbformat_minor").asInt() < 5) {
            // the version with the ids of the cells
            root.put("nbformat_minor", 5);
        }
        String json = MAPPER.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(root);
        FileUtilities.writeFile(json + "\n", file);
    }

    public List<Cell> getCells() {
        return cells;
    }

    /**
     * @return the cell or <code>null</code>.
     */
    public Cell getCell( String id ) {
        for( Cell cell : cells ) {
            if (cell.id.equals(id)) {
                return cell;
            }
        }
        return null;
    }

    /**
     * Replace the cells with new ones, as sent by the page: the code cells keep the outputs of the
     * cells with the same id.
     */
    public void setCells( List<Cell> newCells ) {
        for( Cell cell : newCells ) {
            Cell old = getCell(cell.id);
            if (old != null && cell.isCode() && old.isCode() && cell.outputs.isEmpty()) {
                cell.executionCount = old.executionCount;
                cell.outputs.addAll(old.outputs);
            }
        }
        cells.clear();
        cells.addAll(newCells);
    }

    /**
     * @return a new id of a cell.
     */
    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * @return the text of a multiline field of nbformat, a string or a list of strings.
     */
    static String joinText( JsonNode node ) {
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            for( JsonNode line : node ) {
                sb.append(line.asText());
            }
            return sb.toString();
        }
        return node.asText("");
    }

    private static ArrayNode splitLines( String text ) {
        ArrayNode lines = MAPPER.createArrayNode();
        if (text != null && !text.isEmpty()) {
            for( String line : text.split("(?<=\n)") ) {
                lines.add(line);
            }
        }
        return lines;
    }
}
