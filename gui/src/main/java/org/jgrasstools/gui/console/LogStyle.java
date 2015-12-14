/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package org.jgrasstools.gui.console;

import java.awt.Color;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public enum LogStyle {
    NORMAL, COMMENT, ERROR;

    private SimpleAttributeSet errorSet;
    private SimpleAttributeSet commentSet;
    private SimpleAttributeSet normalSet;

    LogStyle() {
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
