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
package org.jgrasstools.lesto.modules.utilities;

import static java.lang.Math.round;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.media.jai.iterator.WritableRandomIter;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.ALasDataManager;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.chart.Scatter;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;

@Description("Creates vertical slices of a las file. The resulting raster will have the slice height value as valid pixel value.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("slices, lidar, las")
@Label(JGTConstants.LESTO + "/utilities")
@Name("lasslicer")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class LasSlicer extends JGTModel {
    @Description("Las file or folder path.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inLas = null;

    @Description("DTM path for normalization.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDtm = null;

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

    @Execute
    public void process() throws Exception {
        checkNull(inLas);

        boolean doRaster = true;
        if (pMode.equals("chart")) {
            doRaster = false;
        }

        File lasFile = new File(inLas);
        File parentFile = lasFile.getParentFile();
        File outputFolder = new File(parentFile, "vertical_slices");
        if (!outputFolder.exists() && !outputFolder.mkdir()) {
            throw new ModelsIOException("Can't create folder: " + outputFolder, this);
        }

        GridCoverage2D dtm = getRaster(inDtm);

        try (ALasDataManager dataManager = ALasDataManager.getDataManager(lasFile, dtm, pGroundThreshold, null)) {
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
            pm.message("Generating charts of " + chartWidth + "x" + chartHeigth);

            double[] xRange = NumericsUtilities.range2Bins(minX, maxX, 3.0, false);
            double[] yRange = NumericsUtilities.range2Bins(minY, maxY, 3.0, false);

            int tilesNum = xRange.length * yRange.length;
            int tilesCount = 0;
            LinkedHashMap<String, List<LasRecord>> recordsMap = new LinkedHashMap<>();
            pm.beginTask("Producing slices...", (xRange.length - 1));
            for( int x = 0; x < xRange.length - 1; x++ ) {
                for( int y = 0; y < yRange.length - 1; y++ ) {
                    tilesCount++;
                    Envelope env = new Envelope(xRange[x], xRange[x + 1], yRange[y], yRange[y + 1]);
                    Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(env);
                    List<LasRecord> pointsInGeometry = dataManager.getPointsInGeometry(polygon, true);

                    pm.message("Points in tile " + x + "/" + y + "(" + tilesCount + " of " + tilesNum + "): "
                            + pointsInGeometry.size());
                    if (pointsInGeometry.size() == 0) {
                        continue;
                    }
                    for( double z = minZ + pInterval; z < maxZ; z = z + pInterval ) {
                        String key = String.valueOf(z);
                        List<LasRecord> pointsInSlice = recordsMap.get(key);
                        if (pointsInSlice == null) {
                            pointsInSlice = new ArrayList<LasRecord>();
                            recordsMap.put(key, pointsInSlice);
                        }
                        double height = z - minZ;
                        double low = height - pThickness / 2.0;
                        double high = height + pThickness / 2.0;
                        List<LasRecord> pointsInHeightRange = ALasDataManager.getPointsInHeightRange(pointsInGeometry, low, high);
                        if (pointsInHeightRange.size() > 0) {
                            pointsInSlice.addAll(pointsInHeightRange);
                            // pm.message("Added points: " + pointsInHeightRange.size());
                            if (!doRaster)
                                chartPoints(outputFolder, height, pointsInSlice, chartWidth, chartHeigth, minX, maxX, minY, maxY);
                        }
                    }
                }
                pm.worked(1);
            }
            pm.done();

            if (doRaster) {
                Set<Entry<String, List<LasRecord>>> entrySet = recordsMap.entrySet();
                int size = entrySet.size();
                pm.beginTask("Generating rasters...", size);
                for( Entry<String, List<LasRecord>> entry : entrySet ) {
                    double z = Double.parseDouble(entry.getKey());
                    double height = z - minZ;
                    List<LasRecord> points = entry.getValue();

                    dumpRaster(outputFolder, height, points, pResolution, minX, maxX, minY, maxY, crs);
                    pm.worked(1);
                }
                pm.done();
            }

        }
    }

    private void dumpRaster( File outputFolder, double height, List<LasRecord> points, double resolution, double minX,
            double maxX, double minY, double maxY, CoordinateReferenceSystem crs ) throws Exception {

        File rasterFile = new File(outputFolder, "slice_" + height + ".asc");

        int rows = (int) round((maxY - minY) / resolution);
        int cols = (int) round((maxX - minX) / resolution);

        GridGeometry2D gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(maxY, minY, maxX, minX, cols, rows, crs);
        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(gridGeometry);

        WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        Point point = new Point();
        for( LasRecord dot : points ) {
            CoverageUtilities.colRowFromCoordinate(new Coordinate(dot.x, dot.y), gridGeometry, point);
            outIter.setSample(point.x, point.y, 0, height);
        }
        outIter.done();

        GridCoverage2D outRaster = CoverageUtilities.buildCoverage(rasterFile.getName(), outWR, regionMap, crs);
        dumpRaster(outRaster, rasterFile.getAbsolutePath());
    }

    private void chartPoints( File chartFolder, double z, List<LasRecord> pointsInSlice, int width, int height, double minX,
            double maxX, double minY, double maxY ) throws IOException {
        File chartFile = new File(chartFolder, "slice_" + z + ".png");

        int size = pointsInSlice.size();
        double[] xPlanim = new double[size];
        double[] yPlanim = new double[size];
        for( int i = 0; i < size; i++ ) {
            LasRecord dot = pointsInSlice.get(i);
            xPlanim[i] = dot.x;
            yPlanim[i] = dot.y;
        }

        Scatter scatterPlanim = new Scatter("Slice " + z);
        scatterPlanim.addSeries("planimetry", xPlanim, yPlanim);
        scatterPlanim.setShowLines(false);
        scatterPlanim.setXLabel("longitude");
        scatterPlanim.setYLabel("latitude");
        scatterPlanim.setXRange(minX, maxX);
        scatterPlanim.setYRange(minY, maxY);
        BufferedImage imagePlanim = scatterPlanim.getImage(width, height);
        ImageIO.write(imagePlanim, "png", chartFile);

    }

}
