/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.nww.utils.cache;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;

import javax.swing.JFrame;

import org.jgrasstools.gui.utils.GuiUtilities;

import gov.nasa.worldwind.cache.BasicDataFileStore;
import gov.nasa.worldwind.cache.FileStore;

/**
 * Utils related to data caches.
 * 
 * @author Antonello Andrea (www.hydrologis.com)
 *
 */
public class CacheUtils {

    public static void openCacheManager() {
        JFrame frame = new JFrame();
        frame.setPreferredSize(new Dimension(800, 300));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        File cacheRoot = getCacheRoot();
        DataCacheViewer viewerPanel = new DataCacheViewer(cacheRoot);
        frame.getContentPane().add(viewerPanel.getPanel(), BorderLayout.CENTER);
        frame.pack();

        // Center the application on the screen.
        GuiUtilities.centerOnScreen(frame);
        frame.setVisible(true);
    }

    public static File getCacheRoot() {
        FileStore store = new BasicDataFileStore();
        File cacheRoot = store.getWriteLocation();
        return cacheRoot;
    }

}
