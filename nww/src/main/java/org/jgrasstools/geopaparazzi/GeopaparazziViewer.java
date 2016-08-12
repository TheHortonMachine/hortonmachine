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
package org.jgrasstools.geopaparazzi;

import java.awt.Dimension;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextPane;

import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsLog;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.Image;
import org.jgrasstools.gui.utils.DefaultGuiBridgeImpl;
import org.jgrasstools.gui.utils.GuiBridgeHandler;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The spatialtoolbox view controller.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GeopaparazziViewer extends GeopaparazziController {
    private static final Logger logger = LoggerFactory.getLogger(GeopaparazziController.class);
    private static final long serialVersionUID = 1L;

    public GeopaparazziViewer( GuiBridgeHandler guiBridge ) {
        super(guiBridge);
    }

    @Override
    protected void setViewQueryButton( JButton _viewQueryButton, Dimension preferredButtonSize, JTextPane sqlEditorArea ) {
        // TODO Auto-generated method stub

    }

    @Override
    protected List<Action> makeColumnActions( GpsLog selectedLog ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<Action> makeDatabaseAction( ProjectInfo project ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<Action> makeTableAction( Image selectedImage ) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void main( String[] args ) throws Exception {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final GeopaparazziViewer controller = new GeopaparazziViewer(gBridge);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "Geopaparazzi Projects Viewer");

        Class<GeopaparazziViewer> class1 = GeopaparazziViewer.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/jgrasstools/images/geopaparazzi_icon.png"));
        frame.setIconImage(icon.getImage());

        GuiUtilities.addClosingListener(frame, controller);
    }
}
