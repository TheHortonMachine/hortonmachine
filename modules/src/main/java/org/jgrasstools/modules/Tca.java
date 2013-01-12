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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_outLoop_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTCA_outTca_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.geomorphology.tca.OmsTca;

@Description(OMSTCA_DESCRIPTION)
@Documentation(OMSTCA_DOCUMENTATION)
@Author(name = OMSTCA_AUTHORNAMES, contact = OMSTCA_AUTHORCONTACTS)
@Keywords(OMSTCA_KEYWORDS)
@Label(OMSTCA_LABEL)
@Name("_" + OMSTCA_NAME)
@Status(OMSTCA_STATUS)
@License(OMSTCA_LICENSE)
public class Tca extends JGTModel {
    @Description(OMSTCA_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(OMSTCA_outTca_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outTca = null;

    @Description(OMSTCA_outLoop_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outLoop = null;

    @Execute
    public void process() throws Exception {
        OmsTca omstca = new OmsTca();
        omstca.inFlow = getRaster(inFlow);
        omstca.pm = pm;
        omstca.doProcess = doProcess;
        omstca.doReset = doReset;
        omstca.process();
        dumpRaster(omstca.outTca, outTca);
        dumpVector(omstca.outLoop, outLoop);
    }

}