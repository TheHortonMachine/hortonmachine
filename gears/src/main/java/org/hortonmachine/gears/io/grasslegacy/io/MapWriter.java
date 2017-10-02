/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.io.grasslegacy.io;

import org.hortonmachine.gears.io.grasslegacy.utils.Window;

/**
 * <p>
 * A map writer object
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public abstract class MapWriter {
    public static final int RASTER_WRITER = 1;

    public static final int VECTOR_WRITER = 2;

    public static final int POINT_WRITER = 3;

    protected Window mapWindow = null;

    protected Window dataWindow = null;

    protected String errorString = "";

    protected Object dataObject = null; // format on disk 0 = float, 1= double

    protected int writerType = 0;

    protected String historyComment = "";

    /** Creates a new instance of MapReader */
    public MapWriter( int _writerType ) {
        writerType = _writerType;
    }

    public void close() {
    }

    public void setOutputDataObject( Object _dataObject ) {
        dataObject = _dataObject;
    }

    public Object getOutputDataObject() {
        return dataObject;
    }

    /**
     * Returns the window that corresponds to the map file that the reader has opened.
     */
    public Window getMapWindow() {
        return mapWindow;
    }

    public void setDataWindow( Window window ) {
        dataWindow = window;
    }

    /**
     * utility to set particular parameters supported keys: "novalue"
     * 
     * @param key
     * @param obj
     */
    public void setParameter( String key, Object obj ) {

    }

    public void setHistoryComment( String history ) {
        historyComment = history;
    }

    public abstract boolean open( String fileName, String locationPath, String mapsetName );

    /**
     * Open the resource
     * 
     * @param mapPath the full path to the resource that needs to be opened
     * @return true if the resource was successfully opened
     */
    public abstract boolean open( String mapPath );

    public boolean write( Object data ) throws Exception {
        return false;
    }

    public String getErrorString() {
        return errorString;
    }
}
