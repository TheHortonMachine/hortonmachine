/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.modules;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_IN_GEODATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_OUT_GEODATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCONVERTER_UI;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectorconverter.OmsVectorConverter;

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

@Description(OMSVECTORCONVERTER_DESCRIPTION)
@Author(name = OMSVECTORCONVERTER_AUTHORNAMES, contact = OMSVECTORCONVERTER_AUTHORCONTACTS)
@Keywords(OMSVECTORCONVERTER_KEYWORDS)
@Label(OMSVECTORCONVERTER_LABEL)
@Name("_" + OMSVECTORCONVERTER_NAME)
@Status(OMSVECTORCONVERTER_STATUS)
@License(OMSVECTORCONVERTER_LICENSE)
@UI(OMSVECTORCONVERTER_UI)
public class VectorConverter extends HMModel {

    @Description(OMSVECTORCONVERTER_IN_GEODATA_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inGeodata;

    @Description(OMSVECTORCONVERTER_OUT_GEODATA_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outGeodata;

    @Execute
    public void process() throws Exception {
        OmsVectorConverter vectorconverter = new OmsVectorConverter();
        vectorconverter.inGeodata = getVector(inGeodata);
        vectorconverter.pm = pm;
        vectorconverter.doProcess = doProcess;
        vectorconverter.doReset = doReset;
        vectorconverter.process();
        dumpVector(vectorconverter.outGeodata, outGeodata);
    }

}
