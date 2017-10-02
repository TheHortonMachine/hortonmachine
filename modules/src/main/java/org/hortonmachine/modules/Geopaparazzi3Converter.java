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

import oms3.annotations.*;

import static org.hortonmachine.gears.i18n.GearsMessages.*;
import static org.hortonmachine.gears.io.geopaparazzi.OmsGeopaparazzi3Converter.*;

import java.io.IOException;

import org.hortonmachine.gears.io.geopaparazzi.OmsGeopaparazzi3Converter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

@Description(DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(HMConstants.MOBILE)
@Name(OMSGEOPAPARAZZICONVERTER_NAME + "_v3")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class Geopaparazzi3Converter extends HMModel {

    @Description(OMSGEOPAPARAZZICONVERTER_IN_GEOPAPARAZZI_DESCRIPTION)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inGeopaparazzi = null;

    @Description(OMSGEOPAPARAZZICONVERTER_DO_NOTES_DESCRIPTION)
    @In
    public boolean doNotes = true;

    @Description(OMSGEOPAPARAZZICONVERTER_DO_LOG_LINES_DESCRIPTION)
    @In
    public boolean doLoglines = true;

    @Description(OMSGEOPAPARAZZICONVERTER_DO_LOG_POINTS_DESCRIPTION)
    @In
    public boolean doLogpoints = false;

    @Description(OMSGEOPAPARAZZICONVERTER_DO_MEDIA_DESCRIPTION)
    @In
    public boolean doMedia = true;

    @Description(OMSGEOPAPARAZZICONVERTER_OUT_DATA_DESCRIPTION)
    @UI(HMConstants.FOLDEROUT_UI_HINT)
    @In
    public String outData = null;

    @Execute
    public void process() throws IOException {
        OmsGeopaparazzi3Converter geopaparazziconverter = new OmsGeopaparazzi3Converter();
        geopaparazziconverter.inGeopaparazzi = inGeopaparazzi;
        geopaparazziconverter.doNotes = doNotes;
        geopaparazziconverter.doLoglines = doLoglines;
        geopaparazziconverter.doLogpoints = doLogpoints;
        geopaparazziconverter.doMedia = doMedia;
        geopaparazziconverter.outData = outData;
        geopaparazziconverter.pm = pm;
        geopaparazziconverter.process();
    }

}
