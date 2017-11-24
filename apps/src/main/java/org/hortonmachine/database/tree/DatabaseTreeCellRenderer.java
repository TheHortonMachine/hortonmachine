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
package org.hortonmachine.database.tree;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.DbLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.compat.objects.TypeLevel;
import org.hortonmachine.dbs.spatialite.ESpatialiteGeometryType;
import org.hortonmachine.gui.utils.ImageCache;

/**
 * Database tree cell renderer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class DatabaseTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 1L;

    private ASpatialDb db;

    public DatabaseTreeCellRenderer( ASpatialDb db ) {
        this.db = db;
    }

    public void setDb( ASpatialDb db ) {
        this.db = db;
    }

    @Override
    public java.awt.Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus ) {

        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (value instanceof DbLevel) {
            if (db != null) {
                switch( db.getType() ) {
                case H2GIS:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.H2GIS32));
                    break;
                case SPATIALITE:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.SPATIALITE32));
                    break;
                default:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.DATABASE));
                    break;
                }
            }
        } else if (value instanceof TypeLevel) {
            setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_FOLDER));
        } else if (value instanceof TableLevel) {
            TableLevel tableLevel = (TableLevel) value;
            try {
                ETableType tableType = db.getTableType(tableLevel.tableName);
                if (tableLevel.isGeo) {
                    if (tableType == ETableType.EXTERNAL) {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_SPATIAL_VIRTUAL));
                    } else {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_SPATIAL));
                    }
                } else {
                    if (tableType == ETableType.VIEW) {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.VIEW));
                    } else {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE));
                    }
                }
            } catch (Exception e) {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE));
                e.printStackTrace();
            }
        } else if (value instanceof ColumnLevel) {
            ColumnLevel columnLevel = (ColumnLevel) value;
            if (columnLevel.isPK) {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_PRIMARYKEY));
            } else if (columnLevel.index != null) {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_INDEX));
            } else if (columnLevel.references != null) {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_FK));
            } else if (columnLevel.geomColumn != null) {
                ESpatialiteGeometryType gType = ESpatialiteGeometryType.forValue(columnLevel.geomColumn.geometryType);
                switch( gType ) {
                case POINT_XY:
                case POINT_XYM:
                case POINT_XYZ:
                case POINT_XYZM:
                case MULTIPOINT_XY:
                case MULTIPOINT_XYM:
                case MULTIPOINT_XYZ:
                case MULTIPOINT_XYZM:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POINT));
                    break;
                case LINESTRING_XY:
                case LINESTRING_XYM:
                case LINESTRING_XYZ:
                case LINESTRING_XYZM:
                case MULTILINESTRING_XY:
                case MULTILINESTRING_XYM:
                case MULTILINESTRING_XYZ:
                case MULTILINESTRING_XYZM:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_LINE));
                    break;
                case POLYGON_XY:
                case POLYGON_XYM:
                case POLYGON_XYZ:
                case POLYGON_XYZM:
                case MULTIPOLYGON_XY:
                case MULTIPOLYGON_XYM:
                case MULTIPOLYGON_XYZ:
                case MULTIPOLYGON_XYZM:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POLYGON));
                    break;
                default:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN));
                    break;
                }
            } else {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN));
            }
        }

        return this;
    }

}
