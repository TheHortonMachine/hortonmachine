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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.rastergenerator.OmsRasterGenerator;

@Description(OmsRasterGenerator.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsRasterGenerator.KEYWORDS)
@Label(JGTConstants.RASTERPROCESSING)
@Name(OmsRasterGenerator.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class RasterGenerator extends JGTModel {

    @Description(OmsRasterGenerator.pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OmsRasterGenerator.pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OmsRasterGenerator.pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OmsRasterGenerator.pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OmsRasterGenerator.pXres_DESCRIPTION)
    @UI(JGTConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description(OmsRasterGenerator.pYres_DESCRIPTION)
    @UI(JGTConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description(OmsRasterGenerator.pXres_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OmsRasterGenerator.pValue_DESCRIPTION)
    @In
    public double pValue = 0.0;

    @Description(OmsRasterGenerator.doRandom_DESCRIPTION)
    @In
    public boolean doRandom = false;

    @Description(OmsRasterGenerator.pOffset_DESCRIPTION)
    @In
    public double pOffset = 0.0;

    @Description(OmsRasterGenerator.pScale_DESCRIPTION)
    @In
    public double pScale = 1.0;

    @Description(OmsRasterGenerator.outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsRasterGenerator omsRasterGenerator = new OmsRasterGenerator();
        omsRasterGenerator.pNorth = pNorth;
        omsRasterGenerator.pSouth = pSouth;
        omsRasterGenerator.pWest = pWest;
        omsRasterGenerator.pEast = pEast;
        omsRasterGenerator.pXres = pXres;
        omsRasterGenerator.pYres = pYres;
        omsRasterGenerator.pCode = pCode;
        omsRasterGenerator.pValue = pValue;
        omsRasterGenerator.doRandom = doRandom;
        omsRasterGenerator.pOffset = pOffset;
        omsRasterGenerator.pScale = pScale;
        omsRasterGenerator.process();

        dumpRaster(omsRasterGenerator.outRaster, outRaster);

    }

}
