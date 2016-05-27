package org.jgrasstools.nww.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import com.vividsolutions.jts.geom.Coordinate;

import org.jgrasstools.nww.gui.NwwPanel;
import org.jgrasstools.nww.layers.defaults.NwwLayer;

import gov.nasa.worldwind.layers.Layer;

public class ZoomToLayerAction extends AbstractAction {

    protected NwwPanel wwdPanel;
    protected Layer layer;
    protected boolean selected;

    public ZoomToLayerAction(NwwPanel wwdPanel, Layer layer) {
        super("", new ImageIcon(ZoomToLayerAction.class.getResource("/org/jgrasstools/images/zoom.gif")));
        this.wwdPanel = wwdPanel;
        this.layer = layer;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (layer instanceof NwwLayer) {
            NwwLayer nwwLayer = (NwwLayer) layer;
            Coordinate center = nwwLayer.getCenter();
            wwdPanel.goTo(center.x, center.y, null, false);
        }

    }
}
