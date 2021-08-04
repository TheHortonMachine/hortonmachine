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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.image.Raster;
import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModelIM;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

@Description("Calculate zonal stats on image mosaic datasets.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("zonalstats, image mosaic")
@Label(HMConstants.RASTERPROCESSING)
@Name("zonalstats_im")
@Status(Status.EXPERIMENTAL)
@License(OMSHYDRO_LICENSE)
public class OmsZonalStatsIM extends HMModelIM {

    @Description("The image mosaic map to process..")
    @In
    public String inRaster = null;

    @Description("The polygons map on which to do the stats.")
    @In
    public SimpleFeatureCollection inVector = null;

    @Description("The size of the bins to use for pretiling the region.")
    @In
    public double pBinSize = 10000;

    @Description("Percentage of minimum active cells to have a valid stat.")
    @In
    public double pPercentageThres = 20.0;

    @Description("Total meanvalue (also produced by this module) for the calculation of the mean absolute deviation.")
    @In
    public Double pTotalMean = null;

    @Description("The input polygons with the added stats values.")
    @Out
    public SimpleFeatureCollection outVector;

    /**
     * The array holding:
     * <ul>
     *  <li>totalMean</li>
     *  <li>userTotalMean</li>
     *  <li>totalActiveCells</li>
     * </ul>
     * if {@link #pTotalMean} is != <code>null</code>.
     */
    double[] tm_usertm_tactivecells = new double[3];

    @Execute
    public void process() throws Exception {
        checkNull(inVector);

        addSource(new File(inRaster));

        boolean hasUserTotalMean = false;
        if (pTotalMean != null) {
            hasUserTotalMean = true;
            tm_usertm_tactivecells[1] = pTotalMean;
        }

        ReferencedEnvelope bounds = inVector.getBounds();
        double[] xBins = NumericsUtilities.range2Bins(bounds.getMinX(), bounds.getMaxX(), pBinSize, false);
        double[] yBins = NumericsUtilities.range2Bins(bounds.getMinY(), bounds.getMaxY(), pBinSize, false);

        SimpleFeatureBuilder featureBuilder = OmsZonalStats.createFeatureBuilder(bounds.getCoordinateReferenceSystem(),
                hasUserTotalMean);

        outVector = new DefaultFeatureCollection();
        List<Geometry> geometriesList = FeatureUtilities.featureCollectionToGeometriesList(inVector, true, null);

        ConcurrentLinkedQueue<Geometry> allGeometriesQueue = new ConcurrentLinkedQueue<Geometry>();
        allGeometriesQueue.addAll(geometriesList);
        ConcurrentLinkedQueue<Geometry> keepGeometriesQueue;
        ConcurrentLinkedQueue<Geometry> removeGeometriesQueue;
        pm.beginTask("Processing polygons...", xBins.length - 1);
        for( int x = 0; x < xBins.length - 1; x++ ) {
            for( int y = 0; y < yBins.length - 1; y++ ) {
                Envelope envelope = new Envelope(xBins[x], xBins[x + 1], yBins[y], yBins[y + 1]);
                Envelope readEnvelope = null;
                keepGeometriesQueue = new ConcurrentLinkedQueue<Geometry>();
                removeGeometriesQueue = new ConcurrentLinkedQueue<Geometry>();
                for( Geometry geometry : allGeometriesQueue ) {
                    Envelope geometryenvelope = geometry.getEnvelopeInternal();
                    if (geometryenvelope.intersects(envelope)) {
                        removeGeometriesQueue.add(geometry);
                        if (readEnvelope == null) {
                            readEnvelope = new Envelope(geometryenvelope);
                        } else {
                            readEnvelope.expandToInclude(geometryenvelope);
                        }
                    } else {
                        keepGeometriesQueue.add(geometry);
                    }
                }
                allGeometriesQueue = keepGeometriesQueue;
                if (removeGeometriesQueue.size() == 0) {
                    continue;
                }

                // pm.message("" + readEnvelope);
                GridCoverage2D readGC = getGridCoverage(0, readEnvelope);
                double novalue = HMConstants.getNovalue(readGC);
                GridGeometry2D gridGeometry = readGC.getGridGeometry();
                Raster readRaster = readGC.getRenderedImage().getData();

                RandomIter readIter = RandomIterFactory.create(readRaster, null);
                for( Geometry geometry : removeGeometriesQueue ) {
                    double[] polygonStats = OmsZonalStats.polygonStats(geometry, gridGeometry, readIter, novalue,
                            hasUserTotalMean, tm_usertm_tactivecells, pPercentageThres, pm);
                    if (polygonStats == null) {
                        continue;
                    }

                    Object[] values;
                    if (pTotalMean == null) {
                        values = new Object[]{geometry, //
                                polygonStats[0], //
                                polygonStats[1], //
                                polygonStats[2], //
                                polygonStats[3], //
                                polygonStats[4], //
                                (int) polygonStats[5], //
                                (int) polygonStats[6] //
                        };
                    } else {
                        values = new Object[]{geometry, //
                                polygonStats[0], //
                                polygonStats[1], //
                                polygonStats[2], //
                                polygonStats[3], //
                                polygonStats[4], //
                                polygonStats[5], //
                                (int) polygonStats[6], //
                                (int) polygonStats[7] //
                        };
                    }

                    featureBuilder.addAll(values);
                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    ((DefaultFeatureCollection) outVector).add(feature);
                }
            }
            pm.worked(1);
        }
        pm.done();

        if (!hasUserTotalMean) {
            tm_usertm_tactivecells[0] = tm_usertm_tactivecells[0] / tm_usertm_tactivecells[2];
            pm.message("Total mean: " + tm_usertm_tactivecells[0]);
        }

        dispose();
    }

    protected void processCell( int readCol, int readRow, int writeCol, int writeRow, int readCols, int readRows, int writeCols,
            int writeRows ) {
        // not used in this case
    }
}
