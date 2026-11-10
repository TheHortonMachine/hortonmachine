/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package org.hortonmachine.nww;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.nww.gui.LayersPanelController;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.ToolsPanelController;
import org.hortonmachine.nww.layers.defaults.raster.EsriSatellite;
import org.hortonmachine.nww.layers.defaults.raster.OepnvkarteLayer;
import org.hortonmachine.nww.layers.defaults.raster.OpenTopoLayer;
import org.hortonmachine.nww.tools.INwwTool;
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
     * @return new instances of the tools docked in the tabs at the side of the map, each viewer
     *          gets its own.
     */
    public static List<INwwTool> createDefaultTools() {
        List<INwwTool> tools = new ArrayList<>();
        return tools;
    }

    /**A way to open the NWW viewer and get the tools panel, which has reference to the rest.
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
                // resizable panels: layers | map | tools, the map gets the extra space
                Dimension small = new Dimension(100, 100);
                nwwComponent.setMinimumSize(small);
                ((Component) wwjPanel.getWwd()).setMinimumSize(small);
                layerPanel.setMinimumSize(small);
                JScrollPane toolsScroll = new JScrollPane(toolsPanel);
                toolsScroll.setBorder(BorderFactory.createEmptyBorder());
                toolsScroll.getVerticalScrollBar().setUnitIncrement(16);
                NwwContext context = new NwwContext(wwjPanel, layerPanel, toolsPanel);
                toolsTabs = new NwwToolsTabs(toolsScroll, context);
                for( INwwTool tool : createDefaultTools() ) {
                    toolsTabs.addTool(tool);
                }
                toolsTabs.setMinimumSize(small);

                JSplitPane mapToolsSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nwwComponent, toolsTabs);
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
                nwwFrame.addWindowListener(new WindowAdapter(){
                    @Override
                    public void windowClosing( WindowEvent e ) {
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
        if (Configuration.isMacOS() && APPNAME != null) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APPNAME);
        }

        GuiUtilities.setDefaultLookAndFeel();

        Logger.INSTANCE.init();
        openNww(APPNAME, -1);

    }

}
