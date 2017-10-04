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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DO_ERODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DO_POLYGON_BORDER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DO_USE_ONLY_INTERNAL_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_F_ELEVATION_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_IN_AREA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_IN_ELEVATIONS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_P_MAX_BUFFER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.bobthebuilder.OmsBobTheBuilder;

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

@Description(OMSBOBTHEBUILDER_DESCRIPTION)
@Author(name = OMSBOBTHEBUILDER_AUTHORNAMES, contact = OMSBOBTHEBUILDER_AUTHORCONTACTS)
@Keywords(OMSBOBTHEBUILDER_KEYWORDS)
@Label(OMSBOBTHEBUILDER_LABEL)
@Name("_" + OMSBOBTHEBUILDER_NAME)
@Status(OMSBOBTHEBUILDER_STATUS)
@License(OMSBOBTHEBUILDER_LICENSE)
public class BobTheBuilder extends HMModel {

    @Description(OMSBOBTHEBUILDER_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster = null;

    @Description(OMSBOBTHEBUILDER_IN_AREA_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inArea = null;

    @Description(OMSBOBTHEBUILDER_IN_ELEVATIONS_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inElevations = null;

    @Description(OMSBOBTHEBUILDER_P_MAX_BUFFER_DESCRIPTION)
    @In
    public double pMaxbuffer = -1;

    @Description(OMSBOBTHEBUILDER_F_ELEVATION_DESCRIPTION)
    @In
    public String fElevation = null;

    @Description(OMSBOBTHEBUILDER_DO_ERODE_DESCRIPTION)
    @In
    public boolean doErode = false;

    @Description(OMSBOBTHEBUILDER_DO_USE_ONLY_INTERNAL_DESCRIPTION)
    @In
    public boolean doUseOnlyInternal = false;

    @Description(OMSBOBTHEBUILDER_DO_POLYGON_BORDER_DESCRIPTION)
    @In
    public boolean doPolygonborder = false;

    @Description(OMSBOBTHEBUILDER_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, inArea, inElevations, fElevation);

        OmsBobTheBuilder bob = new OmsBobTheBuilder();
        bob.pm = pm;
        bob.inRaster = getRaster(inRaster);
        bob.inArea = getVector(inArea);
        bob.inElevations = getVector(inElevations);
        bob.pMaxbuffer = pMaxbuffer;
        bob.fElevation = fElevation;
        bob.doErode = doErode;
        bob.doUseOnlyInternal = doUseOnlyInternal;
        bob.doPolygonborder = doPolygonborder;
        bob.process();

        dumpRaster(bob.outRaster, outRaster);
    }
}
