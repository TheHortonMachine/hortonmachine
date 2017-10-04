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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_F_DEPTH_LINES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_F_DEPTH_POLYGONS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_IN_CARVE_R_LINES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_IN_CARVE_R_POLYGONS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_P_DEPTH_LINES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_P_DEPTH_POLYGONS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.carver.OmsCarver;

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

@Description(OMSCARVER_DESCRIPTION)
@Author(name = OMSCARVER_AUTHORNAMES, contact = OMSCARVER_AUTHORCONTACTS)
@Keywords(OMSCARVER_KEYWORDS)
@Label(OMSCARVER_LABEL)
@Name("_" + OMSCARVER_NAME)
@Status(OMSCARVER_STATUS)
@License(OMSCARVER_LICENSE)
public class Carver extends HMModel {

    @Description(OMSCARVER_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster = null;

    @Description(OMSCARVER_IN_CARVE_R_POLYGONS_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inCarverPolygons = null;

    @Description(OMSCARVER_IN_CARVE_R_LINES_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inCarverLines = null;

    @Description(OMSCARVER_F_DEPTH_LINES_DESCRIPTION)
    @In
    public String fDepthLines = null;

    @Description(OMSCARVER_P_DEPTH_LINES_DESCRIPTION)
    @In
    public double pDepthLines = 6.0;

    @Description(OMSCARVER_F_DEPTH_POLYGONS_DESCRIPTION)
    @In
    public String fDepthPolygons = null;

    @Description(OMSCARVER_P_DEPTH_POLYGONS_DESCRIPTION)
    @In
    public double pDepthPolygons = 6.0;

    @Description(OMSCARVER_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        OmsCarver carver = new OmsCarver();
        carver.pm = pm;
        carver.inRaster = getRaster(inRaster);
        carver.inCarverPolygons = getVector(inCarverPolygons);
        carver.inCarverLines = getVector(inCarverLines);
        carver.fDepthLines = fDepthLines;
        carver.pDepthLines = pDepthLines;
        carver.fDepthPolygons = fDepthPolygons;
        carver.pDepthPolygons = pDepthPolygons;
        carver.process();
        dumpRaster(carver.outRaster, outRaster);
    }

}
