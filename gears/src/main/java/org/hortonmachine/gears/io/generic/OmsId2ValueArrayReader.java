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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_FILE_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_P_COLS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_P_SEPARATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSID2VALUEARRAYREADER_UI;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSID2VALUEARRAYREADER_DESCRIPTION)
@Author(name = OMSID2VALUEARRAYREADER_AUTHORNAMES, contact = OMSID2VALUEARRAYREADER_AUTHORCONTACTS)
@Keywords(OMSID2VALUEARRAYREADER_KEYWORDS)
@Label(OMSID2VALUEARRAYREADER_LABEL)
@Name(OMSID2VALUEARRAYREADER_NAME)
@Status(OMSID2VALUEARRAYREADER_STATUS)
@License(OMSID2VALUEARRAYREADER_LICENSE)
@UI(OMSID2VALUEARRAYREADER_UI)
public class OmsId2ValueArrayReader extends HMModel {

    @Description(OMSID2VALUEARRAYREADER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String file = null;

    @Description(OMSID2VALUEARRAYREADER_P_COLS_DESCRIPTION)
    @In
    public int pCols = -1;

    @Description(OMSID2VALUEARRAYREADER_P_SEPARATOR_DESCRIPTION)
    @In
    public String pSeparator = ",";

    @Description(OMSID2VALUEARRAYREADER_FILE_NOVALUE_DESCRIPTION)
    @In
    public String fileNovalue = "-9999.0";

    @Description(OMSID2VALUEARRAYREADER_DATA_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> data;

    private BufferedReader csvReader;

    private void ensureOpen() throws IOException {
        if (csvReader == null)
            csvReader = new BufferedReader(new FileReader(file));
    }

    @Execute
    public void readNextLine() throws IOException {
        ensureOpen();
        data = new HashMap<Integer, double[]>();
        String line = null;
        if ((line = csvReader.readLine()) != null) {
            String[] lineSplit = line.trim().split(pSeparator);
            for( int i = 0; i < lineSplit.length; i++ ) {
                int id = (int) Double.parseDouble(lineSplit[i].trim());

                double[] values = new double[pCols];
                for( int j = i + 1, k = 0; j < i + pCols + 1; j++, k++ ) {
                    double value = Double.parseDouble(lineSplit[j].trim());
                    if (fileNovalue != null) {
                        if (lineSplit[j].trim().equals(fileNovalue)) {
                            // set to internal novalue
                            value = doubleNovalue;
                        }
                    }
                    values[k] = value;
                }
                data.put(id, values);
                i = i + pCols;
            }
        }
    }

    @Finalize
    public void close() throws IOException {
        csvReader.close();
    }
}
