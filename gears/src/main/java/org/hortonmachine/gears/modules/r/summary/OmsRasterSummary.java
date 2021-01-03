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
package org.hortonmachine.gears.modules.r.summary;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_KEYWORDS;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_LABEL;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_LICENSE;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_NAME;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_STATUS;

import java.awt.image.RenderedImage;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.math.CoupledFieldsMoments;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Polygon;

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

@Description(OMSRASTERSUMMARY_DESCRIPTION)
@Documentation(OMSRASTERSUMMARY_DOCUMENTATION)
@Author(name = OMSRASTERSUMMARY_AUTHORNAMES, contact = OMSRASTERSUMMARY_AUTHORCONTACTS)
@Keywords(OMSRASTERSUMMARY_KEYWORDS)
@Label(OMSRASTERSUMMARY_LABEL)
@Name(OMSRASTERSUMMARY_NAME)
@Status(OMSRASTERSUMMARY_STATUS)
@License(OMSRASTERSUMMARY_LICENSE)
public class OmsRasterSummary extends HMModel {

    @Description(OMSRASTERSUMMARY_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERSUMMARY_P_BINS_DESCRIPTION)
    @In
    public int pBins = 100;

    @Description(OMSRASTERSUMMARY_DO_HISTOGRAM_DESCRIPTION)
    @In
    public boolean doHistogram = false;

    @Description(OMSRASTERSUMMARY_OUT_MIN_DESCRIPTION)
    @Out
    public Double outMin = null;

    @Description(OMSRASTERSUMMARY_OUT_MAX_DESCRIPTION)
    @Out
    public Double outMax = null;

    @Description(OMSRASTERSUMMARY_OUT_MEAN_DESCRIPTION)
    @Out
    public Double outMean = null;

    @Description(OMSRASTERSUMMARY_OUT_S_DEV_DESCRIPTION)
    @Out
    public Double outSdev = null;

    @Description(OMSRASTERSUMMARY_OUT_RANGE_DESCRIPTION)
    @Out
    public Double outRange = null;

    @Description(OMSRASTERSUMMARY_OUT_SUM_DESCRIPTION)
    @Out
    public Double outSum = null;

    @Description(OMSRASTERSUMMARY_OUT_CB_DESCRIPTION)
    @Out
    public double[][] outCb = null;

    public boolean printToConsole = true;

    public static final String OMSRASTERSUMMARY_DESCRIPTION = "Calculate a summary of the map with base statistics.";
    public static final String OMSRASTERSUMMARY_DOCUMENTATION = "OmsRasterSummary.html";
    public static final String OMSRASTERSUMMARY_KEYWORDS = "Statistics, Raster, OmsMapcalc";
    public static final String OMSRASTERSUMMARY_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERSUMMARY_NAME = "rsummary";
    public static final int OMSRASTERSUMMARY_STATUS = 40;
    public static final String OMSRASTERSUMMARY_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERSUMMARY_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERSUMMARY_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERSUMMARY_IN_RASTER_DESCRIPTION = "The map to analize.";
    public static final String OMSRASTERSUMMARY_P_BINS_DESCRIPTION = "The number of bins for the histogram (default = 100).";
    public static final String OMSRASTERSUMMARY_DO_HISTOGRAM_DESCRIPTION = "Flag that defines if the histogram should be done also (default = false).";
    public static final String OMSRASTERSUMMARY_OUT_MIN_DESCRIPTION = "The min value.";
    public static final String OMSRASTERSUMMARY_OUT_MAX_DESCRIPTION = "The max value.";
    public static final String OMSRASTERSUMMARY_OUT_MEAN_DESCRIPTION = "The mean value.";
    public static final String OMSRASTERSUMMARY_OUT_S_DEV_DESCRIPTION = "The standard deviation value.";
    public static final String OMSRASTERSUMMARY_OUT_RANGE_DESCRIPTION = "The range value.";
    public static final String OMSRASTERSUMMARY_OUT_SUM_DESCRIPTION = "The sum value.";
    public static final String OMSRASTERSUMMARY_OUT_CB_DESCRIPTION = "The histogram.";

