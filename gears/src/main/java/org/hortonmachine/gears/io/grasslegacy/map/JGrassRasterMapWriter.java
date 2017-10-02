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
import java.util.Date;

import org.hortonmachine.gears.io.grasslegacy.io.MapIOFactory;
import org.hortonmachine.gears.io.grasslegacy.io.MapWriter;
import org.hortonmachine.gears.io.grasslegacy.io.RasterWritingFailureException;
import org.hortonmachine.gears.io.grasslegacy.utils.GrassLegacyConstans;
import org.hortonmachine.gears.io.grasslegacy.utils.Window;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * <p>
 * Facility to write JGrass maps
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public class JGrassRasterMapWriter {

    private MapWriter writer = null;
    private IHMProgressMonitor monitor = new LogProgressMonitor();
    private String mapName = null;
    private String mapsetName = null;
    private String locationPath = null;
    private String mapPath;
    private String fullMapPath = null;

    /**
     * <p>
     * Creates a jgrass raster map writer with some default values
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param _novalue the value to write as novalue
     * @param mapType the raster map type to read (ex. {@link GrassLegacyConstans#GRASSBINARYRASTERMAP})
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( Window writeWindow, String mapName, String mapsetName, String locationPath, Object novalue,
            String mapType, IHMProgressMonitor monitor ) {
        this.monitor = monitor;
        this.mapName = mapName;
        this.mapsetName = mapsetName;
        this.locationPath = locationPath;
        writer = MapIOFactory.createGrassRasterMapWriter(mapType);
        writer.setDataWindow(writeWindow);
        writer.setParameter("novalue", novalue);
        writer.setOutputDataObject(new Double(2)); // write data to
        writer.setHistoryComment("Created by JGrass in " + new Date().toString());

        fullMapPath = locationPath + File.separator + mapsetName + File.separator + GrassLegacyConstans.CELL + File.separator
                + mapName;

    }
    /**
     * <p>
     * Creates a jgrass raster map writer with some default values
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param _novalue the value to write as novalue
     * @param mapType the raster map type to read (ex. {@link GrassLegacyConstans#GRASSBINARYRASTERMAP})
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( Window writeWindow, String mapPath, Object novalue, String mapType, IHMProgressMonitor monitor ) {
        this.monitor = monitor;
        this.mapPath = mapPath;
        writer = MapIOFactory.createGrassRasterMapWriter(mapType);
        writer.setDataWindow(writeWindow);
        writer.setParameter("novalue", novalue);
        writer.setOutputDataObject(new Double(2)); // write data to
        writer.setHistoryComment("Created by JGrass in " + new Date().toString());

        fullMapPath = mapPath;

    }

    /**
     * <p>
     * Creates a jgrass raster map writer with some default values
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param _novalue the value to write as novalue
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( Window writeWindow, String mapName, String mapsetName, String locationPath, Object novalue,
            IHMProgressMonitor monitor ) {

        this(writeWindow, mapName, mapsetName, locationPath, novalue, GrassLegacyConstans.GRASSBINARYRASTERMAP, monitor);
    }

    /**
     * <p>
     * Creates a jgrass raster map writer with some default values
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param mapType the raster map type to read (ex. {@link GrassLegacyConstans#GRASSBINARYRASTERMAP})
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( Window writeWindow, String mapName, String mapsetName, String locationPath, String mapType,
            IHMProgressMonitor monitor ) {

        this(writeWindow, mapName, mapsetName, locationPath, GrassLegacyConstans.defaultNovalue, mapType, monitor);
    }

    /**
     * <p>
     * Creates a jgrass raster map reader with some default values (data are read as double values,
     * novalue is default {@link GrassLegacyConstans#defaultNovalue}).
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( Window writeWindow, String mapName, String mapsetName, String locationPath,
            IHMProgressMonitor monitor ) {

        this(writeWindow, mapName, mapsetName, locationPath, GrassLegacyConstans.defaultNovalue, GrassLegacyConstans.GRASSBINARYRASTERMAP,
                monitor);
    }

    /**
     * <p>
     * Opens the raster map and does some first checking
     * </p>
     * 
     * @return true if everything went alright
     */
    public boolean open() throws RasterWritingFailureException {
        boolean ok;
        if (mapPath != null) {
            ok = writer.open(mapPath);
        } else if (locationPath != null && mapsetName != null && mapName != null) {
            ok = writer.open(mapName, locationPath, mapsetName);
        } else {
            return false;
        }

        return ok;
    }

    /**
     * <p>
     * Write the rasterData to disk.
     * </p>
     * 
     * @param rasterData
     * @return true if everything went well
     * @throws Exception
     */
    public boolean write( RasterData rasterData ) throws RasterWritingFailureException {
        try {
            return writer.write(rasterData);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RasterWritingFailureException(e.getLocalizedMessage());
        }

    }

    public void close() {
        writer.close();
    }

    public String getFullMapPath() {
        return fullMapPath;
    }

    public void cloneColorTableFromReader( JGrassRasterMapReader jgReader ) throws IOException {
        // start
        String readerMapPath = jgReader.getFullMapPath();
        String tmpMapName = new File(readerMapPath).getName();
        File mapsetFile = new File(readerMapPath).getParentFile().getParentFile();
        String colorFilePath = mapsetFile.getAbsolutePath() + File.separator + GrassLegacyConstans.COLR + File.separator + tmpMapName;
        // destination
        String destMapName = new File(fullMapPath).getName();
        File destMapsetFile = new File(fullMapPath).getParentFile().getParentFile();
        String destColorFilePath = destMapsetFile.getAbsolutePath() + File.separator + GrassLegacyConstans.COLR + File.separator
                + destMapName;

        // copy it over
        FileUtilities.copyFile(colorFilePath, destColorFilePath);
    }

    public void cloneCategoriesFromReader( JGrassRasterMapReader jgReader ) throws IOException {
        // start
        String readerMapPath = jgReader.getFullMapPath();
        String tmpMapName = new File(readerMapPath).getName();
        File mapsetFile = new File(readerMapPath).getParentFile().getParentFile();
        String catsFilePath = mapsetFile.getAbsolutePath() + File.separator + GrassLegacyConstans.CATS + File.separator + tmpMapName;
        // destination
        String destMapName = new File(fullMapPath).getName();
        File destMapsetFile = new File(fullMapPath).getParentFile().getParentFile();
        String destCatsFilePath = destMapsetFile.getAbsolutePath() + File.separator + GrassLegacyConstans.CATS + File.separator
                + destMapName;

        // copy it over
        FileUtilities.copyFile(catsFilePath, destCatsFilePath);
    }

}
