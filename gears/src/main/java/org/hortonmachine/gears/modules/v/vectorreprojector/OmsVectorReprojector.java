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

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector.OMSVECTORREPROJECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector.OMSVECTORREPROJECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector.OMSVECTORREPROJECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector.OMSVECTORREPROJECTOR_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector.OMSVECTORREPROJECTOR_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector.OMSVECTORREPROJECTOR_LABEL;
import static org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector.OMSVECTORREPROJECTOR_LICENSE;
import static org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector.OMSVECTORREPROJECTOR_NAME;
import static org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector.OMSVECTORREPROJECTOR_STATUS;

import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

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

    public static final String OMSVECTORREPROJECTOR_DESCRIPTION = "Reprojects a vector to another coordinate reference system.";
    public static final String OMSVECTORREPROJECTOR_DOCUMENTATION = """
            The vertices are transformed one by one, so a long straight segment stays straight in the new crs, \
            where it should become a curve: the long lines need more vertices first if their shape matters, \
            as the vertex spacing of GridsGenerator gives to its grids. The attributes don't change: the \
            lengths and the areas stored in the fields keep the values of the original crs.

            A vector without crs, or with a wrong one, as a shapefile without its prj, can be given its \
            crs with the force code, before the reprojection. The force code doesn't move the geometries.

            Between crs on different datums the transformation needs the datum shift parameters (TOWGS84, \
            Bursa-Wolf), that the EPSG database has for most of the national datums. When they are \
            missing, the lenient mode reprojects anyway ignoring the datum shift, with errors that can reach \
            some hundred meters; without it, the reprojection stops.

            A lat/long output has the longitude as first axis, as x, the order of the shapefiles and of \
            most GIS. Setting the longitude first switch to false gives instead the order of the EPSG \
            definition, the latitude first for EPSG:4326, as some standards require.
            """;
    public static final String OMSVECTORREPROJECTOR_KEYWORDS = "CRS, Reprojection, Vector";
    public static final String OMSVECTORREPROJECTOR_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORREPROJECTOR_NAME = "vreproject";
    public static final int OMSVECTORREPROJECTOR_STATUS = 40;
    public static final String OMSVECTORREPROJECTOR_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSVECTORREPROJECTOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORREPROJECTOR_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSVECTORREPROJECTOR_IN_VECTOR_DESCRIPTION = "The vector that has to be reprojected, or with the wrapper a folder of vectors.";
    public static final String OMSVECTORREPROJECTOR_P_CODE_DESCRIPTION = "The code defining the target coordinate reference system, composed by authority and code number (ex. EPSG:32632).";
    public static final String OMSVECTORREPROJECTOR_DO_LONGITUDE_FIRST_DESCRIPTION = "The longitude as first axis of a lat/long output (default); false for the axis order of the EPSG definition, the latitude first for EPSG:4326.";
    public static final String OMSVECTORREPROJECTOR_P_FORCE_CODE_DESCRIPTION = "An optional coordinate reference system to give to the input vector, if it has none or a wrong one, composed by authority and code number (ex. EPSG:3003).";
    public static final String OMSVECTORREPROJECTOR_DO_LENIENT_DESCRIPTION = "Switch that set to true allows for some error due to different datums. If set to false, it won't reproject without Bursa Wolf parameters.";
    public static final String OMSVECTORREPROJECTOR_OUT_VECTOR_DESCRIPTION = "The output reprojected vector.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        // longitude first unless asked otherwise: the order of the files, whatever the referencing hints
        boolean longitudeFirst = doLongitudeFirst == null || doLongitudeFirst;
        CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs(pCode, longitudeFirst);
        if (pForceCode != null) {
            pm.beginTask("Forcing input crs...", IHMProgressMonitor.UNKNOWN);
            CoordinateReferenceSystem forcedCrs = HMCrsRegistry.INSTANCE.getCrs(pForceCode);
            inVector = new ForceCoordinateSystemFeatureResults(inVector, forcedCrs);
            pm.done();
        }

        pm.beginTask("Reprojecting features...", IHMProgressMonitor.UNKNOWN);
        try {
            HMCrsTransformer transformer = new HMCrsTransformer(inVector.getSchema().getCoordinateReferenceSystem(),
                    targetCrs);
            transformer.setAcceptLenientDatumShift(doLenient);
            try {
                transformer.getMathTransform();
            } catch (Exception e) {
                throw new ModelsIllegalargumentException(
                        "Couldn't find the transformation to the target crs (without the datum transformation parameters it needs doLenient): "
                                + e.getLocalizedMessage(),
                        this, pm);
            }
            outVector = transformer.transform(inVector);
        } finally {

            pm.done();
        }

    }

}
