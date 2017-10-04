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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_P_CQL_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFILTER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter;

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

@Description(OMSVECTORFILTER_DESCRIPTION)
@Author(name = OMSVECTORFILTER_AUTHORNAMES, contact = OMSVECTORFILTER_AUTHORCONTACTS)
@Keywords(OMSVECTORFILTER_KEYWORDS)
@Label(OMSVECTORFILTER_LABEL)
@Name("_" + OMSVECTORFILTER_NAME)
@Status(OMSVECTORFILTER_STATUS)
@License(OMSVECTORFILTER_LICENSE)
public class VectorFilter extends HMModel {

    @Description(OMSVECTORFILTER_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description(OMSVECTORFILTER_P_CQL_DESCRIPTION)
    @In
    public String pCql = null;

    @Description(OMSVECTORFILTER_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVector;

    @Execute
    public void process() throws Exception {
        OmsVectorFilter omsvectorfilter = new OmsVectorFilter();
        omsvectorfilter.inVector = getVector(inVector);
        omsvectorfilter.pCql = pCql;
        omsvectorfilter.pm = pm;
        omsvectorfilter.doProcess = doProcess;
        omsvectorfilter.doReset = doReset;
        omsvectorfilter.process();
        dumpVector(omsvectorfilter.outVector, outVector);
    }

}
