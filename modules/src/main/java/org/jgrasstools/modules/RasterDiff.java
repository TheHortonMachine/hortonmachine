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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_inRaster1_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_inRaster2_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERDIFF_pThreshold_DESCRIPTION;
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
import org.jgrasstools.gears.modules.r.rasterdiff.OmsRasterDiff;

@Description(OMSRASTERDIFF_DESCRIPTION)
@Documentation(OMSRASTERDIFF_DOCUMENTATION)
@Author(name = OMSRASTERDIFF_AUTHORNAMES, contact = OMSRASTERDIFF_AUTHORCONTACTS)
@Keywords(OMSRASTERDIFF_KEYWORDS)
@Label(OMSRASTERDIFF_LABEL)
@Name(OMSRASTERDIFF_NAME)
@Status(OMSRASTERDIFF_STATUS)
@License(OMSRASTERDIFF_LICENSE)
public class RasterDiff extends JGTModel {

    @Description(OMSRASTERDIFF_inRaster1_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster1;

    @Description(OMSRASTERDIFF_inRaster2_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster2;

    @Description(OMSRASTERDIFF_pThreshold_DESCRIPTION)
    @In
    public Double pThreshold;

    @Description(OMSRASTERDIFF_outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsRasterDiff rasterdiff = new OmsRasterDiff();
        rasterdiff.inRaster1 = getRaster(inRaster1);
        rasterdiff.inRaster2 = getRaster(inRaster2);
        rasterdiff.pThreshold = pThreshold;
        rasterdiff.pm = pm;
        rasterdiff.doProcess = doProcess;
        rasterdiff.doReset = doReset;
        rasterdiff.process();
        dumpRaster(rasterdiff.outRaster, outRaster);
    }

}
