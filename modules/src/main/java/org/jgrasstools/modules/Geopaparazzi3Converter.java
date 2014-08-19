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

import oms3.annotations.*;
import org.jgrasstools.gears.io.geopaparazzi.OmsGeopaparazzi3Converter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

import java.io.IOException;

import static org.jgrasstools.gears.i18n.GearsMessages.*;

@Description(OmsGeopaparazzi3Converter.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(JGTConstants.MOBILE)
@Name(OMSGEOPAPARAZZICONVERTER_NAME + "_v3")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class Geopaparazzi3Converter extends JGTModel {

    @Description(OMSGEOPAPARAZZICONVERTER_inGeopaparazzi_DESCRIPTION)
    @UI(JGTConstants.FOLDERIN_UI_HINT)
    @In
    public String inGeopaparazzi = null;

    @Description(OMSGEOPAPARAZZICONVERTER_doNotes_DESCRIPTION)
    @In
    public boolean doNotes = true;

    @Description(OMSGEOPAPARAZZICONVERTER_doLoglines_DESCRIPTION)
    @In
    public boolean doLoglines = true;

    @Description(OMSGEOPAPARAZZICONVERTER_doLogpoints_DESCRIPTION)
    @In
    public boolean doLogpoints = false;

    @Description(OMSGEOPAPARAZZICONVERTER_doMedia_DESCRIPTION)
    @In
    public boolean doMedia = true;

    @Description(OMSGEOPAPARAZZICONVERTER_outData_DESCRIPTION)
    @UI(JGTConstants.FOLDEROUT_UI_HINT)
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
