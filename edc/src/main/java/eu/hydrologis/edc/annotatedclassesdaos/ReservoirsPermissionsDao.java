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
import eu.hydrologis.edc.annotatedclasses.PermissionsControlLevelsTable;
import eu.hydrologis.edc.annotatedclasses.PermissionsDischargeTable;
import eu.hydrologis.edc.annotatedclasses.PermissionsUsageTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsPermissionsTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ReservoirsPermissionsDao extends AbstractEdcDao {

    public ReservoirsPermissionsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        ReservoirsPermissionsTable rpT = new ReservoirsPermissionsTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            rpT.setId(id);
        }

        String reservoir = lineSplit[1].trim();
        if (reservoir.length() > 0) {
            ReservoirsTable rT = new ReservoirsTable();
            rT.setId(new Long(reservoir));
            rpT.setReservoir(rT);
        }
        
        String permissionsControlLevel = lineSplit[2].trim();
        if (permissionsControlLevel.length() > 0) {
            PermissionsControlLevelsTable pclT = new PermissionsControlLevelsTable();
            pclT.setId(new Long(permissionsControlLevel));
            rpT.setPermissionsControlLevel(pclT);
        }
        
        String permissionsDischarge = lineSplit[3].trim();
        if (permissionsDischarge.length() > 0) {
            PermissionsDischargeTable pdT = new PermissionsDischargeTable();
            pdT.setId(new Long(permissionsDischarge));
            rpT.setPermissionsDischarge(pdT);
        }
        
        String code = lineSplit[4].trim();
        if (code.length() > 0) {
            rpT.setCode(code);
        }
        
        String agency = lineSplit[5].trim();
        if (agency.length() > 0) {
            rpT.setAgency(agency);
        }
        
        String approvalDateStr = lineSplit[6].trim();
        if (approvalDateStr.length() > 0) {
            DateTime approvalDate = formatter.parseDateTime(approvalDateStr);
            rpT.setApprovalDate(approvalDate);
        }
        
        String startDateStr = lineSplit[7].trim();
        if (startDateStr.length() > 0) {
            DateTime startDate = formatter.parseDateTime(startDateStr);
            rpT.setStartDate(startDate);
        }

        String endDateStr = lineSplit[8].trim();
        if (endDateStr.length() > 0) {
            DateTime endDate = formatter.parseDateTime(endDateStr);
            rpT.setEndDate(endDate);
        }
        
        String ratedPower = lineSplit[9].trim();
        if (ratedPower.length() > 0) {
            rpT.setRatedPower(new Double(ratedPower));
        }
        
        String dmvStr = lineSplit[10].trim();
        if (dmvStr.length() > 0) {
            DmvTable dT = new DmvTable();
            dT.setId(new Long(dmvStr));
            rpT.setDmv(dT);
        }
        
        String permissionsUsageId = lineSplit[11].trim();
        if (permissionsUsageId.length() > 0) {
            PermissionsUsageTable permissionsUsage = new PermissionsUsageTable();
            permissionsUsage.setId(Long.parseLong(permissionsUsageId));
            rpT.setPermissionsUsage(permissionsUsage);
        }
        
        session.save(rpT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(ReservoirsPermissionsTable.class)).append(": ");
        sB.append(columnAnnotationToString(ReservoirsPermissionsTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsPermissionsTable.class, "reservoir")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsPermissionsTable.class, "permissionsControlLevel")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsPermissionsTable.class, "permissionsDischarge")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsPermissionsTable.class, "code")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsPermissionsTable.class, "agency")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsPermissionsTable.class, "approvalDate")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsPermissionsTable.class, "startDate")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsPermissionsTable.class, "endDate")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsPermissionsTable.class, "ratedPower")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsPermissionsTable.class, "dmv")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsPermissionsTable.class, "permissionsUsage"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new ReservoirsPermissionsDao(null).getRecordDefinition());
    }

}
