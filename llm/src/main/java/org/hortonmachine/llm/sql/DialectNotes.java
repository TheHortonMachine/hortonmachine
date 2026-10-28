/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.llm.sql;

import org.hortonmachine.dbs.compat.EDb;

/**
 * Short notes on the SQL dialect of each database type, given to the model with the schema.
 *
 * <p>They follow what the HortonMachine connections actually offer: the GeoPackage connection, for
 * example, only registers a few bounding box functions, so the notes tell the model not to use
 * other spatial functions there.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class DialectNotes {

    private static final String SPATIALITE = """
            SQLite with the SpatiaLite extension. Geometries are SpatiaLite blobs.
            - Functions: ST_Intersects, ST_Contains, ST_Within, ST_Distance, ST_Area, ST_Length, ST_Buffer,
              ST_Centroid, ST_Transform(geom, srid), ST_AsText(geom), ST_GeomFromText(wkt, srid),
              MakePoint(x, y, srid), BuildMbr(x1, y1, x2, y2, srid).
            - Spatial predicates return 1, 0 or -1: write ST_Intersects(a, b) = 1.
            - Measures are in the units of the CRS of the geometry.
            - Spatial index of table t with geometry column g, if it has one: add
              t.ROWID IN (SELECT ROWID FROM SpatialIndex WHERE f_table_name = 't' AND search_frame = <geometry or BuildMbr(...)>)
              to the WHERE of the spatial filter.
            - Identifiers can be quoted with double quotes. Limit rows with LIMIT n.
            """;

    private static final String SQLITE = """
            SQLite without spatial functions.
            - Identifiers can be quoted with double quotes. Limit rows with LIMIT n.
            """;

    private static final String GEOPACKAGE = """
            SQLite database in GeoPackage format. Geometries are GeoPackage binary blobs.
            - The only spatial functions available are ST_MinX(g), ST_MaxX(g), ST_MinY(g), ST_MaxY(g)
              and ST_IsEmpty(g). There is no ST_Intersects, ST_Area, ST_Transform, ST_AsText or any other:
              spatial filters can only compare bounding boxes.
            - Spatial index of table t with geometry column g, if it has one: the R-tree table rtree_t_g
              with columns id (the row id of t), minx, maxx, miny, maxy. Filter a box with
              t.fid IN (SELECT id FROM rtree_t_g WHERE minx <= X2 AND maxx >= X1 AND miny <= Y2 AND maxy >= Y1)
              using the primary key of t in place of fid.
            - Don't query the gpkg_ metadata tables unless asked.
            - Identifiers can be quoted with double quotes. Limit rows with LIMIT n.
            """;

    private static final String H2GIS = """
            H2 database with the H2GIS extension.
            - Functions: ST_Intersects, ST_Contains, ST_Within (they return booleans), ST_Distance, ST_Area,
              ST_Length, ST_Buffer, ST_Centroid, ST_Transform(geom, srid), ST_AsText(geom),
              ST_GeomFromText(wkt, srid), ST_MakePoint(x, y), ST_MakeEnvelope(x1, y1, x2, y2, srid).
            - Measures are in the units of the CRS of the geometry.
            - Spatial index: the && operator uses it, e.g. g && ST_MakeEnvelope(...) AND ST_Intersects(g, ...).
            - H2 turns unquoted identifiers to upper case: quote lower or mixed case names with double quotes.
            - Limit rows with LIMIT n.
            """;

    private static final String H2 = """
            H2 database without spatial functions.
            - H2 turns unquoted identifiers to upper case: quote lower or mixed case names with double quotes.
            - Limit rows with LIMIT n.
            """;

    private static final String POSTGIS = """
            PostgreSQL with the PostGIS extension.
            - Functions: ST_Intersects, ST_Contains, ST_Within (they return booleans), ST_DWithin(a, b, distance),
              ST_Distance, ST_Area, ST_Length, ST_Buffer, ST_Centroid, ST_Transform(geom, srid), ST_AsText(geom),
              ST_GeomFromText(wkt, srid), ST_MakePoint(x, y), ST_MakeEnvelope(x1, y1, x2, y2, srid).
            - Measures are in the units of the CRS; for meters on geographic coordinates cast to geography.
            - Spatial index: ST_Intersects and ST_DWithin use it by themselves; for boxes use g && ST_MakeEnvelope(...).
              Use ST_DWithin, not ST_Distance < d, for distance filters.
            - Tables can be in schemas: write schema.table when a schema is shown.
            - Quote mixed case identifiers with double quotes. Limit rows with LIMIT n.
            """;

    private static final String POSTGRES = """
            PostgreSQL without spatial functions.
            - Tables can be in schemas: write schema.table when a schema is shown.
            - Quote mixed case identifiers with double quotes. Limit rows with LIMIT n.
            """;

    private DialectNotes() {
    }

    /**
     * @param type the type of database.
     * @return the name of the dialect, as given to the model.
     */
    public static String getName( EDb type ) {
        switch( type ) {
        case SPATIALITE:
        case SPATIALITE4ANDROID:
            return "SpatiaLite";
        case SQLITE:
        case MBTILES:
            return "SQLite";
        case GEOPACKAGE:
        case GEOPACKAGE4ANDROID:
            return "GeoPackage (SQLite)";
        case H2GIS:
            return "H2GIS";
        case H2:
            return "H2";
        case POSTGIS:
            return "PostGIS";
        case POSTGRES:
            return "PostgreSQL";
        default:
            return type.name();
        }
    }

    /**
     * @param type the type of database.
     * @return the notes on the dialect.
     */
    public static String getNotes( EDb type ) {
        switch( type ) {
        case SPATIALITE:
        case SPATIALITE4ANDROID:
            return SPATIALITE;
        case GEOPACKAGE:
        case GEOPACKAGE4ANDROID:
            return GEOPACKAGE;
        case H2GIS:
            return H2GIS;
        case H2:
            return H2;
        case POSTGIS:
            return POSTGIS;
        case POSTGRES:
            return POSTGRES;
        default:
            return SQLITE;
        }
    }
}
