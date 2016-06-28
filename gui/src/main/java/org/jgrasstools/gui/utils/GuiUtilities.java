/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gui.utils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Utilities class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GuiUtilities {

    public static final String LAST_PATH = "KEY_LAST_PATH";

    /**
     * Set the location of a component to center it on the screen.
     * 
     * @param component the component to center.
     */
    public static void centerOnScreen( Component component ) {
        Dimension prefSize = component.getPreferredSize();
        Dimension parentSize;
        java.awt.Point parentLocation = new java.awt.Point(0, 0);
        parentSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = parentLocation.x + (parentSize.width - prefSize.width) / 2;
        int y = parentLocation.y + (parentSize.height - prefSize.height) / 2;
        component.setLocation(x, y);
    }

    /**
     * Handle the last set path preference.
     * 
     * @return the last set path or the user home.
     */
    public static File getLastFile() {
        Preferences preferences = Preferences.userRoot().node(GuiBridgeHandler.PREFS_NODE_NAME);

        String userHome = System.getProperty("user.home");
        String lastPath = preferences.get(LAST_PATH, userHome);
        File file = new File(lastPath);
        if (!file.exists()) {
            return new File(userHome);
        }
        return file;
    }

    /**
     * Save the passed path as last path available.
     * 
     * @param lastPath the last path to save.
     */
    public static void setLastPath( String lastPath ) {
        File file = new File(lastPath);
        if (!file.isDirectory()) {
            lastPath = file.getParentFile().getAbsolutePath();
        }
        Preferences preferences = Preferences.userRoot().node(GuiBridgeHandler.PREFS_NODE_NAME);
        preferences.put(LAST_PATH, lastPath);
    }

    public static void copyToClipboard( String text ) {
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    public static void openFile( File file ) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(file);
        }
    }

    public static JDialog openDialogWithPanel( JPanel panel, String title, Dimension dimension ) {
        JDialog f = new JDialog();
        f.add(panel, BorderLayout.CENTER);
        f.setTitle(title);
        f.pack();
        if (dimension != null)
            f.setSize(dimension);
        f.setLocationRelativeTo(null); // Center on screen
        f.setVisible(true);
        f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        return f;
    }

    /**
     * Create a simple multi input pane, that returns what the use inserts.
     * 
     * @param parentComponent
     * @param title the dialog title.
     * @param labels the labels to set.
     * @param defaultValues a set of default values.
     * @return the result inserted by the user.
     */
    public static String[] showMultiInputDialog( Component parentComponent, String title, String[] labels,
            String[] defaultValues ) {
        JTextField textFields[] = new JTextField[labels.length];
        JPanel panel = new JPanel();
//        panel.setPreferredSize(new Dimension(400, 300));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String input[] = new String[labels.length];
        panel.setLayout(new GridLayout(labels.length, 2, 5, 5));
        for( int i = 0; i < labels.length; i++ ) {
            panel.add(new JLabel(labels[i]));
            textFields[i] = new JTextField();
            panel.add(textFields[i]);
            if (defaultValues != null) {
                textFields[i].setText(defaultValues[i]);
            }
        }
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(550, 300));
        int result = JOptionPane.showConfirmDialog(parentComponent, scrollPane, title, JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        for( int i = 0; i < labels.length; i++ )
            input[i] = textFields[i].getText();
        return input;
    }

}
