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
package org.hortonmachine.dbs.compat.objects;

import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.spatialite.ESpatialiteGeometryType;

/**
 * Class representing a db column.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ColumnLevel {
    public TableLevel parent;

    public String columnName;
    public String columnType;
    public boolean isPK = false;
    public GeometryColumn geomColumn;

    /**
     * if not null, it describes the table(colname) it references as foreign key.
     */
    public String references;

    public String[] tableColsFromFK() {
        String tmpReferences = references.replaceFirst("->", "").trim();
        String[] split = tmpReferences.split("\\(|\\)");
        String refTable = split[0];
        String refColumn = split[1];
        return new String[]{refTable, refColumn};
    }

    public void setFkReferences( ForeignKey fKey ) {
        references = " -> " + fKey.toTable + "(" + fKey.to + ")";
    }

    @Override
    public String toString() {
        if (geomColumn == null) {
            String col = columnName + " (" + columnType + ")";
            if (references != null) {
                col += " " + references;
            }
            return col;
        } else {
            String gType = ESpatialiteGeometryType.forValue(geomColumn.geometryType).getDescription();
            boolean indexEnabled = geomColumn.isSpatialIndexEnabled == 1 ? true : false;
            return columnName + " [" + gType + ",EPSG:" + geomColumn.srid + ",idx:" + indexEnabled + "]";
        }
    }
}
