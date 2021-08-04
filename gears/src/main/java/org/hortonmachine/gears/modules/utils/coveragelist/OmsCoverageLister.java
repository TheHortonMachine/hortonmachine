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
package org.hortonmachine.gears.modules.utils.coveragelist;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_IN_FILES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_OUT_GC_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_P_COLS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_P_EAST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_P_NORTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_P_ROWS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_P_SOUTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_P_WEST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_P_X_RES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_P_Y_RES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCOVERAGELISTER_STATUS;

import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSCOVERAGELISTER_DESCRIPTION)
@Documentation(OMSCOVERAGELISTER_DOCUMENTATION)
@Author(name = OMSCOVERAGELISTER_AUTHORNAMES, contact = OMSCOVERAGELISTER_AUTHORCONTACTS)
@Keywords(OMSCOVERAGELISTER_KEYWORDS)
@Label(OMSCOVERAGELISTER_LABEL)
@Name(OMSCOVERAGELISTER_NAME)
@Status(OMSCOVERAGELISTER_STATUS)
@License(OMSCOVERAGELISTER_LICENSE)
public class OmsCoverageLister extends HMModel {

    @Description(OMSCOVERAGELISTER_IN_FILES_DESCRIPTION)
    @UI(HMConstants.FILESPATHLIST_UI_HINT)
    @In
    public List<String> inFiles;

    @Description(OMSCOVERAGELISTER_P_NORTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSCOVERAGELISTER_P_SOUTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSCOVERAGELISTER_P_WEST_DESCRIPTION)
    @UI(HMConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSCOVERAGELISTER_P_EAST_DESCRIPTION)
    @UI(HMConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSCOVERAGELISTER_P_X_RES_DESCRIPTION)
    @UI(HMConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description(OMSCOVERAGELISTER_P_Y_RES_DESCRIPTION)
    @UI(HMConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description(OMSCOVERAGELISTER_P_ROWS_DESCRIPTION)
    @UI(HMConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSCOVERAGELISTER_P_COLS_DESCRIPTION)
    @UI(HMConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSCOVERAGELISTER_OUT_GC_DESCRIPTION)
    @Out
    public List<GridCoverage2D> outGC = null;

    @Execute
    public void process() throws Exception {

        outGC = new ArrayList<GridCoverage2D>();

        for( String file : inFiles ) {
            OmsRasterReader reader = new OmsRasterReader();
            reader.file = file;
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
