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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERVALUEROUNDER_pPattern_DESCRIPTION;
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
import org.jgrasstools.gears.modules.r.rastervaluerounder.OmsRasterValueRounder;

@Description(OMSRASTERVALUEROUNDER_DESCRIPTION)
@Documentation(OMSRASTERVALUEROUNDER_DOCUMENTATION)
@Author(name = OMSRASTERVALUEROUNDER_AUTHORNAMES, contact = OMSRASTERVALUEROUNDER_AUTHORCONTACTS)
@Keywords(OMSRASTERVALUEROUNDER_KEYWORDS)
@Label(OMSRASTERVALUEROUNDER_LABEL)
@Name("_" + OMSRASTERVALUEROUNDER_NAME)
@Status(OMSRASTERVALUEROUNDER_STATUS)
@License(OMSRASTERVALUEROUNDER_LICENSE)
public class RasterValueRounder extends JGTModel {

    @Description(OMSRASTERVALUEROUNDER_inRaster_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster;

    @Description(OMSRASTERVALUEROUNDER_pPattern_DESCRIPTION)
    @In
    public String pPattern = null;

    @Description(OMSRASTERVALUEROUNDER_outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsRasterValueRounder rastervaluerounder = new OmsRasterValueRounder();
        rastervaluerounder.inRaster = getRaster(inRaster);
        rastervaluerounder.pPattern = pPattern;
        rastervaluerounder.pm = pm;
        rastervaluerounder.doProcess = doProcess;
        rastervaluerounder.doReset = doReset;
        rastervaluerounder.process();
        dumpRaster(rastervaluerounder.outRaster, outRaster);
    }
}
