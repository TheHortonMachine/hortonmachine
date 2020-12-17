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
package org.hortonmachine.hmachine.modules.networktools.trento_p;

import static org.hortonmachine.gears.libs.modules.HMConstants.OTHER;
import static org.hortonmachine.gears.libs.modules.Variables.CALIBRATION;
import static org.hortonmachine.gears.libs.modules.Variables.PROJECT;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_LABEL;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_LICENSE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_NAME;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_STATUS;

import java.io.File;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.CalibrationOptionalParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.CalibrationTimeParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectNeededParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectOptionalParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectTimeParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeatureType;
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

@Description(OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION)
@Author(name = OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES, contact = OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS)
@Keywords(OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS)
@Label(OMSTRENTOPPROJECTFILESGENERATOR_LABEL)
@Name(OMSTRENTOPPROJECTFILESGENERATOR_NAME)
@Status(OMSTRENTOPPROJECTFILESGENERATOR_STATUS)
@License(OMSTRENTOPPROJECTFILESGENERATOR_LICENSE)
public class OmsTrentoPProjectFilesGenerator extends HMModel {

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_inFolder_DESCRIPTION)
    @In
    public String inFolder = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pMode_DESCRIPTION)
    @UI("combo:" + PROJECT + "," + CALIBRATION)
    @In
    public String pMode = PROJECT;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pCode_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    public static final String OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION = "Generates the input shapefiles for a OmsTrentoP simulation.";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_DOCUMENTATION = "OmsTrentoPProjectFilesGenerator.html";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS = "OmsTrentoP";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_LABEL = OTHER;
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_NAME = "";
    public static final int OMSTRENTOPPROJECTFILESGENERATOR_STATUS = 10;
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES = "Antonello Andrea, Silvia Franceschi, Daniele Andreis";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS = "";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_inFolder_DESCRIPTION = "The folder into which to create the base files.";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_pMode_DESCRIPTION = "Select the file type: project (default mode) or calibration.";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_pCode_DESCRIPTION = "The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:4328).";

    @Execute
    public void process() throws Exception {
        checkNull(inFolder, pCode, pMode);

        boolean isProject = pMode.equals(PROJECT);

        CoordinateReferenceSystem crs = CrsUtilities.getCrsFromEpsg(pCode, null);
        if (isProject) {
            pm.beginTask("Generating project files...", 3);
            String networkPath = new File(inFolder, Constants.NETWORK_PROJECT_NAME_SHP).getAbsolutePath();
            OmsShapefileFeatureWriter.writeEmptyShapefile(networkPath, getProjectFeatureType(crs), pm);
            pm.worked(1);
        } else {
            pm.beginTask("Generating calibration files...", 3);
            String networkPath = new File(inFolder, Constants.NETWORK_CALIBRATION_NAME_SHP).getAbsolutePath();
            OmsShapefileFeatureWriter.writeEmptyShapefile(networkPath, getCalibrationFeatureType(crs), pm);
            pm.worked(1);
        }
        String areasPath = new File(inFolder, Constants.AREA_NAME_SHP).getAbsolutePath();
        OmsShapefileFeatureWriter.writeEmptyShapefile(areasPath, getAreasFeatureType(crs), pm);
        pm.worked(1);

        String pozzettiPath = new File(inFolder, Constants.JUNCTIONS_NAME_SHP).getAbsolutePath();
        OmsShapefileFeatureWriter.writeEmptyShapefile(pozzettiPath, getJunctionsFeatureType(crs), pm);
        pm.worked(1);

        StringBuilder parametersSb = new StringBuilder();
        parametersSb.append("parameter;value;default;unit;min;max\n");
        if (isProject) {
            ProjectNeededParameterCodes[] projectsNeeded = ProjectNeededParameterCodes.values();
            for( ProjectNeededParameterCodes code : projectsNeeded ) {
                parametersSb.append(code.getKey()).append(";");
                parametersSb.append("TODO").append(";");
                parametersSb.append(nc(code.getDefaultValue())).append(";");
                parametersSb.append(nc(code.getUnit())).append(";");
                parametersSb.append(nc(code.getMinRange())).append(";");
                parametersSb.append(nc(code.getMaxRange())).append("\n");
            }
            ProjectTimeParameterCodes[] projectsTime = ProjectTimeParameterCodes.values();
            for( ProjectTimeParameterCodes code : projectsTime ) {
                parametersSb.append(code.getKey()).append(";");
                parametersSb.append("TODO").append(";");
                parametersSb.append(nc(code.getDefaultValue())).append(";");
                parametersSb.append(nc(code.getUnit())).append(";");
                parametersSb.append(nc(code.getMinRange())).append(";");
                parametersSb.append(nc(code.getMaxRange())).append("\n");
            }
            ProjectOptionalParameterCodes[] projectsOptional = ProjectOptionalParameterCodes.values();
            for( ProjectOptionalParameterCodes code : projectsOptional ) {
                parametersSb.append(code.getKey()).append(";");
                parametersSb.append("TODO").append(";");
                parametersSb.append(nc(code.getDefaultValue())).append(";");
                parametersSb.append(nc(code.getUnit())).append(";");
                parametersSb.append(nc(code.getMinRange())).append(";");
                parametersSb.append(nc(code.getMaxRange())).append("\n");
            }
        } else {
            CalibrationTimeParameterCodes[] calibrationsTime = CalibrationTimeParameterCodes.values();
            for( CalibrationTimeParameterCodes code : calibrationsTime ) {
                parametersSb.append(code.getKey()).append(";");
                parametersSb.append("TODO").append(";");
                parametersSb.append(nc(code.getDefaultValue())).append(";");
                parametersSb.append(nc(code.getUnit())).append(";");
                parametersSb.append(nc(code.getMinRange())).append(";");
                parametersSb.append(nc(code.getMaxRange())).append("\n");
            }
            CalibrationOptionalParameterCodes[] calibrationsOptional = CalibrationOptionalParameterCodes.values();
            for( CalibrationOptionalParameterCodes code : calibrationsOptional ) {
                parametersSb.append(code.getKey()).append(";");
                parametersSb.append("TODO").append(";");
                parametersSb.append(nc(code.getDefaultValue())).append(";");
                parametersSb.append(nc(code.getUnit())).append(";");
                parametersSb.append(nc(code.getMinRange())).append(";");
                parametersSb.append(nc(code.getMaxRange())).append("\n");
            }
        }
        File paramsFile = new File(inFolder, Constants.PARAMETERS_CSV);
        FileUtilities.writeFile(parametersSb.toString(), paramsFile);

        StringBuilder diametersSb = new StringBuilder();
        diametersSb.append("id;ext diameter [cm];thickness [cm]\n");
        File diametersFile = new File(inFolder, Constants.DIAMETERS_CSV);
        FileUtilities.writeFile(diametersSb.toString(), diametersFile);

        pm.worked(1);
        pm.done();
    }

    private String nc( Object value ) {
        if (value == null) {
            value = "";
        }
        return value.toString();
    }

    /**
     * Build the Calibration Type.
     * 
     * @param crs
     * @return the type for the calibration shp.
     */
    private SimpleFeatureType getCalibrationFeatureType( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("pipes");
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        b.add(TrentoPFeatureType.PipesTrentoP.ID.getAttributeName(), TrentoPFeatureType.PipesTrentoP.ID.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.PER_AREA.getAttributeName(), TrentoPFeatureType.PipesTrentoP.PER_AREA.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.RUNOFF_COEFFICIENT.getAttributeName(),
                TrentoPFeatureType.PipesTrentoP.RUNOFF_COEFFICIENT.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.AVERAGE_RESIDENCE_TIME.getAttributeName(),
                TrentoPFeatureType.PipesTrentoP.AVERAGE_RESIDENCE_TIME.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.KS.getAttributeName(), TrentoPFeatureType.PipesTrentoP.KS.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.AVERAGE_SLOPE.getAttributeName(),
                TrentoPFeatureType.PipesTrentoP.AVERAGE_SLOPE.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.DIAMETER.getAttributeName(), TrentoPFeatureType.PipesTrentoP.DIAMETER.getClazz());
        return b.buildFeatureType();
    }

    /**
     * Build the Project Type.
     * 
     * @param crs
     * @return the type for the calibration shp.
     */
    private SimpleFeatureType getProjectFeatureType( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("pipes");
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        b.add(TrentoPFeatureType.PipesTrentoP.ID.getAttributeName(), TrentoPFeatureType.PipesTrentoP.ID.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.PER_AREA.getAttributeName(), TrentoPFeatureType.PipesTrentoP.PER_AREA.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.RUNOFF_COEFFICIENT.getAttributeName(),
                TrentoPFeatureType.PipesTrentoP.RUNOFF_COEFFICIENT.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.AVERAGE_RESIDENCE_TIME.getAttributeName(),
                TrentoPFeatureType.PipesTrentoP.AVERAGE_RESIDENCE_TIME.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.KS.getAttributeName(), TrentoPFeatureType.PipesTrentoP.KS.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.MINIMUM_PIPE_SLOPE.getAttributeName(),
                TrentoPFeatureType.PipesTrentoP.MINIMUM_PIPE_SLOPE.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.PIPE_SECTION_TYPE.getAttributeName(),
                TrentoPFeatureType.PipesTrentoP.PIPE_SECTION_TYPE.getClazz());
        b.add(TrentoPFeatureType.PipesTrentoP.AVERAGE_SLOPE.getAttributeName(),
                TrentoPFeatureType.PipesTrentoP.AVERAGE_SLOPE.getClazz());
        return b.buildFeatureType();
    }

    /**
     * Build the areas Type.
     * 
     * @param crs
     * @return the type for the areas shp.
     */
    private SimpleFeatureType getAreasFeatureType( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("areas");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add(TrentoPFeatureType.AreasTrentoP.ID.getAttributeName(), TrentoPFeatureType.AreasTrentoP.ID.getClazz());
        b.add(TrentoPFeatureType.AreasTrentoP.FORCEAREA.getAttributeName(), TrentoPFeatureType.AreasTrentoP.FORCEAREA.getClazz());

        return b.buildFeatureType();
    }

    /**
     * Build the wells Type.
     * 
     * @param crs
     * @return the type for the wells shp.
     */
    private SimpleFeatureType getJunctionsFeatureType( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("junctions");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add(TrentoPFeatureType.JunctionsTrentoP.ID.getAttributeName(), TrentoPFeatureType.JunctionsTrentoP.ID.getClazz());
        b.add(TrentoPFeatureType.JunctionsTrentoP.ELEVATION.getAttributeName(), TrentoPFeatureType.JunctionsTrentoP.ELEVATION.getClazz());
        b.add(TrentoPFeatureType.JunctionsTrentoP.DEPTH.getAttributeName(), TrentoPFeatureType.JunctionsTrentoP.DEPTH.getClazz());

        return b.buildFeatureType();
    }

    public static void main( String[] args ) throws Exception {
        OmsTrentoPProjectFilesGenerator gen = new OmsTrentoPProjectFilesGenerator();
        gen.inFolder = "D:\\Dropbox\\hydrologis\\lavori\\2020_10_trentop\\file_vuoti_check\\project";
        gen.pMode = Variables.PROJECT;
//        gen.inFolder = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_10_trentop/file_vuoti_check/calibration";
//        gen.pMode = Variables.CALIBRATION;
        gen.pCode = "EPSG:32632";
        gen.process();
    }

}
