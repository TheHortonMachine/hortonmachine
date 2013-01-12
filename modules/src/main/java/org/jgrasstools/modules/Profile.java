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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_fLineid_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_inCoordinates_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_outFolder_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_outProfile_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.profile.OmsProfile;

@Description(OMSPROFILE_DESCRIPTION)
@Documentation(OMSPROFILE_DOCUMENTATION)
@Author(name = OMSPROFILE_AUTHORNAMES, contact = OMSPROFILE_AUTHORCONTACTS)
@Keywords(OMSPROFILE_KEYWORDS)
@Label(OMSPROFILE_LABEL)
@Name("_" + OMSPROFILE_NAME)
@Status(OMSPROFILE_STATUS)
@License(OMSPROFILE_LICENSE)
public class Profile extends JGTModel {

    @Description(OMSPROFILE_inRaster_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster;

    @Description(OMSPROFILE_inCoordinates_DESCRIPTION)
    @In
    public String inCoordinates;

    @Description(OMSPROFILE_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector;

    @Description(OMSPROFILE_fLineid_DESCRIPTION)
    @In
    public String fLineid;

    @Description(OMSPROFILE_outFolder_DESCRIPTION)
    @In
    public String outFolder;

    @Description(OMSPROFILE_outProfile_DESCRIPTION)
    @Out
    public double[][] outProfile;

    @Execute
    public void process() throws Exception {
        OmsProfile profile = new OmsProfile();
        profile.inRaster = getRaster(inRaster);
        profile.inCoordinates = inCoordinates;
        profile.inVector = getVector(inVector);
        profile.fLineid = fLineid;
        profile.outFolder = outFolder;
        profile.pm = pm;
        profile.doProcess = doProcess;
        profile.doReset = doReset;
        profile.process();
        outProfile = profile.outProfile;
    }
}
