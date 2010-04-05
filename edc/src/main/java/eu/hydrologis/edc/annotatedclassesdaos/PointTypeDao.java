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

import eu.hydrologis.edc.annotatedclasses.PointTypeTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PointTypeDao extends AbstractEdcDao {

    public PointTypeDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        PointTypeTable ptT = new PointTypeTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            ptT.setId(id);
        }

        String decsription = lineSplit[1].trim();
        if (decsription.length() > 0) {
            ptT.setDescription(decsription);
        }

        session.save(ptT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(PointTypeTable.class)).append(": ");
        sB.append(columnAnnotationToString(PointTypeTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(PointTypeTable.class, "description")).append(", ");
        return sB.toString();
    }
}
