/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.nww.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

/**
 * The tabs at the side of the map of the viewer: the general tools first, then one tab per
 * {@link INwwTool}, whose panel is created the first time the tab is selected.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NwwToolsTabs extends JTabbedPane {
    private static final long serialVersionUID = 1L;

    private final NwwContext context;
    /** The tools, at the index of their tab minus one. */
    private final List<INwwTool> tools = new ArrayList<>();
    /** If the panel of the tool at the same index has been created. */
    private final List<Boolean> created = new ArrayList<>();
    /** The tool of the selected tab, <code>null</code> for the general tools. */
    private INwwTool activeTool;

    private final List<INwwLayerTool> layerTools = new ArrayList<>();
    /** The panels of the layers, with the tool that made them. */
    private final Map<Layer, LayerPanel> layerPanels = new IdentityHashMap<>();
    /** The content of the layer tab, which shows the panel of the current layer. */
    private final JPanel layerHolder = new JPanel(new BorderLayout());

    private record LayerPanel( INwwLayerTool tool, JComponent panel ) {
    }

    /**
     * @param generalTools the panel of the general tools, in the first tab.
     * @param context the access to the viewer given to the tools.
     */
    public NwwToolsTabs( Component generalTools, NwwContext context ) {
        this.context = context;
        addTab("Tools", generalTools);
        addChangeListener(e -> onTabSelected());
    }

    /**
     * Add the tab of a tool, its panel is created when the tab is first selected.
     *
     * @param tool the tool to add.
     */
    public void addTool( INwwTool tool ) {
        tools.add(tool);
        created.add(false);
        addTab(tool.getName(), tool.getIcon(), new JPanel());
    }

    /**
     * Add a tool for a kind of layers, whose panel is shown in the layer tab.
     *
     * @param tool the tool to add.
     */
    public void addLayerTool( INwwLayerTool tool ) {
        layerTools.add(tool);
    }

    /**
     * Show the panel of a layer in the layer tab and select it. The tab is removed if no tool
     * handles the layer.
     *
     * @param layer the current layer, or <code>null</code> if there is none.
     */
    public void showLayer( Layer layer ) {
        dropRemovedLayers();
        LayerPanel layerPanel = layer == null ? null : layerPanels.get(layer);
        if (layer != null && layerPanel == null) {
            for( INwwLayerTool tool : layerTools ) {
                if (tool.canHandle(layer)) {
                    layerPanel = new LayerPanel(tool, tool.createPanel(context, layer));
                    layerPanels.put(layer, layerPanel);
                    break;
                }
            }
        }
        int layerTabIndex = indexOfComponent(layerHolder);
        if (layerPanel == null) {
            if (layerTabIndex >= 0) {
                if (getSelectedIndex() == layerTabIndex) {
                    setSelectedIndex(0);
                }
                removeTabAt(layerTabIndex);
            }
            return;
        }
        layerHolder.removeAll();
        layerHolder.add(layerPanel.panel(), BorderLayout.CENTER);
        layerHolder.revalidate();
        layerHolder.repaint();
        if (layerTabIndex < 0) {
            addTab(layerPanel.tool().getName(), layerHolder);
            layerTabIndex = indexOfComponent(layerHolder);
        } else {
            setTitleAt(layerTabIndex, layerPanel.tool().getName());
        }
        setToolTipTextAt(layerTabIndex, layer.getName());
        setSelectedIndex(layerTabIndex);
    }

    /**
     * Drop the panels of the layers no longer in the viewer.
     */
    private void dropRemovedLayers() {
        LayerList layers = context.getNwwPanel().getWwd().getModel().getLayers();
        Iterator<Map.Entry<Layer, LayerPanel>> iterator = layerPanels.entrySet().iterator();
        while( iterator.hasNext() ) {
            Map.Entry<Layer, LayerPanel> entry = iterator.next();
            if (!layers.contains(entry.getKey())) {
                entry.getValue().tool().onClose(entry.getKey());
                iterator.remove();
            }
        }
    }

    private void onTabSelected() {
        int index = getSelectedIndex();
        // the general tools and the layer tab have no tool
        INwwTool selected = index > 0 && index - 1 < tools.size() && getComponentAt(index) != layerHolder
                ? tools.get(index - 1)
                : null;
        if (selected == activeTool) {
            return;
        }
        if (activeTool != null) {
            activeTool.onDeactivated();
        }
        activeTool = selected;
        if (selected == null) {
            return;
        }
        if (!created.get(index - 1)) {
            created.set(index - 1, true);
            JComponent panel = selected.createPanel(context);
            setComponentAt(index, panel);
        }
        selected.onActivated();
    }

    /**
     * Tell the tools whose panel has been created that the viewer closes.
     */
    public void closeTools() {
        for( int i = 0; i < tools.size(); i++ ) {
            if (created.get(i)) {
                tools.get(i).onClose();
            }
        }
        for( Map.Entry<Layer, LayerPanel> entry : layerPanels.entrySet() ) {
            entry.getValue().tool().onClose(entry.getKey());
        }
        layerPanels.clear();
    }
}
