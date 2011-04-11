/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules.r.summary;

import jaitools.media.jai.zonalstats.Result;
import jaitools.media.jai.zonalstats.ZonalStats;
import jaitools.media.jai.zonalstats.ZonalStatsDescriptor;
import jaitools.numeric.Statistic;

import java.awt.image.RenderedImage;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import oms3.annotations.Author;
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.math.CoupledFieldsMoments;

@Description("Calculate a summary of the map with base statistics.")
@Documentation("CoverageSummary.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Statistics, Raster, Mapcalc")
@Label(JGTConstants.RASTERPROCESSING)
@Name("rsummary")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class CoverageSummary extends JGTModel {

    @Description("The map to analize.")
    @In
    public GridCoverage2D inMap;

    @Description("The number of bins for the histogram (default = 100).")
    @In
    public int pBins = 100;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The min value.")
    @Out
    public Double outMin = null;

    @Description("The max value.")
    @Out
    public Double outMax = null;

    @Description("The mean value.")
    @Out
    public Double outMean = null;

    @Description("The standard deviation value.")
    @Out
    public Double outSdev = null;

    @Description("The range value.")
    @Out
    public Double outRange = null;

    @Description("The sum value.")
    @Out
    public Double outSum = null;

    @Description("The histogram.")
    @Out
    public double[][] outCb = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMin == null, doReset)) {
            return;
        }
        RenderedImage inRI = inMap.getRenderedImage();
        ParameterBlockJAI pb = new ParameterBlockJAI("ZonalStats");
        pb.setSource("dataImage", inRI);
        // pb.setSource("zoneImage", null);

        Statistic[] stats = {
                Statistic.MIN, 
                Statistic.MAX, 
                Statistic.MEAN, 
                Statistic.SDEV,
                Statistic.RANGE, 
                Statistic.SUM};
        pb.setParameter("stats", stats);

        RenderedOp op = JAI.create("ZonalStats", pb);

        ZonalStats zonalStats = (ZonalStats) op
                .getProperty(ZonalStatsDescriptor.ZONAL_STATS_PROPERTY);
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
                outCb[i][0] = JGTConstants.doubleNovalue;
                double nans = pixelsNum - sum;
                outCb[i][1] = nans;
                outCb[i][2] = nans * 100.0 / pixelsNum;
            }

        }

    }

}
