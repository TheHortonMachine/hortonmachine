
package org.jgrasstools.nww.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jgrasstools.nww.gui.actions.DeleteLayerAction;
import org.jgrasstools.nww.gui.actions.SelectLayerAction;
import org.jgrasstools.nww.gui.actions.StyleVectorLayerAction;
import org.jgrasstools.nww.gui.actions.ZoomToLayerAction;
import org.jgrasstools.nww.layers.defaults.NwwVectorLayer;

import gov.nasa.worldwind.layers.Layer;

public class LayerListRowPanel extends JPanel {

    public LayerListRowPanel(LayerEventsListener layerListener, final NwwPanel wwdPanel, final Layer layer) {
        super(new BorderLayout(10, 10));

        SelectLayerAction action = new SelectLayerAction(layerListener, wwdPanel.getWwd(), layer, layer.isEnabled());
        JCheckBox checkBox = new JCheckBox(action);
        checkBox.setSelected(action.isSelected());
        add(checkBox, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        add(buttonsPanel, BorderLayout.EAST);

        DeleteLayerAction deleteAction = new DeleteLayerAction(layerListener, wwdPanel.getWwd(), layer);
        JButton deleteButton = new JButton(deleteAction);
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setContentAreaFilled(false);
        buttonsPanel.add(deleteButton);

        ZoomToLayerAction zoomToLayerAction = new ZoomToLayerAction(wwdPanel, layer);
        JButton zoomToButton = new JButton(zoomToLayerAction);
        zoomToButton.setBorderPainted(false);
        zoomToButton.setFocusPainted(false);
        zoomToButton.setContentAreaFilled(false);
        buttonsPanel.add(zoomToButton);

        if (layer instanceof NwwVectorLayer) {
            StyleVectorLayerAction styleVectorLayerAction = new StyleVectorLayerAction(wwdPanel,
                    (NwwVectorLayer) layer);
            JButton styleButton = new JButton(styleVectorLayerAction);
            styleButton.setBorderPainted(false);
            styleButton.setFocusPainted(false);
            styleButton.setContentAreaFilled(false);
            buttonsPanel.add(styleButton);
        }
    }

}
