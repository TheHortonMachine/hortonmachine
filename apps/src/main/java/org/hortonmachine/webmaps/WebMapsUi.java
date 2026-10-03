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
package org.hortonmachine.webmaps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.hortonmachine.gui.utils.GuiUtilities;
import org.locationtech.jts.geom.Envelope;

/**
 * Small swing helpers shared by the web services browser panels.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
class WebMapsUi {
    static final String HTML_START = "<html><body style='font-family:sans-serif; padding:6px'>";
    static final String HTML_END = "</body></html>";
    static final String OK_COLOR = "#1a8a3a";
    static final String KO_COLOR = "#c0392b";

    private WebMapsUi() {
    }

    static String escape( String s ) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String nn( String s ) {
        return s == null ? "" : s;
    }

    static void row( StringBuilder sb, String key, String value ) {
        sb.append("<tr><td valign='top'><b>").append(escape(key)).append("</b></td><td>").append(value).append("</td></tr>");
    }

    static String colored( String color, String text ) {
        return "<font color='" + color + "'>" + escape(text) + "</font>";
    }

    static String bboxString( Envelope env ) {
        if (env == null || env.isNull())
            return "-";
        return String.format(Locale.ROOT, "W %.5f, S %.5f, E %.5f, N %.5f", env.getMinX(), env.getMinY(), env.getMaxX(),
                env.getMaxY());
    }

    /**
     * @return the envelope by axis, for envelopes in a CRS whose first axis might not be the easting.
     */
    static String axesString( Envelope env ) {
        if (env == null || env.isNull())
            return "-";
        return String.format(Locale.ROOT, "axis 1: %.5f .. %.5f, axis 2: %.5f .. %.5f", env.getMinX(), env.getMaxX(), env.getMinY(),
                env.getMaxY());
    }

    static String humanSize( long bytes ) {
        if (bytes < 1024)
            return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        double v = bytes;
        int u = -1;
        while( v >= 1024 && u < units.length - 1 ) {
            v /= 1024;
            u++;
        }
        return String.format("%.1f %s", v, units[u]);
    }

    static JEditorPane htmlPane() {
        JEditorPane pane = new JEditorPane("text/html", "");
        pane.setEditable(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        return pane;
    }

    static JTable table( TableModel model ) {
        JTable table = new JTable(model){
            @Override
            public String getToolTipText( MouseEvent e ) {
                // show the full content of truncated cells
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row < 0 || col < 0)
                    return null;
                Object value = getValueAt(row, col);
                return value == null || value instanceof Boolean ? null : value.toString();
            }
        };
        table.setRowSorter(new TableRowSorter<>(model));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        return table;
    }

    static JButton button( String text, Runnable action ) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    static JPanel buttons( JButton... buttons ) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for( JButton b : buttons ) {
            p.add(b);
        }
        return p;
    }

    static JPanel section( String title ) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    static void addRow( JPanel panel, int row, Component... components ) {
        for( int i = 0; i < components.length; i++ ) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.gridy = row;
            c.insets = new Insets(2, 3, 2, 3);
            c.anchor = GridBagConstraints.WEST;
            boolean isField = components[i] instanceof JTextField || components[i] instanceof JSpinner
                    || components[i] instanceof JComboBox;
            c.fill = isField || components.length == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
            c.weightx = isField ? 1 : 0;
            if (components.length == 1)
                c.gridwidth = 4;
            else if (i == components.length - 1 && components.length == 2)
                c.gridwidth = 3; // single label + field rows span the full width
            panel.add(components[i], c);
        }
    }

    /** The width notes wrap at, fitting the minimum width of the request panel. */
    private static final int NOTE_WIDTH = 250;

    /**
     * @return an italic note wrapped at a fixed width (html labels don't re-wrap to the available width in a
     *          GridBagLayout, and text areas made the request panel scroll by itself).
     */
    static JLabel note( String text ) {
        return new JLabel("<html><div style='width:" + NOTE_WIDTH + "px'><i>" + escape(text) + "</i></div></html>");
    }

    static void onTextChange( JTextField field, Runnable action ) {
        field.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate( DocumentEvent e ) {
                action.run();
            }
            public void removeUpdate( DocumentEvent e ) {
                action.run();
            }
            public void changedUpdate( DocumentEvent e ) {
                action.run();
            }
        });
    }

    /**
     * @return a panel with a read only url field and a button to copy it.
     */
    static JPanel urlPanel( JTextField urlField ) {
        urlField.setEditable(false);
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        panel.add(new JLabel("Request:"), BorderLayout.WEST);
        panel.add(urlField, BorderLayout.CENTER);
        panel.add(button("Copy", () -> GuiUtilities.copyToClipboard(urlField.getText())), BorderLayout.EAST);
        return panel;
    }

    /** A panel that, inside a scrollpane, adapts to the viewport width instead of scrolling horizontally. */
    @SuppressWarnings("serial")
    static class WidthTrackingPanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction ) {
            return 16;
        }
        public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
            return visibleRect.height;
        }
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Shows an image scaled to fit on a checkerboard (to see transparency), or a message. */
    @SuppressWarnings("serial")
    static class ImagePanel extends JPanel {
        private BufferedImage image;
        private String text;

        ImagePanel( String message ) {
            this.text = message;
            setPreferredSize(new Dimension(300, 200));
        }

        void setImage( BufferedImage image, String caption ) {
            this.image = image;
            this.text = caption + " (" + image.getWidth() + "x" + image.getHeight() + ")";
            repaint();
        }

        void setMessage( String message ) {
            this.image = null;
            this.text = message;
            repaint();
        }

        BufferedImage getImage() {
            return image;
        }

        @Override
        protected void paintComponent( Graphics g ) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int textHeight = g2.getFontMetrics().getHeight();
            if (image != null) {
                int w = getWidth() - 8;
                int h = getHeight() - textHeight - 12;
                double scale = Math.min(1.0 * w / image.getWidth(), 1.0 * h / image.getHeight());
                int iw = Math.max(1, (int) (image.getWidth() * scale));
                int ih = Math.max(1, (int) (image.getHeight() * scale));
                int x = (getWidth() - iw) / 2;
                int y = 4;
                int cell = 8;
                for( int cy = 0; cy < ih; cy += cell ) {
                    for( int cx = 0; cx < iw; cx += cell ) {
                        g2.setColor(((cx / cell + cy / cell) % 2 == 0) ? Color.WHITE : new Color(220, 220, 220));
                        g2.fillRect(x + cx, y + cy, Math.min(cell, iw - cx), Math.min(cell, ih - cy));
                    }
                }
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(image, x, y, iw, ih, null);
            }
            g2.setColor(Color.GRAY);
            if (text != null)
                g2.drawString(text, 6, getHeight() - 6);
        }
    }
}
