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
package org.hortonmachine.gui.editor;

import java.awt.Font;
import java.io.InputStream;

import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.hortonmachine.dbs.log.Logger;

import com.formdev.flatlaf.FlatLaf;

/**
 * Creates the code editors used by the applications, so that they share look and behavior.
 *
 * <p>
 * The editors are {@link RSyntaxTextArea}s, which bring syntax highlighting, bracket matching,
 * code folding and undo out of the box. The theme follows the look and feel (light or dark).
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class CodeEditorFactory {

    private CodeEditorFactory() {
    }

    /**
     * Create a code editor.
     *
     * @param syntaxStyle the syntax style, one of the {@link org.fife.ui.rsyntaxtextarea.SyntaxConstants}
     *          (ex. SYNTAX_STYLE_GROOVY).
     * @return the editor.
     */
    public static RSyntaxTextArea createEditor( String syntaxStyle ) {
        RSyntaxTextArea area = new RSyntaxTextArea(25, 80);
        area.setSyntaxEditingStyle(syntaxStyle);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setMarkOccurrences(true);
        area.setBracketMatchingEnabled(true);
        area.setPaintMatchedBracketPair(true);
        area.setAutoIndentEnabled(true);
        area.setTabSize(4);
        area.setTabsEmulated(true);
        area.setClearWhitespaceLinesEnabled(false);
        applyTheme(area);
        return area;
    }

    /**
     * Wrap an editor in a scrollpane with line numbers and fold indicators.
     *
     * @param area the editor.
     * @return the scrollpane.
     */
    public static RTextScrollPane createScrollPane( RSyntaxTextArea area ) {
        RTextScrollPane scrollPane = new RTextScrollPane(area);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setFoldIndicatorEnabled(true);
        return scrollPane;
    }

    /**
     * Apply the theme that fits the current look and feel and a monospaced font sized like the ui.
     *
     * @param area the editor.
     */
    public static void applyTheme( RSyntaxTextArea area ) {
        String themeName = isDarkLaf() ? "dark.xml" : "idea.xml";
        try (InputStream in = RSyntaxTextArea.class.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/" + themeName)) {
            Theme.load(in).apply(area);
        } catch (Exception e) {
            Logger.INSTANCE.insertError("", "Unable to load the editor theme " + themeName, e);
        }
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, getUiFontSize() + 1));
    }

    /**
     * @return <code>true</code> if the current look and feel is a dark one.
     */
    public static boolean isDarkLaf() {
        return FlatLaf.isLafDark();
    }

    private static int getUiFontSize() {
        Font font = UIManager.getFont("Label.font");
        return font != null ? font.getSize() : 13;
    }
}
