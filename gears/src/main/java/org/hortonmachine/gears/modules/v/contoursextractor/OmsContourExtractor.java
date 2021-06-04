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
package org.hortonmachine.gears.modules.v.contoursextractor;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor.OMSCONTOUREXTRACTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor.OMSCONTOUREXTRACTOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor.OMSCONTOUREXTRACTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor.OMSCONTOUREXTRACTOR_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor.OMSCONTOUREXTRACTOR_KEYWORDS;
import static org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor.OMSCONTOUREXTRACTOR_LABEL;
import static org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor.OMSCONTOUREXTRACTOR_LICENSE;
import static org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor.OMSCONTOUREXTRACTOR_NAME;
import static org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor.OMSCONTOUREXTRACTOR_STATUS;

import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.raster.ContourProcess;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;

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

@Description(OMSCONTOUREXTRACTOR_DESCRIPTION)
@Documentation(OMSCONTOUREXTRACTOR_DOCUMENTATION)
@Author(name = OMSCONTOUREXTRACTOR_AUTHORNAMES, contact = OMSCONTOUREXTRACTOR_AUTHORCONTACTS)
@Keywords(OMSCONTOUREXTRACTOR_KEYWORDS)
@Label(OMSCONTOUREXTRACTOR_LABEL)
@Name(OMSCONTOUREXTRACTOR_NAME)
@Status(OMSCONTOUREXTRACTOR_STATUS)
@License(OMSCONTOUREXTRACTOR_LICENSE)
public class OmsContourExtractor extends HMModel {

    @Description(OMSCONTOUREXTRACTOR_IN_COVERAGE_DESCRIPTION)
    @In
    public GridCoverage2D inCoverage;

    @Description(OMSCONTOUREXTRACTOR_P_MIN_DESCRIPTION)
    @In
    public Double pMin;

    @Description(OMSCONTOUREXTRACTOR_P_MAX_DESCRIPTION)
    @In
    public Double pMax;

    @Description(OMSCONTOUREXTRACTOR_P_INTERVAL_DESCRIPTION)
    @In
    public Double pInterval;

    @Description(OMSCONTOUREXTRACTOR_OUT_GEODATA_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outGeodata = null;

    public static final String OMSCONTOUREXTRACTOR_DESCRIPTION = "Module that extracts contour lines from a raster.";
    public static final String OMSCONTOUREXTRACTOR_DOCUMENTATION = "OmsContourExtractor.html";
    public static final String OMSCONTOUREXTRACTOR_KEYWORDS = "Raster, Vector";
    public static final String OMSCONTOUREXTRACTOR_LABEL = VECTORPROCESSING;
    public static final String OMSCONTOUREXTRACTOR_NAME = "contourextract";
    public static final int OMSCONTOUREXTRACTOR_STATUS = 5;
    public static final String OMSCONTOUREXTRACTOR_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSCONTOUREXTRACTOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSCONTOUREXTRACTOR_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSCONTOUREXTRACTOR_IN_COVERAGE_DESCRIPTION = "The raster on which to calculate the contours.";
    public static final String OMSCONTOUREXTRACTOR_P_MIN_DESCRIPTION = "The minimum value for the contours.";
    public static final String OMSCONTOUREXTRACTOR_P_MAX_DESCRIPTION = "The maximum value for the contours.";
    public static final String OMSCONTOUREXTRACTOR_P_INTERVAL_DESCRIPTION = "The contours interval.";
    public static final String OMSCONTOUREXTRACTOR_OUT_GEODATA_DESCRIPTION = "The generated contour lines vector.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outGeodata == null, doReset)) {
            return;
        }
        checkNull(inCoverage, pMin, pMax, pInterval);

        if (pMin > pMax) {
            throw new ModelsIllegalargumentException("Min has to be bigger than Max.", this, pm);
        }

        List<Double> contourIntervals = new ArrayList<Double>();
        pm.message("Adding levels:");
        for( double level = pMin; level <= pMax; level += pInterval ) {
            contourIntervals.add(level);
            pm.message("-> " + level);
        }
        double[] levels = new double[contourIntervals.size()];
        for( int i = 0; i < levels.length; i++ ) {
            levels[i] = contourIntervals.get(i);
        }

        pm.beginTask("Extracting contours...", IHMProgressMonitor.UNKNOWN);
        ContourProcess contourProcess = new ContourProcess();
        SimpleFeatureCollection contoursFC = contourProcess.execute(inCoverage, 0, levels, null, null, null, null, null);
        outGeodata = new DefaultFeatureCollection();

        List<Geometry> contours = FeatureUtilities.featureCollectionToGeometriesList(contoursFC, true, "value");
        for( Geometry geom : contours ) {
            LineString lineString = (LineString) geom;
            Object userData = lineString.getUserData();
            double elev = -1.0;
            if (userData instanceof Double) {
                elev = (Double) userData;
                lineString.setUserData(null);
            }
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(contoursFC.getSchema());
            Object[] values = new Object[]{lineString, elev};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            ((DefaultFeatureCollection) outGeodata).add(feature);
        }

    }
}
