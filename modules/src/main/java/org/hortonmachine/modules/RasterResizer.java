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

import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_LABEL;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_LICENSE;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_NAME;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.*;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer.OmsRasterResizer_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer;

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

@Description(OmsRasterResizer_DESCRIPTION)
@Documentation(OmsRasterResizer_DOCUMENTATION)
@Author(name = OmsRasterResizer_AUTHORNAMES, contact = OmsRasterResizer_AUTHORCONTACTS)
@Keywords(OmsRasterResizer_KEYWORDS)
@Label(OmsRasterResizer_LABEL)
@Name("_" + OmsRasterResizer_NAME)
@Status(OmsRasterResizer_STATUS)
@License(OmsRasterResizer_LICENSE)
public class RasterResizer extends HMModel {

    @Description(OmsRasterResizer_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector = null;

    @Description(OmsRasterResizer_IN_MASKRASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inMaskRaster;

    @Description(OmsRasterResizer_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OmsRasterResizer_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;


    @Execute
    public void process() throws Exception {
        OmsRasterResizer r = new OmsRasterResizer();
        r.inRaster = getRaster(inRaster) ;
        r.inMaskRaster = getRaster(inMaskRaster) ;
        r.inVector = getVector(inVector);
        r.pm = pm;
        r.doProcess = doProcess;
        r.doReset = doReset;
        r.process();
        dumpRaster(r.outRaster, outRaster);
    }
    
    public static void main( String[] args ) throws Exception {
        RasterResizer r = new RasterResizer();
        r.inMaskRaster = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/DEM_gura.tif";
        r.inRaster = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/evapotranspiration_toni/ET0_gura_1.tif";
        r.outRaster = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/evapotranspiration_toni/ET0_gura_onDEM.tif";
        r.process();
    }
}


