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

import eu.hydrologis.edc.annotatedclasses.DynamicMonitoringPointsTable;
import eu.hydrologis.edc.annotatedclasses.MeasuresTable;
import eu.hydrologis.edc.annotatedclasses.SurveysTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MeasuresDao extends AbstractEdcDao {

    public MeasuresDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        MeasuresTable mT = new MeasuresTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            mT.setId(id);
        }

        String surveyId = lineSplit[1].trim();
        if (surveyId.length() > 0) {
            SurveysTable surveysTable = new SurveysTable();
            surveysTable.setId(new Long(surveyId));
            mT.setSurvey(surveysTable);
        }

        String dynamicMonitoringPointId = lineSplit[2].trim();
        if (dynamicMonitoringPointId.length() > 0) {
            DynamicMonitoringPointsTable dynamicMonitoringPointsTable = new DynamicMonitoringPointsTable();
            dynamicMonitoringPointsTable.setId(new Long(dynamicMonitoringPointId));
            mT.setDynamicMonitoringPoint(dynamicMonitoringPointsTable);
        }

        String creationDateStr = lineSplit[3].trim();
        if (creationDateStr.length() > 0) {
            DateTime creationDate = formatter.parseDateTime(creationDateStr);
            mT.setCreationDate(creationDate);
        }

        String longitude = lineSplit[4].trim();
        if (longitude.length() > 0) {
            mT.setLongitude(new Double(longitude));
        }
        
        String latitude = lineSplit[5].trim();
        if (latitude.length() > 0) {
            mT.setLatitude(new Double(latitude));
        }
        
        String epsg = lineSplit[6].trim();
        if (epsg.length() > 0) {
            mT.setEpsg(epsg);
        }
        
        String elevation = lineSplit[7].trim();
        if (elevation.length() > 0) {
            mT.setElevation(new Double(elevation));
        }
        
        String progressive = lineSplit[8].trim();
        if (progressive.length() > 0) {
            mT.setProgressive(new Double(progressive));
        }
        
        String distance = lineSplit[9].trim();
        if (distance.length() > 0) {
            mT.setDistance(new Double(distance));
        }
        
        String depth = lineSplit[10].trim();
        if (depth.length() > 0) {
            mT.setDepth(new Double(depth));
        }
        
        String primaryMeasure = lineSplit[11].trim();
        if (primaryMeasure.length() > 0) {
            mT.setPrimaryMeasure(new Double(primaryMeasure));
        }
        
        String accessoryMeasure = lineSplit[12].trim();
        if (accessoryMeasure.length() > 0) {
            mT.setAccessoryMeasure(new Double(accessoryMeasure));
        }
        
        String maxRange = lineSplit[13].trim();
        if (maxRange.length() > 0) {
            mT.setMaxRange(new Double(maxRange));
        }
        
        String minRange = lineSplit[14].trim();
        if (minRange.length() > 0) {
            mT.setMinRange(new Double(minRange));
        }
        
        String maxError = lineSplit[15].trim();
        if (maxError.length() > 0) {
            mT.setMaxError(new Double(maxError));
        }
        
        String minError = lineSplit[16].trim();
        if (minError.length() > 0) {
            mT.setMinError(new Double(minError));
        }
        
        String unit = lineSplit[17].trim();
        if (unit.length() > 0) {
            UnitsTable uT = new UnitsTable();
            uT.setId(new Long(unit));
            mT.setUnit(uT);
        }
        
        String reportUrlString = lineSplit[18].trim();
        if (reportUrlString.length() > 0) {
            mT.setReportUrlString(reportUrlString);
        }

        String attachmentUrlString = lineSplit[19].trim();
        if (attachmentUrlString.length() > 0) {
            mT.setAttachmentUrlString(attachmentUrlString);
        }

        session.save(mT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(MeasuresTable.class)).append(": ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(MeasuresTable.class, "survey")).append(", ");
        sB.append(joinColumnAnnotationToString(MeasuresTable.class, "dynamicMonitoringPoint")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "creationDate")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "longitude")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "latitude")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "epsg")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "elevation")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "progressive")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "distance")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "depth")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "primaryMeasure")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "accessoryMeasure")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "maxRange")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "minRange")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "maxError")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "minError")).append(", ");
        sB.append(joinColumnAnnotationToString(MeasuresTable.class, "unit")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "reportUrlString")).append(", ");
        sB.append(columnAnnotationToString(MeasuresTable.class, "attachmentUrlString"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new MeasuresDao(null).getRecordDefinition());
    }

}
