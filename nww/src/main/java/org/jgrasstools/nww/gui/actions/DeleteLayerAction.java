package org.jgrasstools.nww.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.jgrasstools.nww.gui.LayerEventsListener;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

public class DeleteLayerAction extends AbstractAction {

    protected WorldWindow wwd;
    protected Layer layer;
    protected boolean selected;
    private LayerEventsListener eventsListener;

    public DeleteLayerAction(LayerEventsListener eventsListener, WorldWindow wwd, Layer layer) {
        super("", new ImageIcon(DeleteLayerAction.class.getResource("/org/jgrasstools/images/trash.gif")));

        this.eventsListener = eventsListener;
        this.wwd = wwd;
        this.layer = layer;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        LayerList layers = wwd.getModel().getLayers();
        layers.remove(layer);
        eventsListener.onLayerRemoved(layer);
        wwd.redraw();
    }
}
