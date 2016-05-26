/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package org.jgrasstools.nww;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

import org.jgrasstools.nww.gui.MainPanelController;

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
            final JFrame frame = new JFrame();
            frame.setTitle(appName);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    frame.setVisible(true);
                }
            });

            MainPanelController mainPanel = new MainPanelController();
            frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
            frame.setResizable(true);

            //            int frameWidth = 200;
            //            int frameHeight = 100;
            //            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            //            frame.setBounds((int) screenSize.getWidth() - frameWidth, 0, frameWidth,
            //                frameHeight);
            //            frame.setVisible(true);

            frame.setPreferredSize(new Dimension(1000, 600));
            frame.pack();
            WWUtil.alignComponent(null, frame, AVKey.CENTER);

        } catch (Exception e) {
            Logging.logger().log(java.util.logging.Level.SEVERE, "Exception at application start", e);
        }

    }
}
