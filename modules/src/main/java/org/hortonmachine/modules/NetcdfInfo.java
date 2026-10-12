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
import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.locationtech.jts.geom.Coordinate;

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
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Parameter;

@Description("Prints the content of a NetCDF file or url: its variables, coordinate systems and grids, with their extents and times.")
@Documentation(NetcdfInfo.DOCUMENTATION)
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("IO, NetCDF, HDF, MODIS, Metadata")
@Label(HMConstants.NETCDF)
@Name("_netcdfinfo")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class NetcdfInfo extends HMModel {
    public static final String DOCUMENTATION = """
            The file is read with the Unidata netcdf-java library, which opens much more than NetCDF 3 and 4: also HDF4 and HDF-EOS, as the MODIS products, HDF5, GRIB and the OPeNDAP and THREDDS urls. The report has a first part on the whole file and a second one on its grids.

            The first part lists the global attributes, the coordinate systems that netcdf-java recognizes, their axes with the attributes and the coordinate transforms, which are the projections of the projected grids, and then every variable, with its type, unit and attributes.

            The second part, Grid definitions, lists the variables that are grids, with their **full name**: the one to give to NetcdfGridDumper, as `MODIS_Grid_8Day_1km_LST/Data_Fields/LST_Day_1km` for the groups of an HDF file. For each grid it gives the projection, the range and number of values of the x and y axes, the time axis with its first and last dates, and the bounds in longitude and latitude, computed from all the cells.

            Projected axes are given in kilometers, as netcdf-java converts them, also when the file has them in meters. The bounds are those of the cell centers, half a cell inside the bounds of the cells.
            """;

    @Description("The netcdf file or url to check.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inPath = null;

    @Execute
    public void process() throws Exception {
        StringBuilder sb = new StringBuilder();
        File infile = new File(inPath);
        if (!infile.exists()) {
            if (!inPath.startsWith("http")) {
                throw new ModelsIllegalargumentException("The input doesn't exist.", this);
            }
        }

        DatasetUrl datasetUrl = DatasetUrl.findDatasetUrl(inPath);
        NetcdfDataset netcdfDataset = NetcdfDatasets.acquireDataset(datasetUrl, null);
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
        sb.append(NL);

        List<Attribute> globalAttributes = netcdfDataset.getGlobalAttributes();
        sb.append("Global Attributes").append(NL);
        for( Attribute attribute : globalAttributes ) {
            sb.append(IND).append(attribute.getFullName()).append(": ").append(valueOf(attribute)).append(NL);
        }

        sb.append(NL);
        sb.append("Coordinate systems").append(NL);
        List<CoordinateSystem> coordinateSystems = netcdfDataset.getCoordinateSystems();
        int i = 1;
        for( CoordinateSystem cs : coordinateSystems ) {
            sb.append(IND).append(i++).append(") ").append(cs.getName()).append(": ").append(cs).append(NL);
            sb.append(IND + IND).append("has X/Y CoordAxis + CoordTrans Projection: ").append(cs.isGeoXY()).append(NL);
            sb.append(IND + IND).append("has lat/long CoordAxis: ").append(cs.isLatLon()).append(NL);
            sb.append(IND + IND).append("has radial distance + azimuth CoordAxis: ").append(cs.isRadial()).append(NL);
            sb.append(IND + IND).append("has time Axis: ").append(cs.hasTimeAxis()).append(NL);
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
                sb.append(IND + IND + IND).append(attrName).append(": ").append(valueOf(attribute)).append(NL);
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
                sb.append(IND + IND + IND).append(parameter.getName()).append(": ").append(valueOf(parameter)).append(NL);
            }

        }

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
                    sb.append(IND + IND + IND).append(attr.getFullName()).append(" -> ").append(valueOf(attr, variable)).append(NL);
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

        pm.message(sb.toString());

        sb = new StringBuilder();
        sb.append(NL);
        sb.append("Grid definitions: ").append(NL);
        GridDataset gds = GridDataset.open(inPath);
        List<GridDatatype> grids = gds.getGrids();
        i = 0;
        for( GridDatatype grid : grids ) {
            List<Dimension> dimensions = grid.getDimensions();
            if (dimensions.size() >= 2) {
                String gridName = grid.getFullName();
                List<Attribute> attributes = grid.getAttributes();
                sb.append(IND).append(i++).append(") ").append(gridName).append(": ").append(NL);
                sb.append(IND + IND).append("Attributes:").append(NL);
                for( Attribute attribute : attributes ) {
                    sb.append(IND + IND + IND).append(attribute.getFullName()).append(": ").append(valueOf(attribute, grid.getVariable()))
                            .append(NL);
                }

                // GridDatatype grid = gds.findGridDatatype("pr");
                GridCoordSystem coordSys = grid.getCoordinateSystem();
                ProjectionImpl proj = coordSys.getProjection();

                sb.append(IND + IND).append("Projection: ").append(proj).append(NL);

                CoordinateAxis xAxis = coordSys.getXHorizAxis();
                CoordinateAxis yAxis = coordSys.getYHorizAxis();

                double minXValue = xAxis.getMinValue();
                double maxXValue = xAxis.getMaxValue();
                long xSize = xAxis.getSize();

                String fullName = xAxis.getFullName();
                sb.append(IND + IND).append("Proj X Axis (" + fullName + "): ").append(minXValue).append(" -> ").append(maxXValue)
                        .append("   (" + xSize + ")").append("  extent: ").append(maxXValue - minXValue).append(NL);
                double minYValue = yAxis.getMinValue();
                double maxYValue = yAxis.getMaxValue();
                long ySize = yAxis.getSize();

                fullName = yAxis.getFullName();
                sb.append(IND + IND).append("Proj Y Axis (" + fullName + "): ").append(minYValue).append(" -> ").append(maxYValue)
                        .append("   (" + ySize + ")").append("  extent: ").append(maxYValue - minYValue).append(NL);

                if (coordSys.hasTimeAxis1D()) {
                    CoordinateAxis1DTime tAxis1D = coordSys.getTimeAxis1D();
                    fullName = tAxis1D.getFullName();
                    long tSize = tAxis1D.getSize();
                    List<CalendarDate> dates = tAxis1D.getCalendarDates();
                    sb.append(IND + IND).append("Time Axis (" + fullName + "): ").append(dates.get(0)).append(" -> ")
                            .append(dates.get(dates.size() - 1)).append("   (" + tSize + ")").append(NL);
                }

                Array xValues = xAxis.read();
                Array yValues = yAxis.read();
                int[] xShape = xValues.getShape();
                int[] yShape = yValues.getShape();
                Index xIndex = xValues.getIndex();
                Index yIndex = yValues.getIndex();
                // first find final bounds
                org.locationtech.jts.geom.Envelope env = new org.locationtech.jts.geom.Envelope();
                for( int y = 0; y < yShape[0]; y++ ) {
                    for( int x = 0; x < xShape[0]; x++ ) {
                        double xVal = xValues.getDouble(xIndex.set(x));
                        double yVal = yValues.getDouble(yIndex.set(y));
                        double longitude = xVal;
                        double latitude = yVal;

                        if (!(proj instanceof LatLonProjection)) {
                            LatLonPoint latLonPoint = proj.projToLatLon(xVal, yVal);
                            latitude = latLonPoint.getLatitude();
                            longitude = latLonPoint.getLongitude();
                        }

                        env.expandToInclude(new Coordinate(longitude, latitude));
                    }
                }
                // and create the target gridgeometry
                double lonMin = env.getMinX();
                double latMin = env.getMinY();
                double lonMax = env.getMaxX();
                double latMax = env.getMaxY();
                sb.append(IND + IND).append("Longitude: ").append(lonMin).append(" -> ").append(lonMax).append("  extent: ")
                        .append(lonMax - lonMin).append(NL);
                sb.append(IND + IND).append("Latitude: ").append(latMin).append(" -> ").append(latMax).append("  extent: ")
                        .append(latMax - latMin).append(NL);

            }
        }
        pm.message(sb.toString());
        gds.close();
        netcdfDataset.close();
    }

    /**
     * @return the value of an attribute, numeric ones too, as text.
     */
    private static String valueOf( Attribute attribute ) {
        return valueOf(attribute, null);
    }

    /**
     * @param variable the variable of the attribute, or null for global ones.
     * @return the value of an attribute, numeric ones too, as text.
     */
    private static String valueOf( Attribute attribute, Variable variable ) {
        if (attribute.isString() || attribute.getValues() == null) {
            return attribute.getStringValue();
        }
        // integer attributes, as valid_range and _FillValue, have the type of the variable, which can be
        // unsigned before netcdf-java converts it
        boolean unsignedVariable = variable instanceof VariableDS && ((VariableDS) variable).getOriginalDataType() != null
                && ((VariableDS) variable).getOriginalDataType().isUnsigned() && attribute.getDataType().isIntegral();
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < attribute.getLength(); i++ ) {
            Number number = attribute.getNumericValue(i);
            if (attribute.getDataType().isUnsigned() || unsignedVariable) {
                // the unsigned values are stored in the signed java types
                number = DataType.widenNumberIfNegative(number);
            }
            sb.append(i > 0 ? " " : "").append(number);
        }
        return sb.toString();
    }

    /**
     * @return the value of a parameter, numeric ones too, as text.
     */
    private static String valueOf( Parameter parameter ) {
        if (parameter.isString()) {
            return parameter.getStringValue();
        }
        return Arrays.toString(parameter.getNumericValues());
    }
}
