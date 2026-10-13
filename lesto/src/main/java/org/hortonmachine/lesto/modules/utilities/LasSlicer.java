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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.chart.Scatter;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.jfree.data.xy.XYSeries;
import org.locationtech.jts.geom.Envelope;
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
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description("Cuts a point cloud in horizontal slices, at regular elevations or heights over the ground, and writes a raster or a chart of the points of each slice.")
@Documentation(LasSlicer.DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("slices, lidar, las")
@Label(HMConstants.LESTO + "/utilities")
@Name("lasslicer")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class LasSlicer extends HMModel {
    @Description("The lidar data: a las file, the index file of a folder of las files created by LasIndexer or a las database.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas = null;

    @Description("An optional dtm: if given, the slices are cut at heights over the ground instead of elevations.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm = null;

    @Description("The distance between the centers of the slices, which are multiples of it.")
    @In
    @Unit("m")
    public double pInterval = 1.0;

    @Description("The slice thickness.")
    @In
    @Unit("m")
    public double pThickness = 0.8;

    @Description("With the dtm, the points not higher than this over the ground are left out.")
    @In
    @Unit("m")
    public double pGroundThreshold = 0.5;

    @Description("Type of output to use.")
    @UI("combo:raster,chart")
    @In
    public String pMode = "raster";

    @Description("The resolution of the rasters, in raster mode.")
    @In
    @Unit("m")
    public double pResolution = 0.5;

    @Description("The folder into which the slices are written, one file each, named after the center of the slice.")
    @UI(HMConstants.FOLDEROUT_UI_HINT)
    @In
    public String outFolder = null;

    public static final String DOCUMENTATION = """
            The slices are horizontal layers of the given thickness, with their centers on the multiples of the interval between the lowest and the highest point: with an interval of 1 m, at 1, 2, 3 m and so on. Without the dtm the values are the elevations of the points, with it their heights over the ground, so that each slice follows the terrain, as a layer of the forest at a given height.

            A slice keeps the points with a value over its lower bound and up to its upper bound. With a thickness smaller than the interval there are gaps between the slices, with a larger one the slices overlap and a point can be in two of them.

            In raster mode each slice is a GeoTIFF, `slice_<center>.tif`, on a grid of the given resolution over the bounds of the data: the cells with at least one point of the slice have the center of the slice as value, the others no value. In chart mode each slice is a png image, `slice_<center>.png`, with the points of the slice in plan view. The slices with no points are not written.
            """;

    private static final DecimalFormat NAME_FORMAT = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.ROOT));

    @Execute
    public void process() throws Exception {
        checkNull(inLas, outFolder);
        if (pInterval <= 0 || pThickness <= 0) {
            throw new ModelsIllegalargumentException("The interval and the thickness have to be larger than 0.", this);
        }
        boolean doRaster = !pMode.trim().equals("chart");
        if (doRaster && pResolution <= 0) {
            throw new ModelsIllegalargumentException("The resolution has to be larger than 0.", this);
        }

        File outputFolder = new File(outFolder);
        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            throw new ModelsIOException("Can't create folder: " + outputFolder, this);
        }

        GridCoverage2D dtm = null;
        if (inDtm != null) {
            dtm = getRaster(inDtm);
        }
        boolean useHeight = dtm != null;

        try (ALasDataManager dataManager = ALasDataManager.getDataManager(new File(inLas), dtm, pGroundThreshold, null)) {
            dataManager.open();
            ReferencedEnvelope3D dataEnvelope = dataManager.getEnvelope3D();
            CoordinateReferenceSystem crs = dataEnvelope.getCoordinateReferenceSystem();

            double west = dataEnvelope.getMinX();
            double south = dataEnvelope.getMinY();
            double xDelta = dataEnvelope.getWidth();
            double yDelta = dataEnvelope.getHeight();

            Polygon dataPolygon = GeometryUtilities.createPolygonFromEnvelope(new Envelope(west, dataEnvelope.getMaxX(), south,
                    dataEnvelope.getMaxY()));
            pm.beginTask("Reading the points...", -1);
            List<LasRecord> points = dataManager.getPointsInGeometry(dataPolygon, true);
            pm.done();
            if (points.isEmpty()) {
                pm.errorMessage("No points to slice.");
                return;
            }
            double minValue = Double.POSITIVE_INFINITY;
            double maxValue = Double.NEGATIVE_INFINITY;
            for( LasRecord p : points ) {
                double value = useHeight ? p.groundElevation : p.z;
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            }

            int cols = 0;
            int rows = 0;
            RegionMap region = null;
            if (doRaster) {
                cols = Math.max(1, (int) Math.ceil(xDelta / pResolution));
                rows = Math.max(1, (int) Math.ceil(yDelta / pResolution));
                region = RegionMap.fromBoundsAndGrid(west, west + cols * pResolution, south, south + rows * pResolution, cols,
                        rows);
            }
            int chartWidth = 1600;
            int chartHeight = (int) (chartWidth * yDelta / xDelta);

            // the slices that reach the values of the points
            long firstSlice = (long) Math.ceil((minValue - pThickness / 2.0) / pInterval);
            long lastSlice = (long) Math.floor((maxValue + pThickness / 2.0) / pInterval);
            pm.beginTask("Slicing...", (int) (lastSlice - firstSlice + 1));
            for( long slice = firstSlice; slice <= lastSlice; slice++ ) {
                double center = slice * pInterval;
                double low = center - pThickness / 2.0;
                double high = center + pThickness / 2.0;
                String name = "slice_" + NAME_FORMAT.format(center);

                if (doRaster) {
                    try (HMRaster sliceRaster = new HMRaster.HMRasterWritableBuilder().setName(name).setRegion(region)
                            .setCrs(crs).setNoValue(HMConstants.doubleNovalue).build()) {
                        int count = 0;
                        for( LasRecord p : points ) {
                            double value = useHeight ? p.groundElevation : p.z;
                            if (value > low && value <= high) {
                                // the points on the north and east bounds go to the last cells
                                int col = Math.min(cols - 1, Math.max(0, (int) Math.floor((p.x - west) / pResolution)));
                                int row = Math.min(rows - 1,
                                        Math.max(0, (int) Math.floor((region.getNorth() - p.y) / pResolution)));
                                sliceRaster.setValue(col, row, center);
                                count++;
                            }
                        }
                        if (count > 0) {
                            sliceRaster.writeToFile(new File(outputFolder, name + ".tif").getAbsolutePath());
                        }
                    }
                } else {
                    XYSeries xySeries = new XYSeries("planimetry");
                    for( LasRecord p : points ) {
                        double value = useHeight ? p.groundElevation : p.z;
                        if (value > low && value <= high) {
                            xySeries.add(p.x, p.y);
                        }
                    }
                    if (xySeries.getItemCount() > 0) {
                        Scatter scatterPlanim = new Scatter("Slice " + NAME_FORMAT.format(center));
                        scatterPlanim.addSeries(xySeries);
                        scatterPlanim.setShowLines(Arrays.asList(false));
                        scatterPlanim.setXLabel("easting");
                        scatterPlanim.setYLabel("northing");
                        scatterPlanim.setXRange(west, dataEnvelope.getMaxX());
                        scatterPlanim.setYRange(south, dataEnvelope.getMaxY());
                        BufferedImage imagePlanim = scatterPlanim.getImage(chartWidth, chartHeight);
                        ImageIO.write(imagePlanim, "png", new File(outputFolder, name + ".png"));
                    }
                }
                pm.worked(1);
            }
            pm.done();
        }
    }

}
