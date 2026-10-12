/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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

import java.io.IOException;

import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.datum.Ellipsoid;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.joda.time.DateTime;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

@Description(OmsGridCoverage2NetcdfConverter.DESCRIPTION)
@Documentation(OmsGridCoverage2NetcdfConverter.DOCUMENTATION)
@Author(name = OmsGridCoverage2NetcdfConverter.AUTHORNAMES, contact = OmsGridCoverage2NetcdfConverter.AUTHORCONTACTS)
@Keywords(OmsGridCoverage2NetcdfConverter.KEYWORDS)
@Label(OmsGridCoverage2NetcdfConverter.LABEL)
@Name(OmsGridCoverage2NetcdfConverter.NAME)
@Status(OmsGridCoverage2NetcdfConverter.STATUS)
@License(OmsGridCoverage2NetcdfConverter.LICENSE)
public class OmsGridCoverage2NetcdfConverter extends HMModel {

    @Description(DESCR_inRaster)
    @In
    public GridCoverage2D inRaster;

    @Description(DESCR_pTimestamp)
    @In
    public String pTimestamp;

    @Description(DESCR_pVariableName)
    @In
    public String pVariableName = "data";

    @Description(DESCR_pLongName)
    @In
    public String pLongName;

    @Description(DESCR_pUnits)
    @In
    public String pUnits;

    @Description(DESCR_outPath)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPath;

    public static final String DESCRIPTION = "Writes rasters to a CF NetCDF file: a single raster, or a time series of rasters on the same grid, one per timestep.";
    public static final String DOCUMENTATION = """
            The file is a NetCDF 3 classic file following the CF 1.8 conventions, written in pure Java, readable by GDAL, and so QGIS, by netcdf-java and by the other CF readers. The values are written as 32 bit floats, with the novalue of the raster as fill value; the classic format limits a variable to about 2 GB.

            A raster in lat/long WGS84 gets `lat` and `lon` axes in degrees, a projected one `x` and `y` axes in the units of its crs. The axes hold the cell centers, with the rows from south to north, as most CF files have them.

            The crs is described in the grid mapping variable `crs`: as WKT in the attributes `crs_wkt` (CF) and `spatial_ref` (GDAL), with its EPSG code in `epsg_code` when it has one, and with the ellipsoid. For the transverse Mercator projections, as UTM and the Gauss-Boaga zones, also the CF projection parameters are written, which other CF readers, as netcdf-java, use.

            A time series has an unlimited time axis in seconds since 1970-01-01 UTC, and the rasters are written one at a time, so long series don't need to fit in memory. All its rasters must have the same grid and crs. Without timestamp the file has no time axis and holds a single raster.

            NetcdfGridDumper reads the files back with their crs and grid unchanged.
            """;
    public static final String KEYWORDS = "IO, NetCDF, CF, Raster, Time series, Writing";
    public static final String LABEL = HMConstants.NETCDF;
    public static final String NAME = "rastertonetcdf";
    public static final int STATUS = Status.CERTIFIED;
    public static final String LICENSE = HMConstants.GPL3_LICENSE;
    public static final String AUTHORNAMES = "Andrea Antonello";
    public static final String AUTHORCONTACTS = "https://g-ant.eu";
    public static final String DESCR_inRaster = "The raster to write: in a time series, the raster of the current timestep.";
    public static final String DESCR_pTimestamp = "The UTC timestamp of the raster, as yyyy-MM-dd HH:mm. If not set, the file has no time and holds a single raster.";
    public static final String DESCR_pVariableName = "The name of the variable of the rasters.";
    public static final String DESCR_pLongName = "The optional long name of the variable.";
    public static final String DESCR_pUnits = "The optional units of the values of the variable.";
    public static final String DESCR_outPath = "The NetCDF file to write.";

    public static final String CRS_VARIABLE = "crs";
    public static final String TIME_UNITS = "seconds since 1970-01-01 00:00:00";

