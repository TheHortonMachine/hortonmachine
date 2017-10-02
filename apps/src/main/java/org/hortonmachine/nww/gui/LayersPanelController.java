package org.hortonmachine.nww.gui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import com.jgoodies.forms.layout.CellConstraints;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

public class LayersPanelController extends LayersPanelView implements LayerEventsListener {

    private NwwPanel wwdPanel;

    /**
    * Default constructor
     * @param wwdPanel 
     * @param impiantiList 
    */
    public LayersPanelController(NwwPanel wwdPanel) {
        this.wwdPanel = wwdPanel;

//        setMinimumSize(new Dimension(450, 300));
        setMaximumSize(new Dimension(400, 2300));

        refreshLayersList();
    }
    
    public void refreshLayersList() {
        _layersGridView.removeAll();

        LayerList layerList = wwdPanel.getWwd().getModel().getLayers();
        List<Layer> layersReverse = new ArrayList<>();
        for (int i = 0; i < layerList.size(); i++) {
            Layer layer = layerList.get(i);
            if (layer.getName().startsWith("hide")) {
                continue;
            }
            layersReverse.add(0, layer);
        }

        int index = 1;
        CellConstraints cc = new CellConstraints();
        for (int i = 0; i < layersReverse.size(); i++) {
            Layer layer = layersReverse.get(i);
            LayerListRowPanel row = new LayerListRowPanel(this, wwdPanel, layer);
            _layersGridView.add(row, cc.xy(1, index));
            index = index + 2;
        }

        _layersGridView.revalidate();
        _layersGridView.repaint();
    }

    @Override
    public void onLayerAdded(Layer addedLayer) {
        refreshLayersList();
    }

    @Override
    public void onLayerRemoved(Layer removeLayer) {
        refreshLayersList();
    }

    @Override
    public void onLayerSelected(Layer selectdLayer, boolean isSelected) {
    }

}
