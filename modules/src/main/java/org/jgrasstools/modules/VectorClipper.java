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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_inClipper_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_inMap_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_outMap_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORCLIPPER_pMaxThreads_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.vectorclipper.OmsVectorClipper;

@Description(OMSVECTORCLIPPER_DESCRIPTION)
@Documentation(OMSVECTORCLIPPER_DOCUMENTATION)
@Author(name = OMSVECTORCLIPPER_AUTHORNAMES, contact = OMSVECTORCLIPPER_AUTHORCONTACTS)
@Keywords(OMSVECTORCLIPPER_KEYWORDS)
@Label(OMSVECTORCLIPPER_LABEL)
@Name("_" + OMSVECTORCLIPPER_NAME)
@Status(OMSVECTORCLIPPER_STATUS)
@License(OMSVECTORCLIPPER_LICENSE)
public class VectorClipper extends JGTModel {

    @Description(OMSVECTORCLIPPER_inMap_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap = null;

    @Description(OMSVECTORCLIPPER_inClipper_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inClipper = null;

    @Description(OMSVECTORCLIPPER_pMaxThreads_DESCRIPTION)
    @In
    public int pMaxThreads = 1;

    @Description(OMSVECTORCLIPPER_outMap_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsVectorClipper vectorclipper = new OmsVectorClipper();
        vectorclipper.inMap = getVector(inMap);
        vectorclipper.inClipper = getVector(inClipper);
        vectorclipper.pMaxThreads = pMaxThreads;
        vectorclipper.pm = pm;
        vectorclipper.doProcess = doProcess;
        vectorclipper.doReset = doReset;
        vectorclipper.process();
        dumpVector(vectorclipper.outMap, outMap);
    }
}
