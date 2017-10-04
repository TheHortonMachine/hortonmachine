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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_DO_LEGACY_GRASS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_FILE_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_GEO_DATA_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_P_COLS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_P_EAST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_P_NORTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_P_ROWS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_P_SOUTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_P_WEST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_P_X_RES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_P_Y_RES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERREADER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.GeneralEnvelope;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

@Description(OMSRASTERREADER_DESCRIPTION)
@Author(name = OMSRASTERREADER_AUTHORNAMES, contact = OMSRASTERREADER_AUTHORCONTACTS)
@Keywords(OMSRASTERREADER_KEYWORDS)
@Label(OMSRASTERREADER_LABEL)
@Name("_" + OMSRASTERREADER_NAME)
@Status(OMSRASTERREADER_STATUS)
@License(OMSRASTERREADER_LICENSE)
public class RasterReader extends HMModel {

    @Description(OMSRASTERREADER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String file = null;

    @Description(OMSRASTERREADER_FILE_NOVALUE_DESCRIPTION)
    @In
    public Double fileNovalue = -9999.0;

    @Description(OMSRASTERREADER_GEO_DATA_NOVALUE_DESCRIPTION)
    @In
    public Double geodataNovalue = doubleNovalue;

    @Description(OMSRASTERREADER_P_NORTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSRASTERREADER_P_SOUTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSRASTERREADER_P_WEST_DESCRIPTION)
    @UI(HMConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSRASTERREADER_P_EAST_DESCRIPTION)
    @UI(HMConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSRASTERREADER_P_X_RES_DESCRIPTION)
    @UI(HMConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description(OMSRASTERREADER_P_Y_RES_DESCRIPTION)
    @UI(HMConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description(OMSRASTERREADER_P_ROWS_DESCRIPTION)
    @UI(HMConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSRASTERREADER_P_COLS_DESCRIPTION)
    @UI(HMConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSRASTERREADER_DO_LEGACY_GRASS_DESCRIPTION)
    @In
    public Boolean doLegacyGrass = false;

    @Description(OMSRASTERREADER_OUT_RASTER_DESCRIPTION)
    @In
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

    /**
     * Utility method to quickly read a grid in default mode.
     * 
     * @param path the path to the file.
     * @return the read coverage.
     * @throws Exception
     */
    public static GridCoverage2D readRaster( String path ) throws Exception {
        return OmsRasterReader.readRaster(path);
    }
}
