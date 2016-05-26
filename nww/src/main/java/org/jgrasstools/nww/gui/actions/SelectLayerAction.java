package org.jgrasstools.nww.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

import org.jgrasstools.nww.gui.LayerEventsListener;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;

public class SelectLayerAction extends AbstractAction {

    protected WorldWindow wwd;
    protected Layer layer;
    protected boolean selected;
    private LayerEventsListener eventsListener;

    public SelectLayerAction(LayerEventsListener eventsListener, WorldWindow wwd, Layer layer, boolean selected) {
        super(layer.getName());
        this.eventsListener = eventsListener;

        this.wwd = wwd;
        this.layer = layer;
        this.selected = selected;
        this.layer.setEnabled(this.selected);
    }

    public boolean isSelected() {
        return selected;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        boolean isSelected = ((JCheckBox) actionEvent.getSource()).isSelected();
        this.layer.setEnabled(isSelected);
        eventsListener.onLayerSelected(layer, isSelected);
        wwd.redraw();
    }
}
