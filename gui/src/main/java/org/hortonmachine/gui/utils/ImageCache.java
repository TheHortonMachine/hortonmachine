/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gui.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import org.geotools.swing.MapLayerTableCellRenderer.LayerControlItem;
import org.geotools.swing.control.JMenuStatusBarItem;
import org.geotools.swing.control.JRendererStatusBarItem;

import org.geotools.swing.action.InfoAction;
import org.geotools.swing.action.NoToolAction;
import org.geotools.swing.action.PanAction;
import org.geotools.swing.action.ResetAction;
import org.geotools.swing.action.ZoomInAction;
import org.geotools.swing.action.ZoomOutAction;
import org.hortonmachine.gui.utils.MaterialIcon.IconColor;

/**
 * A singleton cache for images.
 *
 * <p>Keys mapped in {@link #GLYPHS} are drawn as Material Symbols ({@link MaterialIcon}),
 * all others (and all keys, if the icon font is not available) are loaded from the bitmap files.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class ImageCache {
    public static final String HOME = "home.gif";
    public static final String TRASH = "trash.gif";

    public static final String CATEGORY = "category.gif";
    public static final String MODULE = "module.gif";
    public static final String MODULEEXP = "module_exp.gif";
    public static final String MODULE_TEMPLATE = "module_template.gif";
    public static final String TEMPLATE = "template.gif";
    public static final String RUN = "run_module.gif";
    public static final String RUN_TO_FILE = "run_script.gif";
    public static final String RUN_TO_SHAPEFILE = "run_to_shp.gif";
    public static final String STOP = "stop_module.gif";
    public static final String GRID = "grid_obj.gif";
    public static final String PROGRESS_STOP = "progress_stop.gif";

    public static final String NEW = "new.gif";
    public static final String OPEN = "prj_obj.gif";
    public static final String SAVE = "save_edit.gif";
    public static final String COPY = "copy_edit.gif";

    public static final String MEMORY = "memory.gif";
    public static final String DEBUG = "debug.gif";
    public static final String STEP_OVER = "stepover_co.gif";
    public static final String STEP_INTO = "stepinto_co.gif";
    public static final String STEP_OUT = "stepreturn_co.gif";
    public static final String FONT = "font.gif";
    public static final String FILE = "file.gif";
    public static final String FOLDER = "folder.gif";
    public static final String REFRESH = "refresh.gif";

    public static final String CONNECT = "connect.gif";
    public static final String CONNECT_REMOTE = "connect_remote.gif";
    public static final String DISCONNECT = "disconnect.gif";
    public static final String HISTORY_DB = "history_db.gif";
    public static final String NEW_DATABASE = "new_database.gif";
    public static final String DATABASE = "database.gif";
    public static final String TABLE_FOLDER = "table_folder.gif";

    public static final String TABLE = "table.gif";
    public static final String TABLETYPE = "tabletype.gif";
    public static final String VIEW = "view.gif";
    public static final String TABLE_SPATIAL = "table_spatial.gif";
    public static final String TABLE_SPATIAL_VIRTUAL = "table_spatial_virtual.gif";

    public static final String TABLE_COLUMN = "table_column.gif";
    public static final String TABLE_COLUMN_PRIMARYKEY = "table_column_pk.gif";
    public static final String TABLE_COLUMN_INDEX = "table_column_index.gif";
    public static final String TABLE_COLUMN_FK = "table_column_fk.gif";
    public static final String DBIMAGE = "db_image.gif";
    public static final String LOG = "log.gif";

    public static final String INFO = "information.png";
    public static final String INFO16 = "information16.png";
    public static final String PHOTO = "photo.png";
    public static final String NOTE = "note.gif";

    public static final String EXPORT = "export_wiz.gif";
    public static final String IMPORT = "import_wiz.gif";

    public static final String ZOOM = "zoom.gif";
    public static final String LIST = "list.gif";
    public static final String FOLDER_OPEN = "folder_open.gif";

    public static final String INFOTOOL_ON = "info_on.gif";
    public static final String INFOTOOL_OFF = "info_off.png";

    public static final String GLOBE = "globe.gif";
    public static final String ZOOM_TO_ALL = "zoom_to_all.png";
    public static final String ZOOM_TO_NEXT = "zoom_next.png";
    public static final String ZOOM_TO_PREVIOUS = "zoom_previous.png";
    public static final String SELECTION_MODE = "selection_mode.png";

    public static final String BROWSER = "browser.gif";

    public static final String VECTOR = "vector.png";

    public static final String GEOM_POINT = "geom_point.png";
    public static final String GEOM_LINE = "geom_line.png";
    public static final String GEOM_POLYGON = "geom_polygon.png";
    public static final String DEM = "raster_icon.png";

    public static final String TREE_OPEN = "tree_open.png";
    public static final String TREE_CLOSED = "tree_closed.png";

    public static final String SPATIALITE32 = "spatialite32.png";
    public static final String H2GIS32 = "h2gis32.png";

    public static final String GPKG32 = "gpkg32.png";
    public static final String POSTGIS32 = "postgis32.png";
    public static final String MONGO32 = "mongo32.png";
    public static final String GEOFRAME32 = "geoframe32.png";

    public static final String SETTINGS = "settings.gif";

    public static final String PALETTE = "palette.png";

    public static final String FORM_PICTURE = "form_picture.png";
    public static final String FORM_SKETCH = "form_sketch.png";
    public static final String FORM_MAP = "form_map.png";
    public static final String FORM_DATE = "form_date.png";
    public static final String FORM_TIME = "form_time.png";
    public static final String FORM_PLUS = "form_plus.png";

    public static final String HORTONMACHINE_FRAME_ICON = "hm150.png";
    public static final String GEOPAPARAZZI_ICON = "geopaparazzi_icon.png";

    public static final String RUN_SCRIPT = "run16x16.gif";
    public static final String GENERATE_SCRIPT = "generate_script.gif";
    public static final String PROCESSING_REGION = "processingregion.png";

    /*
     * Keys that only exist as Material Symbols glyph, without bitmap. If the icon font is not
     * available, they return null and the caller keeps its own icon.
     */
    public static final String MAP_POINTER = "glyph:arrow_selector_tool";
    public static final String MAP_PAN = "glyph:pan_tool";
    public static final String MAP_ZOOM_IN = "glyph:zoom_in";
    public static final String MAP_ZOOM_OUT = "glyph:zoom_out";
    public static final String MAP_INFO = "glyph:info";
    public static final String MAP_RESET = "glyph:fit_screen";
    public static final String ARROW_UP = "glyph:arrow_upward";
    public static final String ARROW_DOWN = "glyph:arrow_downward";
    public static final String MAP_LAYER_VISIBLE = "glyph:visibility";
    public static final String MAP_LAYER_HIDDEN = "glyph:visibility_off";
    public static final String MAP_LAYER_SELECTED = "glyph:check_box";
    public static final String MAP_LAYER_UNSELECTED = "glyph:check_box_outline_blank";
    public static final String MAP_RENDER_BUSY = "glyph:progress_activity";
    public static final String MAP_RENDER_IDLE = "glyph:radio_button_unchecked";

    private static boolean mapLayerTableIconsApplied = false;

    private record Glyph(char codepoint, int size, IconColor color) {
    }

    private static Glyph g( int codepoint ) {
        return new Glyph((char) codepoint, 16, IconColor.DEFAULT);
    }

    private static Glyph g( int codepoint, IconColor color ) {
        return new Glyph((char) codepoint, 16, color);
    }

    private static Glyph g( int codepoint, int size ) {
        return new Glyph((char) codepoint, size, IconColor.DEFAULT);
    }

    /**
     * Image keys drawn with Material Symbols glyphs. The codepoints are those of
     * gui/tools/material-icons/glyphs.txt, the glyph names are in the comments.
     */
    private static final Map<String, Glyph> GLYPHS = Map.ofEntries(//
            Map.entry(HOME, g(0xe9b2)), // home
            Map.entry(TRASH, g(0xe92e)), // delete
            Map.entry(CATEGORY, g(0xe72c)), // category
            Map.entry(MODULE, g(0xe87b)), // extension
            Map.entry(MODULEEXP, g(0xea4b)), // science
            Map.entry(MODULE_TEMPLATE, g(0xe745)), // edit_note
            Map.entry(TEMPLATE, g(0xe873)), // description
            Map.entry(RUN, g(0xe1c4, IconColor.RUN)), // play_circle
            Map.entry(RUN_TO_FILE, g(0xf17f, IconColor.RUN)), // file_save
            Map.entry(RUN_TO_SHAPEFILE, g(0xf8d3, IconColor.RUN)), // shape_line
            Map.entry(STOP, g(0xef71, IconColor.STOP)), // stop_circle
            Map.entry(GRID, g(0xe3ec)), // grid_on
            Map.entry(PROGRESS_STOP, g(0xe888, IconColor.STOP)), // cancel
            Map.entry(NEW, g(0xe145)), // add
            Map.entry(OPEN, g(0xe2c8)), // folder_open
            Map.entry(SAVE, g(0xe161)), // save
            Map.entry(COPY, g(0xe14d)), // content_copy
            Map.entry(MEMORY, g(0xe322)), // memory
            Map.entry(DEBUG, g(0xe868)), // bug_report
            Map.entry(STEP_OVER, g(0xf6ff)), // step_over
            Map.entry(STEP_INTO, g(0xf701)), // step_into
            Map.entry(STEP_OUT, g(0xf700)), // step_out
            Map.entry(FONT, g(0xe262)), // text_fields
            Map.entry(FILE, g(0xe66d)), // draft
            Map.entry(FOLDER, g(0xe2c7)), // folder
            Map.entry(REFRESH, g(0xe5d5)), // refresh
            Map.entry(CONNECT, g(0xe63c)), // power
            Map.entry(CONNECT_REMOTE, g(0xf15c)), // cloud
            Map.entry(DISCONNECT, g(0xe646)), // power_off
            Map.entry(HISTORY_DB, g(0xe8b3)), // history
            Map.entry(NEW_DATABASE, g(0xe03c)), // library_add
            Map.entry(DATABASE, g(0xf20e)), // database
            Map.entry(TABLE_FOLDER, g(0xf586)), // folder_data
            Map.entry(TABLE, g(0xf191)), // table
            Map.entry(TABLETYPE, g(0xf1be)), // table_view
            Map.entry(VIEW, g(0xf1c5)), // preview
            Map.entry(TABLE_SPATIAL, g(0xe55b)), // map
            Map.entry(TABLE_SPATIAL_VIRTUAL, g(0xe53b)), // layers
            Map.entry(TABLE_COLUMN, g(0xe8ec)), // view_column
            Map.entry(TABLE_COLUMN_PRIMARYKEY, g(0xe73c, IconColor.ACCENT)), // key
            Map.entry(TABLE_COLUMN_INDEX, g(0xea0b)), // bolt
            Map.entry(TABLE_COLUMN_FK, g(0xe250)), // link
            Map.entry(DBIMAGE, g(0xe3f4)), // image
            Map.entry(LOG, g(0xef42)), // article
            Map.entry(INFO, g(0xe88e, 32)), // info
            Map.entry(INFO16, g(0xe88e)), // info
            Map.entry(PHOTO, g(0xe412, 32)), // photo_camera
            Map.entry(NOTE, g(0xf1fc)), // sticky_note_2
            Map.entry(EXPORT, g(0xf09b)), // upload
            Map.entry(IMPORT, g(0xf090)), // download
            Map.entry(ZOOM, g(0xe8ff)), // zoom_in
            Map.entry(LIST, g(0xe896)), // list
            Map.entry(FOLDER_OPEN, g(0xe2c8)), // folder_open
            Map.entry(INFOTOOL_ON, g(0xe88e, IconColor.ACCENT)), // info
            Map.entry(INFOTOOL_OFF, g(0xe88e)), // info
            Map.entry(GLOBE, g(0xe80b)), // public
            Map.entry(ZOOM_TO_ALL, g(0xea10)), // fit_screen
            Map.entry(ZOOM_TO_NEXT, g(0xe5cc)), // navigate_next
            Map.entry(ZOOM_TO_PREVIOUS, g(0xe5cb)), // navigate_before
            Map.entry(SELECTION_MODE, g(0xef52)), // highlight_alt
            Map.entry(BROWSER, g(0xe2db)), // travel_explore
            Map.entry(VECTOR, g(0xf8d3)), // shape_line
            Map.entry(GEOM_POINT, g(0xe268)), // scatter_plot
            Map.entry(GEOM_LINE, g(0xebbb)), // polyline
            Map.entry(GEOM_POLYGON, g(0xeb50)), // pentagon
            Map.entry(DEM, g(0xe564)), // terrain
            Map.entry(TREE_OPEN, g(0xe5cf)), // expand_more
            Map.entry(TREE_CLOSED, g(0xe5cc)), // chevron_right
            Map.entry(SETTINGS, g(0xe8b8)), // settings
            Map.entry(PALETTE, g(0xe40a)), // palette
            Map.entry(RUN_SCRIPT, g(0xeb8e)), // terminal
            Map.entry(GENERATE_SCRIPT, g(0xe86f)), // code
            Map.entry(PROCESSING_REGION, g(0xe3c2)), // crop_free
            Map.entry(MAP_POINTER, g(0xf82f)), // arrow_selector_tool
            Map.entry(MAP_PAN, g(0xe925)), // pan_tool
            Map.entry(MAP_ZOOM_IN, g(0xe8ff)), // zoom_in
            Map.entry(MAP_ZOOM_OUT, g(0xe900)), // zoom_out
            Map.entry(MAP_INFO, g(0xe88e)), // info
            Map.entry(MAP_RESET, g(0xea10)), // fit_screen
            Map.entry(ARROW_UP, g(0xe5d8)), // arrow_upward
            Map.entry(ARROW_DOWN, g(0xe5db)), // arrow_downward
            Map.entry(MAP_LAYER_VISIBLE, g(0xe8f4)), // visibility
            Map.entry(MAP_LAYER_HIDDEN, g(0xe8f5)), // visibility_off
            Map.entry(MAP_LAYER_SELECTED, g(0xe9de)), // check_box
            Map.entry(MAP_LAYER_UNSELECTED, g(0xe835)), // check_box_outline_blank
            Map.entry(MAP_RENDER_BUSY, g(0xe9d0, IconColor.ACCENT)), // progress_activity
            Map.entry(MAP_RENDER_IDLE, g(0xe836)) // radio_button_unchecked
    );

    private static ImageCache imageCache;

    private HashMap<String, ImageIcon> imageMap = new HashMap<String, ImageIcon>();
    private Class<ImageCache> class1;

    private ImageCache() {
        class1 = ImageCache.class;
    }

    public static ImageCache getInstance() {
        if (imageCache == null) {
            imageCache = new ImageCache();
        }
        return imageCache;
    }

    /**
     * Get an image for a certain key.
     * 
     * <p>
     * <b>The only keys to be used are the static strings in this class!!</b>
     * </p>
     * 
     * @param key
     *            a file key, as for example {@link ImageCache#DATABASE_VIEW}.
     * @return the image.
     */
    public ImageIcon getImage( String key ) {
        ImageIcon image = imageMap.get(key);
        if (image == null) {
            image = createImage(key);
            imageMap.put(key, image);
        }
        return image;
    }

    public BufferedImage getBufferedImage( String key ) {
        ImageIcon icon = getImage(key);
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return bi;
    }

    private ImageIcon createImage( String key ) {
        Glyph glyph = GLYPHS.get(key);
        if (glyph != null && MaterialIcon.isAvailable()) {
            return new MaterialIcon(glyph.codepoint(), glyph.size(), glyph.color());
        }
        URL resource = class1.getResource("/org/hortonmachine/images/" + key);
        if (resource == null) {
            // glyph only key and icon font not available
            return null;
        }
        ImageIcon icon = new ImageIcon(resource);
        return icon;
    }

    /**
     * Replace the bitmap icons of the GeoTools map tool buttons in a toolbar with the matching
     * Material Symbols icons. Buttons of other actions are left untouched.
     *
     * @param toolBar the toolbar, for example of a {@link org.geotools.swing.JMapFrame}.
     */
    public static void applyMapToolIcons( JToolBar toolBar ) {
        if (toolBar == null) {
            return;
        }
        for( Component component : toolBar.getComponents() ) {
            if (component instanceof AbstractButton button) {
                String key = getMapToolKey(button.getAction());
                ImageIcon icon = key != null ? get(key, SIZE_TOOLBAR) : null;
                if (icon != null) {
                    button.setIcon(icon);
                }
            }
        }
    }

    /**
     * Show the glyph icons of the buttons of a toolbar at a larger size. The icons shared with
     * menus (through actions) are left as they are, the buttons get resized copies.
     *
     * @param toolBar the toolbar.
     * @param size the unscaled size in pixels, for example {@link #SIZE_TOOLBAR}.
     */
    public static void setToolBarIconSize( JToolBar toolBar, int size ) {
        if (toolBar == null) {
            return;
        }
        for( Component component : toolBar.getComponents() ) {
            if (component instanceof AbstractButton button && button.getIcon() instanceof MaterialIcon icon) {
                button.setIcon(icon.withSize(size));
            }
        }
    }

    /**
     * Replace the bitmaps of the GeoTools layer table (visibility, selection, style and remove
     * icons of the layers and the buttons below them) with Material Symbols icons.
     *
     * <p>The GeoTools icons are shared static instances, so their images are replaced in place,
     * once, for all the layer tables.</p>
     */
    public static synchronized void applyMapLayerTableIcons() {
        if (mapLayerTableIconsApplied || !MaterialIcon.isAvailable()) {
            return;
        }
        mapLayerTableIconsApplied = true;
        for( LayerControlItem item : LayerControlItem.values() ) {
            switch( item ) {
            case VISIBLE:
                replaceImage(item.getIcon(), MAP_LAYER_VISIBLE);
                replaceImage(item.getOffIcon(), MAP_LAYER_HIDDEN);
                break;
            case SELECTED:
                replaceImage(item.getIcon(), MAP_LAYER_SELECTED);
                replaceImage(item.getOffIcon(), MAP_LAYER_UNSELECTED);
                break;
            case STYLE:
                replaceImage(item.getIcon(), PALETTE);
                replaceImage(item.getOffIcon(), PALETTE);
                break;
            case REMOVE:
                replaceImage(item.getIcon(), TRASH);
                replaceImage(item.getOffIcon(), TRASH);
                break;
            default:
                break;
            }
        }
    }

    /**
     * Replace the bitmaps of the GeoTools map status bar (renderer busy/idle and settings menu)
     * found in a container with Material Symbols icons.
     *
     * @param container the container holding the status bar, for example a map frame's content pane.
     */
    public static void applyMapStatusBarIcons( Container container ) {
        if (container == null || !MaterialIcon.isAvailable()) {
            return;
        }
        for( Component component : container.getComponents() ) {
            if (component instanceof JRendererStatusBarItem) {
                // the icons are private, but swapped in place on every render event
                for( String[] fieldAndKey : new String[][]{{"busyIcon", MAP_RENDER_BUSY}, {"idleIcon", MAP_RENDER_IDLE}} ) {
                    try {
                        Field field = JRendererStatusBarItem.class.getDeclaredField(fieldAndKey[0]);
                        field.setAccessible(true);
                        replaceImage((Icon) field.get(component), fieldAndKey[1]);
                    } catch (Exception e) {
                        // keep the GeoTools icon
                    }
                }
                for( Component child : ((Container) component).getComponents() ) {
                    if (child instanceof JLabel label && label.getIcon() != null) {
                        label.repaint();
                    }
                }
            } else if (component instanceof JMenuStatusBarItem) {
                ImageIcon settings = get(SETTINGS);
                for( Component child : ((Container) component).getComponents() ) {
                    if (child instanceof AbstractButton button && settings != null) {
                        button.setIcon(settings);
                    }
                }
            } else if (component instanceof Container child) {
                applyMapStatusBarIcons(child);
            }
        }
    }

    private static void replaceImage( Icon target, String key ) {
        ImageIcon glyph = get(key);
        if (target instanceof ImageIcon imageIcon && glyph instanceof MaterialIcon) {
            imageIcon.setImage(glyph.getImage());
        }
    }

    private static String getMapToolKey( Action action ) {
        if (action instanceof NoToolAction) {
            return MAP_POINTER;
        } else if (action instanceof PanAction) {
            return MAP_PAN;
        } else if (action instanceof ZoomInAction) {
            return MAP_ZOOM_IN;
        } else if (action instanceof ZoomOutAction) {
            return MAP_ZOOM_OUT;
        } else if (action instanceof InfoAction) {
            return MAP_INFO;
        } else if (action instanceof ResetAction) {
            return MAP_RESET;
        }
        return null;
    }

    public static ImageIcon get( String key ) {
        return ImageCache.getInstance().getImage(key);
    }

    /** Icon size for big toolbar buttons, with the text below the icon. */
    public static final int SIZE_TOOLBAR_LARGE = 24;
    /** Icon size for compact toolbar buttons. */
    public static final int SIZE_TOOLBAR = 20;

    /**
     * Get an icon at a given size. Only glyph icons can be resized: for bitmap keys, or if the
     * icon font is not available, the normal icon is returned.
     *
     * @param key the image key.
     * @param size the unscaled size in pixels, for example {@link #SIZE_TOOLBAR}.
     * @return the icon.
     */
    public static ImageIcon get( String key, int size ) {
        Glyph glyph = GLYPHS.get(key);
        if (glyph == null || glyph.size() == size || !MaterialIcon.isAvailable()) {
            return get(key);
        }
        return ImageCache.getInstance().imageMap.computeIfAbsent(key + "@" + size,
                k -> new MaterialIcon(glyph.codepoint(), size, glyph.color()));
    }

    public static BufferedImage getBuffered( String key ) {
        return ImageCache.getInstance().getBufferedImage(key);
    }

    /**
     * Clears the internal map.
     */
    public void dispose() {
        imageMap.clear();
    }

}
