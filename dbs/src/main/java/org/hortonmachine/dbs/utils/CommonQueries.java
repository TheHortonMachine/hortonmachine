/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.dbs.utils;

import java.io.File;
import java.util.LinkedHashMap;

import org.hortonmachine.dbs.compat.ASqlTemplates;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
import org.locationtech.jts.geom.LineString;

/**
 * A simple utils class to collect common db queries as functions.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CommonQueries {

    public static LinkedHashMap<String, String> getTemplatesMap( EDb dbType ) {
        LinkedHashMap<String, String> templatesMap = new LinkedHashMap<String, String>();
        templatesMap.put("simple select", "select * from TABLENAME");
        templatesMap.put("where select", "select * from TABLENAME where FIELD > VALUE");
        templatesMap.put("limited select", "select * from TABLENAME limit 10");
        templatesMap.put("sorted select", "select * from TABLENAME order by FIELD asc");
        templatesMap.put("add column", "alter table TABLENAME add column NEWCOLUMN COLUMNTYPE");
        templatesMap.put("count with case",
                "select count(case when COLUMN is null then 1 else null end ) as NULLCOUNT, count(case when COLUMN is null then null else 1 end ) as NONNULLCOUNT from TABLENAME order by COLUMN asc");

        try {
            ASqlTemplates sqlTemplates = dbType.getSqlTemplates();

            String formatTimeSyntax = sqlTemplates.getFormatTimeSyntax("timestampcolumn", "YYYY-MM-dd HH:mm:ss");
            templatesMap.put("timestamp select", "select " + formatTimeSyntax + " from TABLENAME");

            String attachShapefile = sqlTemplates.attachShapefile(new File("/path/to/file.shp"));
            templatesMap.put("attach shapefile", attachShapefile);

        } catch (Exception e) {
            e.printStackTrace();
        }
        switch( dbType ) {
        case SQLITE:
            templatesMap.put("unix epoch timestamp where select",
                    "select * from TABLENAME where longtimestamp >= cast(strftime('%s','YYYY-MM-YY HH:mm:ss') as long)*1000");
            break;
        case H2:

            break;

        default:
            break;
        }

        if (dbType.isSpatial()) {
            templatesMap.put("geometry select", "select the_geom from TABLENAME");
            templatesMap.put("aggregate and merge lines",
                    "select column, ST_LineMerge(geometry) from tablename\n" + "where column like 'pattern%' group by column");

            switch( dbType ) {
            case SPATIALITE:
                templatesMap.put("spatial index geom intersection part",
                        "AND table1.ROWID IN (\nSELECT ROWID FROM SpatialIndex\nWHERE f_table_name='table2' AND search_frame=table2Geom)");

                templatesMap.put("create intersection of table1 with buffer of table2",
                        "SELECT intersection(t1.the_geom, buffer(t2.the_geom, 100)) as the_geom FROM table1 t1, table2 t2\n"
                                + "where (\nintersects (t1.the_geom, buffer(t2.the_geom, 100))=1\n"
                                + "AND t1.ROWID IN (\nSELECT ROWID FROM SpatialIndex\nWHERE f_table_name='table1' AND search_frame=buffer(t2.the_geom, 100)\n))");

                templatesMap.put("create new spatial table from select",
                        "create table newtablename as SELECT * FROM tablename;\n"
                                + "SELECT RecoverGeometryColumn('newtablename', 'geometry',  4326, 'LINESTRING', 'XY');\n"
                                + "SELECT CreateSpatialIndex('newtablename', 'geometry');");
                break;
            case H2GIS:
                templatesMap.put("create intersection of table1 with buffer of table2",
                        "SELECT intersection(t1.the_geom, buffer(t2.the_geom, 100)) as the_geom FROM table1 t1, table2 t2\n"
                                + "where (\nintersects (t1.the_geom, buffer(t2.the_geom, 100))=1");
                templatesMap.put("create new spatial table from select", "CREATE TABLE newtablename as SELECT * FROM tablename;\n"
                        + "CREATE SPATIAL INDEX myspatialindex ON newtablename(the_geom);");
                break;
            case POSTGIS:
                templatesMap.put("reproject table", "ALTER TABLE tableName ALTER COLUMN the_geom\n  TYPE Geometry(geomtype, toSrid) \n  USING ST_Transform(the_geom, toSrid);");
                break;

            default:
                break;
            }
        }

//        templatesMap.put("unix epoch timestamp select", "strftime('%Y-%m-%d %H:%M:%S', timestampcolumn / 1000, 'unixepoch')");
//        templatesMap.put("unix epoch timestamp where select",
//                "select * from TABLENAME where longtimestamp >= cast(strftime('%s','YYYY-MM-YY HH:mm:ss') as long)*1000");

//        templatesMap.put("spatial index geom intersection part",
//                "AND table1.ROWID IN (\nSELECT ROWID FROM SpatialIndex\nWHERE f_table_name='table2' AND search_frame=table2Geom)");
//        templatesMap.put("create intersection of table1 with buffer of table2",
//                "SELECT intersection(t1.the_geom, buffer(t2.the_geom, 100)) as the_geom FROM table1 t1, table2 t2\n"
//                        + "where (\nintersects (t1.the_geom, buffer(t2.the_geom, 100))=1\n"
//                        + "AND t1.ROWID IN (\nSELECT ROWID FROM SpatialIndex\nWHERE f_table_name='table1' AND search_frame=buffer(t2.the_geom, 100)\n))");
//        templatesMap.put("create new spatial table from select",
//                "create table newtablename as SELECT * FROM tablename;\n"
//                        + "SELECT RecoverGeometryColumn('newtablename', 'geometry',  4326, 'LINESTRING', 'XY');\n"
//                        + "SELECT CreateSpatialIndex('newtablename', 'geometry');");

        return templatesMap;
    }

    /**
     * Calculates the GeodesicLength between to points.
     * 
     * @param connection the database connection.
     * @param p1 the first point.
     * @param p2 the second point.
     * @param srid the srid. If <0, 4326 will be used. This needs to be a geographic prj.
     * @return the distance.
     * @throws Exception
     */
    public static double getDistanceBetween( IHMConnection connection, Coordinate p1, Coordinate p2, int srid ) throws Exception {
        if (srid < 0) {
            srid = 4326;
        }
        GeometryFactory gf = new GeometryFactory();
        LineString lineString = gf.createLineString(new Coordinate[]{p1, p2});
        String sql = "select GeodesicLength(LineFromText(\"" + lineString.toText() + "\"," + srid + "));";
        try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql);) {
            if (rs.next()) {
                double length = rs.getDouble(1);
                return length;
            }
            throw new RuntimeException("Could not calculate distance.");
        }

    }
}
