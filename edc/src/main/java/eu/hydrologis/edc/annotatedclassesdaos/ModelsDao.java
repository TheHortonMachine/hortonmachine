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

import eu.hydrologis.edc.annotatedclasses.ModelsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ModelsDao extends AbstractEdcDao {

    public ModelsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        ModelsTable mtT = new ModelsTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            mtT.setId(id);
        }

        String name = lineSplit[1].trim();
        if (name.length() > 0) {
            mtT.setName(name);
        }
        
        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            mtT.setDescription(description);
        }

        session.save(mtT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(ModelsTable.class)).append(": ");
        sB.append(columnAnnotationToString(ModelsTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(ModelsTable.class, "name")).append(", ");
        sB.append(columnAnnotationToString(ModelsTable.class, "description"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new ModelsDao(null).getRecordDefinition());
    }

}
