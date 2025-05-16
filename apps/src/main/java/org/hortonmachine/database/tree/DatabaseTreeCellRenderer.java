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

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.DbLevel;
import org.hortonmachine.dbs.compat.objects.LeafLevel;
import org.hortonmachine.dbs.compat.objects.SchemaLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.compat.objects.TableTypeLevel;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.nosql.INosqlDb;
import org.hortonmachine.dbs.utils.SqlName;
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

    private ImageIcon h2gisIcon = ImageCache.getInstance().getImage(ImageCache.H2GIS32);

    private ImageIcon gpkgIcon = ImageCache.getInstance().getImage(ImageCache.GPKG32);

    private ImageIcon spatialiteIcon = ImageCache.getInstance().getImage(ImageCache.SPATIALITE32);

    private ImageIcon postgisIcon = ImageCache.getInstance().getImage(ImageCache.POSTGIS32);

    private ImageIcon databaseIcon = ImageCache.getInstance().getImage(ImageCache.DATABASE);

    private ImageIcon mongoIcon = ImageCache.getInstance().getImage(ImageCache.MONGO32);

    private ImageIcon tableFolderIcon = ImageCache.getInstance().getImage(ImageCache.TABLE_FOLDER);

    private ImageIcon tableTypeIcon = ImageCache.getInstance().getImage(ImageCache.TABLETYPE);

    private ImageIcon tableSpatialVirtualIcon = ImageCache.getInstance().getImage(ImageCache.TABLE_SPATIAL_VIRTUAL);

    private ImageIcon tableSpatialIcon = ImageCache.getInstance().getImage(ImageCache.TABLE_SPATIAL);

    private ImageIcon viewIcon = ImageCache.getInstance().getImage(ImageCache.VIEW);

    private ImageIcon tableIcon = ImageCache.getInstance().getImage(ImageCache.TABLE);

    private ImageIcon pkIcon = ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_PRIMARYKEY);

    private ImageIcon geomPointIon = ImageCache.getInstance().getImage(ImageCache.GEOM_POINT);

    private ImageIcon geomLineIcon = ImageCache.getInstance().getImage(ImageCache.GEOM_LINE);

    private ImageIcon geomPolyIcon = ImageCache.getInstance().getImage(ImageCache.GEOM_POLYGON);

    private ImageIcon tableColumnIcon = ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN);

    private ImageIcon tableColumnIndexIcon = ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_INDEX);

    private ImageIcon tableColumnFkIcon = ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_FK);

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
                    setIcon(h2gisIcon);
                    break;
                case GEOPACKAGE:
                    setIcon(gpkgIcon);
                    break;
                case SPATIALITE:
                case SQLITE:
                    setIcon(spatialiteIcon);
                    break;
                case POSTGIS:
                case POSTGRES:
                    setIcon(postgisIcon);
                    break;
                default:
                    setIcon(databaseIcon);
                    break;
                }
            } else if (nosqlDb != null) {
                switch( nosqlDb.getType() ) {
                case MONGODB:
                    setIcon(mongoIcon);
                    break;
                default:
                    setIcon(databaseIcon);
                    break;
                }
            }
        } else if (value instanceof SchemaLevel) {
            setIcon(tableFolderIcon);
        } else if (value instanceof TableTypeLevel) {
            setIcon(tableTypeIcon);
        } else if (value instanceof TableLevel) {
            TableLevel tableLevel = (TableLevel) value;
            ETableType tableType = tableLevel.tableType;
            if (tableLevel.isGeo) {
                if (tableType == ETableType.EXTERNAL) {
                    setIcon(tableSpatialVirtualIcon);
                } else {
                    setIcon(tableSpatialIcon);
                }
            } else {
                if (tableType == ETableType.VIEW) {
                    setIcon(viewIcon);
                } else {
                    setIcon(tableIcon);
                }
            }
        } else if (value instanceof ColumnLevel) {
            ColumnLevel columnLevel = (ColumnLevel) value;
            if (columnLevel.isPK) {
                setIcon(pkIcon);
            } else if (columnLevel.geomColumn != null) {
                EGeometryType gType = columnLevel.geomColumn.geometryType;
                if (gType != null) {
                    switch( gType ) {
                    case POINT:
                    case MULTIPOINT:
                        setIcon(geomPointIon);
                        break;
                    case LINESTRING:
                    case MULTILINESTRING:
                        setIcon(geomLineIcon);
                        break;
                    case POLYGON:
                    case MULTIPOLYGON:
                        setIcon(geomPolyIcon);
                        break;
                    default:
                        setIcon(tableColumnIcon);
                        break;
                    }
                } else {
                    setIcon(tableSpatialIcon);
                }
            } else if (columnLevel.index != null) {
                setIcon(tableColumnIndexIcon);
            } else if (columnLevel.references != null) {
                setIcon(tableColumnFkIcon);
            } else {
                setIcon(tableColumnIcon);
            }
        } else if (value instanceof LeafLevel) {
            LeafLevel leafLevel = (LeafLevel) value;
            if (leafLevel.isPK) {
                setIcon(pkIcon);
            } else if (leafLevel.geomColumn != null) {
                EGeometryType gType = leafLevel.geomColumn.geometryType;
                if (gType != null) {
                    switch( gType ) {
                    case POINT:
                    case MULTIPOINT:
                        setIcon(geomPointIon);
                        break;
                    case LINESTRING:
                    case MULTILINESTRING:
                        setIcon(geomLineIcon);
                        break;
                    case POLYGON:
                    case MULTIPOLYGON:
                        setIcon(geomPolyIcon);
                        break;
                    default:
                        setIcon(tableColumnIcon);
                        break;
                    }
                } else {
                    setIcon(tableSpatialIcon);
                }
            } else {
                setIcon(tableColumnIcon);
            }
        }

        return this;
    }

}
