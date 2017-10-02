package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;

import com.jgoodies.forms.layout.CellConstraints;

public class MainPanelController extends MainPanelView {

    public MainPanelController() {

        NwwPanel wwjPanel = (NwwPanel) NwwPanel.createNwwPanel(false);
        LayersPanelController layerManagerPanel = new LayersPanelController(wwjPanel);
        ToolsPanelController toolsPanel = new ToolsPanelController(wwjPanel, layerManagerPanel);

        _layersPanel.add(layerManagerPanel,
            new CellConstraints(1, 1, 1, 1, CellConstraints.FILL, CellConstraints.FILL));
        _toolsPanel.add(toolsPanel, new CellConstraints(1, 1, 1, 1, CellConstraints.FILL, CellConstraints.FILL));

        _nwwPanel.setLayout(new BorderLayout());
        _nwwPanel.add(wwjPanel, BorderLayout.CENTER);

    }

}
