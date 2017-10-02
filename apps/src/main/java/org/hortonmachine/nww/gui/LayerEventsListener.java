package org.hortonmachine.nww.gui;

import gov.nasa.worldwind.layers.Layer;

public interface LayerEventsListener {

    public void onLayerAdded(Layer addedLayer);

    public void onLayerRemoved(Layer removeLayer);

    public void onLayerSelected(Layer selectdLayer, boolean isSelected);

}
