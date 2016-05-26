package org.jgrasstools.nww.gui;

import java.util.ArrayList;
import java.util.List;

import com.jgoodies.forms.layout.CellConstraints;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

public class LayersPanelController extends LayersPanelView implements LayerEventsListener {

    private WorldWindow wwd;

    /**
    * Default constructor
     * @param wwd 
     * @param impiantiList 
    */
    public LayersPanelController(WorldWindow wwd) {
        this.wwd = wwd;

        refreshLayersList();
    }

    public void refreshLayersList() {
        _layersGridView.removeAll();

        LayerList layerList = wwd.getModel().getLayers();
        List<Layer> layersReverse = new ArrayList<>();
        for (int i = 0; i < layerList.size(); i++) {
            Layer layer = layerList.get(i);
            layersReverse.add(0, layer);
        }

        int index = 1;
        CellConstraints cc = new CellConstraints();
        for (int i = 0; i < layersReverse.size(); i++) {
            Layer layer = layersReverse.get(i);
            LayerListRowPanel row = new LayerListRowPanel(this, wwd, layer);
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
