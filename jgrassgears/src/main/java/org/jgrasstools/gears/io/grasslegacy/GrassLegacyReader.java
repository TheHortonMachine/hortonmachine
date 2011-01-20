/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.io.grasslegacy;

import java.io.File;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.grassraster.JGrassMapEnvironment;
import org.geotools.gce.grassraster.JGrassRegion;
import org.jgrasstools.gears.io.grasslegacy.io.GrassRasterReader;
import org.jgrasstools.gears.io.grasslegacy.io.MapReader;
import org.jgrasstools.gears.io.grasslegacy.utils.GrassLegacyUtilities;
import org.jgrasstools.gears.io.grasslegacy.utils.Window;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

@Description("Legacy class for reading grass data the old way.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Grass, Raster, Reading")
@Label(JGTConstants.RASTERREADER)
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class GrassLegacyReader extends JGTModel {
    @Description("The file to the map to be read (the cell file).")
    @Label(JGTConstants.FILE_LABEL)
    @In
    public String file = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("Flag that defines if the map should be read as a whole (false) or"
            + " on the active region (true and default).")
    @In
    public boolean doActive = true;

    @Description("The region to read.")
    @In
    public Window inWindow = null;

    @Description("The read output map as limited coverage version.")
    @Out
    public GridCoverage2D outGC = null;

    @Description("The read output map data.")
    @Out
    public double[][] geodata = null;

    @Execute
    public void readCoverage() throws Exception {
        if (!concatOr(geodata == null, doReset)) {
            return;
        }
        JGrassMapEnvironment mapEnvironment = new JGrassMapEnvironment(new File(file));
        JGrassRegion jGrassRegion = null;
        if (inWindow == null) {
            if (doActive) {
                jGrassRegion = mapEnvironment.getActiveRegion();
            } else {
                jGrassRegion = mapEnvironment.getFileRegion();
            }
            inWindow = new Window(jGrassRegion.getWest(), jGrassRegion.getEast(), jGrassRegion.getSouth(),
                    jGrassRegion.getNorth(), jGrassRegion.getWEResolution(), jGrassRegion.getNSResolution());
        }

        GrassRasterReader reader = new GrassRasterReader();
        try {
            reader.setReaderType(MapReader.RASTER_READER);
            reader.setOutputDataObject(new double[0][0]);
            reader.setDataWindow(inWindow);

            reader.open(mapEnvironment.getCELL().getAbsolutePath());
            if (reader.hasMoreData(pm)) {
                geodata = (double[][]) reader.getNextData();
            }
        } finally {
            reader.close();
        }
        
        CoordinateReferenceSystem crs = mapEnvironment.getCoordinateReferenceSystem();
        outGC = new GrassLegacyGridCoverage2D(inWindow, geodata, crs);
    }

    /**
     * Get a single value in a position of the raster.
     * 
     * @param coordinate the coordinate in which the value is read.
     * @return the value read in the given coordinate.
     */
    public double getValueAt( Coordinate coordinate ) {
        if (geodata == null) {
            throw new IllegalArgumentException("The data have first to be read!");
        }
        int[] coordinateToNearestRowCol = GrassLegacyUtilities.coordinateToNearestRowCol(inWindow, coordinate);
        if (coordinateToNearestRowCol != null) {
            int row = coordinateToNearestRowCol[0];
            int col = coordinateToNearestRowCol[1];
            if (row > 0 && row < geodata.length) {
                if (col > 0 && col < geodata[0].length) {
                    return geodata[row][col];
                }
            }
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Get a single value in a position of the raster.
     * 
     * <p>This opens and closes the raster every time it is called. Bad performance on many calls.
     * 
     * @param window the grid on which to base on (if <code>null</code>, the active region is picked).
     * @param coordinate the coordinate in which the value is read.
     * @param filePath the path to the map.
     * @param pm the progress monitor or null.
     * @return the value read in the given coordinate.
     * @throws Exception
     */
    public static double getValueAt( Window window, Coordinate coordinate, String filePath, IJGTProgressMonitor pm )
            throws Exception {
        JGrassMapEnvironment mapEnvironment = new JGrassMapEnvironment(new File(filePath));
        if (window == null) {
            JGrassRegion jgr = mapEnvironment.getActiveRegion();
            window = new Window(jgr.getWest(), jgr.getEast(), jgr.getSouth(), jgr.getNorth(), jgr.getWEResolution(),
                    jgr.getNSResolution());
        }
        Window rectangleAroundPoint = GrassLegacyUtilities.getRectangleAroundPoint(window, coordinate.x, coordinate.y);

        GrassLegacyReader reader = new GrassLegacyReader();
        reader.file = filePath;
        reader.inWindow = rectangleAroundPoint;
        if (pm != null)
            reader.pm = pm;
        reader.readCoverage();
        double[][] data = reader.geodata;

        if (data.length != 1 || data[0].length != 1) {
            throw new IllegalAccessException("Wrong region extracted for picking a single point.");
        }

        return data[0][0];
    }

    // public static void main( String[] args ) throws Exception {
    // // 660205.062241|5116931.07884||932.92|
    // PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
    // double value = getValueAt(null, new Coordinate(660205.062241, 5116931.07884),
    // "/home/moovida/DTM_TRENTINO/grassdb/trentino/solo/cell/dtm_all_wgs", pm);
    // System.out.println(value);
    //
    // }

}
