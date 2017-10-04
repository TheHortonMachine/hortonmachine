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
package org.hortonmachine.lesto.modules.utilities;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.io.FilenameFilter;

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

import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Print out information of a las file or las data folder.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("info, lidar, las")
@Label(HMConstants.LESTO + "/utilities")
@Name("lasinfo")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class LasInfo extends HMModel {
    @Description("Las file or folder path.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas = null;

    @Execute
    public void process() throws Exception {
        checkNull(inLas);

        File[] lasFiles = null;
        File inLasFile = new File(inLas);
        if (inLasFile.isDirectory()) {
            lasFiles = inLasFile.listFiles(new FilenameFilter(){
                public boolean accept( File dir, String name ) {
                    return name.toLowerCase().endsWith(".las");
                }
            });
        } else {
            lasFiles = new File[]{inLasFile};
        }

        for( File file : lasFiles ) {
            pm.message("***********************************************");
            pm.message("*   " + file.toString());
            pm.message("***********************************************");

            try (ALasReader lasReader = ALasReader.getReader(file, null)) {
                lasReader.open();
                ILasHeader header = lasReader.getHeader();
                pm.message("*   HEADER");
                pm.message("**************");
                pm.message(header.toString());

                CoordinateReferenceSystem crs = header.getCrs();
                pm.message("**************");
                pm.message("*   CRS");
                pm.message("**************");
                if (crs != null) {
                    pm.message(crs.toString());
                } else {
                    pm.message("No CRS information.");
                }

                pm.message("****************");
                pm.message("*   SAMPLE DATA");
                pm.message("****************");
                int i = 1;
                while( lasReader.hasNextPoint() ) {
                    LasRecord dot = lasReader.getNextPoint();
                    pm.message("Point " + i);
                    pm.message(LasUtils.lasRecordToString(dot));
                    if (++i > 3) {
                        break;
                    }
                }
            }
            pm.message("***********************************************");
        }
    }

    /**
     * Utility method to run info.
     * 
     * @param filePath the file to print info of.
     * @throws Exception
     */
    public static void printInfo( String filePath ) throws Exception {
        LasInfo lasInfo = new LasInfo();
        lasInfo.inLas = filePath;
        lasInfo.process();
    }

}
