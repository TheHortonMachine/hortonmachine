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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_doLegacyGrass_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_fileNovalue_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_geodataNovalue_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_pCols_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_pRows_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_pSouth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_pWest_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_pXres_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREADER_pYres_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
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
import org.geotools.geometry.GeneralEnvelope;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSRASTERREADER_DESCRIPTION)
@Author(name = OMSRASTERREADER_AUTHORNAMES, contact = OMSRASTERREADER_AUTHORCONTACTS)
@Keywords(OMSRASTERREADER_KEYWORDS)
@Label(OMSRASTERREADER_LABEL)
@Name("_" + OMSRASTERREADER_NAME)
@Status(OMSRASTERREADER_STATUS)
@License(OMSRASTERREADER_LICENSE)
public class RasterReader extends JGTModel {

    @Description(OMSRASTERREADER_file_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description(OMSRASTERREADER_fileNovalue_DESCRIPTION)
    @In
    public Double fileNovalue = -9999.0;

    @Description(OMSRASTERREADER_geodataNovalue_DESCRIPTION)
    @In
    public Double geodataNovalue = doubleNovalue;

    @Description(OMSRASTERREADER_pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSRASTERREADER_pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSRASTERREADER_pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSRASTERREADER_pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSRASTERREADER_pXres_DESCRIPTION)
    @UI(JGTConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description(OMSRASTERREADER_pYres_DESCRIPTION)
    @UI(JGTConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description(OMSRASTERREADER_pRows_DESCRIPTION)
    @UI(JGTConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSRASTERREADER_pCols_DESCRIPTION)
    @UI(JGTConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSRASTERREADER_doLegacyGrass_DESCRIPTION)
    @In
    public Boolean doLegacyGrass = false;

    @Description(OMSRASTERREADER_outRaster_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    /**
     * Flag to read only envelope (if true, the output geodata is null).
     */
    public boolean doEnvelope = false;

    /**
     * The original envelope of the coverage.
     */
    public GeneralEnvelope originalEnvelope;

    @Execute
    public void process() throws Exception {
        OmsRasterReader rasterreader = new OmsRasterReader();
        rasterreader.file = file;
        rasterreader.fileNovalue = fileNovalue;
        rasterreader.geodataNovalue = geodataNovalue;
        rasterreader.pNorth = pNorth;
        rasterreader.pSouth = pSouth;
        rasterreader.pWest = pWest;
        rasterreader.pEast = pEast;
        rasterreader.pXres = pXres;
        rasterreader.pYres = pYres;
        rasterreader.pRows = pRows;
        rasterreader.pCols = pCols;
        rasterreader.doLegacyGrass = doLegacyGrass;
        rasterreader.doEnvelope = doEnvelope;
        rasterreader.pm = pm;
        rasterreader.doProcess = doProcess;
        rasterreader.doReset = doReset;
        rasterreader.process();
        outRaster = rasterreader.outRaster;
        originalEnvelope = rasterreader.originalEnvelope;
    }
}
