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

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Vector;

import org.hortonmachine.gears.io.grasslegacy.map.color.ColorMapBuffer;
import org.hortonmachine.gears.io.grasslegacy.map.color.ColorTable;
import org.hortonmachine.gears.io.grasslegacy.utils.Window;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * <p>
 * A map reader object
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @author John Preston
 * @since 1.1.0
 */
public abstract class MapReader {

    public static final int RASTER_READER = 1;

    public static final int VECTOR_READER = 2;

    public static final int POINT_READER = 3;

    public static final String FEATURECOLLECTION = "featurecollection";

    public static final int COLOR = 1;

    public static final int DATA = 1;

    protected Window fileWindow = null;

    protected Window dataWindow = null;

    protected ColorTable colorTable = null;

    protected Object dataObject = null;

    protected int readerType = 0;

    /** Creates a new instance of MapReader */
    public MapReader( int _readerType ) {
        readerType = _readerType;
    }

    /**
     * Define the reader type
     * 
     * @param readerType the type, can be one of {@link MapReader#RASTER_READER},
     *        {@link MapReader#VECTOR_READER}, {@link MapReader#POINT_READER}
     */
    public void setReaderType( int readerType ) {
        this.readerType = readerType;
    }

    /**
     * @return the reader type, can be one of {@link MapReader#RASTER_READER},
     *         {@link MapReader#VECTOR_READER}, {@link MapReader#POINT_READER}
     */
    public int getReaderType() {
        return readerType;
    }

    /**
     * close the resource, deallocation whatever needed to be deallocated
     */
    public abstract void close();

    /**
     * Define the output data object. The output data object is the kind of result that the reader
     * will give. This can be for example:
     * <ul>
     * <li> when you need to elaborate the data, a double matrix, so the odo would be <b>new
     * double[][]</b></li>
     * <li> when you need to visualize the data, a floatbuffer, so the odo would be <b>new
     * {@link FloatBuffer}</b></li>
     * </ul>
     * 
     * @param outputDataObject the output data object
     */
    public void setOutputDataObject( Object outputDataObject ) {
        dataObject = outputDataObject;
        if (outputDataObject instanceof Vector) {
            if (((Vector) outputDataObject).capacity() == 0)
                dataObject = new Vector();
        }
    }

    /**
     * @return the output data object
     */
    public Object getOutputDataObject() {
        return dataObject;
    }

    /**
     * @return the colortable object
     */
    public ColorTable getColorTable() {
        return colorTable;
    }

    /**
     * @return the window that corresponds to the map file that the reader has opened.
     */
    public Window getMapWindow() {
        return new Window(fileWindow);
    }

    /**
     * @return the window that corresponds to the active window.
     */
    public Window getDataWindow() {
        return new Window(dataWindow);
    }

    /**
     * Set the active reader window, i.e. the region out of which the reader will read
     * 
     * @param window the region object
     */
    public void setDataWindow( Window window ) {
        dataWindow = window;
    }

    /**
     * utility to set particular parameters supported keys: "novalue" "matrixtype" "progressbar"
     * "commandoptions"
     * 
     * @param key
     * @param obj
     */
    public void setParameter( String key, Object obj ) {
    }

    /**
     * @param key the parameter key
     * @param obj a default return object
     * @return the parameter corrisponding to the passed key
     */
    public Object getParameter( String key, Object obj ) {
        return obj;
    }

    /**
     * Open the resource
     * 
     * @param fileName the name of the resource
     * @param locationPath the path to the GRASS location
     * @param mapsetName the name of the mapset holding the resource
     * @return true if the resource was successfully opened
     */
    public abstract boolean open( String fileName, String locationPath, String mapsetName );
    
    /**
     * Open the resource
     * 
     * @param mapPath the full path to the resource that needs to be opened
     * @return true if the resource was successfully opened
     */
    public abstract boolean open( String mapPath );

    /**
     * @param monitor progressmonitor
     * @return true, if the resource has more data to read
     * @throws Exception 
     */
    public abstract boolean hasMoreData( IHMProgressMonitor monitor ) throws Exception;

    /**
     * @return get the data object
     */
    public abstract Object getNextData();

    /**
     * @param attIndex
     * @return the data color
     */
    public abstract ColorMapBuffer getNextDataColor( int attIndex );

    /**
     * @return the string representing the legend
     * @throws IOException 
     */
    public abstract String getLegendString() throws IOException;

    /**
     * @return an array containing the min and max value of the resource
     */
    public abstract double[] getRange();
}