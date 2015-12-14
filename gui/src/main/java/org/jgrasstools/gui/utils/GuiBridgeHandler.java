package org.jgrasstools.gui.utils;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * A class to help to bridge with extenral softwares.
 * 
 * <p>Implementing apps need to create the wrapper for this bridge</p>.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface GuiBridgeHandler {

    String SPATIAL_TOOLBOX_PREFERENCES_KEY = "SPATIAL_TOOLBOX_PREFERENCES";
    String HEAP_KEY = "jgt_prefs_heap";
    String DEBUG_KEY = "jgt_prefs_debug";
    
    String PREFS_NODE_NAME = "/org/jgrasstools/gui";

    /**
     * Open a directory selection dialog.
     * 
     * @param title
     * @param initialPath
     * @return
     */
    public File[] showOpenDirectoryDialog( String title, File initialPath );

    /**
     * Open an open file dialog.
     * 
     * @param title
     * @param initialPath
     * @return
     */
    public File[] showOpenFileDialog( String title, File initialPath );

    /**
     * Open a save file dialog.
     * 
     * @param title
     * @param initialPath
     * @return
     */
    public File[] showSaveFileDialog( String title, File initialPath );

    public void messageDialog( String message, String title, int messageType );

    public void messageDialog( final String message, final String messageArgs[], final String title, final int messageType );

    /**
     * A check to see if this handler supports a map context.
     * 
     *  <p>This also defines if conversion between screen and world coordinates
     *  and prompting for a crs are supported.</p>
     * 
     * @return <code>true</code> if a mapcontext is supported.
     */
    public boolean supportsMapContext();

    /**
     * Get the world {@link Point2D} from and screen pixel x/y (ex. coming from a mouse event).
     * 
     * @param x the screen X.
     * @param y the screen Y.
     * @return the world position or <code>null</code>.
     */
    public Point2D getWorldPoint( int x, int y );

    /**
     * Open a dialog to prompt for a CRS.
     * 
     * @return the selected epsg code or <code>null</code>.
     */
    public String promptForCrs();

    /**
     * Get the map of user preferences.
     * 
     * @return the {@link HashMap} of preferences.
     */
    public HashMap<String, String> getSpatialToolboxPreferencesMap();

    /**
     * Save SpatialToolbox preferences map.
     * 
     * @param prefsMap
     */
    public void setSpatialToolboxPreferencesMap( HashMap<String, String> prefsMap );

    /**
     * Get the folder inside which the libraries to browse are contained. 
     * 
     * @return the file to the libraries folder.
     */
    public File getLibsFolder();

    /**
     * @param libsFolder
     */
    public void setLibsFolder( File libsFolder );

    /**
     * Show a {@link JComponent} inside a window.
     * 
     * @param component the component to show.
     * @param windowTitle
     */
    public JFrame showWindow( JComponent component, String windowTitle );

}