    private String[] stats;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMin == null, doReset)) {
            return;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        if (printToConsole) {
            pm.message("Bounds and resolution");
            pm.message("---------------------");
            pm.message(regionMap.toStringJGT());
            pm.message("");
            pm.message("Coordinate Reference System");
            pm.message("---------------------------");
            pm.message(inRaster.getCoordinateReferenceSystem().toWKT());
            pm.message("");
        }

        // TODO use the geotools bridge instead of jaitools:
        // http://svn.osgeo.org/geotools/trunk/modules/library/coverage/src/test/java/org/geotools/coverage/processing/operation/ZonalStasTest.java

        RenderedImage inRI = inRaster.getRenderedImage();
        Polygon regionPolygon = CoverageUtilities.getRegionPolygon(inRaster);
        SimpleFeatureCollection regionFC = FeatureUtilities.featureCollectionFromGeometry(inRaster.getCoordinateReferenceSystem(),
                regionPolygon);

        OmsZonalStats zs = new OmsZonalStats();
        zs.pm = new DummyProgressMonitor();
        zs.inRaster = inRaster;
        zs.inVector = regionFC;
        zs.pPercentageThres = 0;
        zs.process();
        SimpleFeatureCollection outVector = zs.outVector;
        List<SimpleFeature> testList = FeatureUtilities.featureCollectionToList(outVector);
        SimpleFeature feature = testList.get(0);

        if (stats == null) {
            stats = new String[]{Variables.MIN, Variables.MAX, Variables.AVG, Variables.SDEV, Variables.VAR, Variables.SUM};
        }

        for( String statName : stats ) {
            Object attribute = feature.getAttribute(statName);
            if (attribute != null) {
                switch( statName ) {
                case Variables.MIN:
                    outMin = (Double) attribute;
                    break;
                case Variables.MAX:
                    outMax = (Double) attribute;
                    break;
                case Variables.AVG:
                    outMean = (Double) attribute;
                    break;
                case Variables.SDEV:
                    outSdev = (Double) attribute;
                    break;
                case Variables.SUM:
                    outSum = (Double) attribute;
                    break;

                default:
                    break;
                }
            }
        }

        if (outMin != null && outMax != null) {
            outRange = outMax - outMin;
        }
        if (!doHistogram)
            return;

        double[][] cb = new CoupledFieldsMoments().process(inRI, null, pBins, 1, 2, pm, 1);

        int width = inRI.getWidth();
        int height = inRI.getHeight();
        int pixelsNum = width * height;
        outCb = new double[cb.length + 1][3];

        double sum = 0;
        for( int i = 0; i < outCb.length; i++ ) {
            if (i < outCb.length - 1) {
                outCb[i][0] = cb[i][0];
                outCb[i][1] = cb[i][1];
                sum = sum + cb[i][1];
                outCb[i][2] = cb[i][1] * 100.0 / pixelsNum;
            } else {
                outCb[i][0] = HMConstants.doubleNovalue;
                double nans = pixelsNum - sum;
                outCb[i][1] = nans;
                outCb[i][2] = nans * 100.0 / pixelsNum;
            }

        }

    }

    public static double[] getMinMax( GridCoverage2D raster ) throws Exception {
        OmsRasterSummary summary = new OmsRasterSummary();
        summary.inRaster = raster;
        summary.doHistogram = false;
        summary.stats = new String[]{Variables.MIN, Variables.MAX};
        summary.printToConsole = false;
        summary.process();

        double min = summary.outMin;
        double max = summary.outMax;
        return new double[]{min, max};
    }

    public static double[] getMinMaxAvgSum( GridCoverage2D raster ) throws Exception {
        OmsRasterSummary summary = new OmsRasterSummary();
        summary.inRaster = raster;
        summary.doHistogram = false;
        summary.stats = new String[]{Variables.MIN, Variables.MAX, Variables.AVG, Variables.SUM};
        summary.printToConsole = false;
        summary.process();

        double min = summary.outMin;
        double max = summary.outMax;
        double avg = summary.outMean;
        double sum = summary.outSum;
        return new double[]{min, max, avg, sum};
    }

}
