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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_doInverse_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_inMask_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_pMax_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCUTOUT_pMin_DESCRIPTION;
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
import org.jgrasstools.gears.modules.r.cutout.OmsCutOut;

@Description(OMSCUTOUT_DESCRIPTION)
@Author(name = OMSCUTOUT_AUTHORNAMES, contact = OMSCUTOUT_AUTHORCONTACTS)
@Keywords(OMSCUTOUT_KEYWORDS)
@Label(OMSCUTOUT_LABEL)
@Name("_" + OMSCUTOUT_NAME)
@Status(OMSCUTOUT_STATUS)
@License(OMSCUTOUT_LICENSE)
public class CutOut extends JGTModel {

    @Description(OMSCUTOUT_inRaster_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster;

    @Description(OMSCUTOUT_inMask_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMask;

    @Description(OMSCUTOUT_pMax_DESCRIPTION)
    @In
    public Double pMax;

    @Description(OMSCUTOUT_pMin_DESCRIPTION)
    @In
    public Double pMin;

    @Description(OMSCUTOUT_doInverse_DESCRIPTION)
    @In
    public boolean doInverse = false;

    @Description(OMSCUTOUT_outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        OmsCutOut c = new OmsCutOut();
        c.pm = pm;
        c.inRaster = getRaster(inRaster);
        c.inMask = getRaster(inMask);
        c.pMax = pMax;
        c.pMin = pMin;
        c.doInverse = doInverse;
        c.process();
        dumpRaster(c.outRaster, outRaster);
    }
}
