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
package org.jgrasstools.gui.spatialtoolbox.widgets;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jgrasstools.gui.spatialtoolbox.core.FieldData;

/**
 * Class representing an label gui.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GuiLabel extends ModuleGuiElement {
    private final String constraints;
    private FieldData data;

    public int WRAP = 25;
    private JLabel jlabel;
    private boolean isBold;

    public GuiLabel( FieldData data, String constraints, boolean isBold ) {
        this.data = data;
        this.constraints = constraints;
        this.isBold = isBold;
    }

    @Override
    public JComponent makeGui( final JComponent parent ) {
        parent.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;

        jlabel = new JLabel();
        parent.add(jlabel, c);

        // if (isBold) {
        // FontData[] fD = jlabel.getFont().getFontData();
        // fD[0].setStyle(SWT.BOLD);
        // jlabel.setFont(new Font(parent.getDisplay(), fD[0]));
        // }

        String label = setLabel();
        jlabel.setText(label);

        // parent.addControlListener(new ControlListener(){
        // public void controlResized( ControlEvent e ) {
        // setLabel();
        // }
        //
        // @Override
        // public void controlMoved( ControlEvent e ) {
        //
        // }
        // });

        return jlabel;
    }

    private String setLabel() {
        String label = data.fieldDescription;
        int length = data.fieldDescription.length();

        if (length > WRAP) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while( i < length ) {
                int startIndex = i;
                i = i + WRAP;
                // find first space
                while( i < length && data.fieldDescription.charAt(i) != ' ' ) {
                    i = i + 1;
                }

                int endIndex = i;

                String sub = null;
                if (endIndex > length) {
                    sub = data.fieldDescription.substring(startIndex).trim();
                } else {
                    sub = data.fieldDescription.substring(startIndex, endIndex).trim();
                }
                sb.append(sub).append("\n");
            }
            label = sb.toString();
        }
        return label;
    }

    public FieldData getFieldData() {
        return data;
    }

    public boolean hasData() {
        return false;
    }

    @Override
    public String validateContent() {
        return null;
    }
}
