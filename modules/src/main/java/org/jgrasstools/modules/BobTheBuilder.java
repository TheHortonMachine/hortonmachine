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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_doErode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_doPolygonborder_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_doUseOnlyInternal_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_fElevation_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_inArea_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_inElevations_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_pMaxbuffer_DESCRIPTION;
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
import org.jgrasstools.gears.modules.r.bobthebuilder.OmsBobTheBuilder;

@Description(OMSBOBTHEBUILDER_DESCRIPTION)
@Author(name = OMSBOBTHEBUILDER_AUTHORNAMES, contact = OMSBOBTHEBUILDER_AUTHORCONTACTS)
@Keywords(OMSBOBTHEBUILDER_KEYWORDS)
@Label(OMSBOBTHEBUILDER_LABEL)
@Name("_" + OMSBOBTHEBUILDER_NAME)
@Status(OMSBOBTHEBUILDER_STATUS)
@License(OMSBOBTHEBUILDER_LICENSE)
public class BobTheBuilder extends JGTModel {

    @Description(OMSBOBTHEBUILDER_inRaster_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster = null;

    @Description(OMSBOBTHEBUILDER_inArea_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inArea = null;

    @Description(OMSBOBTHEBUILDER_inElevations_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElevations = null;

    @Description(OMSBOBTHEBUILDER_pMaxbuffer_DESCRIPTION)
    @In
    public double pMaxbuffer = -1;

    @Description(OMSBOBTHEBUILDER_fElevation_DESCRIPTION)
    @In
    public String fElevation = null;

    @Description(OMSBOBTHEBUILDER_doErode_DESCRIPTION)
    @In
    public boolean doErode = false;

    @Description(OMSBOBTHEBUILDER_doUseOnlyInternal_DESCRIPTION)
    @In
    public boolean doUseOnlyInternal = false;

    @Description(OMSBOBTHEBUILDER_doPolygonborder_DESCRIPTION)
    @In
    public boolean doPolygonborder = false;

    @Description(OMSBOBTHEBUILDER_outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
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
