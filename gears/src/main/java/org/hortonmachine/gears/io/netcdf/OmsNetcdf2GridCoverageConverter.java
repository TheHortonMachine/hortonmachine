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
package org.hortonmachine.gears.io.netcdf;
import java.awt.image.WritableRaster;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.imagen.iterator.WritableRandomIter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.locationtech.jts.geom.Coordinate;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.RotatedPole;

@Description("Converts NetCDF grids to rasters: projected grids of known crs keep it, the others are resampled to lat/long WGS84.")
@Documentation(OmsNetcdf2GridCoverageConverter.DOCUMENTATION)
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("netdcf")
@Label(HMConstants.NETCDF)
@Name("netcdfgriddumper")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class OmsNetcdf2GridCoverageConverter extends HMModel implements INetcdfUtils {
    @Description(DESCR_inPath)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inPath = null;

    @Description(DESCR_pGridName)
    @In
    public String pGridName = null;

    @Description(DESCR_pIncludePattern)
    @In
    public String pIncludePattern = null;

    @Description(DESCR_pExcludePattern)
    @In
    public String pExcludePattern = null;

    @Description(DESCR_pFromTimestep)
    @In
    public Integer pFromTimestep = 0;

    @Description(DESCR_pToTimestep)
    @In
    public Integer pToTimestep;

    @Description(DESCR_pFalseEastingCorrection)
    @In
    public Double pFalseEastingCorrection = null;

    @Description(DESCR_pFalseNorthingCorrection)
    @In
    public Double pFalseNorthingCorrection = null;

    @Description(DESCR_pNorthPoleLongitudeCorrection)
    @In
    public Double pNorthPoleLongitudeCorrection = null;

    @Description(DESCR_pNorthPoleLatitudeCorrection)
    @In
    public Double pNorthPoleLatitudeCorrection = null;

    @Description(DESCR_doLatLon)
    @In
    public boolean doLatLon = false;

    @Description(DESCR_outRaster)
    @Out
    public GridCoverage2D outRaster;

    /**
     * The current date processed, null if the grid has no time.
     */
    public Date currentDate;

    public static final String DOCUMENTATION = """
            The grid is the one with the given full name, as listed by NetcdfInfo, or the first one whose name contains the inclusion text and not the exclusion one, or else the first grid of the file. Of grids with a vertical dimension only the first level is read. The x and y axes have to be one dimensional: grids with two dimensional latitude and longitude, as the swaths of satellites, are not supported.

            The values are the physical ones: the scale factor and offset of the variable are applied, so a MODIS temperature stored as integers comes out in kelvin, and the fill and missing values become the novalue -9999 of the output.

            **Projected grids** whose crs is in the file, as an EPSG code or a WKT in the attributes `epsg_code`, `crs_wkt` or `spatial_ref` of their grid mapping variable, as GDAL and RasterToNetcdf write them, keep their grid and crs: the cells are copied as they are, with the rows ordered from north to south. Set doLatLon to resample them to lat/long anyway.

            **All the other grids**, the lat/long ones and the projected ones without a known crs, as the sinusoidal tiles of MODIS, are resampled to lat/long WGS84. The output has the same number of rows and columns of the grid, over the lat/long bounds of its cells: the center of every output cell is projected back to the grid and takes the value of the cell it falls in, a nearest neighbour resampling with no holes, which matches the one of GDAL. The cells outside the grid, as the corners of the bounds of a sinusoidal tile, are novalue. Longitudes from 0 to 360 are supported.

            The axes of the HDF-EOS grids, as the MODIS tiles, are moved by half a cell: netcdf-java gives the upper left corners of their cells instead of the centers.

            The corrections of false easting, false northing and rotated pole are applied to the projection of the file before reading it, to fix files with wrong projection parameters.

            The dumped files are named after the input file, the UTC timestamp of the timestep (left out if the grid has no time) and the name of the grid.
            """;

    public static final String DESCR_outFolder = "The output folder in which to store the dumped rasters.";
    public static final String DESCR_outRaster = "The output raster map generated for the current timestamp.";
    public static final String DESCR_pFalseNorthingCorrection = "An optional correction on the false northing.";
    public static final String DESCR_pFalseEastingCorrection = "An optional correction on the false easting.";
    public static final String DESCR_pNorthPoleLongitudeCorrection = "An optional correction of the north pole longitude of a rotated pole grid.";
    public static final String DESCR_pNorthPoleLatitudeCorrection = "An optional correction of the north pole latitude of a rotated pole grid.";
    public static final String DESCR_pToTimestep = "The index of the timestep at which to stop, excluded. If not set, only the start timestep is dumped.";
    public static final String DESCR_pFromTimestep = "The index of the first timestep to dump.";
    public static final String DESCR_pGridName = "The full name of the grid to dump, as listed by NetcdfInfo.";
    public static final String DESCR_inPath = "The netcdf file or url to dump.";
    public static final String DESCR_pIncludePattern = "In case of no grid name, the first grid whose name contains this text is dumped.";
    public static final String DESCR_pExcludePattern = "In case of no grid name, grids whose name contains this text are skipped.";
    public static final String DESCR_doLatLon = "If true, projected grids are resampled to lat/long WGS84 also when their crs is known.";

    public String selectedGridName = null;

    /**
     * The crs of a projected grid kept in its projection, null if the grid is resampled to lat/long.
     */
    private CoordinateReferenceSystem nativeCrs;

    /**
     * The factor from the axis units of netcdf-java (km for projected grids) to the native crs units.
     */
    private double nativeAxisFactor = 1;

    private ProjectionImpl netcdfProj = null;
    private GridDatatype dumpGrid = null;

    private GridGeometry2D outGridGeometry2D;

    private List<CalendarDate> datesList;

    private Iterator<Integer> timestepIterator;

    private GridCoordSystem coordSys;

    private double[] xAxisValues;

    private double[] yAxisValues;

    private int cols;

    private int rows;

    private double nodata;

    /**
     * The date of a file without time axis, as the _MODIS_Date of HDF-EOS MODIS files.
     */
    private CalendarDate fileDate;

    @Initialize
    public void initProcess() throws Exception {
        if (timestepIterator == null) {
            File infile = new File(inPath);
            if (!infile.exists()) {
                if (!inPath.startsWith("http")) {
                    throw new ModelsIllegalargumentException("The input doesn't exist.", this);
                }
            }

            if (pFromTimestep == null || pFromTimestep < 0) {
                pFromTimestep = 0;
            }
            if (pToTimestep == null || pToTimestep <= pFromTimestep) {
                pToTimestep = pFromTimestep + 1;
            }

            GridDataset gds = GridDataset.open(inPath);
            List<Attribute> globalAttributes = gds.getGlobalAttributes();
            boolean isHdfEos = false;
            for( Attribute attribute : globalAttributes ) {
                // set by the netcdf-java HDF-EOS MODIS convention
                if (attribute.getFullName().equals("_MODIS_Date")) {
                    CalendarDateFormatter f = new CalendarDateFormatter("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    fileDate = f.parse(attribute.getStringValue());
                } else if (attribute.getFullName().equals("_CoordSysBuilder") && attribute.isString()
                        && attribute.getStringValue().contains("HdfEos")) {
                    isHdfEos = true;
                }
            }

            List<GridDatatype> grids = gds.getGrids();
            long count = grids.stream().filter(g -> g.getDimensions().size() >= 2).count();
            pm.message("Grid definitions found: " + count);

            for( GridDatatype grid : grids ) {
                List<Dimension> dimensions = grid.getDimensions();
                if (dimensions.size() >= 2) {
                    String gridName = grid.getFullName();
                    if (pGridName != null) {
                        if (pGridName.equals(gridName)) {
                            dumpGrid = grid;
                            break;
                        }
                    } else {
                        if (pIncludePattern != null) {
                            // check inclusion and exclusion patterns
                            boolean canUse = false;
                            if (gridName.contains(pIncludePattern)) {
                                if (pExcludePattern != null && gridName.contains(pExcludePattern)) {
                                    canUse = false;
                                } else {
                                    canUse = true;
                                }
                            }
                            if (canUse) {
                                // take the first that matches
                                dumpGrid = grid;
                                break;
                            }
                        } else {
                            // take first
                            dumpGrid = grid;
                            break;
                        }
                    }
                }
            }
            if (dumpGrid == null) {
                throw new ModelsIllegalargumentException("No grid found to dump in: " + inPath, this);
            }
            String gridName = dumpGrid.getFullName();
            pm.message("Dumping grid: " + gridName);
            selectedGridName = gridName;

            coordSys = dumpGrid.getCoordinateSystem();
            netcdfProj = coordSys.getProjection();

            fixFalseEasting(netcdfProj);
            fixFalseNorthing(netcdfProj);
            fixNorthPole(netcdfProj);

            CoordinateAxis xAxis = coordSys.getXHorizAxis();
            CoordinateAxis yAxis = coordSys.getYHorizAxis();
            if (xAxis.getRank() != 1 || yAxis.getRank() != 1) {
                throw new ModelsIllegalargumentException("Only grids with 1D x and y axes are supported.", this);
            }

            datesList = new ArrayList<>();
            if (coordSys.hasTimeAxis1D()) {
                CoordinateAxis1DTime tAxis1D = coordSys.getTimeAxis1D();
                datesList.addAll(tAxis1D.getCalendarDates());
            }
            if (datesList.isEmpty()) {
                // a single slice, with the date of the file if it has one
                datesList.add(fileDate);
                pFromTimestep = 0;
                pToTimestep = 1;
            } else if (pToTimestep > datesList.size()) {
                pToTimestep = datesList.size();
                pm.errorMessage("The dataset contains " + datesList.size() + " time slices. toTimestep has been set accordingly.");
            }

            xAxisValues = (double[]) xAxis.read().get1DJavaArray(double.class);
            yAxisValues = (double[]) yAxis.read().get1DJavaArray(double.class);
            cols = xAxisValues.length;
            rows = yAxisValues.length;
            if (isHdfEos) {
                // netcdf-java gives the upper left corners of the HDF-EOS cells, not their centers
                shiftToCellCenters(xAxisValues);
                shiftToCellCenters(yAxisValues);
            }

            nativeCrs = null;
            if (!doLatLon && !(netcdfProj instanceof LatLonProjection) && isRegular(xAxisValues)
                    && isRegular(yAxisValues)) {
                nativeCrs = findNativeCrs(gds, dumpGrid, xAxis.getUnitsString());
            }

            GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, cols, rows);
            if (nativeCrs != null) {
                pm.message("Keeping the grid in its crs: " + nativeCrs.getName());
                double xMin = Math.min(xAxisValues[0], xAxisValues[cols - 1]) * nativeAxisFactor;
                double xMax = Math.max(xAxisValues[0], xAxisValues[cols - 1]) * nativeAxisFactor;
                double yMin = Math.min(yAxisValues[0], yAxisValues[rows - 1]) * nativeAxisFactor;
                double yMax = Math.max(yAxisValues[0], yAxisValues[rows - 1]) * nativeAxisFactor;
                double xRes = cols > 1 ? (xMax - xMin) / (cols - 1) : 1;
                double yRes = rows > 1 ? (yMax - yMin) / (rows - 1) : 1;
                ReferencedEnvelope envelope = new ReferencedEnvelope(xMin - xRes / 2, xMax + xRes / 2, yMin - yRes / 2,
                        yMax + yRes / 2, nativeCrs);
                outGridGeometry2D = new GridGeometry2D(gridRange, envelope);
            } else {
                // the lat/long bounds of the cell centers
                org.locationtech.jts.geom.Envelope env = new org.locationtech.jts.geom.Envelope();
                for( int y = 0; y < rows; y++ ) {
                    for( int x = 0; x < cols; x++ ) {
                        Coordinate coordinate = toLatLong(netcdfProj, new Coordinate(xAxisValues[x], yAxisValues[y]));
                        env.expandToInclude(coordinate);
                    }
                }
                // the output keeps the number of cells of the grid, over the lat/long bounds of the cells
                double xRes = cols > 1 ? env.getWidth() / (cols - 1) : 1;
                double yRes = rows > 1 ? env.getHeight() / (rows - 1) : 1;
                ReferencedEnvelope envelope = new ReferencedEnvelope(env.getMinX() - xRes / 2, env.getMaxX() + xRes / 2,
                        env.getMinY() - yRes / 2, env.getMaxY() + yRes / 2, DefaultGeographicCRS.WGS84);
                outGridGeometry2D = new GridGeometry2D(gridRange, envelope);
            }

            VariableDS variable = dumpGrid.getVariable();
            Number nodataTmp = getNodata(variable);
            nodata = nodataTmp == null ? Double.NaN : nodataTmp.doubleValue();

            timestepIterator = IntStream.range(pFromTimestep, pToTimestep).boxed().iterator();
            doProcess = timestepIterator.hasNext();
            pm.beginTask("Processing " + (pToTimestep - pFromTimestep) + " timesteps...", pToTimestep - pFromTimestep);
        }
    }

    /**
     * Find the crs of a projected grid, from the epsg code or the wkt of its grid mapping, as GDAL
     * and the HortonMachine write them.
     *
     * @return the crs, or null if it is not known or its units are not those of the axes.
     */
    private CoordinateReferenceSystem findNativeCrs( GridDataset gds, GridDatatype grid, String axisUnits ) {
        Attribute gridMappingAttribute = grid.getVariable().findAttribute("grid_mapping");
        if (gridMappingAttribute == null || !gridMappingAttribute.isString()) {
            return null;
        }
        Variable gridMapping = gds.getNetcdfFile().findVariable(gridMappingAttribute.getStringValue());
        if (gridMapping == null) {
            return null;
        }
        CoordinateReferenceSystem crs = null;
        try {
            Attribute epsg = gridMapping.findAttribute("epsg_code");
            if (epsg != null && epsg.isString()) {
                crs = HMCrsRegistry.INSTANCE.getCrs(epsg.getStringValue());
            } else {
                for( String wktName : new String[]{"crs_wkt", "spatial_ref"} ) {
                    Attribute wkt = gridMapping.findAttribute(wktName);
                    if (wkt != null && wkt.isString()) {
                        crs = CRS.parseWKT(wkt.getStringValue());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            pm.errorMessage("Unable to read the crs of the grid, it is resampled to lat/long: " + e.getMessage());
            return null;
        }
        if (crs == null || crs.getCoordinateSystem().getDimension() != 2) {
            return null;
        }
        // netcdf-java gives the projected axes in km
        String crsUnit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
        if (!crsUnit.equals("m")) {
            return null;
        }
        if ("km".equals(axisUnits)) {
            nativeAxisFactor = 1000;
        } else if ("m".equals(axisUnits)) {
            nativeAxisFactor = 1;
        } else {
            return null;
        }
        return crs;
    }

    /**
     * @return true if the values of the axis have the same spacing.
     */
    static boolean isRegular( double[] axis ) {
        if (axis.length < 3) {
            return true;
        }
        double step = (axis[axis.length - 1] - axis[0]) / (axis.length - 1);
        for( int i = 1; i < axis.length; i++ ) {
            if (Math.abs(axis[i] - axis[i - 1] - step) > Math.abs(step) * 1E-4) {
                return false;
            }
        }
        return true;
    }

    /**
     * Move the values of a regular axis from the cell corners to the cell centers.
     *
     * @param axis the corner values, modified in place.
     */
    static void shiftToCellCenters( double[] axis ) {
        if (axis.length > 1) {
            double halfStep = (axis[1] - axis[0]) / 2;
            for( int i = 0; i < axis.length; i++ ) {
                axis[i] += halfStep;
            }
        }
    }

    /**
     * Find the index of the axis value nearest to a coordinate.
     *
     * @param axis the monotonic values of the cell centers of an axis.
     * @param value the coordinate.
     * @return the index of the cell containing the coordinate, or -1 if it is outside the axis.
     */
    static int nearestIndex( double[] axis, double value ) {
        int n = axis.length;
        if (n == 1) {
            return 0;
        }
        boolean ascending = axis[n - 1] > axis[0];
        double halfFirst = Math.abs(axis[1] - axis[0]) / 2;
        double halfLast = Math.abs(axis[n - 1] - axis[n - 2]) / 2;
        double min = ascending ? axis[0] - halfFirst : axis[n - 1] - halfLast;
        double max = ascending ? axis[n - 1] + halfLast : axis[0] + halfFirst;
        if (value < min || value > max) {
            return -1;
        }
        int low = 0;
        int high = n - 1;
        while( high - low > 1 ) {
            int mid = (low + high) >>> 1;
            if (axis[mid] > value == ascending) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return Math.abs(axis[low] - value) <= Math.abs(axis[high] - value) ? low : high;
    }

    private void fixFalseEasting( Object obj ) {
        try {
            if (pFalseEastingCorrection != null) {
                Method method = obj.getClass().getMethod("setFalseEasting", double.class);
                method.invoke(obj, pFalseEastingCorrection.doubleValue());
                pm.errorMessage("Set false easting to: " + pFalseEastingCorrection);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void fixFalseNorthing( Object obj ) {
        try {
            if (pFalseNorthingCorrection != null) {
                Method method = obj.getClass().getMethod("setFalseNorthing", double.class);
                method.invoke(obj, pFalseNorthingCorrection.doubleValue());
                pm.errorMessage("Set false northing to: " + pFalseNorthingCorrection);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void fixNorthPole( Object obj ) {
        try {
            if (pNorthPoleLongitudeCorrection != null || pNorthPoleLatitudeCorrection != null) {
                if (obj instanceof RotatedPole) {
                    RotatedPole rp = (RotatedPole) obj;
                    ProjectionPoint northPole = rp.getNorthPole();
                    double lon = northPole.getX();
                    double lat = northPole.getY();
                    if (pNorthPoleLongitudeCorrection != null) {
                        lon = pNorthPoleLongitudeCorrection;
                    }
                    if (pNorthPoleLatitudeCorrection != null) {
                        lat = pNorthPoleLatitudeCorrection;
                    }
                    netcdfProj = new RotatedPole(lat, lon);
                    pm.errorMessage("Correcting RotatedPole North Pole x/y to: " + lon + " / " + lat);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Execute
    public void process() throws Exception {
        if (timestepIterator != null && timestepIterator.hasNext()) {
            Integer timestep = timestepIterator.next();
            CalendarDate date = datesList.get(timestep);
            currentDate = date == null ? null : date.toDate();

            WritableRandomIter iter = null;
            try {
            	
                WritableRaster wRaster = CoverageUtilities.createWritableRaster(cols, rows, null, null,
                        HMConstants.doubleNovalue);
                iter = CoverageUtilities.getWritableRandomIterator(wRaster);

                // the data are ordered (z,) y, x: the first level is dumped
                Array timestepData = dumpGrid.readVolumeData(timestep);
                int rank = timestepData.getRank();
                Index dataIndex = timestepData.getIndex();
                int[] position = new int[rank];

                ReferencedEnvelope envelope = outGridGeometry2D.getEnvelope2D();
                double west = envelope.getMinX();
                double north = envelope.getMaxY();
                double xRes = envelope.getWidth() / cols;
                double yRes = envelope.getHeight() / rows;
                boolean isLatLon = netcdfProj instanceof LatLonProjection;
                boolean xAscending = cols == 1 || xAxisValues[cols - 1] > xAxisValues[0];
                boolean yAscending = rows == 1 || yAxisValues[rows - 1] > yAxisValues[0];
                for( int row = 0; row < rows; row++ ) {
                    double lat = north - (row + 0.5) * yRes;
                    for( int col = 0; col < cols; col++ ) {
                        int gridCol;
                        int gridRow;
                        if (nativeCrs != null) {
                            // the same grid, with the rows from north to south
                            gridCol = xAscending ? col : cols - 1 - col;
                            gridRow = yAscending ? rows - 1 - row : row;
                        } else {
                            // every output cell takes the value of the grid cell its center falls in
                            double lon = west + (col + 0.5) * xRes;
                            ProjectionPoint projPoint = netcdfProj.latLonToProj(LatLonPoint.create(lat, lon));
                            double x = projPoint.getX();
                            gridCol = nearestIndex(xAxisValues, x);
                            if (gridCol < 0 && isLatLon) {
                                // longitudes of the grid can be in [0, 360]
                                gridCol = nearestIndex(xAxisValues, x + 360);
                                if (gridCol < 0) {
                                    gridCol = nearestIndex(xAxisValues, x - 360);
                                }
                            }
                            gridRow = nearestIndex(yAxisValues, projPoint.getY());
                            if (gridCol < 0 || gridRow < 0) {
                                continue;
                            }
                        }
                        position[rank - 2] = gridRow;
                        position[rank - 1] = gridCol;
                        double value = timestepData.getDouble(dataIndex.set(position));
                        if (!Double.isNaN(value) && value != nodata) {
                            iter.setSample(col, row, 0, value);
                        }
                    }
                }
                RegionMap regionMap = RegionMap.fromGridGeometry(outGridGeometry2D);
                CoordinateReferenceSystem outCrs = nativeCrs != null ? nativeCrs : DefaultGeographicCRS.WGS84;
                outRaster = CoverageUtilities.buildCoverageWithNovalue("raster", wRaster, regionMap, outCrs,
                        HMConstants.doubleNovalue);

            } finally {
                if (iter != null)
                    iter.done();
                pm.worked(1);
                doProcess = timestepIterator.hasNext();
            }
        } else {
            pm.done();
        }

    }

}
