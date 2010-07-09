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

import eu.hydrologis.edc.annotatedclasses.HydrometersDischargeScalesTable;
import eu.hydrologis.edc.annotatedclasses.HydrometersTable;
import eu.hydrologis.edc.annotatedclasses.ScaleTypeTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HydrometersDischargeScalesDao extends AbstractEdcDao {

    public HydrometersDischargeScalesDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        HydrometersDischargeScalesTable hdsT = new HydrometersDischargeScalesTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            hdsT.setId(id);
        }

        String hydrometer_id = lineSplit[1].trim();
        if (hydrometer_id.length() > 0) {
            HydrometersTable hT = new HydrometersTable();
            hT.setId(Long.parseLong(hydrometer_id));
            hdsT.setHydrometer(hT);
        }

        String scaleType_id = lineSplit[2].trim();
        if (scaleType_id.length() > 0) {
            ScaleTypeTable stT = new ScaleTypeTable();
            stT.setId(Long.parseLong(scaleType_id));
            hdsT.setScaleType(stT);
        }

        String levelStr = lineSplit[3].trim();
        if (levelStr.length() > 0) {
            hdsT.setLevel(Double.parseDouble(levelStr));
        }

        String dischargeStr = lineSplit[4].trim();
        if (dischargeStr.length() > 0) {
            hdsT.setDischarge(Double.parseDouble(dischargeStr));
        }
        
        String levelUnit = lineSplit[5].trim();
        if (levelUnit.length() > 0) {
            UnitsTable unitsTable = new UnitsTable();
            unitsTable.setId(Long.parseLong(levelUnit));
            hdsT.setLevelUnit(unitsTable);
        }
        
        String dischargeUnit = lineSplit[6].trim();
        if (dischargeUnit.length() > 0) {
            UnitsTable unitsTable = new UnitsTable();
            unitsTable.setId(Long.parseLong(dischargeUnit));
            hdsT.setDischargeUnit(unitsTable);
        }

        session.save(hdsT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(HydrometersDischargeScalesTable.class)).append(": ");
        sB.append(columnAnnotationToString(HydrometersDischargeScalesTable.class, "id")).append(
                ", ");
        sB
                .append(
                        joinColumnAnnotationToString(HydrometersDischargeScalesTable.class,
                                "hydrometer")).append(", ");
        sB.append(joinColumnAnnotationToString(HydrometersDischargeScalesTable.class, "scaleType"))
                .append(", ");
        sB.append(columnAnnotationToString(HydrometersDischargeScalesTable.class, "level")).append(
                ", ");
        sB.append(columnAnnotationToString(HydrometersDischargeScalesTable.class, "discharge"))
                .append(", ");
        sB.append(columnAnnotationToString(HydrometersDischargeScalesTable.class, "level_unit"))
        .append(", ");
        sB.append(columnAnnotationToString(HydrometersDischargeScalesTable.class, "discharge_unit"));
        return sB.toString();
    }
}
