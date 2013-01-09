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
package org.jgrasstools.gears.io.converters;

import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_inData_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.IDVALUESARRAY2IDVALUESCONVERTER_outData_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
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

import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(IDVALUESARRAY2IDVALUESCONVERTER_DESCRIPTION)
@Author(name = IDVALUESARRAY2IDVALUESCONVERTER_AUTHORNAMES, contact = IDVALUESARRAY2IDVALUESCONVERTER_AUTHORCONTACTS)
@Keywords(IDVALUESARRAY2IDVALUESCONVERTER_KEYWORDS)
@Label(IDVALUESARRAY2IDVALUESCONVERTER_LABEL)
@Name(IDVALUESARRAY2IDVALUESCONVERTER_NAME)
@Status(IDVALUESARRAY2IDVALUESCONVERTER_STATUS)
@License(IDVALUESARRAY2IDVALUESCONVERTER_LICENSE)
public class IdValuesArray2IdValuesConverter extends JGTModel {

    @Description(IDVALUESARRAY2IDVALUESCONVERTER_inData_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inData;

    @Description(IDVALUESARRAY2IDVALUESCONVERTER_outData_DESCRIPTION)
    @Out
    public HashMap<Integer, Double> outData;

    @Execute
    public void convert() throws IOException {
        if (outData == null) {
            outData = new HashMap<Integer, Double>();
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
