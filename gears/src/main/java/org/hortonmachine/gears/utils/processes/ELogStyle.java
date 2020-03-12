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
package org.hortonmachine.gears.utils.processes;

import java.awt.Color;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public enum ELogStyle {
    NORMAL, COMMENT, ERROR;

    private SimpleAttributeSet errorSet;
    private SimpleAttributeSet commentSet;
    private SimpleAttributeSet normalSet;

    ELogStyle() {
        errorSet = new SimpleAttributeSet();
        StyleConstants.setForeground(errorSet, Color.RED);
        // StyleConstants.setBackground(errorSet, Color.YELLOW);
        StyleConstants.setBold(errorSet, true);

        commentSet = new SimpleAttributeSet();
        StyleConstants.setForeground(commentSet, new Color(93, 157, 127));
        // StyleConstants.setBackground(errorSet, Color.YELLOW);
        StyleConstants.setItalic(commentSet, true);

        normalSet = new SimpleAttributeSet();
        StyleConstants.setForeground(normalSet, Color.black);
        // StyleConstants.setBackground(errorSet, Color.YELLOW);
        // StyleConstants.setBold(commentSet, true);

    }

    public SimpleAttributeSet getAttributeSet() {
        switch( this ) {
        case ERROR:
            return errorSet;
        case COMMENT:
            return commentSet;
        default:
            return normalSet;
        }
    }
}
