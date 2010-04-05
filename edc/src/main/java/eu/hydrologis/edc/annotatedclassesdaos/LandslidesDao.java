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

import eu.hydrologis.edc.annotatedclasses.BasinTypeTable;
import eu.hydrologis.edc.annotatedclasses.LandslideBasinRelationTable;
import eu.hydrologis.edc.annotatedclasses.LandslidesClassificationsTable;
import eu.hydrologis.edc.annotatedclasses.LandslidesTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LandslidesDao extends AbstractEdcDao {

    public LandslidesDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        LandslidesTable lT = new LandslidesTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            lT.setId(id);
        }

        String name = lineSplit[1].trim();
        if (name.length() > 0) {
            lT.setName(name);
        }
        
        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            lT.setDescription(description);
        }
        
        String surveyDateStr = lineSplit[3].trim();
        if (surveyDateStr.length() > 0) {
            DateTime surveyDate = formatter.parseDateTime(surveyDateStr);
            lT.setSurveyDate(surveyDate);
        }
        
        String contact = lineSplit[4].trim();
        if (contact.length() > 0) {
            lT.setContact(contact);
        }
        
        String landslideClassificationId = lineSplit[5].trim();
        if (landslideClassificationId.length() > 0) {
            LandslidesClassificationsTable lcT = new LandslidesClassificationsTable();
            lcT.setId(new Long(landslideClassificationId));
            
            lT.setLandslideClassification(lcT);
        }
        
        String basinTypeId = lineSplit[6].trim();
        if (basinTypeId.length() > 0) {
            BasinTypeTable btT = new BasinTypeTable();
            btT.setId(new Long(basinTypeId));
            
            lT.setBasinType(btT);
        }
        
        String landslideBasinRelationId = lineSplit[6].trim();
        if (landslideBasinRelationId.length() > 0) {
            LandslideBasinRelationTable lbrT = new LandslideBasinRelationTable();
            lbrT.setId(new Long(landslideBasinRelationId));
            
            lT.setLandslideBasinRelation(lbrT);
        }
        

        session.save(lT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(LandslideBasinRelationTable.class)).append(": ");
        sB.append(columnAnnotationToString(LandslidesTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(LandslidesTable.class, "name")).append(", ");
        sB.append(columnAnnotationToString(LandslidesTable.class, "description")).append(", ");
        sB.append(columnAnnotationToString(LandslidesTable.class, "surveyDate")).append(", ");
        sB.append(columnAnnotationToString(LandslidesTable.class, "contact")).append(", ");
        sB.append(joinColumnAnnotationToString(LandslidesTable.class, "landslideClassification")).append(", ");
        sB.append(joinColumnAnnotationToString(LandslidesTable.class, "basinType")).append(", ");
        sB.append(joinColumnAnnotationToString(LandslidesTable.class, "landslideBasinRelation"));
        return sB.toString();
    }
}
