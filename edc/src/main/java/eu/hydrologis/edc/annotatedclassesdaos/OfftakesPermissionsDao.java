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

import eu.hydrologis.edc.annotatedclasses.DmvTable;
import eu.hydrologis.edc.annotatedclasses.OfftakesPermissionsTable;
import eu.hydrologis.edc.annotatedclasses.OfftakesTable;
import eu.hydrologis.edc.annotatedclasses.PermissionsDischargeTable;
import eu.hydrologis.edc.annotatedclasses.PermissionsUsageTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OfftakesPermissionsDao extends AbstractEdcDao {

    public OfftakesPermissionsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        OfftakesPermissionsTable opT = new OfftakesPermissionsTable();

        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            opT.setId(id);
        }

        String offtakeId = lineSplit[1].trim();
        if (offtakeId.length() > 0) {
            OfftakesTable oT = new OfftakesTable();
            oT.setId(Long.parseLong(offtakeId));
            opT.setOfftake(oT);
        }

        String permissionsDischargeId = lineSplit[2].trim();
        if (permissionsDischargeId.length() > 0) {
            PermissionsDischargeTable pdT = new PermissionsDischargeTable();
            pdT.setId(Long.parseLong(permissionsDischargeId));
            opT.setPermissionsDischarge(pdT);
        }

        String agency = lineSplit[3].trim();
        if (agency.length() > 0) {
            opT.setAgency(agency);
        }

        String code = lineSplit[4].trim();
        if (code.length() > 0) {
            opT.setCode(code);
        }

        String approvalDateStr = lineSplit[5].trim();
        if (approvalDateStr.length() > 0) {
            DateTime approvalDate = formatter.parseDateTime(approvalDateStr);
            opT.setApprovalDate(approvalDate);
        }

        String startDateStr = lineSplit[6].trim();
        if (startDateStr.length() > 0) {
            DateTime startDate = formatter.parseDateTime(startDateStr);
            opT.setStartDate(startDate);
        }

        String endDateStr = lineSplit[7].trim();
        if (endDateStr.length() > 0) {
            DateTime endDate = formatter.parseDateTime(endDateStr);
            opT.setEndDate(endDate);
        }

        String ratedPower = lineSplit[8].trim();
        if (ratedPower.length() > 0) {
            opT.setRatedPower(Double.parseDouble(ratedPower));
        }

        String dmvStr = lineSplit[9].trim();
        if (dmvStr.length() > 0) {
            DmvTable dT = new DmvTable();
            dT.setId(new Long(dmvStr));
            opT.setDmv(dT);
        }

        String permissionsUsageId = lineSplit[10].trim();
        if (permissionsUsageId.length() > 0) {
            PermissionsUsageTable permissionsUsage = new PermissionsUsageTable();
            permissionsUsage.setId(Long.parseLong(permissionsUsageId));
            opT.setPermissionsUsage(permissionsUsage);
        }

        session.save(opT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(OfftakesPermissionsTable.class)).append(": ");
        sB.append(columnAnnotationToString(OfftakesPermissionsTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(OfftakesPermissionsTable.class, "offtake")).append(
                ", ");
        sB
                .append(
                        joinColumnAnnotationToString(OfftakesPermissionsTable.class,
                                "permissionsDischarge")).append(", ");
        sB.append(columnAnnotationToString(OfftakesPermissionsTable.class, "agency")).append(", ");
        sB.append(columnAnnotationToString(OfftakesPermissionsTable.class, "code")).append(", ");
        sB.append(columnAnnotationToString(OfftakesPermissionsTable.class, "approvalDate")).append(
                ", ");
        sB.append(columnAnnotationToString(OfftakesPermissionsTable.class, "startDate")).append(
                ", ");
        sB.append(columnAnnotationToString(OfftakesPermissionsTable.class, "endDate")).append(", ");
        sB.append(columnAnnotationToString(OfftakesPermissionsTable.class, "ratedPower")).append(
                ", ");
        sB.append(joinColumnAnnotationToString(OfftakesPermissionsTable.class, "dmv")).append(", ");
        sB.append(joinColumnAnnotationToString(OfftakesPermissionsTable.class, "permissionsUsage"));
        return sB.toString();
    }
}
