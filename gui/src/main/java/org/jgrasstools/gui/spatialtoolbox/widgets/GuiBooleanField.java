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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.jgrasstools.gui.spatialtoolbox.core.FieldData;

/**
 * Class representing a gui for boolean choice.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GuiBooleanField extends ModuleGuiElement {

    private final FieldData data;
    private JCheckBox checkButton;

    public GuiBooleanField( FieldData data, String constraints ) {
        this.data = data;
    }

    @Override
    public JComponent makeGui( JComponent parent ) {

        parent.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;

        checkButton = new JCheckBox();
        parent.add(checkButton, c);
        checkButton.setText("");

        boolean checked = Boolean.parseBoolean(data.fieldValue);
        checkButton.setSelected(checked);
        checkButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                if (checkButton.isSelected()) {
                    data.fieldValue = String.valueOf(true);
                } else {
                    data.fieldValue = String.valueOf(false);
                }
            }
        });

        return checkButton;
    }

    public FieldData getFieldData() {
        return data;
    }

    public boolean hasData() {
        return true;
    }

    @Override
    public String validateContent() {
        return null;
    }

}
