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
 * The tabs at the side of the map of the viewer: one tab per {@link INwwTool}, whose panel is
 * created the first time the tab is selected.
 *
 * <p>After them come the tabs of the current layer: the layer tab, shown when a layer is
 * selected in the layers panel, and the style tab, shown when the style button of a layer is
 * pressed.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NwwToolsTabs extends JTabbedPane {
    private static final long serialVersionUID = 1L;

    private final NwwContext context;
    /** The tools, at the index of their tab. */
    private final List<INwwTool> tools = new ArrayList<>();
    /** If the panel of the tool at the same index has been created. */
    private final List<Boolean> created = new ArrayList<>();
    /** The tool of the selected tab, <code>null</code> for the layer tabs. */
    private INwwTool activeTool;

    /** The tab with the panel of the current layer, as its info and filter. */
    private final LayerTab layerTab = new LayerTab();
    /** The tab with the style of a layer. */
    private final LayerTab styleTab = new LayerTab();

    private record LayerPanel( INwwLayerTool tool, JComponent panel ) {
    }

    /**
     * A tab showing the panel of a layer, made by the first of its tools that handles the layer.
     * Each layer gets a panel of its own, kept while the layer is in the viewer.
     */
    private class LayerTab {
        private final List<INwwLayerTool> tools = new ArrayList<>();
        private final Map<Layer, LayerPanel> panels = new IdentityHashMap<>();
        /** The content of the tab, which shows the panel of the layer. */
        private final JPanel holder = new JPanel(new BorderLayout());
        /** The layer whose panel is shown. */
        private Layer shownLayer;

        boolean isOpen() {
            return indexOfComponent(holder) >= 0;
        }

        /**
         * Show the panel of a layer, opening the tab if needed. The tab is closed if no tool
         * handles the layer.
         *
         * @param layer the layer, or <code>null</code> to close the tab.
         * @param select if <code>true</code>, the tab is selected.
         * @return <code>true</code> if the panel is shown.
         */
        boolean show( Layer layer, boolean select ) {
            LayerPanel layerPanel = layer == null ? null : panels.get(layer);
            if (layer != null && layerPanel == null) {
                for( INwwLayerTool tool : tools ) {
                    if (tool.canHandle(layer)) {
                        layerPanel = new LayerPanel(tool, tool.createPanel(context, layer));
                        panels.put(layer, layerPanel);
                        break;
                    }
                }
            }
            if (layerPanel == null) {
                close();
                return false;
            }
            shownLayer = layer;
            holder.removeAll();
            holder.add(layerPanel.panel(), BorderLayout.CENTER);
            holder.revalidate();
            holder.repaint();
            int tabIndex = indexOfComponent(holder);
            if (tabIndex < 0) {
                addTab(layerPanel.tool().getName(), holder);
                tabIndex = indexOfComponent(holder);
            } else {
                setTitleAt(tabIndex, layerPanel.tool().getName());
            }
            setToolTipTextAt(tabIndex, layer.getName());
            if (select) {
                setSelectedIndex(tabIndex);
            }
            return true;
        }

        void close() {
            shownLayer = null;
            int tabIndex = indexOfComponent(holder);
            if (tabIndex >= 0) {
                if (getSelectedIndex() == tabIndex) {
                    setSelectedIndex(0);
                }
                removeTabAt(tabIndex);
            }
        }

        void replaceLayer( Layer layer, Layer replacement ) {
            LayerPanel layerPanel = panels.remove(layer);
            if (layerPanel != null) {
                panels.put(replacement, layerPanel);
            }
            if (shownLayer == layer) {
                shownLayer = replacement;
            }
        }

        void dropRemovedLayers( LayerList layers ) {
            Iterator<Map.Entry<Layer, LayerPanel>> iterator = panels.entrySet().iterator();
            while( iterator.hasNext() ) {
                Map.Entry<Layer, LayerPanel> entry = iterator.next();
                // read before the removal, which invalidates the entries of an IdentityHashMap
                Layer layer = entry.getKey();
                if (!layers.contains(layer)) {
                    entry.getValue().tool().onClose(layer);
                    iterator.remove();
                    if (shownLayer == layer) {
                        close();
                    }
                }
            }
        }

        void closeAll() {
            for( Map.Entry<Layer, LayerPanel> entry : panels.entrySet() ) {
                entry.getValue().tool().onClose(entry.getKey());
            }
            panels.clear();
        }
    }

    /**
     * @param context the access to the viewer given to the tools.
     */
    public NwwToolsTabs( NwwContext context ) {
        this.context = context;
        context.setToolsTabs(this);
        addChangeListener(e -> onTabSelected());
    }

    /**
     * Add the tab of a tool, its panel is created when the tab is first selected. The tools
     * are added before the layer tabs are shown.
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
        layerTab.tools.add(tool);
    }

    /**
     * Add a tool for the style of a kind of layers, whose panel is shown in the style tab.
     *
     * @param tool the tool to add.
     */
    public void addStyleTool( INwwLayerTool tool ) {
        styleTab.tools.add(tool);
    }

    /**
     * Show the panel of a layer in the layer tab and select it. The tab is removed if no tool
     * handles the layer.
     *
     * <p>An open style tab follows the current layer, it is closed if no style tool handles
     * it. If it is the selected tab and it shows the new layer, it stays selected.</p>
     *
     * @param layer the current layer, or <code>null</code> if there is none.
     */
    public void showLayer( Layer layer ) {
        dropRemovedLayers();
        boolean styleSelected = styleTab.isOpen() && getSelectedComponent() == styleTab.holder;
        boolean styleShown = styleTab.isOpen() && (styleTab.shownLayer == layer || styleTab.show(layer, false));
        layerTab.show(layer, !(styleSelected && styleShown));
        if (styleSelected && styleShown) {
            setSelectedComponent(styleTab.holder);
        }
    }

    /**
     * Show the style panel of a layer in the style tab and select it.
     *
     * @param layer the layer.
     * @return <code>true</code> if a style tool handles the layer.
     */
    public boolean showStyle( Layer layer ) {
        dropRemovedLayers();
        return styleTab.show(layer, true);
    }

    /**
     * @param layer a layer.
     * @return <code>true</code> if a style tool handles the layer.
     */
    public boolean hasStyleTool( Layer layer ) {
        for( INwwLayerTool tool : styleTab.tools ) {
            if (tool.canHandle(layer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Give the panels of a layer to the layer that replaces it, as a filtered version of it, so
     * that the panels are not created again.
     *
     * @param layer the replaced layer.
     * @param replacement the layer that replaces it.
     */
    public void replaceLayer( Layer layer, Layer replacement ) {
        layerTab.replaceLayer(layer, replacement);
        styleTab.replaceLayer(layer, replacement);
    }

    /**
     * Drop the panels of the layers no longer in the viewer.
     */
    private void dropRemovedLayers() {
        LayerList layers = context.getNwwPanel().getWwd().getModel().getLayers();
        layerTab.dropRemovedLayers(layers);
        styleTab.dropRemovedLayers(layers);
    }

    private void onTabSelected() {
        int index = getSelectedIndex();
        // the layer tabs have no tool
        Component component = index >= 0 ? getComponentAt(index) : null;
        INwwTool selected = index >= 0 && index < tools.size() && component != layerTab.holder
                && component != styleTab.holder ? tools.get(index) : null;
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
        if (!created.get(index)) {
            created.set(index, true);
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
        layerTab.closeAll();
        styleTab.closeAll();
    }
}
