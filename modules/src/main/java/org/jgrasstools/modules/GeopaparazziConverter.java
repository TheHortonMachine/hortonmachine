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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_TAGS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doBookmarks_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doLoglines_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doLogpoints_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doMedia_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doNotes_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_inGeopaparazzi_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_outData_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.IOException;

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

import org.jgrasstools.gears.io.geopaparazzi.OmsGeopaparazziConverter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSGEOPAPARAZZICONVERTER_DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(OMSGEOPAPARAZZICONVERTER_LABEL)
@Name("_" + OMSGEOPAPARAZZICONVERTER_NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class GeopaparazziConverter extends JGTModel {

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

    @Description(OMSGEOPAPARAZZICONVERTER_doBookmarks_DESCRIPTION)
    @In
    public boolean doBookmarks = true;

    @Description(OMSGEOPAPARAZZICONVERTER_outData_DESCRIPTION)
    @UI(JGTConstants.FOLDEROUT_UI_HINT)
    @In
    public String outData = null;

    @Execute
    public void process() throws IOException {
        OmsGeopaparazziConverter geopaparazziconverter = new OmsGeopaparazziConverter();
        geopaparazziconverter.inGeopaparazzi = inGeopaparazzi;
        geopaparazziconverter.doNotes = doNotes;
        geopaparazziconverter.doLoglines = doLoglines;
        geopaparazziconverter.doLogpoints = doLogpoints;
        geopaparazziconverter.doMedia = doMedia;
        geopaparazziconverter.doBookmarks = doBookmarks;
        geopaparazziconverter.outData = outData;
        geopaparazziconverter.pm = pm;
        geopaparazziconverter.process();
    }

}
