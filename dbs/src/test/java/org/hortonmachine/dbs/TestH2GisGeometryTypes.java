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
package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.h2gis.H2GisDb;
import org.hortonmachine.dbs.h2gis.H2SyntaxHelper;
import org.hortonmachine.dbs.utils.SqlName;
import org.junit.Test;

/**
 * The short geometry column form (<code>the_geom POINT</code>) mapped to the
 * h2gis 2 column types, which carry geometry type and srid.
 */
public class TestH2GisGeometryTypes {

    @Test
    public void testFieldDataConversion() {
        assertEquals("the_geom GEOMETRY(MULTIPOLYGON, 4326)", H2GisDb.toH2GisGeometryFieldData("the_geom MULTIPOLYGON", 4326));
        assertEquals("the_geom GEOMETRY(POINT Z, 32632)", H2GisDb.toH2GisGeometryFieldData("the_geom POINTZ", 32632));
        assertEquals("geom GEOMETRY(GEOMETRYCOLLECTION)", H2GisDb.toH2GisGeometryFieldData("geom GeometryCollection", 0));
        assertEquals("the_geom GEOMETRY(LINESTRING, 4326) NOT NULL",
                H2GisDb.toH2GisGeometryFieldData("the_geom LINESTRING NOT NULL", 4326));
        // already in h2 syntax
        assertEquals("the_geom GEOMETRY(POINT, 4326)", H2GisDb.toH2GisGeometryFieldData("the_geom GEOMETRY(POINT, 4326)", 4326));
    }

    @Test
    public void testCreateTableCompatibility() {
        H2SyntaxHelper helper = new H2SyntaxHelper();
        assertEquals("CREATE TABLE t(id INT PRIMARY KEY, the_geom GEOMETRY(MULTIPOLYGON))",
                helper.checkSqlCompatibilityIssues("CREATE TABLE t(id INT PRIMARY KEY, the_geom MULTIPOLYGON)"));
        assertEquals("CREATE TABLE t(the_geom GEOMETRY(POINT Z), name VARCHAR)",
                helper.checkSqlCompatibilityIssues("CREATE TABLE t(the_geom POINT Z, name VARCHAR)"));
        // columns named like a geometry type and h2 syntax are untouched
        assertEquals("CREATE TABLE t(point INT, the_geom GEOMETRY(POINT, 4326))",
                helper.checkSqlCompatibilityIssues("CREATE TABLE t(point INT, the_geom GEOMETRY(POINT, 4326))"));
    }

    @Test
    public void testSridAndTypeInMetadata() throws Exception {
        File tmpFile = File.createTempFile("hm-h2gis-geomtypes", "");
        tmpFile.delete();
        String dbPath = tmpFile.getAbsolutePath();
        try (H2GisDb db = new H2GisDb()) {
            db.open(dbPath);
            db.initSpatialMetadata(null);

            SqlName table1 = SqlName.m("short form");
            db.createSpatialTable(table1, 32632, "the_geom MULTIPOLYGON", new String[]{"id INT PRIMARY KEY"});
            GeometryColumn gc = db.getGeometryColumnsForTable(table1);
            assertNotNull(gc);
            assertEquals(32632, gc.srid);
            assertEquals(EGeometryType.MULTIPOLYGON, gc.geometryType);
            assertEquals(1, gc.isSpatialIndexEnabled);

            // the old pattern: create the table, then set the srid
            SqlName table2 = SqlName.m("lines");
            db.createTable(table2, "id INT PRIMARY KEY", "the_geom LINESTRING");
            db.addSrid(table2, "4326", "the_geom");
            gc = db.getGeometryColumnsForTable(table2);
            assertEquals(4326, gc.srid);
            assertEquals(EGeometryType.LINESTRING, gc.geometryType);
        } finally {
            new File(dbPath + "." + EDb.H2GIS.getExtension()).delete();
        }
    }

}
