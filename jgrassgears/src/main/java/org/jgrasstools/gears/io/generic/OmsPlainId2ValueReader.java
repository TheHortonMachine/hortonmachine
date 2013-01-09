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
package org.jgrasstools.gears.io.generic;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_UI;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_data_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_fileNovalue_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_novalue_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPLAINID2VALUEREADER_pSeparator_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSPLAINID2VALUEREADER_DESCRIPTION)
@Author(name = OMSPLAINID2VALUEREADER_AUTHORNAMES, contact = OMSPLAINID2VALUEREADER_AUTHORCONTACTS)
@Keywords(OMSPLAINID2VALUEREADER_KEYWORDS)
@Label(OMSPLAINID2VALUEREADER_LABEL)
@Name(OMSPLAINID2VALUEREADER_NAME)
@Status(OMSPLAINID2VALUEREADER_STATUS)
@License(OMSPLAINID2VALUEREADER_LICENSE)
@UI(OMSPLAINID2VALUEREADER_UI)
public class OmsPlainId2ValueReader extends JGTModel {

    @Description(OMSPLAINID2VALUEREADER_file_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description(OMSPLAINID2VALUEREADER_pSeparator_DESCRIPTION)
    @In
    public String pSeparator = ",";

    @Description(OMSPLAINID2VALUEREADER_fileNovalue_DESCRIPTION)
    @In
    public String fileNovalue = "-9999.0";

    @Description(OMSPLAINID2VALUEREADER_novalue_DESCRIPTION)
    @In
    public double novalue = doubleNovalue;

    @Description(OMSPLAINID2VALUEREADER_data_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> data;

    protected BufferedReader csvReader;

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
            String[] lineSplit = line.split(pSeparator);
            for( int i = 0; i < lineSplit.length; i = i + 2 ) {
                int id = (int) Double.parseDouble(lineSplit[i].trim());
                double value = Double.parseDouble(lineSplit[i + 1].trim());
                if (fileNovalue != null) {
                    if (lineSplit[i + 1].trim().equals(fileNovalue)) {
                        // set to internal novalue
                        value = novalue;
                    }
                }
                data.put(id, new double[]{value});
            }
        }
    }

    @Finalize
    public void close() throws IOException {
        csvReader.close();
    }
}
