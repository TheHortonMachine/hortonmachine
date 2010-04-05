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

import eu.hydrologis.edc.annotatedclasses.ParametersSetTable;
import eu.hydrologis.edc.annotatedclasses.ParametersSetTablePK;
import eu.hydrologis.edc.annotatedclasses.ParametersTable;
import eu.hydrologis.edc.annotatedclasses.RunsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ParametersSetDao extends AbstractEdcDao {

    public ParametersSetDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        ParametersSetTable psT = new ParametersSetTable();

        String runId = lineSplit[0].trim();
        if (runId.length() > 0) {
            RunsTable runsTable = new RunsTable();
            runsTable.setId(new Long(runId));

            psT.setRun(runsTable);
        }

        String parameterId = lineSplit[1].trim();
        if (parameterId.length() > 0) {
            ParametersTable pT = new ParametersTable();
            pT.setId(new Long(parameterId));

            psT.setParameter(pT);
        }

        String value = lineSplit[2].trim();
        if (value.length() > 0) {
            psT.setValue(new Double(value));
        }

        session.save(psT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(ParametersSetTable.class)).append(": ");
        sB.append(joinColumnAnnotationToString(ParametersSetTablePK.class, "run")).append(", ");
        sB.append(joinColumnAnnotationToString(ParametersSetTablePK.class, "parameter")).append(
                ", ");
        sB.append(columnAnnotationToString(ParametersSetTable.class, "value"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new ParametersSetDao(null).getRecordDefinition());
    }

}
