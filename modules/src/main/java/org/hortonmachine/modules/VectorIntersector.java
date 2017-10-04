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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_inMap1_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_inMap2_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_outMap_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector;

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

@Description(OmsVectorIntersector.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsVectorIntersector.KEYWORDS)
@Label(HMConstants.VECTORPROCESSING)
@Name(OmsVectorIntersector.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class VectorIntersector extends HMModel {

    @Description(OMSVECTOROVERLAYOPERATORS_inMap1_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMap1 = null;

    @Description(OMSVECTOROVERLAYOPERATORS_inMap2_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMap2 = null;

    @Description(OmsVectorIntersector.KEEP_FIRST_ATTRIBUTES)
    @In
    public boolean doKeepFirstAttributes = true;

    @Description(OMSVECTOROVERLAYOPERATORS_outMap_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsVectorIntersector vint = new OmsVectorIntersector();
        vint.inMap1 = getVector(inMap1);
        vint.inMap2 = getVector(inMap2);
        vint.doKeepFirstAttributes = doKeepFirstAttributes;
        vint.process();
        dumpVector(vint.outMap, outMap);
    }

}
