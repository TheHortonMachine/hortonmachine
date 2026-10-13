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

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.Variables.CALIBRATION;
import static org.hortonmachine.gears.libs.modules.Variables.PROJECT;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_DOCUMENTATION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_LABEL;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_LICENSE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_NAME;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_STATUS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.CalibrationOptionalParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.CalibrationTimeParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.IParametersCode;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectNeededParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectOptionalParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectTimeParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.ITrentoPType;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPDatabase;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.AreasTrentoP;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.JunctionsTrentoP;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
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

@Description(OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION)
@Documentation(OMSTRENTOPPROJECTFILESGENERATOR_DOCUMENTATION)
@Author(name = OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES, contact = OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS)
@Keywords(OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS)
@Label(OMSTRENTOPPROJECTFILESGENERATOR_LABEL)
@Name(OMSTRENTOPPROJECTFILESGENERATOR_NAME)
@Status(OMSTRENTOPPROJECTFILESGENERATOR_STATUS)
@License(OMSTRENTOPPROJECTFILESGENERATOR_LICENSE)
public class OmsTrentoPProjectFilesGenerator extends HMModel {

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_outDatabase_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDatabase = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pUser_DESCRIPTION)
    @In
    public String pUser = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pPassword_DESCRIPTION)
    @In
    public String pPassword = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pMode_DESCRIPTION)
    @UI("combo:" + PROJECT + "," + CALIBRATION)
    @In
    public String pMode = PROJECT;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pCode_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    public static final String OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION = "Creates the spatial database of a TrentoP sewer network, with its empty layers and its tables of parameters and diameters.";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_DOCUMENTATION = """
            The database can be a GeoPackage or H2GIS file, or a PostGIS database. For a project, it gets the \
            layers network_project, areas and junctions, the table parameters, with the parameters of the design, and the \
            table diameters, with an example series of PVC pipes to check and change. For a calibration, the layer \
            network_calibration, areas, junctions and the parameters of the verification.

            The optional parameters get their default as value, while the needed ones are left without value. An existing \
            database is completed: only the layers, tables and parameters it does not have yet are added, so that the \
            project and the calibration of a network can share one database.
            """;
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS = "Sewer network, TrentoP, GeoPackage, PostGIS";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_LABEL = HYDROGEOMORPHOLOGY;
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_NAME = "";
    public static final int OMSTRENTOPPROJECTFILESGENERATOR_STATUS = 10;
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES = "Antonello Andrea, Silvia Franceschi, Daniele Andreis";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS = "";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_outDatabase_DESCRIPTION = "The database to create, or to complete if it exists: a GeoPackage (.gpkg) or H2GIS (.mv.db) file, or a PostGIS database, as jdbc:postgresql://host:port/database.";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_pUser_DESCRIPTION = "The optional user of the database.";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_pPassword_DESCRIPTION = "The optional password of the database.";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_pMode_DESCRIPTION = "The layers and parameters to create: for a project (the design of a new network) or a calibration (the verification of an existing one).";
    public static final String OMSTRENTOPPROJECTFILESGENERATOR_pCode_DESCRIPTION = "The code of the projected reference system of the layers, in meters (ex. EPSG:32632).";

    @Execute
    public void process() throws Exception {
        checkNull(outDatabase, pCode, pMode);
        boolean isProject = pMode.equals(PROJECT);
        CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs(pCode);

        pm.beginTask("Creating the TrentoP database...", isProject ? 5 : 4);
        try (ASpatialDb db = TrentoPDatabase.open(outDatabase, pUser, pPassword)) {
            TrentoPDatabase.addCrs(db, crs);

            List<IParametersCode> parameters = new ArrayList<>();
            if (isProject) {
                createLayer(db, Constants.NETWORK_PROJECT_LAYER, LineString.class, crs, PipesTrentoP.ID, PipesTrentoP.RUNOFF_COEFFICIENT,
                        PipesTrentoP.AVERAGE_RESIDENCE_TIME, PipesTrentoP.KS, PipesTrentoP.MINIMUM_PIPE_SLOPE,
                        PipesTrentoP.PIPE_SECTION_TYPE, PipesTrentoP.AVERAGE_SLOPE);
                parameters.addAll(Arrays.asList(ProjectNeededParameterCodes.values()));
                parameters.addAll(Arrays.asList(ProjectTimeParameterCodes.values()));
                parameters.addAll(Arrays.asList(ProjectOptionalParameterCodes.values()));
            } else {
                createLayer(db, Constants.NETWORK_CALIBRATION_LAYER, LineString.class, crs, PipesTrentoP.ID,
                        PipesTrentoP.RUNOFF_COEFFICIENT, PipesTrentoP.AVERAGE_RESIDENCE_TIME, PipesTrentoP.KS,
                        PipesTrentoP.AVERAGE_SLOPE, PipesTrentoP.DIAMETER);
                parameters.addAll(Arrays.asList(CalibrationTimeParameterCodes.values()));
                parameters.add(ProjectTimeParameterCodes.MINIMUM_TIME);
                parameters.addAll(Arrays.asList(CalibrationOptionalParameterCodes.values()));
            }
            pm.worked(1);
            createLayer(db, Constants.AREAS_LAYER, Polygon.class, crs, AreasTrentoP.ID);
            pm.worked(1);
            createLayer(db, Constants.JUNCTIONS_LAYER, Point.class, crs, JunctionsTrentoP.ID, JunctionsTrentoP.ELEVATION);
            pm.worked(1);
            TrentoPDatabase.addParameters(db, parameters);
            pm.worked(1);
            if (isProject) {
                TrentoPDatabase.addDiameters(db, TrentoPDatabase.EXAMPLE_DIAMETERS);
                pm.worked(1);
            }
        }
        pm.done();
    }

    /**
     * Create an empty layer with the given fields, if it is not in the database.
     */
    private void createLayer( ASpatialDb db, String name, Class< ? > geometryClass, CoordinateReferenceSystem crs,
            ITrentoPType... fields ) throws Exception {
        if (db.hasTable(SqlName.m(name))) {
            pm.message("The layer " + name + " exists already, it is left as it is.");
            return;
        }
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(name);
        b.setCRS(crs);
        b.add("the_geom", geometryClass);
        for( ITrentoPType field : fields ) {
            b.add(field.getAttributeName(), field.getClazz());
        }
        SpatialDbsImportUtils.createTableFromSchema(db, b.buildFeatureType(), SqlName.m(name), null, false);
    }

}
