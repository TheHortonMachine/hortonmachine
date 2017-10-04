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
package org.hortonmachine.gears.io.generic;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_FILE_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_P_SEPARATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYWRITER_UI;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSID2VALUEARRAYWRITER_DESCRIPTION)
@Author(name = OMSID2VALUEARRAYWRITER_AUTHORNAMES, contact = OMSID2VALUEARRAYWRITER_AUTHORCONTACTS)
@Keywords(OMSID2VALUEARRAYWRITER_KEYWORDS)
@Label(OMSID2VALUEARRAYWRITER_LABEL)
@Name(OMSID2VALUEARRAYWRITER_NAME)
@Status(OMSID2VALUEARRAYWRITER_STATUS)
@License(OMSID2VALUEARRAYWRITER_LICENSE)
@UI(OMSID2VALUEARRAYWRITER_UI)
public class OmsId2ValueArrayWriter extends HMModel {

    @Description(OMSID2VALUEARRAYWRITER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String file = null;

    @Description(OMSID2VALUEARRAYWRITER_P_SEPARATOR_DESCRIPTION)
    @In
    public String pSeparator = ",";

    @Description(OMSID2VALUEARRAYWRITER_FILE_NOVALUE_DESCRIPTION)
    @In
    public String fileNovalue = "-9999.0";

    @Description(OMSID2VALUEARRAYWRITER_DATA_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> data;

    private BufferedWriter csvWriter;

    private void ensureOpen() throws IOException {
        if (csvWriter == null)
            csvWriter = new BufferedWriter(new FileWriter(file));
    }

    private double novalue = -9999.0;

    @Execute
    public void writeNextLine() throws IOException {
        ensureOpen();

        novalue = Double.parseDouble(fileNovalue);

        Set<Entry<Integer, double[]>> entrySet = data.entrySet();
        for( Entry<Integer, double[]> entry : entrySet ) {
            Integer id = entry.getKey();
            double[] values = entry.getValue();

            csvWriter.write(id.toString());
            csvWriter.write(pSeparator);
            for( int i = 0; i < values.length; i++ ) {
                double value = values[i];
                if (isNovalue(value)) {
                    value = novalue;
                }
                csvWriter.write(String.valueOf(value));
                csvWriter.write(pSeparator);
            }
        }
        csvWriter.write("\n");
    }

    @Finalize
    public void close() throws IOException {
        csvWriter.close();
    }
}
