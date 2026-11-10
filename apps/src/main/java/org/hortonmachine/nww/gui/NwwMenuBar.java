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
package org.hortonmachine.nww.gui;

import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.utils.EGlobeModes;
import org.hortonmachine.nww.utils.cache.CacheUtils;

/**
 * The menu bar of the viewer, with the general actions of the {@link ToolsPanelController}.
 *
 * <p>The state of the check and radio items is read again each time their menu opens, since it
 * can change from elsewhere, as the globe mode switched to 3D terrain by a terrain DEM.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NwwMenuBar extends JMenuBar {
    private static final long serialVersionUID = 1L;
    private static final String RESET_TERRAIN = "Reset to Copernicus terrain";

    private final ToolsPanelController tools;
    private final JMenu viewMenu;
    /** The index in the view menu where the next panel toggle goes. */
    private int panelTogglesIndex = -1;

    /**
     * @param window the window of the viewer, closed by the close item.
     * @param tools the general actions of the viewer.
     */
    public NwwMenuBar( Window window, ToolsPanelController tools ) {
        this.tools = tools;
        add(createFileMenu(window));
        add(createEditMenu());
        viewMenu = createViewMenu();
        add(viewMenu);
        add(createToolsMenu());
    }

    /**
     * Add a check item to the view menu, to show and hide a panel of the viewer.
     *
     * @param name the name of the panel.
     * @param isShown tells if the panel is shown.
     * @param setShown shows or hides the panel.
     */
    public void addPanelToggle( String name, BooleanSupplier isShown, Consumer<Boolean> setShown ) {
        if (panelTogglesIndex < 0) {
            viewMenu.addSeparator();
            panelTogglesIndex = viewMenu.getMenuComponentCount();
        }
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
        item.addActionListener(e -> setShown.accept(item.isSelected()));
        viewMenu.insert(item, panelTogglesIndex++);
        viewMenu.addMenuListener(onSelected(() -> item.setSelected(isShown.getAsBoolean())));
    }

    private JMenu createFileMenu( Window window ) {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem open = item("Open...", ImageCache.OPEN, e -> tools.openFiles());
        open.setToolTipText("Open files and folders of the supported formats as layers");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        menu.add(open);
        JCheckBoxMenuItem rasterized = new JCheckBoxMenuItem("Open vectors rasterized");
        rasterized.setToolTipText("Draw the vectors opened from now on as image tiles, instead of shapes");
        rasterized.addActionListener(e -> tools.getLayerLoader().setPreferRasterized(rasterized.isSelected()));
        menu.add(rasterized);
        menu.addSeparator();

        JMenuItem fly = item("Fly along vector...", null, e -> tools.chooseVectorToFlyAlong());
        fly.setToolTipText("Fly along the geometries of a shapefile or geopackage table");
        menu.add(fly);
        menu.addSeparator();

        menu.add(item("Close", null, e -> window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING))));

        menu.addMenuListener(onSelected(() -> rasterized.setSelected(tools.getLayerLoader().isPreferRasterized())));
        return menu;
    }

    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        JMenuItem pasteWkt = item("Paste WKT geometry", ImageCache.COPY, e -> tools.pasteWkt());
        pasteWkt.setToolTipText("Add the WKT geometry in the clipboard, in WGS84 longitude/latitude, as a new layer");
        menu.add(pasteWkt);
        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_V);

        JMenu globeMenu = new JMenu("Globe");
        globeMenu.setIcon(icon(ImageCache.GLOBE));
        ButtonGroup globeGroup = new ButtonGroup();
        Map<EGlobeModes, JRadioButtonMenuItem> globeItems = new EnumMap<>(EGlobeModes.class);
        for( String description : EGlobeModes.getModesDescriptions() ) {
            EGlobeModes mode = EGlobeModes.getModeFromDescription(description);
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(description);
            item.addActionListener(e -> tools.setGlobeMode(mode));
            globeGroup.add(item);
            globeItems.put(mode, item);
            globeMenu.add(item);
        }
        menu.add(globeMenu);

        JMenu terrainMenu = new JMenu("Terrain");
        terrainMenu.setIcon(icon(ImageCache.DEM));
        JMenuItem terrainDem = new JMenuItem("Use a DEM as terrain...");
        terrainDem.setToolTipText("Use a DEM as the 3D terrain of its area, instead of the Copernicus one");
        terrainDem.addActionListener(e -> tools.chooseTerrainDem());
        terrainMenu.add(terrainDem);
        JMenuItem resetTerrain = new JMenuItem(RESET_TERRAIN);
        resetTerrain.addActionListener(e -> tools.resetTerrain());
        terrainMenu.add(resetTerrain);
        menu.add(terrainMenu);
        menu.addSeparator();

        JCheckBoxMenuItem white = new JCheckBoxMenuItem("White background");
        white.setToolTipText("Show a white layer below all the others");
        white.addActionListener(e -> tools.setWhiteBackground(white.isSelected()));
        menu.add(white);
        JCheckBoxMenuItem opaque = new JCheckBoxMenuItem("Opaque background");
        opaque.addActionListener(e -> tools.setOpaqueBackground(opaque.isSelected()));
        menu.add(opaque);

        menu.addMenuListener(onSelected(() -> {
            JRadioButtonMenuItem globeItem = globeItems.get(tools.getGlobeMode());
            if (globeItem != null) {
                globeItem.setSelected(true);
            }
            boolean loading = tools.isTerrainDemLoading();
            terrainDem.setEnabled(!loading);
            terrainDem.setText(loading ? "Reading the DEM..." : "Use a DEM as terrain...");
            File demFile = tools.getTerrainDemFile();
            resetTerrain.setEnabled(demFile != null && !loading);
            resetTerrain.setText(demFile == null ? RESET_TERRAIN
                    : RESET_TERRAIN + " (now " + FileUtilities.getNameWithoutExtention(demFile) + ")");
            white.setSelected(tools.isWhiteBackground());
            opaque.setSelected(tools.isOpaqueBackground());
        }));
        return menu;
    }

    private JMenu createToolsMenu() {
        JMenu menu = new JMenu("Tools");
        menu.setMnemonic(KeyEvent.VK_T);
        menu.add(item("Add annotation...", ImageCache.NOTE, e -> tools.addAnnotation()));
        JMenuItem cache = item("Cache manager...", ImageCache.MEMORY, e -> CacheUtils.openCacheManager());
        cache.setToolTipText("See and clear the cache of the downloaded map tiles and elevations");
        menu.add(cache);
        return menu;
    }

    private static JMenuItem item( String text, String iconKey, ActionListener listener ) {
        JMenuItem item = new JMenuItem(text);
        if (iconKey != null) {
            item.setIcon(icon(iconKey));
        }
        item.addActionListener(listener);
        return item;
    }

    private static ImageIcon icon( String key ) {
        return ImageCache.get(key);
    }

    private static MenuListener onSelected( Runnable onSelected ) {
        return new MenuListener(){
            @Override
            public void menuSelected( MenuEvent e ) {
                onSelected.run();
            }

            @Override
            public void menuDeselected( MenuEvent e ) {
            }

            @Override
            public void menuCanceled( MenuEvent e ) {
            }
        };
    }
}
