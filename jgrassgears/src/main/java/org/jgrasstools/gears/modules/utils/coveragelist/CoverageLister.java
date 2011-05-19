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
package org.jgrasstools.gears.modules.utils.coveragelist;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;

import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("A module that reads rasters.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Iterator, Raster")
@Label(JGTConstants.LIST_READER)
@Status(Status.CERTIFIED)
@Name("rasterlister")
@License("General Public License Version 3 (GPLv3)")
public class CoverageLister extends JGTModel {

    @Description("The list of file from which to read rasters.")
    @UI(JGTConstants.FILESPATHLIST_UI_HINT)
    @In
    public List<String> inFiles;

    @Description("The file novalue.")
    @In
    public Double fileNovalue = -9999.0;

    @Description("The novalue wanted in the raster.")
    @In
    public Double geodataNovalue = doubleNovalue;

    @Description("The optional requested boundary north coordinate.")
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description("The optional requested boundary south coordinate.")
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description("The optional requested boundary west coordinate.")
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description("The optional requested boundary east coordinate.")
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description("The optional requested resolution in x.")
    @UI(JGTConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description("The optional requested resolution in y.")
    @UI(JGTConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description("The optional requested numer of rows.")
    @UI(JGTConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description("The optional requested numer of cols.")
    @UI(JGTConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description("All rasters matching read from the input files.")
    @Out
    public List<GridCoverage2D> outGC = null;

    @Execute
    public void process() throws Exception {

        outGC = new ArrayList<GridCoverage2D>();

        for( String file : inFiles ) {
            RasterReader reader = new RasterReader();
            reader.file = file;
            reader.fileNovalue = fileNovalue;
            reader.geodataNovalue = geodataNovalue;
            reader.pNorth = pNorth;
            reader.pSouth = pSouth;
            reader.pWest = pWest;
            reader.pEast = pEast;
            reader.pXres = pXres;
            reader.pYres = pYres;
            reader.pRows = pRows;
            reader.pCols = pCols;
            reader.process();

            outGC.add(reader.outRaster);
        }

    }

}