    private NetcdfFormatWriter writer;
    private HMRaster firstRaster;
    private boolean hasTime;
    private int timeIndex = 0;
    private int cols;
    private int rows;
    private float fillValue;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, outPath, pVariableName);

        HMRaster raster = HMRaster.fromGridCoverage(inRaster);
        if (writer == null) {
            createFile(raster);
        } else {
            if (!hasTime) {
                throw new ModelsIllegalargumentException("A file without timestamps can hold only one raster.", this);
            }
            if (pTimestamp == null) {
                throw new ModelsIllegalargumentException("All the rasters of a time series need a timestamp.", this);
            }
            if (raster.getCols() != cols || raster.getRows() != rows
                    || !raster.getRegionMap().toEnvelope().equals(firstRaster.getRegionMap().toEnvelope())
                    || !HMCrsRegistry.crsEquals(raster.getCrs(), firstRaster.getCrs())) {
                throw new ModelsIllegalargumentException("All the rasters of a time series must have the same grid.", this);
            }
        }

        writeData(raster);
        timeIndex++;
    }

    private void createFile( HMRaster raster ) throws Exception {
        firstRaster = raster;
        hasTime = pTimestamp != null;
        cols = raster.getCols();
        rows = raster.getRows();
        double novalue = raster.getNovalue();
        fillValue = Double.isNaN(novalue) ? Float.NaN : (float) novalue;

        CoordinateReferenceSystem crs = raster.getCrs();
        boolean isGeographic = crs instanceof GeographicCRS;
        String xName = isGeographic ? "lon" : "x";
        String yName = isGeographic ? "lat" : "y";

        NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(outPath);
        builder.addAttribute(new Attribute("Conventions", "CF-1.8"));
        builder.addAttribute(new Attribute("source", "HortonMachine"));

        if (hasTime) {
            builder.addUnlimitedDimension("time");
            builder.addVariable("time", DataType.DOUBLE, "time")//
                    .addAttribute(new Attribute("standard_name", "time"))//
                    .addAttribute(new Attribute("units", TIME_UNITS))//
                    .addAttribute(new Attribute("calendar", "standard"))//
                    .addAttribute(new Attribute("axis", "T"));
        }
        builder.addDimension(yName, rows);
        builder.addDimension(xName, cols);
        if (isGeographic) {
            builder.addVariable(yName, DataType.DOUBLE, yName)//
                    .addAttribute(new Attribute("standard_name", "latitude"))//
                    .addAttribute(new Attribute("units", "degrees_north"))//
                    .addAttribute(new Attribute("axis", "Y"));
            builder.addVariable(xName, DataType.DOUBLE, xName)//
                    .addAttribute(new Attribute("standard_name", "longitude"))//
                    .addAttribute(new Attribute("units", "degrees_east"))//
                    .addAttribute(new Attribute("axis", "X"));
        } else {
            String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
            builder.addVariable(yName, DataType.DOUBLE, yName)//
                    .addAttribute(new Attribute("standard_name", "projection_y_coordinate"))//
                    .addAttribute(new Attribute("units", unit))//
                    .addAttribute(new Attribute("axis", "Y"));
            builder.addVariable(xName, DataType.DOUBLE, xName)//
                    .addAttribute(new Attribute("standard_name", "projection_x_coordinate"))//
                    .addAttribute(new Attribute("units", unit))//
                    .addAttribute(new Attribute("axis", "X"));
        }

        Variable.Builder< ? > crsVariable = builder.addVariable(CRS_VARIABLE, DataType.INT, "");
        addGridMapping(crsVariable, crs);

        String dims = (hasTime ? "time " : "") + yName + " " + xName;
        Variable.Builder< ? > dataVariable = builder.addVariable(pVariableName, DataType.FLOAT, dims);
        if (pLongName != null) {
            dataVariable.addAttribute(new Attribute("long_name", pLongName));
        }
        if (pUnits != null) {
            dataVariable.addAttribute(new Attribute("units", pUnits));
        }
        dataVariable.addAttribute(new Attribute("_FillValue", fillValue));
        dataVariable.addAttribute(new Attribute("grid_mapping", CRS_VARIABLE));

        writer = builder.build();

        // the cell centers, with the rows from south to north as most CF files have them
        double[] xValues = new double[cols];
        for( int c = 0; c < cols; c++ ) {
            xValues[c] = raster.getWorld(c, 0).x;
        }
        double[] yValues = new double[rows];
        for( int r = 0; r < rows; r++ ) {
            yValues[r] = raster.getWorld(0, rows - 1 - r).y;
        }
        writer.write(writer.findVariable(xName), Array.factory(DataType.DOUBLE, new int[]{cols}, xValues));
        writer.write(writer.findVariable(yName), Array.factory(DataType.DOUBLE, new int[]{rows}, yValues));
    }

    /**
     * Describe the crs in the grid mapping variable: as WKT, for GDAL too, and with the CF
     * parameters of the projections netcdf-java can read.
     */
    private void addGridMapping( Variable.Builder< ? > crsVariable, CoordinateReferenceSystem crs ) throws Exception {
        String wkt = crs.toWKT();
        crsVariable.addAttribute(new Attribute("crs_wkt", wkt));
        crsVariable.addAttribute(new Attribute("spatial_ref", wkt));
        try {
            String code = HMCrsRegistry.getCodeFromCrs(crs);
            if (code != null) {
                crsVariable.addAttribute(new Attribute("epsg_code", code));
            }
        } catch (Exception e) {
            // no code, the wkt is enough
        }

        Ellipsoid ellipsoid = org.geotools.referencing.CRS.getEllipsoid(crs);
        if (ellipsoid != null) {
            crsVariable.addAttribute(new Attribute("semi_major_axis", ellipsoid.getSemiMajorAxis()));
            crsVariable.addAttribute(new Attribute("inverse_flattening", ellipsoid.getInverseFlattening()));
        }
        if (crs instanceof GeographicCRS) {
            crsVariable.addAttribute(new Attribute("grid_mapping_name", "latitude_longitude"));
        } else if (crs instanceof ProjectedCRS) {
            ProjectedCRS projectedCRS = (ProjectedCRS) crs;
            String method = projectedCRS.getConversionFromBase().getMethod().getName().getCode();
            if (method.replace('_', ' ').equalsIgnoreCase("Transverse Mercator")) {
                crsVariable.addAttribute(new Attribute("grid_mapping_name", "transverse_mercator"));
                for( GeneralParameterValue value : projectedCRS.getConversionFromBase().getParameterValues().values() ) {
                    if (value instanceof ParameterValue) {
                        String name = value.getDescriptor().getName().getCode();
                        double v = ((ParameterValue< ? >) value).doubleValue();
                        String cfName = switch (name) {
                        case "central_meridian" -> "longitude_of_central_meridian";
                        case "latitude_of_origin" -> "latitude_of_projection_origin";
                        case "scale_factor" -> "scale_factor_at_central_meridian";
                        case "false_easting" -> "false_easting";
                        case "false_northing" -> "false_northing";
                        default -> null;
                        };
                        if (cfName != null) {
                            crsVariable.addAttribute(new Attribute(cfName, v));
                        }
                    }
                }
            }
        }
    }

    private void writeData( HMRaster raster ) throws IOException, InvalidRangeException {
        float[] data = new float[rows * cols];
        int k = 0;
        // from south to north, as the y axis
        for( int r = rows - 1; r >= 0; r-- ) {
            for( int c = 0; c < cols; c++ ) {
                double value = raster.getValue(c, r);
                data[k++] = raster.isNovalue(value) ? fillValue : (float) value;
            }
        }
        if (hasTime) {
            long seconds = DateTime.parse(pTimestamp, HMConstants.utcDateFormatterYYYYMMDDHHMM).getMillis() / 1000L;
            writer.write(writer.findVariable("time"), new int[]{timeIndex},
                    Array.factory(DataType.DOUBLE, new int[]{1}, new double[]{seconds}));
            writer.write(writer.findVariable(pVariableName), new int[]{timeIndex, 0, 0},
                    Array.factory(DataType.FLOAT, new int[]{1, rows, cols}, data));
        } else {
            writer.write(writer.findVariable(pVariableName), Array.factory(DataType.FLOAT, new int[]{rows, cols}, data));
        }
    }

    @Finalize
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
