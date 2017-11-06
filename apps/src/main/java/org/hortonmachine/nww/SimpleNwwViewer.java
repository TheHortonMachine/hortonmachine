/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package org.hortonmachine.nww;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.nww.gui.LayersPanelController;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.ToolsPanelController;
import org.hortonmachine.nww.gui.ViewControlsLayer;

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

            Class<SimpleNwwViewer> class1 = SimpleNwwViewer.class;
            ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));

            Component nwwComponent = NwwPanel.createNwwPanel(true);
            NwwPanel wwjPanel = null;
            LayersPanelController layerPanel = null;
            ToolsPanelController toolsPanel = null;

            if (nwwComponent instanceof NwwPanel) {
                wwjPanel = (NwwPanel) nwwComponent;
                ((Component) wwjPanel.getWwd()).setPreferredSize(new Dimension(500, 500));
                wwjPanel.addOsmLayer();
                ViewControlsLayer viewControls = wwjPanel.addViewControls();
                viewControls.setScale(1.5);

                layerPanel = new LayersPanelController(wwjPanel);
                toolsPanel = new ToolsPanelController(wwjPanel, layerPanel);
            }

            final JFrame nwwFrame = new JFrame();
            nwwFrame.setTitle(appName + ": map view");
            nwwFrame.setIconImage(icon.getImage());

            nwwFrame.setDefaultCloseOperation(onCloseAction);
            java.awt.EventQueue.invokeLater(new Runnable(){

                public void run() {
                    nwwFrame.setVisible(true);
                }
            });
            JPanel mapPanel = new JPanel(new BorderLayout());
            mapPanel.add(nwwComponent, BorderLayout.CENTER);
            nwwFrame.getContentPane().add(mapPanel, BorderLayout.CENTER);
            nwwFrame.setResizable(true);
            nwwFrame.setPreferredSize(new Dimension(800, 800));
            nwwFrame.pack();
            WWUtil.alignComponent(null, nwwFrame, AVKey.CENTER);

            if (wwjPanel != null) {
                final JFrame layersFrame = new JFrame();
                layersFrame.setTitle(appName + ": layers view");
                layersFrame.setIconImage(icon.getImage());
                layersFrame.setDefaultCloseOperation(onCloseAction);
                java.awt.EventQueue.invokeLater(new Runnable(){

                    public void run() {
                        layersFrame.setVisible(true);
                    }
                });
                layersFrame.getContentPane().add(layerPanel, BorderLayout.CENTER);
                layersFrame.setResizable(true);
                layersFrame.setPreferredSize(new Dimension(400, 500));
                layersFrame.setLocation(0, 0);
                layersFrame.pack();
                final JFrame toolsFrame = new JFrame();
                toolsFrame.setTitle(appName + ": tools view");
                toolsFrame.setIconImage(icon.getImage());
                toolsFrame.setDefaultCloseOperation(onCloseAction);
                java.awt.EventQueue.invokeLater(new Runnable(){

                    public void run() {
                        toolsFrame.setVisible(true);
                    }
                });
                toolsFrame.getContentPane().add(toolsPanel, BorderLayout.CENTER);
                toolsFrame.setResizable(true);
                toolsFrame.setPreferredSize(new Dimension(400, 400));
                toolsFrame.setLocation(0, 510);
                toolsFrame.pack();
            }
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
