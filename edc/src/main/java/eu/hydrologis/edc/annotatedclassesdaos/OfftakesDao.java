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
import eu.hydrologis.edc.annotatedclasses.OfftakesTable;
import eu.hydrologis.edc.annotatedclasses.PoiTable;
import eu.hydrologis.edc.annotatedclasses.PointTypeTable;
import eu.hydrologis.edc.annotatedclasses.StatusTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OfftakesDao extends AbstractEdcDao {

    public OfftakesDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        OfftakesTable oT = new OfftakesTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            oT.setId(id);
        }

        String poiId = lineSplit[1].trim();
        if (poiId.length() > 0) {
            PoiTable poi = new PoiTable();
            poi.setId(Long.parseLong(poiId));
            oT.setPoi(poi);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            oT.setDescription(description);
        }

        String startDateStr = lineSplit[3].trim();
        if (startDateStr.length() > 0) {
            DateTime startDate = formatter.parseDateTime(startDateStr);
            oT.setStartDate(startDate);
        }

        String endDateStr = lineSplit[4].trim();
        if (endDateStr.length() > 0) {
            DateTime endDate = formatter.parseDateTime(endDateStr);
            oT.setEndDate(endDate);
        }

        String agency = lineSplit[5].trim();
        if (agency.length() > 0) {
            oT.setAgency(agency);
        }

        String code = lineSplit[6].trim();
        if (code.length() > 0) {
            oT.setCode(code);
        }

        String pointTypeId = lineSplit[7].trim();
        if (pointTypeId.length() > 0) {
            PointTypeTable pointType = new PointTypeTable();
            pointType.setId(Long.parseLong(pointTypeId));
            oT.setPointType(pointType);
        }

        String statusId = lineSplit[8].trim();
        if (statusId.length() > 0) {
            StatusTable status = new StatusTable();
            status.setId(Long.parseLong(statusId));
            oT.setStatus(status);
        }

        String dataSourceId = lineSplit[9].trim();
        if (dataSourceId.length() > 0) {
            DataSourceTable dataSource = new DataSourceTable();
            dataSource.setId(Long.parseLong(dataSourceId));
            oT.setDataSource(dataSource);
        }

        String dataTypeId = lineSplit[10].trim();
        if (dataTypeId.length() > 0) {
            DataTypeTable dataType = new DataTypeTable();
            dataType.setId(Long.parseLong(dataTypeId));
            oT.setDataType(dataType);
        }

        String isBigOfftake = lineSplit[11].trim();
        if (isBigOfftake.length() > 0) {
            oT.setIsBigOfftake(Boolean.parseBoolean(isBigOfftake));
        }

        session.save(oT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(OfftakesTable.class)).append(": ");
        sB.append(columnAnnotationToString(OfftakesTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(OfftakesTable.class, "poi")).append(", ");
        sB.append(columnAnnotationToString(OfftakesTable.class, "description")).append(", ");
        sB.append(columnAnnotationToString(OfftakesTable.class, "startDate")).append(", ");
        sB.append(columnAnnotationToString(OfftakesTable.class, "endDate")).append(", ");
        sB.append(columnAnnotationToString(OfftakesTable.class, "agency")).append(", ");
        sB.append(columnAnnotationToString(OfftakesTable.class, "code")).append(", ");
        sB.append(joinColumnAnnotationToString(OfftakesTable.class, "pointType")).append(", ");
        sB.append(joinColumnAnnotationToString(OfftakesTable.class, "status")).append(", ");
        sB.append(joinColumnAnnotationToString(OfftakesTable.class, "dataSource")).append(", ");
        sB.append(joinColumnAnnotationToString(OfftakesTable.class, "dataType")).append(", ");
        sB.append(columnAnnotationToString(OfftakesTable.class, "isBigOfftake"));
        return sB.toString();
    }
}
