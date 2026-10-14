/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_IN_MAP1_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_IN_MAP2_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_LABEL;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_LICENSE;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_NAME;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_OUT_MAP_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector;

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

@Description(OMSVECTORINTERSECTOR_DESCRIPTION)
@Documentation(OMSVECTORINTERSECTOR_DOCUMENTATION)
@Author(name = OMSVECTORINTERSECTOR_AUTHORNAMES, contact = OMSVECTORINTERSECTOR_AUTHORCONTACTS)
@Keywords(OMSVECTORINTERSECTOR_KEYWORDS)
@Label(OMSVECTORINTERSECTOR_LABEL)
@Name("_" + OMSVECTORINTERSECTOR_NAME)
@Status(OMSVECTORINTERSECTOR_STATUS)
@License(OMSVECTORINTERSECTOR_LICENSE)
public class VectorIntersector extends HMModel {

    @Description(OMSVECTORINTERSECTOR_IN_MAP1_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMap1 = null;

    @Description(OMSVECTORINTERSECTOR_IN_MAP2_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMap2 = null;

    @Description(OMSVECTORINTERSECTOR_OUT_MAP_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsVectorIntersector vectorintersector = new OmsVectorIntersector();
        vectorintersector.inMap1 = getVector(inMap1);
        vectorintersector.inMap2 = getVector(inMap2);
        vectorintersector.pm = pm;
        vectorintersector.doProcess = doProcess;
        vectorintersector.doReset = doReset;
        vectorintersector.process();
        dumpVector(vectorintersector.outMap, outMap);
    }

}
