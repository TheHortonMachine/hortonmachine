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
import oms3.annotations.Unit;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_F_CAT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_IN_GRID_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_IN_MASK_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_P_BUFFER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_P_MAX_THREADS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_P_MODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_STATUS;
import static org.hortonmachine.gears.libs.modules.Variables.IDW;
import static org.hortonmachine.gears.libs.modules.Variables.TPS;

import org.geotools.coverage.grid.GridGeometry2D;
import org.hortonmachine.gears.io.gridgeometryreader.OmsGridGeometryReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.interpolation2d.OmsSurfaceInterpolator;

@Description(OMSSURFACEINTERPOLATOR_DESCRIPTION)
@Author(name = OMSSURFACEINTERPOLATOR_AUTHORNAMES, contact = OMSSURFACEINTERPOLATOR_AUTHORCONTACTS)
@Keywords(OMSSURFACEINTERPOLATOR_KEYWORDS)
@Label(OMSSURFACEINTERPOLATOR_LABEL)
@Name("_" + OMSSURFACEINTERPOLATOR_NAME)
@Status(OMSSURFACEINTERPOLATOR_STATUS)
@License(OMSSURFACEINTERPOLATOR_LICENSE)
public class SurfaceInterpolator extends HMModel {

    @Description(OMSSURFACEINTERPOLATOR_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description(OMSSURFACEINTERPOLATOR_IN_GRID_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inGrid;

    @Description(OMSSURFACEINTERPOLATOR_IN_MASK_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inMask = null;

    @Description(OMSSURFACEINTERPOLATOR_F_CAT_DESCRIPTION)
    @In
    public String fCat;

    @Description(OMSSURFACEINTERPOLATOR_P_MODE_DESCRIPTION)
    @UI("combo:" + TPS + "," + IDW)
    @In
    public String pMode = "TPS";

    @Description(OMSSURFACEINTERPOLATOR_P_BUFFER_DESCRIPTION)
    @Unit("m")
    @In
    public double pBuffer = 4.0;

    @Description(OMSSURFACEINTERPOLATOR_P_MAX_THREADS_DESCRIPTION)
    @In
    public int pMaxThreads = getDefaultThreadsNum();

    @Description(OMSSURFACEINTERPOLATOR_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        OmsSurfaceInterpolator surfaceinterpolator = new OmsSurfaceInterpolator();
        surfaceinterpolator.inVector = getVector(inVector);
        surfaceinterpolator.inGrid = getRaster(inGrid);
        surfaceinterpolator.inMask = getRaster(inMask);
        surfaceinterpolator.fCat = fCat;
        surfaceinterpolator.pMode = pMode;
        surfaceinterpolator.pBuffer = pBuffer;
        surfaceinterpolator.pMaxThreads = pMaxThreads;
        surfaceinterpolator.pm = pm;
        surfaceinterpolator.doProcess = doProcess;
        surfaceinterpolator.doReset = doReset;
        surfaceinterpolator.process();
        dumpRaster(surfaceinterpolator.outRaster, outRaster);
    }
}
