/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
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
package eu.hydrologis.edc.annotatedclassesdaos;

import org.joda.time.DateTime;

import eu.hydrologis.edc.annotatedclasses.GeologyMapTable;
import eu.hydrologis.edc.annotatedclasses.LandcoverMapTable;
import eu.hydrologis.edc.annotatedclasses.MeteoMapTable;
import eu.hydrologis.edc.annotatedclasses.ModelsTable;
import eu.hydrologis.edc.annotatedclasses.MorphologyMapTable;
import eu.hydrologis.edc.annotatedclasses.RunsTable;
import eu.hydrologis.edc.annotatedclasses.SoilTypeMapTable;
import eu.hydrologis.edc.annotatedclasses.UsersTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RunsDao extends AbstractEdcDao {

    public RunsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        RunsTable rT = new RunsTable();

        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            rT.setId(id);
        }

        String title = lineSplit[1].trim();
        if (title.length() > 0) {
            rT.setTitle(title);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            rT.setDescription(description);
        }

        String model = lineSplit[3].trim();
        if (model.length() > 0) {
            ModelsTable mT = new ModelsTable();
            mT.setId(new Long(model));
            rT.setModel(mT);
        }

        String creationDateStr = lineSplit[4].trim();
        if (creationDateStr.length() > 0) {
            DateTime endDate = formatter.parseDateTime(creationDateStr);
            rT.setCreationDate(endDate);
        }

        String startDateStr = lineSplit[5].trim();
        if (startDateStr.length() > 0) {
            DateTime startDate = formatter.parseDateTime(startDateStr);
            rT.setStartDate(startDate);
        }

        String endDateStr = lineSplit[6].trim();
        if (endDateStr.length() > 0) {
            DateTime endDate = formatter.parseDateTime(endDateStr);
            rT.setEndDate(endDate);
        }
        
        String timestepStr = lineSplit[7].trim();
        if (timestepStr.length() > 0) {
            Double timestep = new Double(timestepStr);
            rT.setTimestep(timestep);
        }

        String inputsUrlString = lineSplit[8].trim();
        if (inputsUrlString.length() > 0) {
            rT.setInputsUrlString(inputsUrlString);
        }

        String resultsUrlString = lineSplit[9].trim();
        if (resultsUrlString.length() > 0) {
            rT.setResultsUrlString(resultsUrlString);
        }

        String landcoverMap = lineSplit[10].trim();
        if (landcoverMap.length() > 0) {
            LandcoverMapTable lmT = new LandcoverMapTable();
            lmT.setId(new Long(landcoverMap));
            rT.setLandcoverMap(lmT);
        }

        String soilTypeMap = lineSplit[11].trim();
        if (soilTypeMap.length() > 0) {
            SoilTypeMapTable stmT = new SoilTypeMapTable();
            stmT.setId(new Long(soilTypeMap));
            rT.setSoilTypeMap(stmT);
        }
        
        String geologyMap = lineSplit[12].trim();
        if (geologyMap.length() > 0) {
            GeologyMapTable gmT = new GeologyMapTable();
            gmT.setId(new Long(geologyMap));
            rT.setGeologyMap(gmT);
        }
        
        String morphologyMap = lineSplit[13].trim();
        if (morphologyMap.length() > 0) {
            MorphologyMapTable mmT = new MorphologyMapTable();
            mmT.setId(new Long(morphologyMap));
            rT.setMorphologyMap(mmT);
        }
        
        String meteoMap = lineSplit[14].trim();
        if (meteoMap.length() > 0) {
            MeteoMapTable mmT = new MeteoMapTable();
            mmT.setId(new Long(meteoMap));
            rT.setMeteoMap(mmT);
        }

        String user = lineSplit[15].trim();
        if (user.length() > 0) {
            UsersTable uT = new UsersTable();
            uT.setId(new Long(user));
            rT.setUser(uT);
        }
        
        String parentRun = lineSplit[16].trim();
        if (parentRun.length() > 0) {
            RunsTable prT = new RunsTable();
            prT.setId(new Long(parentRun));
            rT.setParentRun(prT);
        }

        session.save(rT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(RunsTable.class)).append(": ");
        sB.append(columnAnnotationToString(RunsTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(RunsTable.class, "title")).append(", ");
        sB.append(columnAnnotationToString(RunsTable.class, "description")).append(", ");
        sB.append(joinColumnAnnotationToString(RunsTable.class, "model")).append(", ");
        sB.append(columnAnnotationToString(RunsTable.class, "creationDate")).append(", ");
        sB.append(columnAnnotationToString(RunsTable.class, "startDate")).append(", ");
        sB.append(columnAnnotationToString(RunsTable.class, "endDate")).append(", ");
        sB.append(columnAnnotationToString(RunsTable.class, "timestep")).append(", ");
        sB.append(columnAnnotationToString(RunsTable.class, "inputsUrlString")).append(", ");
        sB.append(columnAnnotationToString(RunsTable.class, "resultsUrlString")).append(", ");
        sB.append(joinColumnAnnotationToString(RunsTable.class, "landcoverMap")).append(", ");
        sB.append(joinColumnAnnotationToString(RunsTable.class, "soilTypeMap")).append(", ");
        sB.append(joinColumnAnnotationToString(RunsTable.class, "geologyMap")).append(", ");
        sB.append(joinColumnAnnotationToString(RunsTable.class, "morphologyMap")).append(", ");
        sB.append(joinColumnAnnotationToString(RunsTable.class, "meteoMap")).append(", ");
        sB.append(joinColumnAnnotationToString(RunsTable.class, "user")).append(", ");
        sB.append(joinColumnAnnotationToString(RunsTable.class, "parentRun"));
        return sB.toString();
    }
}
