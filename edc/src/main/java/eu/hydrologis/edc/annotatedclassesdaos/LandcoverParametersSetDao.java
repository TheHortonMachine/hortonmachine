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

import eu.hydrologis.edc.annotatedclasses.LandcoverCategoriesTable;
import eu.hydrologis.edc.annotatedclasses.LandcoverParametersSetTable;
import eu.hydrologis.edc.annotatedclasses.LandcoverParametersSetTablePK;
import eu.hydrologis.edc.annotatedclasses.LandcoverParametersTable;
import eu.hydrologis.edc.annotatedclasses.RunsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LandcoverParametersSetDao extends AbstractEdcDao {

    public LandcoverParametersSetDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        LandcoverParametersSetTable gpsT = new LandcoverParametersSetTable();

        String runId = lineSplit[0].trim();
        if (runId.length() > 0) {
            RunsTable runsTable = new RunsTable();
            runsTable.setId(new Long(runId));

            gpsT.setRun(runsTable);
        }

        String landcoverCategory = lineSplit[1].trim();
        if (landcoverCategory.length() > 0) {
            LandcoverCategoriesTable lcT = new LandcoverCategoriesTable();
            lcT.setId(new Long(landcoverCategory));

            gpsT.setLandcoverCategory(lcT);
        }

        String landcoverParameter = lineSplit[2].trim();
        if (landcoverParameter.length() > 0) {
            LandcoverParametersTable lpT = new LandcoverParametersTable();
            lpT.setId(new Long(landcoverParameter));

            gpsT.setLandcoverParameter(lpT);
        }

        String value = lineSplit[3].trim();
        if (value.length() > 0) {
            gpsT.setValue(new Double(value));
        }

        session.save(gpsT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(LandcoverParametersSetTable.class)).append(": ");
        sB.append(joinColumnAnnotationToString(LandcoverParametersSetTablePK.class, "run")).append(
                ", ");
        sB.append(
                joinColumnAnnotationToString(LandcoverParametersSetTablePK.class,
                        "landcoverCategory")).append(", ");
        sB.append(
                joinColumnAnnotationToString(LandcoverParametersSetTablePK.class,
                        "landcoverParameter")).append(", ");
        sB.append(columnAnnotationToString(LandcoverParametersSetTable.class, "value"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new LandcoverParametersSetDao(null).getRecordDefinition());
    }

}
