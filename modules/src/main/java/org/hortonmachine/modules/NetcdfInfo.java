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
import java.util.List;

import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

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
import ucar.ma2.Range;
import ucar.nc2.Attribute;
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
        GridDatatype grid = gds.findGridDatatype("pr");
        GridCoordSystem coordSys = grid.getCoordinateSystem();
        CoordinateAxis xAxis = coordSys.getXHorizAxis();
        CoordinateAxis yAxis = coordSys.getYHorizAxis();

        double minValue = xAxis.getMinValue();
        double maxValue = xAxis.getMaxValue();
        String fullName = xAxis.getFullName();
        sb.append(IND).append("X Axis (" + fullName + "): ").append(minValue).append(" -> ").append(maxValue).append(NL);
        minValue = yAxis.getMinValue();
        maxValue = yAxis.getMaxValue();
        fullName = yAxis.getFullName();
        sb.append(IND).append("Y Axis (" + fullName + "): ").append(minValue).append(" -> ").append(maxValue).append(NL);

        if (coordSys.hasTimeAxis1D()) {
            CoordinateAxis1DTime tAxis1D = coordSys.getTimeAxis1D();
            fullName = tAxis1D.getFullName();
            List<CalendarDate> dates = tAxis1D.getCalendarDates();
            sb.append(IND).append("Time Axis (" + fullName + "): ").append(dates.get(0)).append(" -> ").append(dates.get(dates.size() - 1)).append(NL);
        }

//        Array firsTsData = grid.readVolumeData(0);

        System.out.println(sb.toString());

    }

    public static void main( String[] args ) throws Exception {
        NetcdfInfo i = new NetcdfInfo();
        i.inPath = "/home/hydrologis/TMP/KLAB/cordex_scenarios/01_pr_EUR-11_IPSL-IPSL-CM5A-MR_rcp45_r1i1p1_SMHI-RCA4_v1_day_20460101-20501231.nc";
//        i.inPath = "/home/hydrologis/TMP/KLAB/cordex_scenarios/02_tas_AFR-22_CCCma-CanESM2_rcp85_r1i1p1_CCCma-CanRCM4_r2_day_20510101-20551231.nc";
        i.process();
    }
}
