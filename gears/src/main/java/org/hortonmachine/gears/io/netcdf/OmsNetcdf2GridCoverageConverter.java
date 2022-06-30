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

import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
import ucar.ma2.IndexIterator;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.RotatedPole;

@Description("Dump NetCDF grids to geotools Gridcoverage converting them to lat/lon EPSG:4326.")
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

    @Description(DESCR_outRaster)
    @Out
    public GridCoverage2D outRaster;

    /**
     * The current date processed.
     */
    public Date currentDate;

    public static final String DESCR_outFolder = "The output folder in which to store the dumped rasters.";
    public static final String DESCR_outRaster = "The output raster map generated for the current timestamp.";
    public static final String DESCR_pFalseNorthingCorrection = "An optional correction on the false northing.";
    public static final String DESCR_pFalseEastingCorrection = "An optional correction on the false easting.";
    public static final String DESCR_pNorthPoleLongitudeCorrection = "An optional correction of the north pole northing .";
    public static final String DESCR_pNorthPoleLatitudeCorrection = "An optional correction on the false easting.";
    public static final String DESCR_doLongitudeShift = "If set to true, a shift of the longitude by 180 degrees is performed.";
    public static final String DESCR_pToTimestep = "The timestap index at which to end to dump. If not set defaults to the start index.";
    public static final String DESCR_pFromTimestep = "The timestap index from which to start to dump.";
    public static final String DESCR_pGridName = "The name of the variable of the grid to dump.";
    public static final String DESCR_inPath = "The netcdf file or url to dump.";

    private ProjectionImpl netcdfProj = null;
    private GridDatatype dumpGrid = null;

    private GridGeometry2D outGridGeometry2D;

    private List<CalendarDate> datesList;

    private Iterator<Integer> timestepIterator;

    private GridCoordSystem coordSys;

    private Array xValues;

    private Array yValues;

    private int[] xShapes;

    private int[] yShapes;

    private Index xIndex;

    private Index yIndex;

    private double nodata;

    @Initialize
    public void initProcess() throws Exception {
        if (timestepIterator == null) {
            File infile = new File(inPath);
            if (!infile.exists()) {
                if (!inPath.startsWith("http")) {
                    throw new ModelsIllegalargumentException("The input doesn't exist.", this);
                }
            }

            if (pToTimestep == null || pToTimestep < 0 || pToTimestep < pFromTimestep) {
                pToTimestep = pFromTimestep + 1;
            }
            if (pFromTimestep == null || pFromTimestep < 0) {
                pFromTimestep = 0;
            }

            GridDataset gds = GridDataset.open(inPath);
            List<GridDatatype> grids = gds.getGrids();
            long count = grids.stream().filter(g -> g.getDimensions().size() > 2).count();
            pm.message("Grid definitions found: " + count);

            for( GridDatatype grid : grids ) {
                List<Dimension> dimensions = grid.getDimensions();
                if (dimensions.size() > 2) {
                    String gridName = grid.getFullName();
                    if (pGridName != null) {
                        if (pGridName.equals(gridName)) {
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
            if (dumpGrid != null) {
                String gridName = dumpGrid.getFullName();
                pm.message("Dumping grid: " + gridName);

                coordSys = dumpGrid.getCoordinateSystem();
                netcdfProj = coordSys.getProjection();

                fixFalseEasting(netcdfProj);
                fixFalseNorthing(netcdfProj);
                fixNorthPole(netcdfProj);

                CoordinateAxis xAxis = coordSys.getXHorizAxis();
                CoordinateAxis yAxis = coordSys.getYHorizAxis();

                datesList = new ArrayList<>();
                if (coordSys.hasTimeAxis1D()) {
                    CoordinateAxis1DTime tAxis1D = coordSys.getTimeAxis1D();
                    datesList = tAxis1D.getCalendarDates();
                }
                int datesCount = datesList.size();
                if (datesCount <= pToTimestep) {
                    pToTimestep = datesCount;
                    pm.errorMessage("The dataset contains " + datesCount + " time slices. toTimestep has been set accordingly.");
                }

                xValues = xAxis.read();
                yValues = yAxis.read();
                xShapes = xValues.getShape();
                yShapes = yValues.getShape();
                xIndex = xValues.getIndex();
                yIndex = yValues.getIndex();

                // first find target bounds
                org.locationtech.jts.geom.Envelope env = new org.locationtech.jts.geom.Envelope();
                int rows = yShapes[0];
                int cols = xShapes[0];
                for( int y = 0; y < rows; y++ ) {
                    for( int x = 0; x < cols; x++ ) {
                        double xVal = xValues.getDouble(xIndex.set(x));
                        double yVal = yValues.getDouble(yIndex.set(y));
                        Coordinate coordinate = new Coordinate(xVal, yVal);

                        coordinate = toLatLong(netcdfProj, coordinate);

                        env.expandToInclude(coordinate);
                    }
                }
                double lonMin = env.getMinX();
                double latMin = env.getMinY();

                Envelope envelope = new Envelope2D(DefaultGeographicCRS.WGS84, lonMin, latMin, env.getWidth(), env.getHeight());
                GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, cols, rows);
                outGridGeometry2D = new GridGeometry2D(gridRange, envelope);

                VariableDS variable = dumpGrid.getVariable();
                Number nodataTmp = getNodata(variable);
                if (nodataTmp == null) {
                    nodata = HMConstants.doubleNovalue;
                } else {
                    nodata = nodataTmp.doubleValue();
                }

                timestepIterator = IntStream.range(pFromTimestep, pToTimestep).boxed().iterator();
                doProcess = timestepIterator.hasNext();
                int totalWork = pToTimestep - pFromTimestep;
                if (totalWork == 0) {
                    totalWork = 1;
                }
                pm.beginTask("Processing " + totalWork + " timesteps...", totalWork);
            }
        }
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
                        lon = pNorthPoleLatitudeCorrection;
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
            currentDate = datesList.get(timestep).toDate();

            WritableRandomIter iter = null;
            try {
                WritableRaster wRaster = CoverageUtilities.createWritableRaster(xShapes[0], yShapes[0], null, null, nodata);
                iter = CoverageUtilities.getWritableRandomIterator(wRaster);

                Array timestepData = dumpGrid.readVolumeData(timestep);
                IndexIterator indexIterator = timestepData.getIndexIterator();
                for( int y = 0; y < yShapes[0]; y++ ) {
                    for( int x = 0; x < xShapes[0]; x++ ) {
                        double xVal = xValues.getDouble(xIndex.set(x));
                        double yVal = yValues.getDouble(yIndex.set(y));

                        Coordinate coordinate = new Coordinate(xVal, yVal);

                        coordinate = toLatLong(netcdfProj, coordinate);

                        if (indexIterator.hasNext()) {
                            Object next = indexIterator.next();
                            if (next instanceof Number) {
                                double value = ((Number) next).doubleValue();

                                int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, outGridGeometry2D, null);
                                if (colRow[0] < xShapes[0] && colRow[1] < yShapes[0]) {
                                    try {
                                        iter.setSample(colRow[0], colRow[1], 0, value);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                        }

                    }
                }
                RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(outGridGeometry2D);
                outRaster = CoverageUtilities.buildCoverageWithNovalue("raster", wRaster, regionMap, DefaultGeographicCRS.WGS84,
                        nodata);

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
