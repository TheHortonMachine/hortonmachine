/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package org.hortonmachine.nww;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.gui.LayersPanelController;
import org.hortonmachine.nww.gui.NwwMenuBar;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.ToolsPanelController;
import org.hortonmachine.nww.layers.defaults.raster.EsriSatellite;
import org.hortonmachine.nww.layers.defaults.raster.OepnvkarteLayer;
import org.hortonmachine.nww.layers.defaults.raster.OpenTopoLayer;
import org.hortonmachine.gears.assistant.Assistants;
import org.hortonmachine.gears.assistant.IAssistantBackend;
import org.hortonmachine.gears.assistant.IScriptAssistant;
import org.hortonmachine.nww.tools.INwwLayerTool;
import org.hortonmachine.nww.tools.INwwMapMode;
import org.hortonmachine.nww.tools.INwwTool;
import org.hortonmachine.nww.tools.NwwMapModesBar;
import org.hortonmachine.nww.tools.features.FeatureInfoTool;
import org.hortonmachine.nww.tools.modes.PanMode;
import org.hortonmachine.nww.tools.modes.SelectMode;
import org.hortonmachine.nww.tools.modes.ZoomByBoxMode;
import org.hortonmachine.nww.tools.layers.RasterLayerTool;
import org.hortonmachine.nww.tools.layers.RasterStyleLayerTool;
import org.hortonmachine.nww.tools.layers.StyleLayerTool;
import org.hortonmachine.nww.tools.layers.VectorLayerTool;
import org.hortonmachine.nww.tools.assistant.AssistantTool;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.nww.tools.NwwToolsTabs;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;

public class SimpleNwwViewer {

    static {
        System.setProperty("java.net.useSystemProxies", "true");
        if (Configuration.isMacOS()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Application");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
        } else if (Configuration.isWindowsOS()) {
            System.setProperty("sun.awt.noerasebackground", "true");
        }
    }

    public static String APPNAME = "SIMPLE NWW VIEWER";

    /**
     * @return new instances of the tools docked in the tabs at the side of the map, opened from
     *          the tools menu, each viewer gets its own.
     */
    public static List<INwwTool> createDefaultTools() {
        List<INwwTool> tools = new ArrayList<>();
        // opened by the identify mode
        tools.add(new FeatureInfoTool());
        // not available, so greyed out in the menu, if no module providing it is installed
        IAssistantBackend backend = Assistants.getBackend().orElse(null);
        IScriptAssistant scriptAssistant = backend == null ? null : backend.getScriptAssistant().orElse(null);
        tools.add(new AssistantTool(backend, scriptAssistant));
        return tools;
    }

    /**
     * @return new instances of the tools of the kinds of layers, whose panel is shown when a
     *          layer is selected in the layers panel: the first that handles a layer is used.
     */
    public static List<INwwLayerTool> createDefaultLayerTools() {
        List<INwwLayerTool> tools = new ArrayList<>();
        tools.add(new VectorLayerTool());
        tools.add(new RasterLayerTool());
        return tools;
    }

    /**
     * @return new instances of the tools of the styles of the kinds of layers, whose panel is
     *          shown when the style button of a layer is pressed: the first that handles a layer
     *          is used.
     */
    public static List<INwwLayerTool> createDefaultStyleTools() {
        List<INwwLayerTool> tools = new ArrayList<>();
        tools.add(new StyleLayerTool());
        tools.add(new RasterStyleLayerTool());
        return tools;
    }

    /**
     * @return new instances of the modes of interaction with the map, in the toolbar at its
     *          side: the first one is the default.
     */
    public static List<INwwMapMode> createDefaultMapModes() {
        List<INwwMapMode> modes = new ArrayList<>();
        modes.add(new PanMode());
        modes.add(new SelectMode());
        modes.add(new ZoomByBoxMode());
        return modes;
    }

    /**
     * A side of a split pane that can be hidden, giving its space to the other side.
     *
     * <p>A side hidden by the user stays hidden; otherwise it is shown only while it has
     * content, as the tabs of the tools, which are none without tools and current layer.</p>
     */
    private static class SplitSide {
        private final JSplitPane split;
        private final Component side;
        /** The divider is drawn even if a side is hidden, so it is shrunk to nothing. */
        private final int dividerSize;
        private int lastDividerLocation = -1;
        private boolean hiddenByUser = false;

        SplitSide( JSplitPane split, Component side ) {
            this.split = split;
            this.side = side;
            this.dividerSize = split.getDividerSize();
        }

        boolean isShown() {
            return side.isVisible();
        }

        /** Show or hide the side, as asked by the user. */
        void setShown( boolean shown ) {
            hiddenByUser = !shown;
            apply(shown);
        }

        /** Show the side if it has content and the user did not hide it, hide it if it has none. */
        void update( boolean hasContent ) {
            apply(hasContent && !hiddenByUser);
        }

