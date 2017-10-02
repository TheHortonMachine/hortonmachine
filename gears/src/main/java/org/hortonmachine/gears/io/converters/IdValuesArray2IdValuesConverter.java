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
package org.hortonmachine.gears.io.converters;

import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_IN_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_OUT_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(IDVALUESARRAY2IDVALUESCONVERTER_DESCRIPTION)
@Author(name = IDVALUESARRAY2IDVALUESCONVERTER_AUTHORNAMES, contact = IDVALUESARRAY2IDVALUESCONVERTER_AUTHORCONTACTS)
@Keywords(IDVALUESARRAY2IDVALUESCONVERTER_KEYWORDS)
@Label(IDVALUESARRAY2IDVALUESCONVERTER_LABEL)
@Name(IDVALUESARRAY2IDVALUESCONVERTER_NAME)
@Status(IDVALUESARRAY2IDVALUESCONVERTER_STATUS)
@License(IDVALUESARRAY2IDVALUESCONVERTER_LICENSE)
@UI(HMConstants.HIDE_UI_HINT)
public class IdValuesArray2IdValuesConverter extends HMModel {

    @Description(IDVALUESARRAY2IDVALUESCONVERTER_IN_DATA_DESCRIPTION)
    @In
    public Map<Integer, double[]> inData;

    @Description(IDVALUESARRAY2IDVALUESCONVERTER_OUT_DATA_DESCRIPTION)
    @Out
    public Map<Integer, Double> outData;

    @Execute
    public void convert() throws IOException {
        if (outData == null) {
            outData = new HashMap<>();
        }
        Set<Entry<Integer, double[]>> entries = inData.entrySet();
        for( Entry<Integer, double[]> entry : entries ) {
            Integer id = entry.getKey();
            double[] value = entry.getValue();

            double avg = 0;
            int num = 0;
            for( double d : value ) {
                if (!isNovalue(d)) {
                    avg = avg + d;
                    num++;
                }
            }
            avg = avg / num;

            outData.put(id, avg);
        }
    }

}
