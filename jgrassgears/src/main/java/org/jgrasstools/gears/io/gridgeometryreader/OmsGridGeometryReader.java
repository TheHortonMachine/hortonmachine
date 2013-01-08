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
package org.jgrasstools.gears.io.gridgeometryreader;

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

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.JGTProcessingRegion;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("GridGeometry reader module.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, GridGeometry, Raster, Reading")
@Label(JGTConstants.GRIDGEOMETRYREADER)
@Status(Status.CERTIFIED)
@Name("gridgeomreader")
@License("General Public License Version 3 (GPLv3)")
public class OmsGridGeometryReader extends JGTModel {
    @Description("The boundary north coordinate.")
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description("The boundary south coordinate.")
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description("The boundary west coordinate.")
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description("The boundary east coordinate.")
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description("The resolution in x.")
    @UI(JGTConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description("The resolution in y.")
    @UI(JGTConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description("The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The output GridGeometry.")
    @Out
    public GridGeometry2D outGridgeom = null;

    @Execute
    public void process() throws Exception {
        checkNull(pNorth, pSouth, pWest, pEast, pXres, pYres, pCode);

        JGTProcessingRegion region = new JGTProcessingRegion(pWest, pEast, pSouth, pNorth, pXres, pYres);
        CoordinateReferenceSystem crs = CRS.decode(pCode);
        outGridgeom = CoverageUtilities.gridGeometryFromRegionValues(pNorth, pSouth, pEast, pWest, region.getCols(),
                region.getRows(), crs);
    }

}
