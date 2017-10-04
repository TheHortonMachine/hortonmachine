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

import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.LINE;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_KEYWORDS;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_LABEL;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_LICENSE;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_NAME;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_STATUS;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_inRaster_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_inVector_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_outMap_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_pCode_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_pCols_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_pHeight_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_pLat_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_pLon_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_pRows_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_pSpacing_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_pType_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.OMSGRIDSGENERATOR_pWidth_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.POINT;
import static org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator.POLYGON;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator;

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

@Description(OMSGRIDSGENERATOR_DESCRIPTION)
@Author(name = OMSGRIDSGENERATOR_AUTHORNAMES, contact = OMSGRIDSGENERATOR_AUTHORCONTACTS)
@Keywords(OMSGRIDSGENERATOR_KEYWORDS)
@Label(OMSGRIDSGENERATOR_LABEL)
@Name("_" + OMSGRIDSGENERATOR_NAME)
@Status(OMSGRIDSGENERATOR_STATUS)
@License(OMSGRIDSGENERATOR_LICENSE)
public class GridsGenerator extends HMModel {

    @Description(OMSGRIDSGENERATOR_inVector_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector = null;

    @Description(OMSGRIDSGENERATOR_inRaster_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster = null;

    @Description(OMSGRIDSGENERATOR_pLon_DESCRIPTION)
    @In
    public double pLon = 0.0;

    @Description(OMSGRIDSGENERATOR_pLat_DESCRIPTION)
    @In
    public double pLat = 0.0;

    @Description(OMSGRIDSGENERATOR_pWidth_DESCRIPTION)
    @In
    public double pWidth = 1.0;

    @Description(OMSGRIDSGENERATOR_pHeight_DESCRIPTION)
    @In
    public double pHeight = 1.0;

    @Description(OMSGRIDSGENERATOR_pRows_DESCRIPTION)
    @In
    public int pRows = 10;

    @Description(OMSGRIDSGENERATOR_pCols_DESCRIPTION)
    @In
    public int pCols = 10;

    @Description(OMSGRIDSGENERATOR_pSpacing_DESCRIPTION)
    @In
    public Double pSpacing = null;

    @Description(OMSGRIDSGENERATOR_pType_DESCRIPTION)
    @UI("combo: " + POLYGON + "," + LINE + "," + POINT)
    @In
    public String pType = POLYGON;

    @Description(OMSGRIDSGENERATOR_pCode_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSGRIDSGENERATOR_outMap_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsGridsGenerator gridsgenerator = new OmsGridsGenerator();
        gridsgenerator.inVector = getVector(inVector);
        gridsgenerator.inRaster = getRaster(inRaster);
        gridsgenerator.pLon = pLon;
        gridsgenerator.pLat = pLat;
        gridsgenerator.pWidth = pWidth;
        gridsgenerator.pHeight = pHeight;
        gridsgenerator.pRows = pRows;
        gridsgenerator.pCols = pCols;
        gridsgenerator.pSpacing = pSpacing;
        gridsgenerator.pType = pType;
        gridsgenerator.pCode = pCode;
        gridsgenerator.pm = pm;
        gridsgenerator.doProcess = doProcess;
        gridsgenerator.doReset = doReset;
        gridsgenerator.process();
        dumpVector(gridsgenerator.outMap, outMap);
    }

}
