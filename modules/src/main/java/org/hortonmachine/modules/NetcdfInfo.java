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
package org.hortonmachine.modules;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.geometry.Envelope;

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
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.Parameter;

@Description("NetdcfInfo command.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("netdcf")
@Label(HMConstants.GDAL)
@Name("_netcdfinfo")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class NetcdfInfo extends HMModel {
    @Description("The netcdf file or url to check.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inPath = null;

    @Execute
    public void process() throws Exception {
        StringBuilder sb = new StringBuilder();
        File infile = new File(inPath);
        if (!infile.exists()) {
            throw new ModelsIllegalargumentException("File doesn't exist.", this);
        }
        NetcdfDataset netcdfDataset = NetCDFUtilities.getDataset(inPath);
        List<Variable> variables = netcdfDataset.getVariables();
        String NL = "\n";
        String IND = "\t";

        String fileTypeId = netcdfDataset.getFileTypeId();
        String fileTypeVersion = netcdfDataset.getFileTypeVersion();
        String fileTypeDescription = netcdfDataset.getFileTypeDescription();
        String location = netcdfDataset.getLocation();
        String title = netcdfDataset.getTitle();

        sb.append("File ").append(infile.getName()).append(NL);
        sb.append("File type id: ").append(fileTypeId).append(NL);
        sb.append("File type version: ").append(fileTypeVersion).append(NL);
        sb.append("File type description: ").append(fileTypeDescription).append(NL);

        sb.append("Coordinate systems").append(NL);
        List<CoordinateSystem> coordinateSystems = netcdfDataset.getCoordinateSystems();
        int i = 1;
        for( CoordinateSystem cs : coordinateSystems ) {
            sb.append(IND).append(i++).append(") ").append(cs.getName()).append(": ").append(cs).append(NL);
        }
        sb.append(NL);
        sb.append("Coordinate Axes").append(NL);
        List<CoordinateAxis> coordinateAxes = netcdfDataset.getCoordinateAxes();
        i = 1;
        for( CoordinateAxis ca : coordinateAxes ) {
            AxisType axisType = ca.getAxisType();
            List<Attribute> attributes = ca.getAttributes();
            sb.append(IND).append(i++).append(") ").append(ca.getFullName()).append(": ").append(NL);

            sb.append(IND + IND).append("Axis Type: ").append(axisType).append(NL);
            sb.append(IND + IND).append("Class: ").append(ca.getClass().getSimpleName()).append(NL);
            sb.append(IND + IND).append("Attributes: ").append(NL);
            for( Attribute attribute : attributes ) {
                String attrName = attribute.getFullName();
                Array attrValue = attribute.getValues();
                sb.append(IND + IND + IND).append(attrName).append(": ").append(attrValue).append(NL);
            }

        }
        List<CoordinateTransform> coordinateTransforms = netcdfDataset.getCoordinateTransforms();
        sb.append(NL);
        sb.append("Coordinate Transforms").append(NL);
        i = 1;
        for( CoordinateTransform ct : coordinateTransforms ) {
            sb.append(IND).append(i++).append(") ").append(ct.getName()).append(": ").append(NL);
            sb.append(IND + IND).append("Authority").append(": ").append(ct.getAuthority()).append(NL);
            sb.append(IND + IND).append("Transform type").append(": ").append(ct.getTransformType()).append(NL);
            sb.append(IND + IND).append("Parameters").append(": ").append(NL);
            List<Parameter> parameters = ct.getParameters();
            for( Parameter parameter : parameters ) {
                sb.append(IND + IND + IND).append(parameter.getName()).append(": ").append(parameter.getStringValue()).append(NL);
            }

        }

//        NetCDFTimeUtilities.getTimeUnits(title, null)

//        GridDataset gds = GridDataset.open(inPath);

        i = 1;
        sb.append("Variables").append(NL);
        for( Variable variable : variables ) {
            DataType dataType = variable.getDataType();
            String fullName = variable.getFullName();
            String unitsString = variable.getUnitsString();
            boolean isCoordinateVariable = variable.isCoordinateVariable();
            boolean isScalar = variable.isScalar();
            String className = variable.getClass().getSimpleName();

            sb.append(IND).append(i++).append(")").append(fullName).append(": ").append(NL);
            sb.append(IND + IND).append("Class: ").append(className).append(NL);
            sb.append(IND + IND).append("Is coordinate: ").append(isCoordinateVariable).append(NL);
            sb.append(IND + IND).append("Is scalar: ").append(isScalar).append(NL);
            sb.append(IND + IND).append("DataType: ").append(dataType.name()).append(NL);
            sb.append(IND + IND).append("Unit: ").append(unitsString).append(NL);

//            Map<String, Object> annotations = variable.getAnnotations();
//            List<Dimension> dimensions = variable.getDimensions();
//
//            String datasetLocation = variable.getDatasetLocation();
//
//            String nameAndDimensions = variable.getNameAndDimensions();

            List<Attribute> attributes = variable.getAttributes();
            if (attributes.size() > 0) {
                sb.append(IND + IND).append("Attributes: ").append(NL);
                for( Attribute attr : attributes ) {
                    sb.append(IND + IND + IND).append(attr.getFullName()).append(" -> ").append(attr.getStringValue()).append(NL);
                }
            }

            List<Range> ranges = variable.getRanges();
            if (ranges.size() > 0) {
                sb.append(IND + IND).append("Ranges: ").append(NL);
                for( Range range : ranges ) {
                    sb.append(IND + IND + IND).append(range.first()).append(" -> ").append(range.last()).append(NL);
                }
            }

        }

        sb.append(NL);
        sb.append("Grid definitions: ").append(NL);
        GridDataset gds = GridDataset.open(inPath);
        List<GridDatatype> grids = gds.getGrids();
        i = 0;
        for( GridDatatype grid : grids ) {
            List<Dimension> dimensions = grid.getDimensions();
            if (dimensions.size() > 2) {
                String gridName = grid.getFullName();
                sb.append(IND).append(i++).append(")").append(gridName).append(": ").append(NL);

                // GridDatatype grid = gds.findGridDatatype("pr");
                GridCoordSystem coordSys = grid.getCoordinateSystem();
                ProjectionImpl proj = coordSys.getProjection();
                CoordinateAxis xAxis = coordSys.getXHorizAxis();
                CoordinateAxis yAxis = coordSys.getYHorizAxis();

                double minXValue = xAxis.getMinValue();
                double maxXValue = xAxis.getMaxValue();
                long xSize = xAxis.getSize();
                double xDelta = (maxXValue - minXValue) / xSize;

                String fullName = xAxis.getFullName();
                sb.append(IND + IND).append("X Axis (" + fullName + "): ").append(minXValue).append(" -> ").append(maxXValue)
                        .append("   (" + xSize + ")").append(NL);
                double minYValue = yAxis.getMinValue();
                double maxYValue = yAxis.getMaxValue();
                long ySize = yAxis.getSize();
                double yDelta = (maxYValue - minYValue) / ySize;

                fullName = yAxis.getFullName();
                sb.append(IND + IND).append("Y Axis (" + fullName + "): ").append(minYValue).append(" -> ").append(maxYValue)
                        .append("   (" + ySize + ")").append(NL);

                if (coordSys.hasTimeAxis1D()) {
                    CoordinateAxis1DTime tAxis1D = coordSys.getTimeAxis1D();
                    fullName = tAxis1D.getFullName();
                    long tSize = tAxis1D.getSize();
                    List<CalendarDate> dates = tAxis1D.getCalendarDates();
                    sb.append(IND + IND).append("Time Axis (" + fullName + "): ").append(dates.get(0)).append(" -> ")
                            .append(dates.get(dates.size() - 1)).append("   (" + tSize + ")").append(NL);
                }

                ProjectionRect boundingBox = coordSys.getBoundingBox();
                sb.append(IND + IND).append("ProjectionRect: ").append(boundingBox).append(NL);
                double minX = boundingBox.getMinX();
                double minY = boundingBox.getMinY();
                double maxX = boundingBox.getMaxX();
                double maxY = boundingBox.getMaxY();

                LatLonRect latLonBoundingBox = coordSys.getLatLonBoundingBox();
                double latMin = latLonBoundingBox.getLatMin();
                double latMax = latLonBoundingBox.getLatMax();
                double latDelta = (latMax - latMin) / ySize;
                double lonMin = latLonBoundingBox.getLonMin();
                double lonMax = latLonBoundingBox.getLonMax();
                double lonDelta = (lonMax - lonMin) / xSize;

                int[] xyMin = coordSys.findXYindexFromLatLonBounded(latMin, lonMin, null);
                int[] xyMax = coordSys.findXYindexFromLatLonBounded(latMax, lonMax, null);
                sb.append(IND + IND).append("Lat Long X: ").append(lonMin).append(" -> ").append(lonMax).append(NL);
                sb.append(IND + IND).append("Lat Long Y: ").append(latMin).append(" -> ").append(latMax).append(NL);

                Envelope envelope = new Envelope2D(DefaultGeographicCRS.WGS84, lonMin, latMin, lonMax - lonMin, latMax - latMin);
                GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, (int) xSize, (int) ySize);
                GridGeometry2D gridGeometry2D = new GridGeometry2D(gridRange, envelope);

                dumpGrid(grid, coordSys, proj, xAxis, yAxis, gridGeometry2D);

//                WritableRaster wRaster = CoverageUtilities.createWritableRaster((int) xSize, (int) ySize, null, null, null);
//                WritableRandomIter iter = CoverageUtilities.getWritableRandomIterator(wRaster);
//                try {
////                    Array firsTsData = grid.readVolumeData(0);
////                    IndexIterator indexIterator = firsTsData.getIndexIterator();
//
//                    int fromRow = xyMin[1];
//                    int toRow = xyMax[1];
//                    int fromCol = xyMin[0];
//                    int toCol = xyMax[0];
//
//                    for( int r = fromRow, row = 0; r <= toRow; r++, row++ ) {
//                        for( int c = fromCol, col = 0; c <= toCol; c++, col++ ) {
//                            Array data = grid.readDataSlice(0, 0, r, c); // note order is t, z,y, x
//                            double value = data.getDouble(0);
//                            iter.setSample(col, row, 0, value);
//                        }
//                    }
//
////                    for( int row = 0; row < ySize; row++ ) {
//////                        double lat = minYValue + row * yDelta;
////                        for( int col = 0; col < xSize; col++ ) {
//////                            double lon = minXValue + col * xDelta;
////
////                            Array data = grid.readDataSlice(0, 0, row, col); // note order is t, z,
////                                                                             // y, x
////                            double value = data.getDouble(0);
////                            iter.setSample(col, row, 0, value);
//////                            if (indexIterator.hasNext()) {
//////                                Object next = indexIterator.next();
//////                                if (next instanceof Number) {
//////                                    double value = ((Number) next).doubleValue();
//////
//////                                    iter.setSample(col, row, 0, value);
////////                                    int[] xy = coordSys.findXYindexFromLatLon(lat, lon, null);
////////                                    LatLonPoint latLon = coordSys.getLatLon(xy[0], xy[1]);
//////                                }
//////                            } else {
//////                                throw new RuntimeException();
//////                            }
////                        }
////                    }
//                    RegionMap regionMap = new RegionMap();
//                    regionMap.put(CoverageUtilities.NORTH, latMax);
//                    regionMap.put(CoverageUtilities.SOUTH, latMin);
//                    regionMap.put(CoverageUtilities.WEST, lonMin);
//                    regionMap.put(CoverageUtilities.EAST, lonMax);
//                    regionMap.put(CoverageUtilities.XRES, xDelta);
//                    regionMap.put(CoverageUtilities.YRES, yDelta);
//                    regionMap.put(CoverageUtilities.ROWS, (double) ySize);
//                    regionMap.put(CoverageUtilities.COLS, (double) xSize);
//                    GridCoverage2D outcoverage = CoverageUtilities.buildCoverage("raster", wRaster, regionMap,
//                            DefaultGeographicCRS.WGS84);
//
//                    dumpRaster(outcoverage, "/home/hydrologis/TMP/KLAB/cordex_scenarios/01_pr_first_ts.tiff");
//                } finally {
//                    iter.done();
//                }
            }
        }

        System.out.println(sb.toString());

    }

    private void dumpGrid( GridDatatype grid, GridCoordSystem coordSys, ProjectionImpl proj, CoordinateAxis xAxis,
            CoordinateAxis yAxis, GridGeometry2D gridGeometry2D ) throws Exception {
        Array xValues = xAxis.read();
        Array yValues = yAxis.read();
        int[] xShape = xValues.getShape();
        int[] yShape = yValues.getShape();
        Index xIndex = xValues.getIndex();
        Index yIndex = yValues.getIndex();

        WritableRaster wRaster = CoverageUtilities.createWritableRaster(xShape[0], yShape[0], null, null,
                HMConstants.doubleNovalue);
        WritableRandomIter iter = CoverageUtilities.getWritableRandomIterator(wRaster);

        try {
            Array firsTsData = grid.readVolumeData(0);
            IndexIterator indexIterator = firsTsData.getIndexIterator();
            pm.beginTask("Processing...", yShape[0]);
            for( int j = 0; j < yShape[0]; j++ ) {
                for( int ii = 0; ii < xShape[0]; ii++ ) {
                    double xVal = xValues.getDouble(xIndex.set(ii));
                    double yVal = yValues.getDouble(yIndex.set(j));
                    double latitude = proj.projToLatLon(xVal, yVal).getLatitude();
                    double longitude = proj.projToLatLon(xVal, yVal).getLongitude();
//                    int[] xy = coordSys.findXYindexFromLatLon(latitude, longitude, null);
//                    Array data = grid.readDataSlice(0, 0, xy[1], xy[0]); // note order is t, z,y, x
//                    double value = data.getDouble(0);

                    if (indexIterator.hasNext()) {
                        Object next = indexIterator.next();
                        if (next instanceof Number) {
                            double value = ((Number) next).doubleValue();

                            int[] colRow = CoverageUtilities.colRowFromCoordinate(new Coordinate(longitude, latitude),
                                    gridGeometry2D, null);
                            if (colRow[0] < xShape[0] && colRow[1] < yShape[0])
                                try {
                                    iter.setSample(colRow[0], colRow[1], 0, value);
                                } catch (Exception e) {
                                    // ignore
                                }
                        }

                    }

//                    System.out.println(xVal + " " + yVal + " " + latitude + "  " + longitude + " " + value);
//                    System.out.println();

                }
                pm.worked(1);
            }
            pm.done();

            RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(gridGeometry2D);
            GridCoverage2D outcoverage = CoverageUtilities.buildCoverageWithNovalue("raster", wRaster, regionMap,
                    DefaultGeographicCRS.WGS84, HMConstants.doubleNovalue);

            dumpRaster(outcoverage, "/home/hydrologis/TMP/KLAB/cordex_scenarios/01_pr_first_ts.tiff");
        } finally {
            iter.done();
        }
    }

    public static void main( String[] args ) throws Exception {
        NetcdfInfo i = new NetcdfInfo();
        i.inPath = "/home/hydrologis/TMP/KLAB/cordex_scenarios/01_pr_EUR-11_IPSL-IPSL-CM5A-MR_rcp45_r1i1p1_SMHI-RCA4_v1_day_20460101-20501231.nc";
//        i.inPath = "/home/hydrologis/TMP/KLAB/cordex_scenarios/02_tas_AFR-22_CCCma-CanESM2_rcp85_r1i1p1_CCCma-CanRCM4_r2_day_20510101-20551231.nc";
        i.process();
    }
}
