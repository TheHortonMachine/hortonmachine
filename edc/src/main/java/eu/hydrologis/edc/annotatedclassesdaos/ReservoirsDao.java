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
import eu.hydrologis.edc.annotatedclasses.IntakesTable;
import eu.hydrologis.edc.annotatedclasses.PoiTable;
import eu.hydrologis.edc.annotatedclasses.PointTypeTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsTable;
import eu.hydrologis.edc.annotatedclasses.StatusTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ReservoirsDao extends AbstractEdcDao {

    public ReservoirsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        ReservoirsTable rT = new ReservoirsTable();

        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            rT.setId(id);
        }

        String poiId = lineSplit[1].trim();
        if (poiId.length() > 0) {
            PoiTable poi = new PoiTable();
            poi.setId(new Long(poiId));
            rT.setPoi(poi);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            rT.setDescription(description);
        }

        String startDateStr = lineSplit[3].trim();
        if (startDateStr.length() > 0) {
            DateTime startDate = formatter.parseDateTime(startDateStr);
            rT.setStartDate(startDate);
        }

        String endDateStr = lineSplit[4].trim();
        if (endDateStr.length() > 0) {
            DateTime endDate = formatter.parseDateTime(endDateStr);
            rT.setEndDate(endDate);
        }

        String agency = lineSplit[5].trim();
        if (agency.length() > 0) {
            rT.setAgency(agency);
        }

        String code = lineSplit[6].trim();
        if (code.length() > 0) {
            rT.setCode(code);
        }

        String pointTypeId = lineSplit[7].trim();
        if (pointTypeId.length() > 0) {
            PointTypeTable pointType = new PointTypeTable();
            pointType.setId(new Long(pointTypeId));
            rT.setPointType(pointType);
        }

        String statusId = lineSplit[8].trim();
        if (statusId.length() > 0) {
            StatusTable status = new StatusTable();
            status.setId(new Long(statusId));
            rT.setStatus(status);
        }

        String dataSourceId = lineSplit[9].trim();
        if (dataSourceId.length() > 0) {
            DataSourceTable dataSource = new DataSourceTable();
            dataSource.setId(new Long(dataSourceId));
            rT.setDataSource(dataSource);
        }

        String dataTypeId = lineSplit[10].trim();
        if (dataTypeId.length() > 0) {
            DataTypeTable dataType = new DataTypeTable();
            dataType.setId(new Long(dataTypeId));
            rT.setDataType(dataType);
        }

        String topDamLevel = lineSplit[11].trim();
        if (topDamLevel.length() > 0) {
            rT.setTopDamLevel(Double.parseDouble(topDamLevel));
        }

        String capacityLevel = lineSplit[12].trim();
        if (capacityLevel.length() > 0) {
            rT.setCapacityLevel(Double.parseDouble(capacityLevel));
        }

        String averageArea = lineSplit[13].trim();
        if (averageArea.length() > 0) {
            rT.setAverageArea(Double.parseDouble(averageArea));
        }

        String totalVolume = lineSplit[14].trim();
        if (totalVolume.length() > 0) {
            rT.setTotalVolume(Double.parseDouble(totalVolume));
        }

        String intakeID = lineSplit[15].trim();
        if (intakeID.length() > 0) {
            IntakesTable iT = new IntakesTable();
            iT.setId(new Long(intakeID));
            rT.setIntake(iT);
        }

        session.save(rT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(ReservoirsTable.class)).append(": ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsTable.class, "poi")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "description")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "startDate")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "endDate")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "agency")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "code")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsTable.class, "pointType")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsTable.class, "status")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsTable.class, "dataSource")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsTable.class, "dataType")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "topDamLevel")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "capacityLevel")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "averageArea")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsTable.class, "totalVolume")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsTable.class, "intake"));
        return sB.toString();
    }
}
