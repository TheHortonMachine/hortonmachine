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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_OUT_GRID_GEOM_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_P_CODE_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_P_EAST_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_P_NORTH_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_P_SOUTH_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_P_WEST_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_P_X_RES_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_P_Y_RES_DESCRIPTION;
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
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OMSGRIDGEOMETRYREADER_DESCRIPTION)
@Author(name = OMSGRIDGEOMETRYREADER_AUTHORNAMES, contact = OMSGRIDGEOMETRYREADER_AUTHORCONTACTS)
@Keywords(OMSGRIDGEOMETRYREADER_KEYWORDS)
@Label(OMSGRIDGEOMETRYREADER_LABEL)
@Name(OMSGRIDGEOMETRYREADER_NAME)
@Status(OMSGRIDGEOMETRYREADER_STATUS)
@License(OMSGRIDGEOMETRYREADER_LICENSE)
public class OmsGridGeometryReader extends JGTModel {

    @Description(OMSGRIDGEOMETRYREADER_P_NORTH_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSGRIDGEOMETRYREADER_P_SOUTH_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSGRIDGEOMETRYREADER_P_WEST_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSGRIDGEOMETRYREADER_P_EAST_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSGRIDGEOMETRYREADER_P_X_RES_DESCRIPTION)
    @UI(JGTConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description(OMSGRIDGEOMETRYREADER_P_Y_RES_DESCRIPTION)
    @UI(JGTConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description(OMSGRIDGEOMETRYREADER_P_CODE_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSGRIDGEOMETRYREADER_OUT_GRID_GEOM_DESCRIPTION)
    @Out
    public GridGeometry2D outGridgeom = null;

    @Execute
    public void process() throws Exception {
        checkNull(pNorth, pSouth, pWest, pEast, pXres, pYres, pCode);

        JGTProcessingRegion region = new JGTProcessingRegion(pWest, pEast, pSouth, pNorth, pXres, pYres);
        CoordinateReferenceSystem crs = CrsUtilities.getCrsFromEpsg(pCode, null);
        outGridgeom = CoverageUtilities.gridGeometryFromRegionValues(pNorth, pSouth, pEast, pWest, region.getCols(),
                region.getRows(), crs);
    }

}
