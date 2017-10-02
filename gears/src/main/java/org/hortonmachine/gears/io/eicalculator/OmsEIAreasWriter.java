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
package org.hortonmachine.gears.io.eicalculator;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_IN_AREAS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_P_SEPARATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSEIAREASWRITER_STATUS;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSEIAREASWRITER_DESCRIPTION)
@Author(name = OMSEIAREASWRITER_AUTHORNAMES, contact = OMSEIAREASWRITER_AUTHORCONTACTS)
@Keywords(OMSEIAREASWRITER_KEYWORDS)
@Label(OMSEIAREASWRITER_LABEL)
@Name(OMSEIAREASWRITER_NAME)
@Status(OMSEIAREASWRITER_STATUS)
@License(OMSEIAREASWRITER_LICENSE)
public class OmsEIAreasWriter extends HMModel {

    @Description(OMSEIAREASWRITER_IN_AREAS_DESCRIPTION)
    @In
    public List<EIAreas> inAreas;

    @Description(OMSEIAREASWRITER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    @Description(OMSEIAREASWRITER_P_SEPARATOR_DESCRIPTION)
    @In
    public String pSeparator = ",";

    private BufferedWriter csvWriter;

    private void ensureOpen() throws IOException {
        if (csvWriter == null)
            csvWriter = new BufferedWriter(new FileWriter(file));
    }

    @Finalize
    public void close() throws IOException {
        csvWriter.close();
    }

    @Execute
    public void write() throws IOException {
        ensureOpen();

        csvWriter.write("# EIAreas writer output\n");
        for( EIAreas areas : inAreas ) {
            StringBuilder sb = new StringBuilder();
            sb.append(areas.basinId);
            sb.append(pSeparator);
            sb.append(areas.altimetricBandId);
            sb.append(pSeparator);
            sb.append(areas.energyBandId);
            sb.append(pSeparator);
            sb.append(areas.areaValue);
            sb.append("\n");
            csvWriter.write(sb.toString());
        }
    }
}
