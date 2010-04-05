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

import eu.hydrologis.edc.annotatedclasses.LevelsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LevelsDao extends AbstractEdcDao {

    public LevelsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        LevelsTable lT = new LevelsTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            lT.setId(id);
        }

        String name = lineSplit[1].trim();
        if (name.length() > 0) {
            lT.setName(name);
        }

        String ordering = lineSplit[2].trim();
        if (ordering.length() > 0) {
            lT.setOrdering(Integer.parseInt(ordering));
        }

        session.save(lT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(LevelsTable.class)).append(": ");
        sB.append(columnAnnotationToString(LevelsTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(LevelsTable.class, "name")).append(", ");
        sB.append(columnAnnotationToString(LevelsTable.class, "ordering"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new LevelsDao(null).getRecordDefinition());
    }

}
