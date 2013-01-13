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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_outPlan_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_outProf_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_outTang_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.geomorphology.curvatures.OmsCurvatures;

@Description(OMSCURVATURES_DESCRIPTION)
@Documentation(OMSCURVATURES_DOCUMENTATION)
@Author(name = OMSCURVATURES_AUTHORNAMES, contact = OMSCURVATURES_AUTHORCONTACTS)
@Keywords(OMSCURVATURES_KEYWORDS)
@Label(OMSCURVATURES_LABEL)
@Name("_" + OMSCURVATURES_NAME)
@Status(OMSCURVATURES_STATUS)
@License(OMSCURVATURES_LICENSE)
public class Curvatures extends JGTModel {
    @Description(OMSCURVATURES_inElev_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev = null;

    // output
    @Description(OMSCURVATURES_outProf_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outProf = null;

    @Description(OMSCURVATURES_outPlan_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outPlan = null;

    @Description(OMSCURVATURES_outTang_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outTang = null;

    @Execute
    public void process() throws Exception {
        OmsCurvatures curv = new OmsCurvatures();
        curv.pm = pm;
        curv.inElev = getRaster(inElev);
        curv.process();
        dumpRaster(curv.outProf, outProf);
        dumpRaster(curv.outPlan, outPlan);
        dumpRaster(curv.outTang, outTang);
    }
}
