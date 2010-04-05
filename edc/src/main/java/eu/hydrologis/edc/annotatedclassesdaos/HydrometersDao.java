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

import eu.hydrologis.edc.annotatedclasses.DataSourceTable;
import eu.hydrologis.edc.annotatedclasses.DataTypeTable;
import eu.hydrologis.edc.annotatedclasses.HydrometersTable;
import eu.hydrologis.edc.annotatedclasses.PoiTable;
import eu.hydrologis.edc.annotatedclasses.PointTypeTable;
import eu.hydrologis.edc.annotatedclasses.StatusTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HydrometersDao extends AbstractEdcDao {

    public HydrometersDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        HydrometersTable hT = new HydrometersTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            hT.setId(id);
        }

        String poiId = lineSplit[1].trim();
        if (poiId.length() > 0) {
            PoiTable poi = new PoiTable();
            poi.setId(Long.parseLong(poiId));
            hT.setPoi(poi);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            hT.setDescription(description);
        }

        String startDateStr = lineSplit[3].trim();
        if (startDateStr.length() > 0) {
            DateTime startDate = formatter.parseDateTime(startDateStr);
            hT.setStartDate(startDate);
        }

        String endDateStr = lineSplit[4].trim();
        if (endDateStr.length() > 0) {
            DateTime endDate = formatter.parseDateTime(endDateStr);
            hT.setEndDate(endDate);
        }

        String agency = lineSplit[5].trim();
        if (agency.length() > 0) {
            hT.setAgency(agency);
        }

        String code = lineSplit[6].trim();
        if (code.length() > 0) {
            hT.setCode(code);
        }

        String pointTypeId = lineSplit[7].trim();
        if (pointTypeId.length() > 0) {
            PointTypeTable pointType = new PointTypeTable();
            pointType.setId(Long.parseLong(pointTypeId));
            hT.setPointType(pointType);
        }

        String statusId = lineSplit[8].trim();
        if (statusId.length() > 0) {
            StatusTable status = new StatusTable();
            status.setId(Long.parseLong(statusId));
            hT.setStatus(status);
        }

        String dataSourceId = lineSplit[9].trim();
        if (dataSourceId.length() > 0) {
            DataSourceTable dataSource = new DataSourceTable();
            dataSource.setId(Long.parseLong(dataSourceId));
            hT.setDataSource(dataSource);
        }

        String dataTypeId = lineSplit[10].trim();
        if (dataTypeId.length() > 0) {
            DataTypeTable dataType = new DataTypeTable();
            dataType.setId(Long.parseLong(dataTypeId));
            hT.setDataType(dataType);
        }

        session.save(hT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(HydrometersTable.class)).append(": ");
        sB.append(columnAnnotationToString(HydrometersTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(HydrometersTable.class, "poi")).append(", ");
        sB.append(columnAnnotationToString(HydrometersTable.class, "description")).append(", ");
        sB.append(columnAnnotationToString(HydrometersTable.class, "startDate")).append(", ");
        sB.append(columnAnnotationToString(HydrometersTable.class, "endDate")).append(", ");
        sB.append(columnAnnotationToString(HydrometersTable.class, "agency")).append(", ");
        sB.append(columnAnnotationToString(HydrometersTable.class, "code")).append(", ");
        sB.append(joinColumnAnnotationToString(HydrometersTable.class, "pointType")).append(", ");
        sB.append(joinColumnAnnotationToString(HydrometersTable.class, "status")).append(", ");
        sB.append(joinColumnAnnotationToString(HydrometersTable.class, "dataSource")).append(", ");
        sB.append(joinColumnAnnotationToString(HydrometersTable.class, "dataType"));
        return sB.toString();
    }
}
