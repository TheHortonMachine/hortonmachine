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
    protected NwwLayer layer;
    protected boolean selected;

    public ZoomToLayerAction(NwwPanel wwdPanel, NwwLayer layer) {
        super("", new ImageIcon(ZoomToLayerAction.class.getResource("/org/jgrasstools/images/zoom.gif")));
        this.wwdPanel = wwdPanel;
        this.layer = layer;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        Coordinate center = layer.getCenter();
        wwdPanel.goTo(center.x, center.y, null, true);
    }
}
