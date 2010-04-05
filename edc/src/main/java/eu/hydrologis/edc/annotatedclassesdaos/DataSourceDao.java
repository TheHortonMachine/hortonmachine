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

import eu.hydrologis.edc.annotatedclasses.DataSourceAccountTable;
import eu.hydrologis.edc.annotatedclasses.DataSourceTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DataSourceDao extends AbstractEdcDao {

    public DataSourceDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        DataSourceTable dsT = new DataSourceTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            dsT.setId(id);
        }

        String name = lineSplit[1].trim();
        if (name.length() > 0) {
            dsT.setName(name);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            dsT.setDescription(description);
        }

        String account = lineSplit[3].trim();
        if (account.length() > 0) {
            DataSourceAccountTable dsaT = new DataSourceAccountTable();
            dsaT.setId(Long.parseLong(account));
            dsT.setAccount(dsaT);
        }

        session.save(dsT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(DataSourceTable.class)).append(": ");
        sB.append(columnAnnotationToString(DataSourceTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(DataSourceTable.class, "name")).append(", ");
        sB.append(columnAnnotationToString(DataSourceTable.class, "description")).append(", ");
        sB.append(joinColumnAnnotationToString(DataSourceTable.class, "account"));
        return sB.toString();
    }
}
