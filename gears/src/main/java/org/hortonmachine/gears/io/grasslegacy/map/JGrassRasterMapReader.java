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
package org.hortonmachine.gears.io.grasslegacy.map;

import java.io.File;
import java.io.IOException;

import org.hortonmachine.gears.io.grasslegacy.io.MapIOFactory;
import org.hortonmachine.gears.io.grasslegacy.io.MapReader;
import org.hortonmachine.gears.io.grasslegacy.utils.GrassLegacyConstans;
import org.hortonmachine.gears.io.grasslegacy.utils.Window;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;

/**
 * Creates a {@link JGrassRasterMapReader} following the builder pattern.
 * 
 * <p>
 * This class makes it easier to create a reader for GRASS rasters.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 1.1.0
 */
public class JGrassRasterMapReader {

    /**
     * Builder to create {@link JGrassRasterMapReader} through the path
     * of the Map.
     * 
     * <p>Required parameters are:
     * <ul>
     * <li>the region to read</li>
     * <li>the path to the map</li>
     * </ul>
     * </p>
     * <p>Optional parameters are:
     * <ul>
     * <li>the number to use internally instead of the map novalues</li>
     * <li>the raster map type to read (ex. {@link GrassLegacyConstans#GRASSBINARYRASTERMAP})</li>
     * <li>a monitor object</li>
     * </ul>
     * </p>
     */
    public static class BuilderFromMapPath {
        // required parameters
        private final String mapPath;
        private final Window readWindow;
        // optional parameters
        private double novalue = GrassLegacyConstans.defaultNovalue;
        private String maptype = GrassLegacyConstans.GRASSBINARYRASTERMAP;
        private IHMProgressMonitor monitor = new LogProgressMonitor();

        /**
         * Constructor for the {@link BuilderFromGeoresource} with the required parameters.
         * 
         * @param readWindow the active region to read from.
         * @param resource the {@link IGeoResource}.
         */
        public BuilderFromMapPath( Window readWindow, String mapPath ) {
            this.readWindow = readWindow;
            this.mapPath = mapPath;
        }

        /**
         * Sets the optional novalue.
         * 
         * @param novalue the novalue to be used.
         * @return the builder object to allow chaining.
         */
        public BuilderFromMapPath novalue( double novalue ) {
            this.novalue = novalue;
            return this;
        }

        /**
         * Sets the optional maptype value.
         * 
         * @param maptype the maptype to be used.
         * @return the builder object to allow chaining.
         */
        public BuilderFromMapPath maptype( String maptype ) {
            this.maptype = maptype;
            return this;
        }

        /**
         * Sets the optional monitor object.
         * 
         * @param monitor the monitor to be used.
         * @return the builder object to allow chaining.
         */
        public BuilderFromMapPath monitor( IHMProgressMonitor monitor ) {
            this.monitor = monitor;
            return this;
        }

        /**
         * Builds the {@link JGrassRasterMapReader}.
         * 
         * @return the JGrassRasterMapReader with the supplied parameters.
         */
        public JGrassRasterMapReader build() {
            return new JGrassRasterMapReader(this);
        }

    }
    /**
     * Builder to create {@link JGrassRasterMapReader} through the path
     * of the Location and the name of Mapset and Map.
     * 
     * <p>Required parameters are:
     * <ul>
     * <li>the region to read</li>
     * <li>the path to the Location</li>
     * <li>the name of the mapset</li>
     * <li>the name of the map</li>
     * </ul>
     * </p>
     * <p>Optional parameters are:
     * <ul>
     * <li>the number to use internally instead of the map novalues</li>
     * <li>the raster map type to read (ex. {@link GrassLegacyConstans#GRASSBINARYRASTERMAP})</li>
     * <li>a monitor object</li>
     * </ul>
     * </p>
     */
    public static class BuilderFromPathAndNames {
        // required parameters
        private Window readWindow = null;
        private String mapName = null;
        private String mapsetName = null;
        private String locationPath = null;
        // optional parameters
        private double novalue = GrassLegacyConstans.defaultNovalue;
        private String maptype = GrassLegacyConstans.GRASSBINARYRASTERMAP;
        private IHMProgressMonitor monitor = new LogProgressMonitor();

