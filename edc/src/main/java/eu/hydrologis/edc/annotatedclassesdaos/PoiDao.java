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

import eu.hydrologis.edc.annotatedclasses.PoiTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PoiDao extends AbstractEdcDao {

    public PoiDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine(String[] lineSplit){
        PoiTable poiT = new PoiTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            poiT.setId(id);
        }

        String name = lineSplit[1].trim();
        if (name.length() > 0) {
            poiT.setName(name);
        }

        String description = lineSplit[2].trim();
        if (description.length() > 0) {
            poiT.setDescription(description);
        }

        String municipality = lineSplit[3].trim();
        if (municipality.length() > 0) {
            poiT.setMunicipality(municipality);
        }

        String province = lineSplit[4].trim();
        if (province.length() > 0) {
            poiT.setProvince(province);
        }

        String district = lineSplit[5].trim();
        if (district.length() > 0) {
            poiT.setDistrict(district);
        }

        String resort = lineSplit[6].trim();
        if (resort.length() > 0) {
            poiT.setResort(resort);
        }

        String agency = lineSplit[7].trim();
        if (agency.length() > 0) {
            poiT.setAgency(agency);
        }

        String elevationStr = lineSplit[8].trim();
        if (elevationStr.length() > 0) {
            double elevation = Double.parseDouble(elevationStr);
            poiT.setElevation(elevation);
        }

        String skyStr = lineSplit[9].trim();
        if (skyStr.length() > 0) {
            double sky = Double.parseDouble(skyStr);
            poiT.setSky(sky);
        }

        session.save(poiT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(PoiTable.class)).append(": ");
        sB.append(columnAnnotationToString(PoiTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(PoiTable.class, "name")).append(", ");
        sB.append(columnAnnotationToString(PoiTable.class, "description")).append(", ");
        sB.append(columnAnnotationToString(PoiTable.class, "municipality")).append(", ");
        sB.append(columnAnnotationToString(PoiTable.class, "province")).append(", ");
        sB.append(columnAnnotationToString(PoiTable.class, "district")).append(", ");
        sB.append(columnAnnotationToString(PoiTable.class, "resort")).append(", ");
        sB.append(columnAnnotationToString(PoiTable.class, "agency")).append(", ");
        sB.append(columnAnnotationToString(PoiTable.class, "elevation")).append(", ");
        sB.append(columnAnnotationToString(PoiTable.class, "sky"));
        return sB.toString();
    }
}
