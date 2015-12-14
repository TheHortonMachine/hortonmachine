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

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gui.spatialtoolbox.core.FieldData;

/**
 * Class representing a gui for combobox choice.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GuiComboField extends ModuleGuiElement {

    private final FieldData data;
    private JComboBox<String> combo;

    public GuiComboField( FieldData data, String constraints ) {
        this.data = data;

    }

    @Override
    public JComponent makeGui( JComponent parent ) {
        parent.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;

        String[] guiHintsSplit = data.guiHints.split(";");
        String[] itemsSplit = new String[]{" - "};
        for( String guiHint : guiHintsSplit ) {
            if (guiHint.startsWith(JGTConstants.COMBO_UI_HINT)) {
                String items = guiHint.replaceFirst(JGTConstants.COMBO_UI_HINT, "").replaceFirst(":", "").trim();
                itemsSplit = items.split(",");
                break;
            }
        }

        combo = new JComboBox<String>(itemsSplit);
        parent.add(combo, c);

        if (data.fieldValue != null) {
            for( int i = 0; i < itemsSplit.length; i++ ) {
                if (data.fieldValue.equals(itemsSplit[i])) {
                    combo.setSelectedIndex(i);
                    data.fieldValue = combo.getSelectedItem().toString();
                    break;
                }
            }
        } else {
            combo.setSelectedIndex(0);
            data.fieldValue = combo.getSelectedItem().toString();
        }
        combo.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                data.fieldValue = combo.getSelectedItem().toString();
            }
        });

        return combo;
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