        /**
         * Constructor for the {@link BuilderFromGeoresource} with the required paraeters.
         * 
         * @param readWindow the active region to read from.
         * @param resource the {@link IGeoResource}.
         */
        public BuilderFromPathAndNames( Window readWindow, String mapName, String mapsetName, String locationPath ) {
            this.readWindow = readWindow;
            this.mapName = mapName;
            this.mapsetName = mapsetName;
            this.locationPath = locationPath;
        }

        /**
         * Sets the optional novalue.
         * 
         * @param novalue the novalue to be used.
         * @return the builder object to allow chaining.
         */
        public BuilderFromPathAndNames novalue( double novalue ) {
            this.novalue = novalue;
            return this;
        }

        /**
         * Sets the optional maptype value.
         * 
         * @param maptype the maptype to be used.
         * @return the builder object to allow chaining.
         */
        public BuilderFromPathAndNames maptype( String maptype ) {
            this.maptype = maptype;
            return this;
        }

        /**
         * Sets the optional monitor object.
         * 
         * @param monitor the monitor to be used.
         * @return the builder object to allow chaining.
         */
        public BuilderFromPathAndNames monitor( IHMProgressMonitor monitor ) {
            this.monitor = monitor;
            return this;
        }

        /**
         * Builds the {@link JGrassRasterMapReader}.
         * 
         * @return the JGrassRasterMapReader with the supplied parameters.
         */
        public JGrassRasterMapReader build() {
            return new JGrassRasterMapReader(this);
        }

    }

    private MapReader reader = null;
    private IHMProgressMonitor monitor = null;
    private String mapName = null;
    private String mapsetName = null;
    private String locationPath = null;
    private boolean hasMoreData = false;
    private String mapPath;
    private String fullMapPath = null;

    /**
     * Creates a jgrass raster map reader through a builder.
     * 
     * @param builder the builder.
     */
    private JGrassRasterMapReader( BuilderFromPathAndNames builder ) {
        this.monitor = builder.monitor;
        this.mapName = builder.mapName;
        this.mapsetName = builder.mapsetName;
        this.locationPath = builder.locationPath;
        reader = MapIOFactory.createGrassRasterMapReader(builder.maptype);
        reader.setParameter("novalue", (new Double(builder.novalue)));
        reader.setDataWindow(builder.readWindow);
        reader.setOutputDataObject(new double[0][0]);

        fullMapPath = locationPath + File.separator + mapsetName + File.separator + GrassLegacyConstans.CELL + File.separator
                + mapName;

    }

    /**
     * Creates a jgrass raster map reader through a builder.
     * 
     * @param builder the builder.
     */
    private JGrassRasterMapReader( BuilderFromMapPath builder ) {
        this.monitor = builder.monitor;
        this.mapPath = builder.mapPath;
        reader = MapIOFactory.createGrassRasterMapReader(builder.maptype);
        reader.setParameter("novalue", (new Double(builder.novalue)));
        reader.setDataWindow(builder.readWindow);
        reader.setOutputDataObject(new double[0][0]);
        fullMapPath = mapPath;
    }

    /**
     * <p>
     * Opens the raster map and does some first checking
     * </p>
     * 
     * @return true if everything went alright
     */
    public boolean open() {
        boolean ok;
        if (mapPath != null) {
            ok = reader.open(mapPath);
        } else if (locationPath != null && mapsetName != null && mapName != null) {
            ok = reader.open(mapName, locationPath, mapsetName);
        } else {
            return false;
        }

        return ok;
    }

    /**
     * @return true if more data are available
     */
    public boolean hasMoreData() throws IOException {
        try {
            if (reader.hasMoreData(monitor)) {
                hasMoreData = true;
            }
            return hasMoreData;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getLocalizedMessage());
        }
    }

    /**
     * @return a next unit of data from the reader
     */
    public RasterData getNextData() {
        if (hasMoreData) {
            return new JGrassRasterData((double[][]) reader.getNextData());
        }
        return null;

    }

    /**
     * This assures a range only after the data were read at least once
     * 
     * @return the range
     */
    public double[] getRange() {
        return reader.getRange();
    }

    /**
     * @return the reader that takes care of the raster reading.
     */
    public MapReader getReader() {
        return reader;
    }

    /**
     * close the reader
     */
    public void close() {
        if (reader != null) {
            reader.close();
        }
    }

    public String getFullMapPath() {
        return fullMapPath;
    }
}
