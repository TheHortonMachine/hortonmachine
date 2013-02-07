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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_fBridgeWidth_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inBridges_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inHecras_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inRiver_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_inSections_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_outSectionPoints_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_outSections_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_pBridgeBuffer_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_pSectionsIntervalDistance_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_pSectionsWidth_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHECRASINPUTBUILDER_pTitle_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder;

@Description(OMSHECRASINPUTBUILDER_DESCRIPTION)
@Author(name = OMSHECRASINPUTBUILDER_AUTHORNAMES, contact = OMSHECRASINPUTBUILDER_AUTHORCONTACTS)
@Keywords(OMSHECRASINPUTBUILDER_KEYWORDS)
@Label(OMSHECRASINPUTBUILDER_LABEL)
@Name("_" + OMSHECRASINPUTBUILDER_NAME)
@Status(OMSHECRASINPUTBUILDER_STATUS)
@License(OMSHECRASINPUTBUILDER_LICENSE)
public class HecrasInputBuilder extends JGTModel {

    @Description(OMSHECRASINPUTBUILDER_inElev_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev = null;

    @Description(OMSHECRASINPUTBUILDER_inRiver_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRiver = null;

    @Description(OMSHECRASINPUTBUILDER_inBridges_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inBridges = null;

    @Description(OMSHECRASINPUTBUILDER_inSections_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inSections = null;

    @Description(OMSHECRASINPUTBUILDER_pTitle_DESCRIPTION)
    @In
    public String pTitle = "DEFAULTID";

    @Description(OMSHECRASINPUTBUILDER_pSectionsIntervalDistance_DESCRIPTION)
    @In
    public double pSectionsIntervalDistance = 0.0D;

    @Description(OMSHECRASINPUTBUILDER_pSectionsWidth_DESCRIPTION)
    @In
    public double pSectionsWidth = 0.0D;

    @Description(OMSHECRASINPUTBUILDER_pBridgeBuffer_DESCRIPTION)
    @In
    public double pBridgeBuffer = 0.0D;

    @Description(OMSHECRASINPUTBUILDER_fBridgeWidth_DESCRIPTION)
    @In
    public String fBridgeWidth;

    @Description(OMSHECRASINPUTBUILDER_inHecras_DESCRIPTION)
    @In
    @UI(JGTConstants.FILEIN_UI_HINT)
    public String inHecras = null;

    @Description(OMSHECRASINPUTBUILDER_outSections_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outSections = null;

    @Description(OMSHECRASINPUTBUILDER_outSectionPoints_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outSectionPoints = null;

    @Execute
    public void process() throws Exception {
        OmsHecrasInputBuilder hecrasinputbuilder = new OmsHecrasInputBuilder();
        hecrasinputbuilder.inElev = getRaster(inElev);
        hecrasinputbuilder.inRiver = getVector(inRiver);
        hecrasinputbuilder.inBridges = getVector(inBridges);
        hecrasinputbuilder.inSections = getVector(inSections);
        hecrasinputbuilder.pTitle = pTitle;
        hecrasinputbuilder.pSectionsIntervalDistance = pSectionsIntervalDistance;
        hecrasinputbuilder.pSectionsWidth = pSectionsWidth;
        hecrasinputbuilder.pBridgeBuffer = pBridgeBuffer;
        hecrasinputbuilder.fBridgeWidth = fBridgeWidth;
        hecrasinputbuilder.inHecras = inHecras;
        hecrasinputbuilder.pm = pm;
        hecrasinputbuilder.doProcess = doProcess;
        hecrasinputbuilder.doReset = doReset;
        hecrasinputbuilder.process();
        if (outSections != null) {
            dumpVector(hecrasinputbuilder.outSections, outSections);
        }
        if (outSectionPoints != null) {
            dumpVector(hecrasinputbuilder.outSectionPoints, outSectionPoints);
        }
    }
}
