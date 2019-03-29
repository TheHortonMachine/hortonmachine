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
package org.hortonmachine.dbs.rasterlite;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

/**
 * An rasterlite2 wrapper class to only read rl2 databases.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Rasterlite2Db {

    public final static int TILESIZE = 256;
    private ASpatialDb database;

    /**
     * Constructor based on an existing ADb object.
     * 
     * @param database the {@link ADb} database.
     */
    public Rasterlite2Db( ASpatialDb database ) {
        EDb type = database.getType();
        if (type != EDb.SPATIALITE4ANDROID && type != EDb.SPATIALITE) {
            throw new IllegalArgumentException("Only spatialite databases are supported.");
        }

        this.database = database;
    }

    /**
     * Get the list of available raster coverages.
     * 
     * @param doOrder
     *            if <code>true</code>, the names are ordered.
     * @return the list of raster coverages.
     * @throws Exception
     */
    public List<Rasterlite2Coverage> getRasterCoverages( boolean doOrder ) throws Exception {
        List<Rasterlite2Coverage> rasterCoverages = new ArrayList<Rasterlite2Coverage>();
        String orderBy = " ORDER BY " + Rasterlite2Coverage.COVERAGE_NAME;
        if (!doOrder) {
            orderBy = "";
        }

        String sql = "SELECT " + Rasterlite2Coverage.COVERAGE_NAME + ", " + Rasterlite2Coverage.TITLE + ", "
                + Rasterlite2Coverage.SRID + ", " + Rasterlite2Coverage.COMPRESSION + ", " + Rasterlite2Coverage.EXTENT_MINX
                + ", " + Rasterlite2Coverage.EXTENT_MINY + ", " + Rasterlite2Coverage.EXTENT_MAXX + ", "
                + Rasterlite2Coverage.EXTENT_MAXY + " FROM " + Rasterlite2Coverage.TABLENAME + orderBy;

        return database.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {

                while( rs.next() ) {
                    int i = 1;
                    String coverageName = rs.getString(i++);
                    String title = rs.getString(i++);
                    int srid = rs.getInt(i++);
                    String compression = rs.getString(i++);
                    double minX = rs.getDouble(i++);
                    double minY = rs.getDouble(i++);
                    double maxX = rs.getDouble(i++);
                    double maxY = rs.getDouble(i++);
                    Rasterlite2Coverage rc = new Rasterlite2Coverage(database, coverageName, title, srid, compression, minX, minY,
                            maxX, maxY);
                    rasterCoverages.add(rc);
                }
                return rasterCoverages;

            }
        });
    }
}
