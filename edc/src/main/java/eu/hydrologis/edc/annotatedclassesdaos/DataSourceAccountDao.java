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
import eu.hydrologis.edc.databases.EdcSessionFactory;
/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DataSourceAccountDao extends AbstractEdcDao {

    public DataSourceAccountDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        DataSourceAccountTable dsAccountT = new DataSourceAccountTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            dsAccountT.setId(id);
        }

        String login = lineSplit[1].trim();
        if (login.length() > 0) {
            dsAccountT.setLogin(login);
        }

        String passwd = lineSplit[2].trim();
        if (passwd.length() > 0) {
            dsAccountT.setPassword(passwd);
        }

        String url = lineSplit[3].trim();
        if (url.length() > 0) {
            dsAccountT.setUrl(url);
        }

        session.save(dsAccountT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(DataSourceAccountTable.class)).append(": ");
        sB.append(columnAnnotationToString(DataSourceAccountTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(DataSourceAccountTable.class, "login")).append(", ");
        sB.append(columnAnnotationToString(DataSourceAccountTable.class, "password")).append(", ");
        sB.append(columnAnnotationToString(DataSourceAccountTable.class, "url"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new DataSourceAccountDao(null).getRecordDefinition());
    }

}
