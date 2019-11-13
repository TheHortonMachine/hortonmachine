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

import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_F_CAT_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_LABEL;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_LICENSE;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_NAME;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_P_COLS_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_P_EAST_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_P_NORTH_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_P_ROWS_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_P_SOUTH_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_P_USEPIP_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_P_VALUE_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_P_WEST_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.OMSSCANLINERASTERIZER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer;

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

@Description(OMSSCANLINERASTERIZER_DESCRIPTION)
@Author(name = OMSSCANLINERASTERIZER_AUTHORNAMES, contact = OMSSCANLINERASTERIZER_AUTHORCONTACTS)
@Keywords(OMSSCANLINERASTERIZER_KEYWORDS)
@Label(OMSSCANLINERASTERIZER_LABEL)
@Name("_" + OMSSCANLINERASTERIZER_NAME)
@Status(OMSSCANLINERASTERIZER_STATUS)
@License(OMSSCANLINERASTERIZER_LICENSE)
public class ScanLineRasterizer extends HMModel {

    @Description(OMSSCANLINERASTERIZER_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector = null;

    @Description(OMSSCANLINERASTERIZER_P_VALUE_DESCRIPTION)
    @In
    public Double pValue = null;

    @Description(OMSSCANLINERASTERIZER_F_CAT_DESCRIPTION)
    @In
    public String fCat = null;

    @Description(OMSSCANLINERASTERIZER_P_NORTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSSCANLINERASTERIZER_P_SOUTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSSCANLINERASTERIZER_P_WEST_DESCRIPTION)
    @UI(HMConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSSCANLINERASTERIZER_P_EAST_DESCRIPTION)
    @UI(HMConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSSCANLINERASTERIZER_P_ROWS_DESCRIPTION)
    @UI(HMConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSSCANLINERASTERIZER_P_COLS_DESCRIPTION)
    @UI(HMConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;
    
    @Description(OMSSCANLINERASTERIZER_P_USEPIP_DESCRIPTION)
    @In
    public Boolean pUsePointInPolygon = false;
    
    @Description(OMSSCANLINERASTERIZER_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OMSSCANLINERASTERIZER_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsScanLineRasterizer scanlinerasterizer = new OmsScanLineRasterizer();
        scanlinerasterizer.inVector = getVector(inVector);
        scanlinerasterizer.inRaster = getRaster(inRaster);
        scanlinerasterizer.pValue = pValue;
        scanlinerasterizer.fCat = fCat;
        scanlinerasterizer.pNorth = pNorth;
        scanlinerasterizer.pSouth = pSouth;
        scanlinerasterizer.pWest = pWest;
        scanlinerasterizer.pEast = pEast;
        scanlinerasterizer.pRows = pRows;
        scanlinerasterizer.pCols = pCols;
        scanlinerasterizer.pUsePointInPolygon = pUsePointInPolygon;
        scanlinerasterizer.pm = pm;
        scanlinerasterizer.doProcess = doProcess;
        scanlinerasterizer.doReset = doReset;
        scanlinerasterizer.process();
        dumpRaster(scanlinerasterizer.outRaster, outRaster);
    }

}
