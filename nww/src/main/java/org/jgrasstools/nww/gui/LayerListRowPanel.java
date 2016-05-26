
package org.jgrasstools.nww.gui;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jgrasstools.nww.gui.actions.DeleteLayerAction;
import org.jgrasstools.nww.gui.actions.SelectLayerAction;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;

public class LayerListRowPanel extends JPanel {

    public LayerListRowPanel(LayerEventsListener layerListener, final WorldWindow wwd, final Layer layer) {
        super(new BorderLayout(10, 10));

        SelectLayerAction action = new SelectLayerAction(layerListener, wwd, layer, layer.isEnabled());
        JCheckBox checkBox = new JCheckBox(action);
        checkBox.setSelected(action.isSelected());
        add(checkBox, BorderLayout.CENTER);

        DeleteLayerAction deleteAction = new DeleteLayerAction(layerListener, wwd, layer);
        JButton deleteButton = new JButton(deleteAction);
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setContentAreaFilled(false);
        add(deleteButton, BorderLayout.EAST);
    }

}
