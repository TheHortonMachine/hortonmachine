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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_DO_HISTOGRAM_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_OUT_CB_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_OUT_MAX_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_OUT_MEAN_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_OUT_MIN_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_OUT_RANGE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_OUT_SUM_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_OUT_S_DEV_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_P_BINS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERSUMMARY_STATUS;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.math.CoupledFieldsMoments;
import org.jaitools.media.jai.zonalstats.Result;
import org.jaitools.media.jai.zonalstats.ZonalStats;
import org.jaitools.media.jai.zonalstats.ZonalStatsDescriptor;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;


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

    private Statistic[] stats;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMin == null, doReset)) {
            return;
        }

        // TODO use the geotools bridge instead of jaitools:
        // http://svn.osgeo.org/geotools/trunk/modules/library/coverage/src/test/java/org/geotools/coverage/processing/operation/ZonalStasTest.java

        RenderedImage inRI = inRaster.getRenderedImage();
        ParameterBlockJAI pb = new ParameterBlockJAI("ZonalStats");
        pb.setSource("dataImage", inRI);
        // pb.setSource("zoneImage", null);

        if (stats == null) {
            stats = new Statistic[]{Statistic.MIN, Statistic.MAX, Statistic.MEAN, Statistic.SDEV, Statistic.RANGE, Statistic.SUM};
        }
        pb.setParameter("stats", stats);
        
        // add novalue
        List<Range<Double>> nodata= new ArrayList<>();
        Range<Double> novalueRange = new Range<>(HMConstants.doubleNovalue);
        nodata.add(novalueRange);
        pb.setParameter("noDataRanges", nodata);

        RenderedOp op = JAI.create("ZonalStats", pb);

        ZonalStats zonalStats = (ZonalStats) op.getProperty(ZonalStatsDescriptor.ZONAL_STATS_PROPERTY);
        List<Result> results = zonalStats.results();
        for( Result result : results ) {
            Statistic statistic = result.getStatistic();
            Double value = result.getValue();

            switch( statistic ) {
            case MIN:
                outMin = value;
                break;
            case MAX:
                outMax = value;
                break;
            case MEAN:
                outMean = value;
                break;
            case SDEV:
                outSdev = value;
                break;
            case RANGE:
                outRange = value;
                break;
            case SUM:
                outSum = value;
                break;
            default:
                break;
            }
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
        summary.stats = new Statistic[]{Statistic.MIN, Statistic.MAX};
        summary.process();

        double min = summary.outMin;
        double max = summary.outMax;
        return new double[]{min, max};
    }

    public static double[] getMinMaxAvgSum( GridCoverage2D raster ) throws Exception {
        OmsRasterSummary summary = new OmsRasterSummary();
        summary.inRaster = raster;
        summary.doHistogram = false;
        summary.stats = new Statistic[]{Statistic.MIN, Statistic.MAX, Statistic.MEAN, Statistic.SUM};
        summary.process();

        double min = summary.outMin;
        double max = summary.outMax;
        double avg = summary.outMean;
        double sum = summary.outSum;
        return new double[]{min, max, sum, avg};
    }

}
