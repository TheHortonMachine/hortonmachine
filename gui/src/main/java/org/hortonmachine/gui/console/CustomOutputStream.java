/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gui.console;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;

/**
 * This class extends from OutputStream to redirect output to a JTextArrea
 * @author www.codejava.net
 *
 */
public class CustomOutputStream extends OutputStream {
    private JTextArea textArea;

    private final StringBuilder sb = new StringBuilder();

    public CustomOutputStream( JTextArea textArea ) {
        this.textArea = textArea;
    }

    @Override
    public void write( int b ) throws IOException {
        char c = (char) b;
        if (c == '\r')
            return;

        if (c == '\n') {
            final String text = sb.toString();
            if (!ConsoleMessageFilter.doRemove(text)) {
                textArea.append(text);
                textArea.append("\n");
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }
            sb.setLength(0);
        } else {
            sb.append(c);
        }

    }
}