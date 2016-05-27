package org.jgrasstools.nww.gui.actions;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jgrasstools.nww.gui.NwwPanel;
import org.jgrasstools.nww.gui.style.StylePanelController;
import org.jgrasstools.nww.layers.defaults.NwwVectorLayer;

import gov.nasa.worldwind.layers.Layer;

public class StyleVectorLayerAction extends AbstractAction {

    protected NwwPanel wwdPanel;
    protected NwwVectorLayer layer;
    protected boolean selected;

    public StyleVectorLayerAction(NwwPanel wwdPanel, NwwVectorLayer layer) {
        super("", new ImageIcon(StyleVectorLayerAction.class.getResource("/org/jgrasstools/images/palette.png")));
        this.wwdPanel = wwdPanel;
        this.layer = layer;
    }

    public void actionPerformed(ActionEvent actionEvent) {

        JFrame d1 = new JFrame("Select the Style");
        // d1=new JDialog(wwdPanel,"Select the Style",true);
        Container contentPane = d1.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(new StylePanelController(layer));
        d1.setSize(400, 400);
        // d1.setLayout(new BorderLayout());
        d1.setVisible(true);

    }
}
