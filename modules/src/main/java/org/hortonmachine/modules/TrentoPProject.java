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

import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.BIBLIOGRAPHY_RODRIGUEZ_ITURBE_VALDES_1979;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_DOCUMENTATION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_KEYWORDS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_LABEL;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_LICENSE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_NAME;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_STATUS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_pOutPipe_DESCRIPTION;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPDatabase;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
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

@Description(OMSTRENTOP_DESCRIPTION)
@Documentation(OMSTRENTOP_DOCUMENTATION)
@Bibliography({BIBLIOGRAPHY_RODRIGUEZ_ITURBE_VALDES_1979})
@Author(name = OMSTRENTOP_AUTHORNAMES, contact = OMSTRENTOP_AUTHORCONTACTS)
@Keywords(OMSTRENTOP_KEYWORDS)
@Label(OMSTRENTOP_LABEL)
@Name(OMSTRENTOP_NAME)
@Status(OMSTRENTOP_STATUS)
@License(OMSTRENTOP_LICENSE)
public class TrentoPProject extends HMModel {

    @Description("The database of the network, with the layers network_project, areas and junctions and the tables parameters and diameters: a GeoPackage (.gpkg) or H2GIS (.mv.db) file, or a PostGIS database, as jdbc:postgresql://host:port/database. The designed network is written into it, as the layer network_project_output.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inDatabase = null;

    @Description("The optional user of the database.")
    @In
    public String pUser = null;

    @Description("The optional password of the database.")
    @In
    public String pPassword = null;

    @Description(OMSTRENTOP_pOutPipe_DESCRIPTION)
    @Unit("-")
    @In
    public Integer pOutPipe = null;

    private double[][] results;

    @Execute
    public void process() throws Exception {
        checkNull(inDatabase, pOutPipe);

        try (ASpatialDb db = TrentoPDatabase.open(inDatabase, pUser, pPassword)) {
            OmsTrentoPProject trento_P = new OmsTrentoPProject();
            trento_P.pm = pm;
            trento_P.pOutPipe = pOutPipe;
            trento_P.inDiameters = TrentoPDatabase.readDiameters(db);
            trento_P.inParameters = TrentoPDatabase.readParameters(db);
            trento_P.inPipes = TrentoPDatabase.readLayer(db, Constants.NETWORK_PROJECT_LAYER);
            trento_P.inAreas = TrentoPDatabase.readLayer(db, Constants.AREAS_LAYER);
            trento_P.inJunctions = TrentoPDatabase.readLayer(db, Constants.JUNCTIONS_LAYER);
            trento_P.process();

            results = trento_P.getResults();

            TrentoPDatabase.writeLayer(db, trento_P.outPipes, Constants.NETWORK_PROJECT_OUTPUT_LAYER, pm);
        }
    }

    public double[][] getResults() {
        return results;
    }

}
