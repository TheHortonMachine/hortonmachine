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
import eu.hydrologis.edc.annotatedclasses.GeologyMapTable;
import eu.hydrologis.edc.annotatedclasses.MapTypeTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeologyMapDao extends AbstractEdcDao {

    public GeologyMapDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        GeologyMapTable gmT = new GeologyMapTable();
        String idString = lineSplit[0].trim();
        if (idString.length() > 0) {
            Long id = new Long(idString);
            gmT.setId(id);
        }
        
        String description = lineSplit[1].trim();
        if (description.length() > 0) {
            gmT.setDescription(description);
        }
        
        String creationDateStr = lineSplit[2].trim();
        if (creationDateStr.length() > 0) {
            DateTime creationDate = formatter.parseDateTime(creationDateStr);
            gmT.setCreationDate(creationDate);
        }

        String north = lineSplit[3].trim();
        if (north.length() > 0) {
            gmT.setNorth(new Double(north));
        }

        String south = lineSplit[4].trim();
        if (south.length() > 0) {
            gmT.setSouth(new Double(south));
        }

        String east = lineSplit[5].trim();
        if (east.length() > 0) {
            gmT.setEast(new Double(east));
        }

        String west = lineSplit[6].trim();
        if (west.length() > 0) {
            gmT.setWest(new Double(west));
        }

        String epsg = lineSplit[7].trim();
        if (epsg.length() > 0) {
            gmT.setEpsg(epsg);
        }

        String mapTypeId = lineSplit[8].trim();
        if (mapTypeId.length() > 0) {
            MapTypeTable mapType = new MapTypeTable();
            mapType.setId(new Long(mapTypeId));

            gmT.setMapType(mapType);
        }

        String url = lineSplit[9].trim();
        if (url.length() > 0) {
            gmT.setUrl(url);
        }

        String fieldName = lineSplit[10].trim();
        if (fieldName.length() > 0) {
            gmT.setFieldName(fieldName);
        }

        String resolution = lineSplit[11].trim();
        if (resolution.length() > 0) {
            gmT.setResolution(new Double(resolution));
        }
        
        String dataSourceId = lineSplit[12].trim();
        if (dataSourceId.length() > 0) {
            DataSourceTable dsT = new DataSourceTable();
            dsT.setId(new Long(dataSourceId));
            gmT.setDataSource(dsT);
        }

        session.save(gmT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(GeologyMapTable.class)).append(": ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "id")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "description")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "creationDate")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "north")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "south")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "east")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "west")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "epsg")).append(", ");
        sB.append(joinColumnAnnotationToString(GeologyMapTable.class, "mapType")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "url")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "fieldName")).append(", ");
        sB.append(columnAnnotationToString(GeologyMapTable.class, "resolution")).append(", ");
        sB.append(joinColumnAnnotationToString(GeologyMapTable.class, "dataSource"));
        return sB.toString();
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(new GeologyMapDao(null).getRecordDefinition());
    }

}
