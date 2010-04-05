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

import eu.hydrologis.edc.annotatedclasses.GeologyCategoriesTable;
import eu.hydrologis.edc.annotatedclasses.GeologyParametersSetTable;
import eu.hydrologis.edc.annotatedclasses.GeologyParametersSetTablePK;
import eu.hydrologis.edc.annotatedclasses.GeologyParametersTable;
import eu.hydrologis.edc.annotatedclasses.RunsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeologyParametersSetDao extends AbstractEdcDao {

    public GeologyParametersSetDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        GeologyParametersSetTable gpsT = new GeologyParametersSetTable();

        String runId = lineSplit[0].trim();
        if (runId.length() > 0) {
            RunsTable runsTable = new RunsTable();
            runsTable.setId(new Long(runId));

            gpsT.setRun(runsTable);
        }

        String geologyCategory = lineSplit[1].trim();
        if (geologyCategory.length() > 0) {
            GeologyCategoriesTable gcT = new GeologyCategoriesTable();
            gcT.setId(new Long(geologyCategory));

            gpsT.setGeologyCategory(gcT);
        }

        String geologyParameter = lineSplit[2].trim();
        if (geologyParameter.length() > 0) {
            GeologyParametersTable gpT = new GeologyParametersTable();
            gpT.setId(new Long(geologyParameter));

            gpsT.setGeologyParameter(gpT);
        }

        String defaultValue = lineSplit[3].trim();
        if (defaultValue.length() > 0) {
            gpsT.setValue(new Double(defaultValue));
        }

        session.save(gpsT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(GeologyParametersSetTable.class)).append(": ");
        sB.append(joinColumnAnnotationToString(GeologyParametersSetTablePK.class, "run")).append(
                ", ");
        sB.append(
                joinColumnAnnotationToString(GeologyParametersSetTablePK.class, "geologyCategory"))
                .append(", ");
        sB
                .append(
                        joinColumnAnnotationToString(GeologyParametersSetTablePK.class,
                                "geologyParameter")).append(", ");
        sB.append(columnAnnotationToString(GeologyParametersSetTable.class, "value"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new GeologyParametersSetDao(null).getRecordDefinition());
    }

}
