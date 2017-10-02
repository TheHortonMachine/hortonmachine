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
package org.hortonmachine.gears.modules.v.vectorreprojector;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORREPROJECTOR_DOCUMENTATION;
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

import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OMSVECTORREPROJECTOR_DESCRIPTION)
@Documentation(OMSVECTORREPROJECTOR_DOCUMENTATION)
@Author(name = OMSVECTORREPROJECTOR_AUTHORNAMES, contact = OMSVECTORREPROJECTOR_AUTHORCONTACTS)
@Keywords(OMSVECTORREPROJECTOR_KEYWORDS)
@Label(OMSVECTORREPROJECTOR_LABEL)
@Name(OMSVECTORREPROJECTOR_NAME)
@Status(OMSVECTORREPROJECTOR_STATUS)
@License(OMSVECTORREPROJECTOR_LICENSE)
public class OmsVectorReprojector extends HMModel {

    @Description(OMSVECTORREPROJECTOR_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

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
    @Out
    public SimpleFeatureCollection outVector = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        CoordinateReferenceSystem targetCrs = CrsUtilities.getCrsFromEpsg(pCode, doLongitudeFirst);
        if (pForceCode != null) {
            pm.beginTask("Forcing input crs...", IHMProgressMonitor.UNKNOWN);
            CoordinateReferenceSystem forcedCrs = CrsUtilities.getCrsFromEpsg(pForceCode);
            inVector = new ForceCoordinateSystemFeatureResults(inVector, forcedCrs);
            pm.done();
        }

        pm.beginTask("Reprojecting features...", IHMProgressMonitor.UNKNOWN);
        try {
            outVector = new ReprojectingFeatureCollection(inVector, targetCrs);
        } finally {

            pm.done();
        }

    }

}
