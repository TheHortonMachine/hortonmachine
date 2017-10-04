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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_inAttribute_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_outAttribute_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hillslopeanalyses.h2ca.OmsH2cA;

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

@Description(OMSH2CA_DESCRIPTION)
@Author(name = OMSH2CA_AUTHORNAMES, contact = OMSH2CA_AUTHORCONTACTS)
@Keywords(OMSH2CA_KEYWORDS)
@Label(OMSH2CA_LABEL)
@Name("_" + OMSH2CA_NAME)
@Status(OMSH2CA_STATUS)
@License(OMSH2CA_LICENSE)
public class H2cA extends HMModel {
    @Description(OMSH2CA_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSH2CA_inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(OMSH2CA_inAttribute_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inAttribute = null;

    @Description(OMSH2CA_outAttribute_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outAttribute = null;

    @Execute
    public void process() throws Exception {
        OmsH2cA h2ca = new OmsH2cA();
        h2ca.inFlow = getRaster(inFlow);
        h2ca.inNet = getRaster(inNet);
        h2ca.inAttribute = getRaster(inAttribute);
        h2ca.pm = pm;
        h2ca.doProcess = doProcess;
        h2ca.doReset = doReset;
        h2ca.process();
        dumpRaster(h2ca.outAttribute, outAttribute);
    }

}
