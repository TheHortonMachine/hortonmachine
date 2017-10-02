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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_STATUS;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

@Description(OMSRASTERWRITER_DESCRIPTION)
@Author(name = OMSRASTERWRITER_AUTHORNAMES, contact = OMSRASTERWRITER_AUTHORCONTACTS)
@Keywords(OMSRASTERWRITER_KEYWORDS)
@Label(OMSRASTERWRITER_LABEL)
@Name("_" + OMSRASTERWRITER_NAME)
@Status(OMSRASTERWRITER_STATUS)
@License(OMSRASTERWRITER_LICENSE)
public class RasterWriter extends HMModel {

    @Description(OMSRASTERWRITER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster = null;

    @Description(OMSRASTERWRITER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    @Execute
    public void process() throws Exception {
        OmsRasterWriter rasterwriter = new OmsRasterWriter();
        rasterwriter.inRaster = inRaster;
        rasterwriter.file = file;
        rasterwriter.pm = pm;
        rasterwriter.doProcess = doProcess;
        rasterwriter.doReset = doReset;
        rasterwriter.process();
    }

    public static void writeRaster( String path, GridCoverage2D coverage ) throws Exception {
        OmsRasterWriter.writeRaster(path, coverage);
    }
}
