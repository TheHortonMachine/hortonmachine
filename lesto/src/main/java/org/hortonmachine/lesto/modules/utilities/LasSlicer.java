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

import static java.lang.Math.round;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import javax.imageio.ImageIO;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.chart.Scatter;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.jfree.data.xy.XYSeries;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
import oms3.annotations.Unit;

@Description("Creates vertical slices of a las file. The resulting raster will have the slice height value as valid pixel value.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("slices, lidar, las")
@Label(HMConstants.LESTO + "/utilities")
@Name("lasslicer")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class LasSlicer extends HMModel {
    @Description("Las file or folder path.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas = null;

    @Description("The slicing interval.")
    @In
    @Unit("m")
    public double pInterval = 1.0;

    @Description("The slice thickness.")
    @In
    @Unit("m")
    public double pThickness = 0.8;

    @Description("Threshold from ground (-1 means no threshold).")
    @In
    @Unit("m")
    public double pGroundThreshold = 0.5;

    @Description("Type of output to use.")
    @UI("combo: chart, raster")
    @In
    public String pMode = "raster";

    @Description("Raster resolution in case of raster mode..")
    @In
    @Unit("m")
    public double pResolution = 0.5;

    private WritableRandomIter outIter;

    private GridGeometry2D gridGeometry;

    private WritableRaster outWR;

    private boolean doRaster = true;

    @Execute
    public void process() throws Exception {
        checkNull(inLas);

        if (pMode.equals("chart")) {
            doRaster = false;
        }

        File lasFile = new File(inLas);
        File parentFile = lasFile.getParentFile();
        File outputFolder = new File(parentFile, "vertical_slices");
        if (!outputFolder.exists() && !outputFolder.mkdir()) {
            throw new ModelsIOException("Can't create folder: " + outputFolder, this);
        }

        try (ALasDataManager dataManager = ALasDataManager.getDataManager(lasFile, null, pGroundThreshold, null)) {
            dataManager.open();
            ReferencedEnvelope3D dataEnvelope = dataManager.getEnvelope3D();
            CoordinateReferenceSystem crs = dataEnvelope.getCoordinateReferenceSystem();

            double minX = dataEnvelope.getMinX();
            double minY = dataEnvelope.getMinY();
            double minZ = dataEnvelope.getMinZ();
            double maxX = dataEnvelope.getMaxX();
            double maxY = dataEnvelope.getMaxY();
            double maxZ = dataEnvelope.getMaxZ();

            double xDelta = maxX - minX;
            double yDelta = maxY - minY;
            int chartWidth = 1600;
            int chartHeigth = (int) (chartWidth * yDelta / xDelta);
            if (!doRaster)
                pm.message("Generating charts of " + chartWidth + "x" + chartHeigth);

            double[] xRange = NumericsUtilities.range2Bins(minX, maxX, 3.0, false);
            double[] yRange = NumericsUtilities.range2Bins(minY, maxY, 3.0, false);

            for( double z = minZ + pInterval; z < maxZ; z = z + pInterval ) {
                double low = z - pThickness / 2.0;
                double high = z + pThickness / 2.0;
                Predicate< ? super LasRecord> checkHeight = p -> {
                    if (p.z > low && p.z <= high) {
                        return true;
                    }
                    return false;
                };

                XYSeries xySeries = new XYSeries("planimetry");
                pm.beginTask("Working on slice of elevation " + z, (xRange.length - 1));
                for( int x = 0; x < xRange.length - 1; x++ ) {
                    for( int y = 0; y < yRange.length - 1; y++ ) {
                        Envelope currentTileEnvelope = new Envelope(xRange[x], xRange[x + 1], yRange[y], yRange[y + 1]);
                        Polygon currentTilePolygon = GeometryUtilities.createPolygonFromEnvelope(currentTileEnvelope);
                        List<LasRecord> pointsInCurrentTile = dataManager.getPointsInGeometry(currentTilePolygon, true);
                        if (pointsInCurrentTile.size() == 0) {
                            continue;
                        }

                        // double miz = Double.POSITIVE_INFINITY;
                        // double maz = Double.NEGATIVE_INFINITY;
                        // for( LasRecord lr : pointsInCurrentTile ) {
                        // miz = Math.min(miz, lr.groundElevation);
                        // maz = Math.max(maz, lr.groundElevation);
                        // }
                        // System.out.println(miz + "/" + maz + " -> " + low + "/" + high);

                        double _z = z;
                        if (doRaster) {
                            pointsInCurrentTile.parallelStream().filter(checkHeight).forEach(p -> {
                                if (outIter == null) {
                                    int rows = (int) round((maxY - minY) / pResolution);
                                    int cols = (int) round((maxX - minX) / pResolution);
                                    gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(maxY, minY, maxX, minX, cols,
                                            rows, crs);
                                    outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null,
                                            HMConstants.doubleNovalue);
                                    outIter = CoverageUtilities.getWritableRandomIterator(outWR);
                                }
                                Point point = new Point();
                                CoverageUtilities.colRowFromCoordinate(new Coordinate(p.x, p.y), gridGeometry, point);
                                outIter.setSample(point.x, point.y, 0, _z);
                            });
                        } else {
                            pointsInCurrentTile.stream().filter(checkHeight).forEach(p -> {
                                try {
                                    xySeries.add(p.x, p.y);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }

                    }
                    pm.worked(1);
                }
                pm.done();
                if (doRaster && outIter != null) {
                    outIter.done();
                    File rasterFile = new File(outputFolder, "slice_" + z + ".asc");
                    RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(gridGeometry);
                    GridCoverage2D outRaster = CoverageUtilities.buildCoverage(rasterFile.getName(), outWR, regionMap, crs);
                    dumpRaster(outRaster, rasterFile.getAbsolutePath());
                    outIter = null;
                } else {
                    int size = xySeries.getItemCount();
                    if (size > 0) {
                        File chartFile = new File(outputFolder, "slice_" + z + ".png");
                        pm.message("Generate chart with points: " + size);
                        Scatter scatterPlanim = new Scatter("Slice " + z);
                        scatterPlanim.addSeries(xySeries);
                        scatterPlanim.setShowLines(Arrays.asList(false));
                        scatterPlanim.setXLabel("longitude");
                        scatterPlanim.setYLabel("latitude");
                        scatterPlanim.setXRange(minX, maxX);
                        scatterPlanim.setYRange(minY, maxY);
                        BufferedImage imagePlanim = scatterPlanim.getImage(chartWidth, chartHeigth);
                        ImageIO.write(imagePlanim, "png", chartFile);
                    } else {
                        pm.message("No points in slice.");
                    }
                }

            }

        }
    }

    public static void main( String[] args ) throws Exception {
        LasSlicer ls = new LasSlicer();
        ls.inLas = "/media/hydrologis/Samsung_T3/UNIBZ/monticolo_tls/monticolo2018_point_cloud_02.sqlite";
        ls.pInterval = 0.5;
        ls.pThickness = 0.5;
        ls.pMode = "raster";
        ls.process();
    }

}
