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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_fCat_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_pCols_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_pMaxThreads_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_pRows_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_pSouth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_pValue_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSCANLINERASTERIZER_pWest_DESCRIPTION;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.scanline.OmsScanLineRasterizer;

@Description(OMSSCANLINERASTERIZER_DESCRIPTION)
@Documentation(OMSSCANLINERASTERIZER_DOCUMENTATION)
@Author(name = OMSSCANLINERASTERIZER_AUTHORNAMES, contact = OMSSCANLINERASTERIZER_AUTHORCONTACTS)
@Keywords(OMSSCANLINERASTERIZER_KEYWORDS)
@Label(OMSSCANLINERASTERIZER_LABEL)
@Name("_" + OMSSCANLINERASTERIZER_NAME)
@Status(OMSSCANLINERASTERIZER_STATUS)
@License(OMSSCANLINERASTERIZER_LICENSE)
public class ScanLineRasterizer extends JGTModel {

    @Description(OMSSCANLINERASTERIZER_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector = null;

    @Description(OMSSCANLINERASTERIZER_pValue_DESCRIPTION)
    @In
    public Double pValue = null;

    @Description(OMSSCANLINERASTERIZER_fCat_DESCRIPTION)
    @In
    public String fCat = null;

    @Description(OMSSCANLINERASTERIZER_pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSSCANLINERASTERIZER_pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSSCANLINERASTERIZER_pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSSCANLINERASTERIZER_pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSSCANLINERASTERIZER_pRows_DESCRIPTION)
    @UI(JGTConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSSCANLINERASTERIZER_pCols_DESCRIPTION)
    @UI(JGTConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSSCANLINERASTERIZER_pMaxThreads_DESCRIPTION)
    @In
    public Integer pMaxThreads = 4;

    @Description(OMSSCANLINERASTERIZER_outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsScanLineRasterizer scanlinerasterizer = new OmsScanLineRasterizer();
        scanlinerasterizer.inVector = getVector(inVector);
        scanlinerasterizer.pValue = pValue;
        scanlinerasterizer.fCat = fCat;
        scanlinerasterizer.pNorth = pNorth;
        scanlinerasterizer.pSouth = pSouth;
        scanlinerasterizer.pWest = pWest;
        scanlinerasterizer.pEast = pEast;
        scanlinerasterizer.pRows = pRows;
        scanlinerasterizer.pCols = pCols;
        scanlinerasterizer.pMaxThreads = pMaxThreads;
        scanlinerasterizer.pm = pm;
        scanlinerasterizer.doProcess = doProcess;
        scanlinerasterizer.doReset = doReset;
        scanlinerasterizer.process();
        dumpRaster(scanlinerasterizer.outRaster, outRaster);
    }
}
