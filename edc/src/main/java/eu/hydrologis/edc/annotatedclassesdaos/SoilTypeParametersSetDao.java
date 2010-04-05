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

import eu.hydrologis.edc.annotatedclasses.LevelsTable;
import eu.hydrologis.edc.annotatedclasses.RunsTable;
import eu.hydrologis.edc.annotatedclasses.SoilTypeCategoriesTable;
import eu.hydrologis.edc.annotatedclasses.SoilTypeParametersSetTable;
import eu.hydrologis.edc.annotatedclasses.SoilTypeParametersSetTablePK;
import eu.hydrologis.edc.annotatedclasses.SoilTypeParametersTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SoilTypeParametersSetDao extends AbstractEdcDao {

    public SoilTypeParametersSetDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        SoilTypeParametersSetTable stpsT = new SoilTypeParametersSetTable();

        String runId = lineSplit[0].trim();
        if (runId.length() > 0) {
            RunsTable runsTable = new RunsTable();
            runsTable.setId(new Long(runId));

            stpsT.setRun(runsTable);
        }

        String soilTypeCategory = lineSplit[1].trim();
        if (soilTypeCategory.length() > 0) {
            SoilTypeCategoriesTable stcT = new SoilTypeCategoriesTable();
            stcT.setId(new Long(soilTypeCategory));

            stpsT.setSoilTypeCategory(stcT);
        }

        String soilTypeParameter = lineSplit[2].trim();
        if (soilTypeParameter.length() > 0) {
            SoilTypeParametersTable stpT = new SoilTypeParametersTable();
            stpT.setId(new Long(soilTypeParameter));

            stpsT.setSoilTypeParameter(stpT);
        }

        String value = lineSplit[3].trim();
        if (value.length() > 0) {
            stpsT.setValue(new Double(value));
        }

        String levelsId = lineSplit[4].trim();
        if (levelsId.length() > 0) {
            LevelsTable levT = new LevelsTable();
            levT.setId(new Long(levelsId));

            stpsT.setSoilLevel(levT);
        }

        session.save(stpsT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(SoilTypeParametersSetTable.class)).append(": ");
        sB.append(joinColumnAnnotationToString(SoilTypeParametersSetTablePK.class, "run")).append(
                ", ");
        sB
                .append(
                        joinColumnAnnotationToString(SoilTypeParametersSetTablePK.class,
                                "soilTypeCategory")).append(", ");
        sB.append(
                joinColumnAnnotationToString(SoilTypeParametersSetTablePK.class,
                        "soilTypeParameter")).append(", ");
        sB.append(columnAnnotationToString(SoilTypeParametersSetTable.class, "value"));
        sB.append(joinColumnAnnotationToString(SoilTypeParametersSetTablePK.class, "soilLevel"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new SoilTypeParametersSetDao(null).getRecordDefinition());
    }

}
