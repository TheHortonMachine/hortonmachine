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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.hortonmachine.gears.utils.display.DisplayImage;
import org.hortonmachine.gears.utils.display.DisplayPart;
import org.hortonmachine.gears.utils.display.DisplayTable;
import org.hortonmachine.gears.utils.display.DisplayText;

/**
 * Converts the {@link DisplayPart display parts} into outputs of the nbformat 4 notebooks, so that
 * the saved notebooks show their results also in Jupyter, VS Code and GitHub.
 *
 * <p>Tables carry their data also as {@value #TABLE_MIME}, which the notebook page shows; the
 * <code>text/html</code> version is only for the other programs, the page never shows the html
 * of a notebook file.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NotebookOutputs {

    /** The media type of the tables, as <code>{"header":[..],"rows":[[..]],"totalRows":n}</code>. */
    public static final String TABLE_MIME = "application/vnd.hortonmachine.table+json";

    private NotebookOutputs() {
    }

    /**
     * Convert rendered parts into outputs.
     *
     * @param parts the parts.
     * @param executionCount the number of the execution, if the parts are the value of the cell,
     *              or <code>null</code> for displayed values.
     * @return the outputs, one per part.
     */
    public static List<Map<String, Object>> fromParts( List<DisplayPart> parts, Integer executionCount ) {
        List<Map<String, Object>> outputs = new ArrayList<>();
        boolean isResult = executionCount != null && parts.size() == 1;
        for( DisplayPart part : parts ) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("output_type", isResult ? "execute_result" : "display_data");
            if (isResult) {
                output.put("execution_count", executionCount);
            }
            output.put("data", data(part));
            output.put("metadata", new LinkedHashMap<>());
            outputs.add(output);
        }
        return outputs;
    }

    private static Map<String, Object> data( DisplayPart part ) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (part instanceof DisplayText) {
            data.put("text/plain", ((DisplayText) part).text());
        } else if (part instanceof DisplayImage) {
            DisplayImage image = (DisplayImage) part;
            data.put("image/png", toPngBase64(image));
            data.put("text/plain", "<image " + image.image().getWidth() + "x" + image.image().getHeight() + ">");
        } else if (part instanceof DisplayTable) {
            DisplayTable table = (DisplayTable) part;
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("header", table.header());
            json.put("rows", table.rows());
            json.put("totalRows", table.totalRows());
            data.put(TABLE_MIME, json);
            data.put("text/html", toHtml(table));
            data.put("text/plain", toText(table));
        }
        return data;
    }

    private static String toPngBase64( DisplayImage image ) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image.image(), "png", bytes);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String toHtml( DisplayTable table ) {
        StringBuilder sb = new StringBuilder("<table><thead><tr>");
        for( String name : table.header() ) {
            sb.append("<th>").append(escape(name)).append("</th>");
        }
        sb.append("</tr></thead><tbody>");
        for( List<String> row : table.rows() ) {
            sb.append("<tr>");
            for( String value : row ) {
                sb.append("<td>").append(escape(value)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        if (table.isTruncated()) {
            sb.append("<p>Showing ").append(table.rows().size()).append(" of ").append(table.totalRows()).append(" rows.</p>");
        }
        return sb.toString();
    }

    private static String toText( DisplayTable table ) {
        StringBuilder sb = new StringBuilder(String.join("\t", table.header()));
        for( List<String> row : table.rows() ) {
            sb.append("\n").append(String.join("\t", row));
        }
        if (table.isTruncated()) {
            sb.append("\n(").append(table.rows().size()).append(" of ").append(table.totalRows()).append(" rows)");
        }
        return sb.toString();
    }

    static String escape( String text ) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for( char c : text.toCharArray() ) {
            switch( c ) {
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '&':
                sb.append("&amp;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            case '\'':
                sb.append("&#39;");
                break;
            default:
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
