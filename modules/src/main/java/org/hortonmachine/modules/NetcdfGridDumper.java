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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.data.DataSourceException;
import org.geotools.geometry.Envelope2D;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
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
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.transform.RotatedPole;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.RotatedLatLon;
import ucar.unidata.util.Parameter;

@Description("Dump NetCDF grids to geotools compatible rasters command.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("netdcf")
@Label(HMConstants.NETCDF)
@Name("_netcdfgriddumper")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class NetcdfGridDumper extends HMModel {
    @Description("The netcdf file or url to dump.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inPath = null;

    public Integer pFromTimestep = 0;

    public Integer pToTimestep;

    public boolean doLongitudeShift = false;

    public String outFolder;

    @Execute
    public void process() throws Exception {
        
        
        GridDataset gds = GridDataset.open(inPath);
        List<GridDatatype> grids = gds.getGrids();
        
        long count = grids.stream().filter(g-> g.getDimensions().size() > 2).count();
        pm.message("Grid definitions found: " + count);
        for( GridDatatype grid : grids ) {
            List<Dimension> dimensions = grid.getDimensions();
            if (dimensions.size() > 2) {
                String gridName = grid.getFullName();
                pm.message("Dumping grid: " + gridName);

                try {
                    NetCDFReader netCDFReader = new NetCDFReader(new File(inPath), null);
                    GridCoverage2D read = netCDFReader.read(gridName, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                GridCoordSystem coordSys = grid.getCoordinateSystem();
                ProjectionImpl proj = coordSys.getProjection();
                CoordinateAxis xAxis = coordSys.getXHorizAxis();
                CoordinateAxis yAxis = coordSys.getYHorizAxis();

                List<CalendarDate> datesList = new ArrayList<>();
                if (coordSys.hasTimeAxis1D()) {
                    CoordinateAxis1DTime tAxis1D = coordSys.getTimeAxis1D();
                    datesList = tAxis1D.getCalendarDates();
                }

                Array xValues = xAxis.read();
                Array yValues = yAxis.read();
                int[] xShape = xValues.getShape();
                int[] yShape = yValues.getShape();
                Index xIndex = xValues.getIndex();
                Index yIndex = yValues.getIndex();
                // first find final bounds
                org.locationtech.jts.geom.Envelope env = new org.locationtech.jts.geom.Envelope();
                int rows = yShape[0];
                int cols = xShape[0];
                for( int y = 0; y < rows; y++ ) {
                    for( int x = 0; x < cols; x++ ) {
                        double xVal = xValues.getDouble(xIndex.set(x));
                        double yVal = yValues.getDouble(yIndex.set(y));
                        double longitude = xVal;
                        double latitude = yVal;

                        if (!(proj instanceof LatLonProjection)) {
                            LatLonPoint latLonPoint = proj.projToLatLon(xVal, yVal);
                            latitude = latLonPoint.getLatitude();
                            longitude = latLonPoint.getLongitude();
                        }

                        longitude = checkLongitude(longitude);
                        env.expandToInclude(new Coordinate(longitude, latitude));
                    }
                }
                // and create the target gridgeometry
                double lonMin = env.getMinX();
                double latMin = env.getMinY();
//                double lonMax = env.getMaxX();
//                double latMax = env.getMaxY();

                Envelope envelope = new Envelope2D(DefaultGeographicCRS.WGS84, lonMin, latMin, env.getWidth(), env.getHeight());
                GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, cols, rows);
                GridGeometry2D gridGeometry2D = new GridGeometry2D(gridRange, envelope);

                dumpGrid(grid, coordSys, proj, xAxis, yAxis, datesList, gridGeometry2D);

            }
        }

    }

    private double checkLongitude( double longitude ) {
        if (doLongitudeShift) {
            if (longitude < 0) {
                longitude = longitude + 180.0;
            } else {
                longitude = longitude - 180.0;
            }
        }
        return longitude;
    }

    private void dumpGrid( GridDatatype grid, GridCoordSystem coordSys, ProjectionImpl proj, CoordinateAxis xAxis,
            CoordinateAxis yAxis, List<CalendarDate> datesList, GridGeometry2D gridGeometry2D ) throws Exception {

        Array xValues = xAxis.read();
        Array yValues = yAxis.read();
        int[] xShape = xValues.getShape();
        int[] yShape = yValues.getShape();
        Index xIndex = xValues.getIndex();
        Index yIndex = yValues.getIndex();

        VariableDS variable = grid.getVariable();
        Number nodata = NetCDFUtilities.getNodata(variable);
        if (nodata == null) {
            nodata = HMConstants.doubleNovalue;
        }

        if (pToTimestep == null || pToTimestep < 0 || pToTimestep < pFromTimestep) {
            pToTimestep = pFromTimestep;
        }

        int totalWork = pToTimestep - pFromTimestep;
        if (totalWork == 0) {
            totalWork = 1;
        }
        pm.beginTask("Processing " + totalWork + " timesteps...", totalWork);
        for( int tsIndex = pFromTimestep; tsIndex <= pToTimestep; tsIndex++ ) {
            WritableRandomIter iter = null;
            try {
                WritableRaster wRaster = CoverageUtilities.createWritableRaster(xShape[0], yShape[0], null, null,
                        nodata.doubleValue());
                iter = CoverageUtilities.getWritableRandomIterator(wRaster);

                Array firsTsData = grid.readVolumeData(tsIndex);
                IndexIterator indexIterator = firsTsData.getIndexIterator();
                for( int y = 0; y < yShape[0]; y++ ) {
                    for( int x = 0; x < xShape[0]; x++ ) {
                        double xVal = xValues.getDouble(xIndex.set(x));
                        double yVal = yValues.getDouble(yIndex.set(y));
                        double latitude = yVal;
                        double longitude = xVal;
                        if (!(proj instanceof LatLonProjection)) {
                            LatLonPoint latLonPoint = proj.projToLatLon(xVal, yVal);
                            latitude = latLonPoint.getLatitude();
                            longitude = latLonPoint.getLongitude();
                        }

                        longitude = checkLongitude(longitude);
//                    int[] xy = coordSys.findXYindexFromLatLon(latitude, longitude, null);
//                    Array data = grid.readDataSlice(0, 0, xy[1], xy[0]); // note order is t, z,y, x
//                    double value = data.getDouble(0);

                        if (indexIterator.hasNext()) {
                            Object next = indexIterator.next();
                            if (next instanceof Number) {
                                double value = ((Number) next).doubleValue();

                                Coordinate coordinate = new Coordinate(longitude, latitude);
                                int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry2D, null);
                                if (colRow[0] < xShape[0] && colRow[1] < yShape[0])
                                    try {
                                        iter.setSample(colRow[0], colRow[1], 0, value);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                            }

                        }

                    }
                }
                RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(gridGeometry2D);
                GridCoverage2D outcoverage = CoverageUtilities.buildCoverageWithNovalue("raster", wRaster, regionMap,
                        DefaultGeographicCRS.WGS84, nodata.doubleValue());

                CalendarDate calendarDate = datesList.get(tsIndex);
                Date date = calendarDate.toDate();
                SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String tsString = f.format(date);
                File folderFile = new File(outFolder);
                File inFile = new File(inPath);
                String name = FileUtilities.getNameWithoutExtention(inFile);
                name = name + "__" + tsString + ".tif";
                File outFile = new File(folderFile, name);
                dumpRaster(outcoverage, outFile.getAbsolutePath());
            } finally {
                if (iter != null)
                    iter.done();
                pm.worked(1);
            }
        }
        pm.done();

    }

    public static void main( String[] args ) throws Exception {
        NetcdfGridDumper i = new NetcdfGridDumper();
        i.doLongitudeShift = false;
        i.inPath = "/home/hydrologis/TMP/KLAB/cordex_scenarios/03_tas_EUR-11_CNRM-CERFACS-CNRM-CM5_rcp85_r1i1p1_CNRM-ALADIN63_v2_day_20510101-20551231.nc";
        i.pFromTimestep = 0;
        i.pToTimestep = 10;
        i.outFolder = "/home/hydrologis/TMP/KLAB/cordex_scenarios/dumps";
        i.process();
    }
}
