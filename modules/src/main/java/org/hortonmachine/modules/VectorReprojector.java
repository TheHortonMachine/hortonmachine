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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_DO_LENIENT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_DO_LONGITUDE_FIRST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_P_CODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_P_FORCE_CODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_STATUS;

import java.io.File;
import java.io.FilenameFilter;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector;
import org.hortonmachine.gears.utils.DataUtilities;

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

@Description(OMSVECTORREPROJECTOR_DESCRIPTION)
@Author(name = OMSVECTORREPROJECTOR_AUTHORNAMES, contact = OMSVECTORREPROJECTOR_AUTHORCONTACTS)
@Keywords(OMSVECTORREPROJECTOR_KEYWORDS)
@Label(OMSVECTORREPROJECTOR_LABEL)
@Name("_" + OMSVECTORREPROJECTOR_NAME)
@Status(OMSVECTORREPROJECTOR_STATUS)
@License(OMSVECTORREPROJECTOR_LICENSE)
public class VectorReprojector extends HMModel {

    @Description(OMSVECTORREPROJECTOR_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description(OMSVECTORREPROJECTOR_P_CODE_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSVECTORREPROJECTOR_DO_LONGITUDE_FIRST_DESCRIPTION)
    @In
    public Boolean doLongitudeFirst = null;

    @Description(OMSVECTORREPROJECTOR_P_FORCE_CODE_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pForceCode;

    @Description(OMSVECTORREPROJECTOR_DO_LENIENT_DESCRIPTION)
    @In
    public boolean doLenient = true;

    @Description(OMSVECTORREPROJECTOR_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVector = null;

    @Execute
    public void process() throws Exception {
        checkNull(inVector, outVector);
        File inVectorFile = new File(inVector);
        if (inVectorFile.isDirectory()) {
            File[] listFiles = inVectorFile.listFiles(new FilenameFilter(){
                @Override
                public boolean accept( File dir, String name ) {
                    return DataUtilities.isSupportedVectorExtension(name);
                }
            });
            
            File outVectorFolder = new File(outVector);
            if (!outVectorFolder.isDirectory()) {
                outVectorFolder = outVectorFolder.getParentFile();
            }
            
            for( File inFile : listFiles ) {
                String name = inFile.getName();
                File outFile = new File(outVectorFolder, name);
                
                OmsVectorReprojector vectorreprojector = new OmsVectorReprojector();
                vectorreprojector.inVector = getVector(inFile.getAbsolutePath());
                vectorreprojector.pCode = pCode;
                vectorreprojector.doLongitudeFirst = doLongitudeFirst;
                vectorreprojector.pForceCode = pForceCode;
                vectorreprojector.doLenient = doLenient;
                vectorreprojector.pm = pm;
                vectorreprojector.doProcess = doProcess;
                vectorreprojector.doReset = doReset;
                vectorreprojector.process();
                dumpVector(vectorreprojector.outVector, outFile.getAbsolutePath());
            }
        } else {
            OmsVectorReprojector vectorreprojector = new OmsVectorReprojector();
            vectorreprojector.inVector = getVector(inVector);
            vectorreprojector.pCode = pCode;
            vectorreprojector.doLongitudeFirst = doLongitudeFirst;
            vectorreprojector.pForceCode = pForceCode;
            vectorreprojector.doLenient = doLenient;
            vectorreprojector.pm = pm;
            vectorreprojector.doProcess = doProcess;
            vectorreprojector.doReset = doReset;
            vectorreprojector.process();
            dumpVector(vectorreprojector.outVector, outVector);
        }
    }
}
