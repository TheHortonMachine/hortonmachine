/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package org.jgrasstools.nww;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.jgoodies.forms.layout.CellConstraints;

import org.jgrasstools.nww.gui.LayersPanelController;
import org.jgrasstools.nww.gui.MainPanelController;
import org.jgrasstools.nww.gui.NwwPanel;
import org.jgrasstools.nww.gui.ToolsPanelController;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;

public class ViewerApp {

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

    public static void main(String[] args) {

        String appName = "SIMPLE NWW VIEWER";
        if (Configuration.isMacOS() && appName != null) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        }

        try {

            NwwPanel wwjPanel = new NwwPanel();
            LayersPanelController layerPanel = new LayersPanelController(wwjPanel);
            ToolsPanelController toolsPanel = new ToolsPanelController(wwjPanel, layerPanel);

            final JFrame nwwFrame = new JFrame();
            nwwFrame.setTitle(appName + ": map view");
            nwwFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    nwwFrame.setVisible(true);
                }
            });
            JPanel mapPanel = new JPanel(new BorderLayout());
            mapPanel.add(wwjPanel, BorderLayout.CENTER);
            nwwFrame.getContentPane().add(mapPanel, BorderLayout.CENTER);
            nwwFrame.setResizable(true);
            nwwFrame.setPreferredSize(new Dimension(800, 800));
            nwwFrame.pack();
            WWUtil.alignComponent(null, nwwFrame, AVKey.CENTER);

            final JFrame layersFrame = new JFrame();
            layersFrame.setTitle(appName + ": layers view");
            layersFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            java.awt.EventQueue.invokeLater(new Runnable() {

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
            toolsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    toolsFrame.setVisible(true);
                }
            });
            toolsFrame.getContentPane().add(toolsPanel, BorderLayout.CENTER);
            toolsFrame.setResizable(true);
            toolsFrame.setPreferredSize(new Dimension(400, 400));
            toolsFrame.setLocation(0, 510);
            toolsFrame.pack();

        } catch (Exception e) {
            Logging.logger().log(java.util.logging.Level.SEVERE, "Exception at application start", e);
        }

    }
}
