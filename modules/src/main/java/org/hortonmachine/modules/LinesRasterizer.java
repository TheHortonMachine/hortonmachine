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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_F_CAT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_CAT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_COLS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_EAST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_NORTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_ROWS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_SOUTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_WEST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSLINESRASTERIZER_DESCRIPTION)
@Author(name = OMSLINESRASTERIZER_AUTHORNAMES, contact = OMSLINESRASTERIZER_AUTHORCONTACTS)
@Keywords(OMSLINESRASTERIZER_KEYWORDS)
@Label(OMSLINESRASTERIZER_LABEL)
@Name("_" + OMSLINESRASTERIZER_NAME)
@Status(OMSLINESRASTERIZER_STATUS)
@License(OMSLINESRASTERIZER_LICENSE)
public class LinesRasterizer extends HMModel {

    @Description(OMSLINESRASTERIZER_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector = null;

    @Description(OMSLINESRASTERIZER_F_CAT_DESCRIPTION)
    @In
    public String fCat;

    @Description(OMSLINESRASTERIZER_P_CAT_DESCRIPTION)
    @In
    public double pCat = 1.0;

    @Description(OMSLINESRASTERIZER_P_NORTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSLINESRASTERIZER_P_SOUTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSLINESRASTERIZER_P_WEST_DESCRIPTION)
    @UI(HMConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSLINESRASTERIZER_P_EAST_DESCRIPTION)
    @UI(HMConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSLINESRASTERIZER_P_ROWS_DESCRIPTION)
    @UI(HMConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSLINESRASTERIZER_P_COLS_DESCRIPTION)
    @UI(HMConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSLINESRASTERIZER_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsLinesRasterizer linesrasterizer = new OmsLinesRasterizer();
        linesrasterizer.inVector = getVector(inVector);
        linesrasterizer.fCat = fCat;
        linesrasterizer.pCat = pCat;
        linesrasterizer.pNorth = pNorth;
        linesrasterizer.pSouth = pSouth;
        linesrasterizer.pWest = pWest;
        linesrasterizer.pEast = pEast;
        linesrasterizer.pRows = pRows;
        linesrasterizer.pCols = pCols;
        linesrasterizer.pm = pm;
        linesrasterizer.doProcess = doProcess;
        linesrasterizer.doReset = doReset;
        linesrasterizer.process();
        dumpRaster(linesrasterizer.outRaster, outRaster);
    }
}
