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

import eu.hydrologis.edc.annotatedclasses.BasinTypeTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;
/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class BasinTypeDao extends AbstractEdcDao {

    public BasinTypeDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        BasinTypeTable btT = new BasinTypeTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            btT.setId(id);
        }

        String name = lineSplit[1].trim();
        if (name.length() > 0) {
            btT.setName(name);
        }

        session.save(btT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(BasinTypeTable.class)).append(": ");
        sB.append(columnAnnotationToString(BasinTypeTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(BasinTypeTable.class, "name"));
        return sB.toString();
    }
}