        private void apply( boolean shown ) {
            if (shown == side.isVisible()) {
                return;
            }
            if (shown) {
                side.setVisible(true);
                split.setDividerSize(dividerSize);
                if (lastDividerLocation > 0) {
                    split.setDividerLocation(lastDividerLocation);
                } else {
                    split.resetToPreferredSizes();
                }
            } else {
                if (split.isShowing()) {
                    lastDividerLocation = split.getDividerLocation();
                }
                side.setVisible(false);
                split.setDividerSize(0);
            }
            split.revalidate();
            split.repaint();
        }
    }

    /**A way to open the NWW viewer and get the general actions, which have reference to the rest.
     *
     * @param appName
     * @return
     */
    public static ToolsPanelController openNww( String appName, int onCloseAction ) {
        try {
            if (appName == null) {
                appName = APPNAME;
            }
            if (onCloseAction < 0) {
                onCloseAction = JFrame.EXIT_ON_CLOSE;
            }

            Component nwwComponent = NwwPanel.createNwwPanel(false);
            NwwPanel wwjPanel = null;

            LayersPanelController layerPanel = null;
            ToolsPanelController toolsPanel = null;
            NwwToolsTabs toolsTabs = null;
            NwwMapModesBar modesBar = null;

            if (nwwComponent instanceof NwwPanel) {
                wwjPanel = (NwwPanel) nwwComponent;
                ((Component) wwjPanel.getWwd()).setPreferredSize(new Dimension(500, 500));
                wwjPanel.addOsmLayer();
                OpenTopoLayer openTopoLayer = new OpenTopoLayer();
                openTopoLayer.setEnabled(false);
                wwjPanel.addLayer(openTopoLayer);
//                OepnvkarteLayer oepnvLayer = new OepnvkarteLayer();
//                oepnvLayer.setEnabled(false);
//                wwjPanel.addLayer(oepnvLayer);
                EsriSatellite esriLayer = new EsriSatellite();
                esriLayer.setEnabled(false);
                wwjPanel.addLayer(esriLayer);

                layerPanel = new LayersPanelController(wwjPanel);
                toolsPanel = new ToolsPanelController(wwjPanel, layerPanel);
            }

            final JFrame nwwFrame = new JFrame();
            nwwFrame.setTitle(appName + ": map view - " + org.hortonmachine.Version.getVersion());
            GuiUtilities.setDefaultFrameIcon(nwwFrame);

            if (layerPanel != null && toolsPanel != null) {
                // resizable panels: layers | modes + map | tools, the map gets the extra space
                Dimension small = new Dimension(100, 100);
                nwwComponent.setMinimumSize(small);
                ((Component) wwjPanel.getWwd()).setMinimumSize(small);
                layerPanel.setMinimumSize(small);
                // the initial widths of the sides, only the width counts
                layerPanel.setPreferredSize(new Dimension(260, 500));
                NwwContext context = new NwwContext(wwjPanel, layerPanel, toolsPanel);
                toolsTabs = new NwwToolsTabs(context);
                for( INwwTool tool : createDefaultTools() ) {
                    toolsTabs.addTool(tool);
                }
                for( INwwLayerTool layerTool : createDefaultLayerTools() ) {
                    toolsTabs.addLayerTool(layerTool);
                }
                for( INwwLayerTool styleTool : createDefaultStyleTools() ) {
                    toolsTabs.addStyleTool(styleTool);
                }
                layerPanel.addCurrentLayerListener(toolsTabs::showLayer);
                layerPanel.setStyleHandler(toolsTabs::hasStyleTool, toolsTabs::showStyle);
                toolsTabs.setMinimumSize(small);
                toolsTabs.setPreferredSize(new Dimension(360, 500));

                modesBar = new NwwMapModesBar(context);
                for( INwwMapMode mode : createDefaultMapModes() ) {
                    modesBar.addMode(mode);
                }
                JPanel mapPanel = new JPanel(new BorderLayout());
                mapPanel.add(modesBar, BorderLayout.WEST);
                mapPanel.add(nwwComponent, BorderLayout.CENTER);
                mapPanel.setMinimumSize(small);

                JSplitPane mapToolsSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapPanel, toolsTabs);
                mapToolsSplit.setResizeWeight(1.0);
                mapToolsSplit.setContinuousLayout(true);
                mapToolsSplit.setOneTouchExpandable(true);
                mapToolsSplit.setBorder(null);
                JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, layerPanel, mapToolsSplit);
                mainSplit.setResizeWeight(0.0);
                mainSplit.setContinuousLayout(true);
                mainSplit.setOneTouchExpandable(true);
                mainSplit.setBorder(null);
                nwwFrame.getContentPane().add(mainSplit, BorderLayout.CENTER);

                SplitSide layersSide = new SplitSide(mainSplit, layerPanel);
                SplitSide toolsSide = new SplitSide(mapToolsSplit, toolsTabs);
                // the tabs come and go with the tools and the current layer
                NwwToolsTabs tabs = toolsTabs;
                toolsSide.update(tabs.getTabCount() > 0);
                tabs.addContainerListener(new ContainerAdapter(){
                    @Override
                    public void componentAdded( ContainerEvent e ) {
                        toolsSide.update(tabs.getTabCount() > 0);
                    }

                    @Override
                    public void componentRemoved( ContainerEvent e ) {
                        toolsSide.update(tabs.getTabCount() > 0);
                    }
                });

                NwwMenuBar menuBar = new NwwMenuBar(nwwFrame, toolsPanel);
                menuBar.addPanelToggle("Layers panel", layersSide::isShown, layersSide::setShown);
                menuBar.addPanelToggle("Tools panel", toolsSide::isShown, toolsSide::setShown);
                Consumer<INwwTool> toolOpener = tool -> {
                    tabs.openTool(tool);
                    // also if the user hid the panel before
                    toolsSide.setShown(true);
                };
                context.setToolOpener(toolOpener);
                menuBar.addEditAction("Clear selection", ImageCache.TRASH, () -> context.getSelection() != null,
                        context::clearSelection);
                for( INwwTool tool : tabs.getTools() ) {
                    menuBar.addToolToggle(tool, () -> tabs.isToolOpen(tool), open -> {
                        if (open) {
                            toolOpener.accept(tool);
                        } else {
                            tabs.closeTool(tool);
                        }
                    });
                }
                nwwFrame.setJMenuBar(menuBar);
            } else {
                nwwFrame.getContentPane().add(nwwComponent, BorderLayout.CENTER);
            }
            nwwFrame.setResizable(true);
            nwwFrame.setPreferredSize(new Dimension(1200, 900));
            nwwFrame.pack();
            WWUtil.alignComponent(null, nwwFrame, AVKey.CENTER);

//            final JFrame nwwFrame = new JFrame();
//            nwwFrame.setTitle(appName + ": map view");
//            nwwFrame.setIconImage(icon.getImage());
//
////            JPanel mapPanel = new JPanel(new BorderLayout());
////            mapPanel.add(nwwComponent, BorderLayout.CENTER);
//            nwwFrame.getContentPane().add(nwwComponent, BorderLayout.CENTER);
//            nwwFrame.setResizable(true);
//            nwwFrame.setPreferredSize(new Dimension(800, 800));
//            nwwFrame.pack();
//            WWUtil.alignComponent(null, nwwFrame, AVKey.CENTER);
//
//            if (wwjPanel != null) {
//                final JFrame layersFrame = new JFrame();
//                layersFrame.setTitle(appName + ": layers view");
//                layersFrame.setIconImage(icon.getImage());
//                java.awt.EventQueue.invokeLater(new Runnable(){
//
//                    public void run() {
//                        layersFrame.setVisible(true);
//                    }
//                });
//                layersFrame.getContentPane().add(layerPanel, BorderLayout.CENTER);
//                layersFrame.setResizable(true);
//                layersFrame.setPreferredSize(new Dimension(400, 500));
//                layersFrame.setLocation(0, 0);
//                layersFrame.pack();
//                final JFrame toolsFrame = new JFrame();
//                toolsFrame.setTitle(appName + ": tools view");
//                toolsFrame.setIconImage(icon.getImage());
//                java.awt.EventQueue.invokeLater(new Runnable(){
//
//                    public void run() {
//                        toolsFrame.setVisible(true);
//                    }
//                });
//                toolsFrame.getContentPane().add(toolsPanel, BorderLayout.CENTER);
//                toolsFrame.setResizable(true);
//                toolsFrame.setPreferredSize(new Dimension(400, 400));
//                toolsFrame.setLocation(0, 510);
//                toolsFrame.pack();
//                
//                toolsFrame.setDefaultCloseOperation(onCloseAction);
//                layersFrame.setDefaultCloseOperation(onCloseAction);
//            }

            nwwFrame.setDefaultCloseOperation(onCloseAction);
            if (toolsTabs != null) {
                NwwToolsTabs tabs = toolsTabs;
                NwwMapModesBar modes = modesBar;
                nwwFrame.addWindowListener(new WindowAdapter(){
                    @Override
                    public void windowClosing( WindowEvent e ) {
                        modes.close();
                        tabs.closeTools();
                    }
                });
            }

            java.awt.EventQueue.invokeLater(new Runnable(){
                public void run() {
                    nwwFrame.setVisible(true);
                }
            });
            return toolsPanel;
        } catch (Exception e) {
            Logging.logger().log(java.util.logging.Level.SEVERE, "Exception at application start", e);
        }
        return null;
    }

    public static void main( String[] args ) throws Exception {
        HMCrsRegistry.initLongitudeFirst();
        if (Configuration.isMacOS() && APPNAME != null) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APPNAME);
        }

        GuiUtilities.setDefaultLookAndFeel();

        Logger.INSTANCE.init();
        openNww(APPNAME, -1);

    }

}
