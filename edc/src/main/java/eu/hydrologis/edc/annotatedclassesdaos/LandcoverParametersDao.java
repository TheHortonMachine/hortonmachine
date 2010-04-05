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

import eu.hydrologis.edc.annotatedclasses.GeologyParametersTable;
import eu.hydrologis.edc.annotatedclasses.LandcoverParametersTable;
import eu.hydrologis.edc.annotatedclasses.ProcessesTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LandcoverParametersDao extends AbstractEdcDao {

    public LandcoverParametersDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        LandcoverParametersTable gpT = new LandcoverParametersTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            gpT.setId(id);
        }

        String name = lineSplit[1].trim();
        if (name.length() > 0) {
            gpT.setName(name);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            gpT.setDescription(description);
        }

        String unit = lineSplit[3].trim();
        if (unit.length() > 0) {
            UnitsTable uT = new UnitsTable();
            uT.setId(new Long(unit));
            gpT.setUnit(uT);
        }

        String defaultValue = lineSplit[4].trim();
        if (defaultValue.length() > 0) {
            gpT.setDefaultValue(new Double(defaultValue));
        }

        String physicalProcesses = lineSplit[5].trim();
        if (physicalProcesses.length() > 0) {
            ProcessesTable uT = new ProcessesTable();
            uT.setId(new Long(physicalProcesses));
            gpT.setProcesses(uT);
        }

        session.save(gpT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(LandcoverParametersTable.class)).append(": ");
        sB.append(columnAnnotationToString(LandcoverParametersTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(LandcoverParametersTable.class, "name")).append(", ");
        sB.append(columnAnnotationToString(LandcoverParametersTable.class, "description")).append(
                ", ");
        sB.append(joinColumnAnnotationToString(LandcoverParametersTable.class, "unit"))
                .append(", ");
        sB.append(columnAnnotationToString(LandcoverParametersTable.class, "defaultValue")).append(
                ", ");
        sB.append(joinColumnAnnotationToString(GeologyParametersTable.class, "processes"));
        return sB.toString();
    }
}
