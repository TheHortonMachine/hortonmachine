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

import static eu.hydrologis.edc.utils.Constants.EDCGEOMETRIES_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.POIGEOMETRIES;

import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import eu.hydrologis.edc.annotatedclasses.PoiTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;
import eu.hydrologis.edc.databases.QueryHandler;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PoiGeometriesDao extends AbstractEdcDao {

    public PoiGeometriesDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) throws Exception {
        Double lat = Double.parseDouble(lineSplit[0].trim());
        Double lon = Double.parseDouble(lineSplit[1].trim());
        String epsg = lineSplit[2].trim();
        Long id = Long.parseLong(lineSplit[3].trim());
        QueryHandler queryHandler = edcSessionFactory.getQueryHandler();

        if (lat != null && lon != null && epsg != null && id != null) {
            queryHandler.insertPointGeometry(session, EDCGEOMETRIES_SCHEMA, POIGEOMETRIES, id,
                    new Coordinate(lon, lat), epsg);
        }
    }

    public Map<Long, Geometry> getGeometries( String epsg, PoiTable... poi ) throws Exception {
        Long[] ids = new Long[poi.length];
        for( int i = 0; i < ids.length; i++ ) {
            ids[i] = poi[i].getId();
        }
        return getGeometries(epsg, ids);
    }

    public Map<Long, Geometry> getGeometries( String epsg, Long[] ids ) throws Exception {
        QueryHandler queryHandler = edcSessionFactory.getQueryHandler();
        Map<Long, Geometry> geometryMap = queryHandler.getGeometries(EDCGEOMETRIES_SCHEMA,
                POIGEOMETRIES, epsg, ids);
        return geometryMap;
    }

    public String getRecordDefinition() {
        return "latitude, longitude, epsg, poi_id";
    }
}
