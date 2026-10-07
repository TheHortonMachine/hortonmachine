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
package org.hortonmachine.geoscript.console;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.codehaus.groovy.runtime.FormatHelper;
import org.hortonmachine.HM;
import org.hortonmachine.gears.utils.coverage.RasterCellInfo;
import org.hortonmachine.gui.editor.CodeEditorFactory;

/**
 * The output of the console: printed text, errors and the results of the scripts.
 *
 * <p>
 * Results are shown in the most useful form: maps and lists as tables, geometries, layers and
 * maps as images, everything else as text.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ConsoleOutputPane extends JTextPane {
    private static final long serialVersionUID = 1L;

    private static final int MAX_LENGTH = 2_000_000;
    private static final int IMAGE_SIZE = 600;

    public final SimpleAttributeSet normalStyle = new SimpleAttributeSet();
    public final SimpleAttributeSet errorStyle = new SimpleAttributeSet();
    public final SimpleAttributeSet promptStyle = new SimpleAttributeSet();
    public final SimpleAttributeSet resultStyle = new SimpleAttributeSet();

    private final List<Object[]> pending = new ArrayList<>();
    private boolean flushScheduled = false;

    public ConsoleOutputPane() {
        setEditable(false);
        // a plain color, since the look and feel greys out the ui resource ones of non editable panes
        Color background = UIManager.getColor("TextArea.background");
        if (background != null) {
            setBackground(new Color(background.getRGB()));
        }
        Font uiFont = UIManager.getFont("Label.font");
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, uiFont != null ? uiFont.getSize() + 1 : 13));
        boolean dark = CodeEditorFactory.isDarkLaf();
        StyleConstants.setForeground(errorStyle, dark ? new Color(0xf87171) : new Color(0xc0392b));
        StyleConstants.setForeground(promptStyle, dark ? new Color(0x4ade80) : new Color(0x1e8449));
        StyleConstants.setBold(promptStyle, true);
        StyleConstants.setForeground(resultStyle, dark ? new Color(0x60a5fa) : new Color(0x1f4e9c));
    }

    /**
     * Append text, from any thread. Text from many calls is added in a single gui update.
     *
     * @param text the text.
     * @param style the style.
     */
    public void append( String text, SimpleAttributeSet style ) {
        synchronized (pending) {
            pending.add(new Object[]{text, style});
            if (flushScheduled) {
                return;
            }
            flushScheduled = true;
        }
        SwingUtilities.invokeLater(this::flushPending);
    }

    /**
     * Append a line of text, from any thread.
     *
     * @param text the text.
     * @param style the style.
     */
    public void appendLine( String text, SimpleAttributeSet style ) {
        append(text + "\n", style);
    }

    private void flushPending() {
        List<Object[]> items;
        synchronized (pending) {
            items = new ArrayList<>(pending);
            pending.clear();
            flushScheduled = false;
        }
        StyledDocument doc = getStyledDocument();
        try {
            for( Object[] item : items ) {
                doc.insertString(doc.getLength(), (String) item[0], (SimpleAttributeSet) item[1]);
            }
            if (doc.getLength() > MAX_LENGTH) {
                doc.remove(0, doc.getLength() - MAX_LENGTH);
            }
        } catch (BadLocationException e) {
            // ignore
        }
        setCaretPosition(doc.getLength());
    }

    /**
     * Append the result of a script, in the gui thread.
     *
     * @param result the result.
     */
    public void appendResult( Object result ) {
        flushPending();
        Object rendered;
        try {
            rendered = render(result);
        } catch (Throwable e) {
            rendered = String.valueOf(result);
        }
        StyledDocument doc = getStyledDocument();
        try {
            boolean isGraphic = rendered instanceof Component || rendered instanceof Icon;
            doc.insertString(doc.getLength(), isGraphic ? "Result:\n" : "Result: ", promptStyle);
            if (rendered instanceof Component) {
                SimpleAttributeSet style = new SimpleAttributeSet();
                StyleConstants.setComponent(style, (Component) rendered);
                doc.insertString(doc.getLength(), " ", style);
                doc.insertString(doc.getLength(), "\n", normalStyle);
            } else if (rendered instanceof Icon) {
                SimpleAttributeSet style = new SimpleAttributeSet();
                StyleConstants.setIcon(style, (Icon) rendered);
                doc.insertString(doc.getLength(), " ", style);
                doc.insertString(doc.getLength(), "\n", normalStyle);
            } else {
                doc.insertString(doc.getLength(), rendered + "\n", resultStyle);
            }
        } catch (BadLocationException e) {
            // ignore
        }
        setCaretPosition(doc.getLength());
    }

    /**
     * Clear the output.
     */
    public void clear() {
        synchronized (pending) {
            pending.clear();
        }
        setText("");
    }

    /**
     * Create a stream that writes into this pane (and into a second stream, if given).
     *
     * <p>
     * Complete lines are checked against the filter before being shown; a partial line (ex. from a
     * <code>print</code>) is shown unfiltered when the stream is flushed. The second stream gets
     * everything.
     *
     * @param style the style of the text.
     * @param tee the stream to also write to, or <code>null</code>.
     * @param lineFilter returns <code>true</code> for the lines to show, or <code>null</code> to show all.
     * @return the stream.
     */
    public PrintStream createStream( SimpleAttributeSet style, OutputStream tee, Predicate<String> lineFilter ) {
        OutputStream out = new OutputStream(){
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            @Override
            public synchronized void write( int b ) {
                write(new byte[]{(byte) b}, 0, 1);
            }
            @Override
            public synchronized void write( byte[] b, int off, int len ) {
                buffer.write(b, off, len);
                if (tee != null) {
                    try {
                        tee.write(b, off, len);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                appendCompleteLines();
            }
            /** The newline byte never occurs inside a multibyte utf-8 character, so lines can be cut on bytes. */
            private void appendCompleteLines() {
                byte[] bytes = buffer.toByteArray();
                int lineStart = 0;
                StringBuilder sb = new StringBuilder();
                for( int i = 0; i < bytes.length; i++ ) {
                    if (bytes[i] == '\n') {
                        String line = new String(bytes, lineStart, i - lineStart + 1, StandardCharsets.UTF_8);
                        if (lineFilter == null || lineFilter.test(line.stripTrailing())) {
                            sb.append(line);
                        }
                        lineStart = i + 1;
                    }
                }
                if (lineStart > 0) {
                    buffer.reset();
                    buffer.write(bytes, lineStart, bytes.length - lineStart);
                }
                if (sb.length() > 0) {
                    append(sb.toString(), style);
                }
            }
            @Override
            public synchronized void flush() {
                appendCompleteLines();
                if (buffer.size() > 0) {
                    append(buffer.toString(StandardCharsets.UTF_8), style);
                    buffer.reset();
                }
                if (tee != null) {
                    try {
                        tee.flush();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        };
        return new PrintStream(out, true, StandardCharsets.UTF_8);
    }

    private Object render( Object result ) throws Exception {
        if (result instanceof Component || result instanceof Icon) {
            return result;
        }
        if (result instanceof Image) {
            return new ImageIcon((Image) result);
        }
        if (result instanceof geoscript.geom.Geometry) {
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("size", List.of(IMAGE_SIZE, IMAGE_SIZE));
            return new ImageIcon(HM.toImage(options, List.of((geoscript.geom.Geometry) result)));
        }
        if (result instanceof geoscript.layer.Layer) {
            return new ImageIcon(geoscript.render.Draw.drawToImage(imageOptions(), (geoscript.layer.Layer) result));
        }
        if (result instanceof geoscript.layer.Raster) {
            return new ImageIcon(geoscript.render.Draw.drawToImage(imageOptions(), (geoscript.layer.Raster) result));
        }
        if (result instanceof geoscript.render.Map) {
            return new ImageIcon(((geoscript.render.Map) result).renderToImage());
        }
        if (result instanceof geoscript.feature.Feature) {
            return mapTable(((geoscript.feature.Feature) result).getAttributes(), "Name");
        }
        if (result instanceof RasterCellInfo) {
            return result.toString();
        }
        if (result instanceof Map) {
            return mapTable((Map< ? , ? >) result, "Key");
        }
        if (result instanceof Collection && !((Collection< ? >) result).isEmpty()) {
            return collectionTable((Collection< ? >) result);
        }
        if (result instanceof double[][]) {
            double[][] matrix = (double[][]) result;
            List<List<Object>> rows = new ArrayList<>();
            for( double[] row : matrix ) {
                List<Object> values = new ArrayList<>();
                for( double v : row ) {
                    values.add(v);
                }
                rows.add(values);
            }
            return collectionTable(rows);
        }
        return FormatHelper.toString(result);
    }

    private static Map<String, Object> imageOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("size", List.of(IMAGE_SIZE, IMAGE_SIZE));
        options.put("backgroundColor", "white");
        return options;
    }

    private JComponent mapTable( Map< ? , ? > map, String keyHeader ) {
        Object[][] data = new Object[map.size()][];
        int i = 0;
        for( Map.Entry< ? , ? > entry : map.entrySet() ) {
            data[i++] = new Object[]{String.valueOf(entry.getKey()), FormatHelper.inspect(entry.getValue())};
        }
        return table(data, new Object[]{keyHeader, "Value"});
    }

    private JComponent collectionTable( Collection< ? > collection ) {
        int columns = 1;
        boolean nested = false;
        for( Object item : collection ) {
            if (item instanceof Collection) {
                nested = true;
                columns = Math.max(columns, ((Collection< ? >) item).size());
            }
        }
        Object[][] data = new Object[collection.size()][columns];
        int i = 0;
        for( Object item : collection ) {
            if (item instanceof Collection) {
                int j = 0;
                for( Object value : (Collection< ? >) item ) {
                    data[i][j++] = String.valueOf(value);
                }
            } else {
                data[i][0] = String.valueOf(item);
            }
            i++;
        }
        Object[] header = new Object[columns];
        for( int c = 0; c < columns; c++ ) {
            header[c] = nested ? "Value " + (c + 1) : "Value";
        }
        return table(data, header);
    }

    private JComponent table( Object[][] data, Object[] header ) {
        JTable table = new JTable(data, header);
        table.setDefaultEditor(Object.class, null);
        Dimension preferred = table.getPreferredSize();
        int width = Math.max(300, Math.min(preferred.width, 900));
        int height = Math.min(preferred.height, 300);
        table.setPreferredScrollableViewportSize(new Dimension(width, height));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setAlignmentY(0.8f);
        return scrollPane;
    }
}
