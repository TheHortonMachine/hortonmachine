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

import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_LABEL;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_LICENSE;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_NAME;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer.OmsRasterOnVectorCutter_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterOnVectorResizer;

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

@Description(OmsRasterOnVectorCutter_DESCRIPTION)
@Documentation(OmsRasterOnVectorCutter_DOCUMENTATION)
@Author(name = OmsRasterOnVectorCutter_AUTHORNAMES, contact = OmsRasterOnVectorCutter_AUTHORCONTACTS)
@Keywords(OmsRasterOnVectorCutter_KEYWORDS)
@Label(OmsRasterOnVectorCutter_LABEL)
@Name("_" + OmsRasterOnVectorCutter_NAME)
@Status(OmsRasterOnVectorCutter_STATUS)
@License(OmsRasterOnVectorCutter_LICENSE)
public class RasterOnVectorResizer extends HMModel {

    @Description(OmsRasterOnVectorCutter_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector = null;

    @Description(OmsRasterOnVectorCutter_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OmsRasterOnVectorCutter_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;


    @Execute
    public void process() throws Exception {
        OmsRasterOnVectorResizer r = new OmsRasterOnVectorResizer();
        r.inRaster = getRaster(inRaster) ;
        r.inVector = getVector(inVector);
        r.pm = pm;
        r.doProcess = doProcess;
        r.doReset = doReset;
        r.process();
        dumpRaster(r.outRaster, outRaster);
    }
    
    public static void main( String[] args ) throws Exception {
        RasterOnVectorResizer r = new RasterOnVectorResizer();
        r.inRaster = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_small_01/brenta_net_10000.asc";
        r.inVector = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_small_01/basin_brenta_small_01.shp";
        r.outRaster = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_small/brenta_net_10000.asc";
        // r.inRaster = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_tca.asc";
        // r.inVector = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/basin_brenta.shp";
        // r.outRaster = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_medium/brenta_tca.asc";
        r.process();
    }
}


