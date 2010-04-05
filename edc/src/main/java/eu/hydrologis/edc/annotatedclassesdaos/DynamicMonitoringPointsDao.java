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

import eu.hydrologis.edc.annotatedclasses.DataTypeTable;
import eu.hydrologis.edc.annotatedclasses.DynamicMonitoringPointsTable;
import eu.hydrologis.edc.annotatedclasses.PointTypeTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DynamicMonitoringPointsDao extends AbstractEdcDao {

    public DynamicMonitoringPointsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        DynamicMonitoringPointsTable dmpT = new DynamicMonitoringPointsTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            dmpT.setId(id);
        }

        String pointTypeId = lineSplit[1].trim();
        if (pointTypeId.length() > 0) {
            PointTypeTable ptT = new PointTypeTable();
            ptT.setId(Long.parseLong(pointTypeId));

            dmpT.setPointType(ptT);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            dmpT.setDescription(description);
        }

        String dataTypeId = lineSplit[3].trim();
        if (dataTypeId.length() > 0) {
            DataTypeTable dtT = new DataTypeTable();
            dtT.setId(Long.parseLong(dataTypeId));

            dmpT.setDataType(dtT);
        }

        String calibration = lineSplit[4].trim();
        if (calibration.length() > 0) {
            dmpT.setCalibration(Double.parseDouble(calibration));
        }

        String unitsId = lineSplit[5].trim();
        if (unitsId.length() > 0) {
            UnitsTable uT = new UnitsTable();
            uT.setId(Long.parseLong(unitsId));

            dmpT.setUnit(uT);
        }

        session.save(dmpT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(DynamicMonitoringPointsTable.class)).append(": ");
        sB.append(columnAnnotationToString(DynamicMonitoringPointsTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(DynamicMonitoringPointsTable.class, "pointType")).append(", ");
        sB.append(columnAnnotationToString(DynamicMonitoringPointsTable.class, "description")).append(", ");
        sB.append(joinColumnAnnotationToString(DynamicMonitoringPointsTable.class, "dataType")).append(", ");
        sB.append(columnAnnotationToString(DynamicMonitoringPointsTable.class, "calibration")).append(", ");
        sB.append(joinColumnAnnotationToString(DynamicMonitoringPointsTable.class, "unit"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new DynamicMonitoringPointsDao(null).getRecordDefinition());
    }

}
