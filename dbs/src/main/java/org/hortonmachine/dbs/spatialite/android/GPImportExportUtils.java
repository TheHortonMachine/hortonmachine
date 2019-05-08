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
package org.hortonmachine.dbs.spatialite.android;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.datatypes.ESpatialiteGeometryType;
import org.hortonmachine.dbs.spatialite.ImportExportUtils;

/**
 * Import and export utilities for Geopaparazzi.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GPImportExportUtils {

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

        GPTransactionExecuter transactionExecuter = new GPTransactionExecuter(db){
            @Override
            public void executeInTransaction(IHMConnection conn) throws Exception {
                ImportExportUtils.executeShapefileImportQueries(db, tableName, shpPath, encoding, srid, geometryType);
            }
        };
        transactionExecuter.execute();
    }

    /**
     * Attach a shapefile into the database using a temporary virtual table.
     * 
     * @param db the database.
     * @param tableName the name for the new table.
     * @param shpPath the shp to import.
     * @param encoding the encoding. If <code>null</code>, UTF-8 is used.
     * @param srid the epsg code for the file.
     * @throws Exception
     */
    public static void attachShapefileThroughVirtualTable( final ASpatialDb db, final String tableName, final String shpPath,
            final String encoding, final int srid ) throws Exception {

        GPTransactionExecuter transactionExecuter = new GPTransactionExecuter(db){
            @Override
            public void executeInTransaction(IHMConnection conn) throws Exception {
                ImportExportUtils.executeShapefileAttachQueries(db, tableName, shpPath, encoding, srid);
            }
        };
        transactionExecuter.execute();
    }

}
