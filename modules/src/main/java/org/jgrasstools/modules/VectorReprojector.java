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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_doLenient_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_doLongitudeFirst_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_pCode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_pForceCode_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.vectorreprojector.OmsVectorReprojector;

@Description(OMSVECTORREPROJECTOR_DESCRIPTION)
@Author(name = OMSVECTORREPROJECTOR_AUTHORNAMES, contact = OMSVECTORREPROJECTOR_AUTHORCONTACTS)
@Keywords(OMSVECTORREPROJECTOR_KEYWORDS)
@Label(OMSVECTORREPROJECTOR_LABEL)
@Name("_" + OMSVECTORREPROJECTOR_NAME)
@Status(OMSVECTORREPROJECTOR_STATUS)
@License(OMSVECTORREPROJECTOR_LICENSE)
public class VectorReprojector extends JGTModel {

    @Description(OMSVECTORREPROJECTOR_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector;

    @Description(OMSVECTORREPROJECTOR_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSVECTORREPROJECTOR_doLongitudeFirst_DESCRIPTION)
    @In
    public Boolean doLongitudeFirst = null;

    @Description(OMSVECTORREPROJECTOR_pForceCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pForceCode;

    @Description(OMSVECTORREPROJECTOR_doLenient_DESCRIPTION)
    @In
    public boolean doLenient = true;

    @Description(OMSVECTORREPROJECTOR_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outVector = null;

    @Execute
    public void process() throws Exception {
        OmsVectorReprojector vectorreprojector = new OmsVectorReprojector();
        vectorreprojector.inVector = getVector(inVector);
        vectorreprojector.pCode = pCode;
        vectorreprojector.doLongitudeFirst = doLongitudeFirst;
        vectorreprojector.pForceCode = pForceCode;
        vectorreprojector.doLenient = doLenient;
        vectorreprojector.pm = pm;
        vectorreprojector.doProcess = doProcess;
        vectorreprojector.doReset = doReset;
        vectorreprojector.process();
        dumpVector(vectorreprojector.outVector, outVector);
    }
}
