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

import eu.hydrologis.edc.annotatedclasses.RamaddaTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RamaddaDao extends AbstractEdcDao {

    public RamaddaDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        RamaddaTable rT = new RamaddaTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            rT.setId(id);
        }
        String login = lineSplit[1].trim();
        if (login.length() > 0) {
            rT.setLogin(login);
        }

        String host = lineSplit[2].trim();
        if (host.length() > 0) {
            rT.setHost(host);
        }

        String portStrs = lineSplit[3].trim();
        if (portStrs.length() > 0) {
            int port = Integer.parseInt(portStrs);
            rT.setPort(port);
        }

        String base = lineSplit[4].trim();
        if (base.length() > 0) {
            rT.setBase(base);
        }

        String parentid = lineSplit[5].trim();
        if (parentid.length() > 0) {
            rT.setParentid(parentid);
        }

        session.save(rT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(RamaddaTable.class)).append(": ");
        sB.append(columnAnnotationToString(RamaddaTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(RamaddaTable.class, "login")).append(", ");
        sB.append(columnAnnotationToString(RamaddaTable.class, "host")).append(", ");
        sB.append(columnAnnotationToString(RamaddaTable.class, "port")).append(", ");
        sB.append(columnAnnotationToString(RamaddaTable.class, "base")).append(", ");
        sB.append(columnAnnotationToString(RamaddaTable.class, "parentid"));
        return sB.toString();
    }
}
