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

import eu.hydrologis.edc.annotatedclasses.PermissionsControlLevelsTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.annotatedclasses.ValueDescriptionTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PermissionsControlLevelsDao extends AbstractEdcDao {

    public PermissionsControlLevelsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        PermissionsControlLevelsTable pclT = new PermissionsControlLevelsTable();

        String oid = lineSplit[0].trim();
        if (oid.length() > 0) {
            pclT.setOid(new Long(oid));
        }

        String id = lineSplit[1].trim();
        if (id.length() > 0) {
            pclT.setId(new Long(id));
        }

        String startMonth = lineSplit[2].trim();
        if (startMonth.length() > 0) {
            pclT.setStartMonth(Integer.parseInt(startMonth));
        }

        String startDay = lineSplit[3].trim();
        if (startDay.length() > 0) {
            pclT.setStartDay(Integer.parseInt(startDay));
        }

        String endMonth = lineSplit[4].trim();
        if (endMonth.length() > 0) {
            pclT.setEndMonth(Integer.parseInt(endMonth));
        }

        String endDay = lineSplit[5].trim();
        if (endDay.length() > 0) {
            pclT.setEndDay(Integer.parseInt(endDay));
        }

        String value = lineSplit[6].trim();
        if (value.length() > 0) {
            pclT.setValue(Double.parseDouble(value));
        }

        String valueDescriptionStr = lineSplit[7].trim();
        if (valueDescriptionStr.length() > 0) {
            ValueDescriptionTable valueDescription = new ValueDescriptionTable();
            valueDescription.setId(Long.parseLong(valueDescriptionStr));
            pclT.setValueDescription(valueDescription);
        }
        
        String unitStr = lineSplit[8].trim();
        if (unitStr.length() > 0) {
            UnitsTable uT = new UnitsTable();
            uT.setId(Long.parseLong(unitStr));
            pclT.setUnit(uT);
        }
        session.save(pclT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(PermissionsControlLevelsTable.class)).append(": ");
        sB.append(columnAnnotationToString(PermissionsControlLevelsTable.class, "oid"))
                .append(", ");
        sB.append(columnAnnotationToString(PermissionsControlLevelsTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(PermissionsControlLevelsTable.class, "startMonth"))
                .append(", ");
        sB.append(columnAnnotationToString(PermissionsControlLevelsTable.class, "startDay"))
                .append(", ");
        sB.append(columnAnnotationToString(PermissionsControlLevelsTable.class, "endMonth"))
                .append(", ");
        sB.append(columnAnnotationToString(PermissionsControlLevelsTable.class, "endDay")).append(
                ", ");
        sB.append(columnAnnotationToString(PermissionsControlLevelsTable.class, "value")).append(
                ", ");
        sB.append(joinColumnAnnotationToString(PermissionsControlLevelsTable.class,
                "valueDescription"));
        return sB.toString();
    }
}
