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

import eu.hydrologis.edc.annotatedclasses.LandslideBasinRelationTable;
import eu.hydrologis.edc.annotatedclasses.LandslidesClassificationsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LandslidesClassificationsDao extends AbstractEdcDao {

    public LandslidesClassificationsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        LandslidesClassificationsTable lcT = new LandslidesClassificationsTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            lcT.setId(id);
        }

        String movementType = lineSplit[1].trim();
        if (movementType.length() > 0) {
            lcT.setMovementType(movementType);
        }
        
        String materialType = lineSplit[2].trim();
        if (materialType.length() > 0) {
            lcT.setMaterialType(materialType);
        }

        session.save(lcT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(LandslideBasinRelationTable.class)).append(": ");
        sB.append(columnAnnotationToString(LandslidesClassificationsTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(LandslidesClassificationsTable.class, "movementType")).append(", ");
        sB.append(columnAnnotationToString(LandslidesClassificationsTable.class, "materialType"));
        return sB.toString();
    }
}
