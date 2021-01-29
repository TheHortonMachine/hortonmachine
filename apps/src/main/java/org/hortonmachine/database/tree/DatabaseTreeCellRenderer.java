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

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.DbLevel;
import org.hortonmachine.dbs.compat.objects.LeafLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.compat.objects.TypeLevel;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.nosql.INosqlDb;
import org.hortonmachine.gui.utils.ImageCache;

/**
 * Database tree cell renderer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class DatabaseTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 1L;

    private ADb db;
    private INosqlDb nosqlDb;

    public DatabaseTreeCellRenderer( ADb db ) {
        this.db = db;
    }

    public DatabaseTreeCellRenderer( INosqlDb nosqlDb ) {
        this.nosqlDb = nosqlDb;
    }

    @Override
    public java.awt.Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus ) {

        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (value instanceof DbLevel) {
            if (db != null) {
                switch( db.getType() ) {
                case H2GIS:
                case H2:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.H2GIS32));
                    break;
                case GEOPACKAGE:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.GPKG32));
                    break;
                case SPATIALITE:
                case SQLITE:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.SPATIALITE32));
                    break;
                case POSTGIS:
                case POSTGRES:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.POSTGIS32));
                    break;
                default:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.DATABASE));
                    break;
                }
            } else if (nosqlDb != null) {
                switch( nosqlDb.getType() ) {
                case MONGODB:
                    setIcon(ImageCache.getInstance().getImage(ImageCache.MONGO32));
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
                if (db != null) {
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
                } else {
                    setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE));
                }
            } catch (Exception e) {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE));
                e.printStackTrace();
            }
        } else if (value instanceof ColumnLevel) {
            ColumnLevel columnLevel = (ColumnLevel) value;
            if (columnLevel.isPK) {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_PRIMARYKEY));
            } else if (columnLevel.geomColumn != null) {
                EGeometryType gType = columnLevel.geomColumn.geometryType;
                if (gType != null) {
                    switch( gType ) {
                    case POINT:
                    case MULTIPOINT:
                        setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POINT));
                        break;
                    case LINESTRING:
                    case MULTILINESTRING:
                        setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_LINE));
                        break;
                    case POLYGON:
                    case MULTIPOLYGON:
                        setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POLYGON));
                        break;
                    default:
                        setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN));
                        break;
                    }
                } else {
                    setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_SPATIAL));
                }
            } else if (columnLevel.index != null) {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_INDEX));
            } else if (columnLevel.references != null) {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_FK));
            } else {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN));
            }
        } else if (value instanceof LeafLevel) {
            LeafLevel leafLevel = (LeafLevel) value;
            if (leafLevel.isPK) {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_PRIMARYKEY));
            } else if (leafLevel.geomColumn != null) {
                EGeometryType gType = leafLevel.geomColumn.geometryType;
                if (gType != null) {
                    switch( gType ) {
                    case POINT:
                    case MULTIPOINT:
                        setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POINT));
                        break;
                    case LINESTRING:
                    case MULTILINESTRING:
                        setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_LINE));
                        break;
                    case POLYGON:
                    case MULTIPOLYGON:
                        setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POLYGON));
                        break;
                    default:
                        setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN));
                        break;
                    }
                } else {
                    setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_SPATIAL));
                }
            } else {
                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN));
            }
        }

        return this;
    }

}
