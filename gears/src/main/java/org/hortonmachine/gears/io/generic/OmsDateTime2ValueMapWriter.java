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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_FILE_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_P_SEPARATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDATETIME2VALUEMAPWRITER_UI;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
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

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;

@Description(OMSDATETIME2VALUEMAPWRITER_DESCRIPTION)
@Author(name = OMSDATETIME2VALUEMAPWRITER_AUTHORNAMES, contact = OMSDATETIME2VALUEMAPWRITER_AUTHORCONTACTS)
@Keywords(OMSDATETIME2VALUEMAPWRITER_KEYWORDS)
@Label(OMSDATETIME2VALUEMAPWRITER_LABEL)
@Name(OMSDATETIME2VALUEMAPWRITER_NAME)
@Status(OMSDATETIME2VALUEMAPWRITER_STATUS)
@License(OMSDATETIME2VALUEMAPWRITER_LICENSE)
@UI(OMSDATETIME2VALUEMAPWRITER_UI)
public class OmsDateTime2ValueMapWriter extends HMModel {

    @Description(OMSDATETIME2VALUEMAPWRITER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    @Description(OMSDATETIME2VALUEMAPWRITER_P_SEPARATOR_DESCRIPTION)
    @In
    public String pSeparator = ",";

    @Description(OMSDATETIME2VALUEMAPWRITER_FILE_NOVALUE_DESCRIPTION)
    @In
    public String fileNovalue = "-9999.0";

    @Description(OMSDATETIME2VALUEMAPWRITER_DATA_DESCRIPTION)
    @In
    public HashMap<DateTime, double[]> data;

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

        Set<Entry<DateTime, double[]>> entrySet = data.entrySet();
        for( Entry<DateTime, double[]> entry : entrySet ) {
            DateTime id = entry.getKey();
            double[] values = entry.getValue();

            csvWriter.write(id.toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS));
            for( int i = 0; i < values.length; i++ ) {
                csvWriter.write(pSeparator);
                double value = values[i];
                if (isNovalue(value)) {
                    value = novalue;
                }
                csvWriter.write(String.valueOf(value));
            }
            csvWriter.write("\n");
        }
    }

    @Finalize
    public void close() throws IOException {
        csvWriter.close();
    }
}
