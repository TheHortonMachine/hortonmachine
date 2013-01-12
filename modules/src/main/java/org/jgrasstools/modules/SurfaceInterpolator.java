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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_pCode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_pSouth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_pWest_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_pXres_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRIDGEOMETRYREADER_pYres_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_fCat_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_inMask_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_pBuffer_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_pMaxThreads_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSURFACEINTERPOLATOR_pMode_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.Variables.IDW;
import static org.jgrasstools.gears.libs.modules.Variables.TPS;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridGeometry2D;
import org.jgrasstools.gears.io.gridgeometryreader.OmsGridGeometryReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.interpolation2d.OmsSurfaceInterpolator;

@Description(OMSSURFACEINTERPOLATOR_DESCRIPTION)
@Documentation(OMSSURFACEINTERPOLATOR_DOCUMENTATION)
@Author(name = OMSSURFACEINTERPOLATOR_AUTHORNAMES, contact = OMSSURFACEINTERPOLATOR_AUTHORCONTACTS)
@Keywords(OMSSURFACEINTERPOLATOR_KEYWORDS)
@Label(OMSSURFACEINTERPOLATOR_LABEL)
@Name("_" + OMSSURFACEINTERPOLATOR_NAME)
@Status(OMSSURFACEINTERPOLATOR_STATUS)
@License(OMSSURFACEINTERPOLATOR_LICENSE)
public class SurfaceInterpolator extends JGTModel {

    @Description(OMSSURFACEINTERPOLATOR_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector;

    @Description(OMSGRIDGEOMETRYREADER_pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSGRIDGEOMETRYREADER_pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSGRIDGEOMETRYREADER_pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSGRIDGEOMETRYREADER_pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSGRIDGEOMETRYREADER_pXres_DESCRIPTION)
    @UI(JGTConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description(OMSGRIDGEOMETRYREADER_pYres_DESCRIPTION)
    @UI(JGTConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description(OMSGRIDGEOMETRYREADER_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSSURFACEINTERPOLATOR_inMask_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMask = null;

    @Description(OMSSURFACEINTERPOLATOR_fCat_DESCRIPTION)
    @In
    public String fCat;

    @Description(OMSSURFACEINTERPOLATOR_pMode_DESCRIPTION)
    @UI("combo:" + TPS + "," + IDW)
    @In
    public String pMode = "TPS";

    @Description(OMSSURFACEINTERPOLATOR_pBuffer_DESCRIPTION)
    @Unit("m")
    @In
    public double pBuffer = 4.0;

    @Description(OMSSURFACEINTERPOLATOR_pMaxThreads_DESCRIPTION)
    @In
    public int pMaxThreads = 1;

    @Description(OMSSURFACEINTERPOLATOR_outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        OmsGridGeometryReader gridgeometryreader = new OmsGridGeometryReader();
        gridgeometryreader.pNorth = pNorth;
        gridgeometryreader.pSouth = pSouth;
        gridgeometryreader.pWest = pWest;
        gridgeometryreader.pEast = pEast;
        gridgeometryreader.pXres = pXres;
        gridgeometryreader.pYres = pYres;
        gridgeometryreader.pCode = pCode;
        gridgeometryreader.pm = pm;
        gridgeometryreader.doProcess = doProcess;
        gridgeometryreader.doReset = doReset;
        gridgeometryreader.process();
        GridGeometry2D outGridgeom = gridgeometryreader.outGridgeom;

        OmsSurfaceInterpolator surfaceinterpolator = new OmsSurfaceInterpolator();
        surfaceinterpolator.inVector = getVector(inVector);
        surfaceinterpolator.inGrid = outGridgeom;
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
