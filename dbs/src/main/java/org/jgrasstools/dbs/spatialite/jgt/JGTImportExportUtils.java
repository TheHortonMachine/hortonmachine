/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.dbs.spatialite.jgt;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.ImportExportUtils;
import org.jgrasstools.dbs.spatialite.ESpatialiteGeometryType;

/**
 * Import and export utilities for JGrasstools.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class JGTImportExportUtils {

    /**
     * Import a shapefile into the database using a temporary virtual table.
     * 
     * @param db the database.
     * @param tableName the name for the new table.
     * @param shpPath the shp to import.
     * @param encoding the encoding. If <code>null</code>, UTF-8 is used.
     * @param srid the epsg code for the file.
     * @param geometryType the geometry type of the file.
     * @throws Exception
     */
    public static void importShapefileThroughVirtualTable( final ASpatialDb db, final String tableName, final String shpPath,
            final String encoding, final int srid, final ESpatialiteGeometryType geometryType ) throws Exception {

        JGTTransactionExecuter transactionExecuter = new JGTTransactionExecuter(db){
            @Override
            public void executeInTransaction() throws Exception {
                ImportExportUtils.executeQueries(db, tableName, shpPath, encoding, srid, geometryType);
            }
        };
        transactionExecuter.execute();
    }

    public static void main( String[] args ) throws Exception {
        String dbPath = "/home/hydrologis/data/naturalearth/ne_10m_admin_1_states_provinces/test.sqlite";
        String path = "/home/hydrologis/data/naturalearth/ne_10m_admin_1_states_provinces/ne_10m_admin_1_states_provinces.shp";

        try (SpatialiteDb db = new SpatialiteDb()) {
            if (!db.open(dbPath)) {
                db.initSpatialMetadata(null);
            }

            importShapefileThroughVirtualTable(db, "admiin_states", path, "UTF-8", 4326, ESpatialiteGeometryType.MULTIPOLYGON_XY);
        }

    }

}
