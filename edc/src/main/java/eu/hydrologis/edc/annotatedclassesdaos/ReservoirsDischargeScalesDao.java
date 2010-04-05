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

import eu.hydrologis.edc.annotatedclasses.ReservoirsDischargeScalesTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsTable;
import eu.hydrologis.edc.annotatedclasses.ScaleTypeTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ReservoirsDischargeScalesDao extends AbstractEdcDao {

    public ReservoirsDischargeScalesDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        ReservoirsDischargeScalesTable rdsT = new ReservoirsDischargeScalesTable();

        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            rdsT.setId(id);
        }

        String reservoirId = lineSplit[1].trim();
        if (reservoirId.length() > 0) {
            ReservoirsTable rT = new ReservoirsTable();
            rT.setId(Long.parseLong(reservoirId));
            rdsT.setReservoir(rT);
        }

        String scaleTypeId = lineSplit[2].trim();
        if (scaleTypeId.length() > 0) {
            ScaleTypeTable stT = new ScaleTypeTable();
            stT.setId(Long.parseLong(scaleTypeId));
            rdsT.setScaleType(stT);
        }

        String sillLevel = lineSplit[3].trim();
        if (sillLevel.length() > 0) {
            rdsT.setSillLevel(Double.parseDouble(sillLevel));
        }

        String aMax = lineSplit[4].trim();
        if (aMax.length() > 0) {
            rdsT.setaMax(Double.parseDouble(aMax));
        }
        
        String aDiscr = lineSplit[5].trim();
        if (aDiscr.length() > 0) {
            rdsT.setaDiscr(Double.parseDouble(aDiscr));
        }
        
        String hasScale = lineSplit[6].trim();
        if (hasScale.length() > 0) {
            rdsT.setHasScale(Boolean.parseBoolean(hasScale));
        }
        
        String hHsMin = lineSplit[7].trim();
        if (hHsMin.length() > 0) {
            rdsT.sethHsMin(Double.parseDouble(hHsMin));
        }
        
        String hHsMax = lineSplit[8].trim();
        if (hHsMax.length() > 0) {
            rdsT.sethHsMax(Double.parseDouble(hHsMax));
        }
        
        String unit = lineSplit[9].trim();
        if (unit.length() > 0) {
            UnitsTable unitsTable = new UnitsTable();
            unitsTable.setId(Long.parseLong(unit));
            rdsT.setUnit(unitsTable);
        }

        session.save(rdsT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(ReservoirsDischargeScalesTable.class)).append(": ");
        sB.append(columnAnnotationToString(ReservoirsDischargeScalesTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsDischargeScalesTable.class, "reservoir")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsDischargeScalesTable.class, "scaleType")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsDischargeScalesTable.class, "sillLevel")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsDischargeScalesTable.class, "aMax")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsDischargeScalesTable.class, "aDiscr")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsDischargeScalesTable.class, "hasScale")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsDischargeScalesTable.class, "hHsMin")).append(", ");
        sB.append(columnAnnotationToString(ReservoirsDischargeScalesTable.class, "hHsMax")).append(", ");
        sB.append(joinColumnAnnotationToString(ReservoirsDischargeScalesTable.class, "unit"));
        return sB.toString();
    }
}
