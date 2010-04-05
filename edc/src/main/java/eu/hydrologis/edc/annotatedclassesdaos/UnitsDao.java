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

import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class UnitsDao extends AbstractEdcDao {

    public UnitsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        UnitsTable uT = new UnitsTable();

        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            uT.setId(id);
        }

        String name = lineSplit[1].trim();
        if (name.length() > 0) {
            uT.setName(name);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            uT.setDescription(description);
        }

        String toPrincipal = lineSplit[3].trim();
        if (toPrincipal.length() > 0) {
            uT.setToPrincipal(Double.parseDouble(toPrincipal));
        }

        session.save(uT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(UnitsTable.class)).append(": ");
        sB.append(columnAnnotationToString(UnitsTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(UnitsTable.class, "name")).append(", ");
        sB.append(columnAnnotationToString(UnitsTable.class, "description")).append(", ");
        sB.append(columnAnnotationToString(UnitsTable.class, "toPrincipal"));
        return sB.toString();
    }
}
