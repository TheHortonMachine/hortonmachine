/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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

import static org.hortonmachine.gears.io.cog.OmsRasterToCog.AUTHORCONTACTS;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.AUTHORNAMES;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.AUTO;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.AVERAGE;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.DEFLATE;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.DESCRIPTION;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.KEYWORDS;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.LABEL;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.LICENSE;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.LZW;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.MODE;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.NAME;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.NEAREST;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.STATUS;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.doPredictor_DESCRIPTION;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.inRaster_DESCRIPTION;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.outCog_DESCRIPTION;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.pCompression_DESCRIPTION;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.pDeflateLevel_DESCRIPTION;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.pResampling_DESCRIPTION;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.pThreads_DESCRIPTION;
import static org.hortonmachine.gears.io.cog.OmsRasterToCog.pTileSize_DESCRIPTION;

import org.hortonmachine.gears.io.cog.OmsRasterToCog;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

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

@Description(DESCRIPTION)
@Author(name = AUTHORNAMES, contact = AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name("_" + NAME)
@Status(STATUS)
@License(LICENSE)
public class RasterToCog extends HMModel {
    @Description(inRaster_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(pResampling_DESCRIPTION)
    @UI("combo:" + AUTO + "," + AVERAGE + "," + NEAREST + "," + MODE)
    @In
    public String pResampling = AUTO;

    @Description(pCompression_DESCRIPTION)
    @UI("combo:" + DEFLATE + "," + LZW)
    @In
    public String pCompression = DEFLATE;

    @Description(pDeflateLevel_DESCRIPTION)
    @In
    public int pDeflateLevel = 6;

    @Description(doPredictor_DESCRIPTION)
    @In
    public boolean doPredictor = true;

    @Description(pTileSize_DESCRIPTION)
    @In
    public int pTileSize = 512;

    @Description(pThreads_DESCRIPTION)
    @In
    public int pThreads = getDefaultThreadsNum();

    @Description(outCog_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outCog;

    @Execute
    public void process() throws Exception {
        OmsRasterToCog toCog = new OmsRasterToCog();
        toCog.inRaster = inRaster;
        toCog.outCog = outCog;
        toCog.pResampling = pResampling;
        toCog.pCompression = pCompression;
        toCog.pDeflateLevel = pDeflateLevel;
        toCog.doPredictor = doPredictor;
        toCog.pTileSize = pTileSize;
        toCog.pThreads = pThreads;
        toCog.pm = pm;
        toCog.doProcess = doProcess;
        toCog.doReset = doReset;
        toCog.process();
    }
}
