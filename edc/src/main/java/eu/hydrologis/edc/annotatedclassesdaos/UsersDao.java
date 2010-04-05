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

import eu.hydrologis.edc.annotatedclasses.UsersTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class UsersDao extends AbstractEdcDao {

    public UsersDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        UsersTable uT = new UsersTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            uT.setId(id);
        }
        String userName = lineSplit[1].trim();
        if (userName.length() > 0) {
            uT.setUserName(userName);
        }

        String firstName = lineSplit[2].trim();
        if (firstName.length() > 0) {
            uT.setFirstName(firstName);
        }

        String lastName = lineSplit[3].trim();
        if (lastName.length() > 0) {
            uT.setLastName(lastName);
        }

        String description = lineSplit[4].trim();
        if (description.length() > 0) {
            uT.setDescription(description);
        }

        String creationDateStr = lineSplit[5].trim();
        if (creationDateStr.length() > 0) {
            DateTime creationDate = formatter.parseDateTime(creationDateStr);
            uT.setCreationDate(creationDate);
        }

        String email = lineSplit[6].trim();
        if (email.length() > 0) {
            uT.setEmail(email);
        }

        session.save(uT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(UsersTable.class)).append(": ");
        sB.append(columnAnnotationToString(UsersTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(UsersTable.class, "userName")).append(", ");
        sB.append(columnAnnotationToString(UsersTable.class, "firstName")).append(", ");
        sB.append(columnAnnotationToString(UsersTable.class, "lastName")).append(", ");
        sB.append(columnAnnotationToString(UsersTable.class, "description")).append(", ");
        sB.append(columnAnnotationToString(UsersTable.class, "creationDate")).append(", ");
        sB.append(columnAnnotationToString(UsersTable.class, "email"));
        return sB.toString();
    }
}
