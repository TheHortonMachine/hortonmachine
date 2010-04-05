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

import eu.hydrologis.edc.annotatedclasses.SurveysTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SurveysDao extends AbstractEdcDao {

    public SurveysDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        SurveysTable mT = new SurveysTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            mT.setId(id);
        }

        String area = lineSplit[1].trim();
        if (area.length() > 0) {
            mT.setArea(new Double(area));
        }

        String longitude = lineSplit[2].trim();
        if (longitude.length() > 0) {
            mT.setLongitude(new Double(longitude));
        }

        String latitude = lineSplit[3].trim();
        if (latitude.length() > 0) {
            mT.setLatitude(new Double(latitude));
        }

        String epsg = lineSplit[4].trim();
        if (epsg.length() > 0) {
            mT.setEpsg(epsg);
        }

        String elevation = lineSplit[5].trim();
        if (elevation.length() > 0) {
            mT.setElevation(new Double(elevation));
        }

        String denomination = lineSplit[6].trim();
        if (denomination.length() > 0) {
            mT.setDenomination(denomination);
        }

        String description = lineSplit[7].trim();
        if (description.length() > 0) {
            mT.setDescription(description);
        }

        String startDateStr = lineSplit[8].trim();
        if (startDateStr.length() > 0) {
            DateTime startDate = formatter.parseDateTime(startDateStr);
            mT.setStartDate(startDate);
        }

        String endDateStr = lineSplit[9].trim();
        if (endDateStr.length() > 0) {
            DateTime endDate = formatter.parseDateTime(endDateStr);
            mT.setEndDate(endDate);
        }

        String code = lineSplit[10].trim();
        if (code.length() > 0) {
            mT.setCode(code);
        }

        session.save(mT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(SurveysTable.class)).append(": ");
        sB.append(columnAnnotationToString(SurveysTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "area")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "longitude")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "latitude")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "epsg")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "elevation")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "denomination")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "description")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "startDate")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "endDate")).append(", ");
        sB.append(columnAnnotationToString(SurveysTable.class, "code"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new SurveysDao(null).getRecordDefinition());
    }

}
