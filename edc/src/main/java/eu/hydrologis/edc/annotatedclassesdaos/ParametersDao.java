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

import eu.hydrologis.edc.annotatedclasses.ParametersTable;
import eu.hydrologis.edc.annotatedclasses.ProcessesTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ParametersDao extends AbstractEdcDao {

    public ParametersDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        ParametersTable pT = new ParametersTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            pT.setId(id);
        }

        String name = lineSplit[1].trim();
        if (name.length() > 0) {
            pT.setName(name);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            pT.setDescription(description);
        }

        String unit = lineSplit[3].trim();
        if (unit.length() > 0) {
            UnitsTable uT = new UnitsTable();
            uT.setId(Long.parseLong(unit));
            pT.setUnit(uT);
        }

        String defaultValue = lineSplit[4].trim();
        if (defaultValue.length() > 0) {
            pT.setDefaultValue(new Double(defaultValue));
        }

        String physicalProcessesId = lineSplit[5].trim();
        if (physicalProcessesId.length() > 0) {
            ProcessesTable rT = new ProcessesTable();
            rT.setId(new Long(physicalProcessesId));
            pT.setProcesses(rT);
        }

        session.save(pT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(ParametersTable.class)).append(": ");
        sB.append(columnAnnotationToString(ParametersTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(ParametersTable.class, "name")).append(", ");
        sB.append(columnAnnotationToString(ParametersTable.class, "description")).append(", ");
        sB.append(joinColumnAnnotationToString(ParametersTable.class, "unit")).append(", ");
        sB.append(columnAnnotationToString(ParametersTable.class, "defaultValue")).append(", ");
        sB.append(joinColumnAnnotationToString(ParametersTable.class, "processes"));
        return sB.toString();
    }
}
