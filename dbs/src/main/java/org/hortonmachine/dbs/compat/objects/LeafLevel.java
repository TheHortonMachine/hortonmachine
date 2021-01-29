package org.hortonmachine.dbs.compat.objects;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.nosql.NosqlGeometryColumn;

/**
 * Class representing a generic leaf.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LeafLevel {

    Object parent;

    public String columnName;
    public String columnType;
    public boolean isPK = false;
    public GeometryColumn geomColumn;

    public List<LeafLevel> leafsList = new ArrayList<LeafLevel>();

    @Override
    public String toString() {
        if (geomColumn == null) {
            String col = columnName + " (" + columnType + ")";
            return col;
        } else {
            if (geomColumn instanceof NosqlGeometryColumn) {
                NosqlGeometryColumn gc = (NosqlGeometryColumn) geomColumn;
                return columnName + " [" + gc.indexType + ",EPSG:" + geomColumn.srid + ",idx: true]";
            } else {
                String gType = geomColumn.geometryType.getTypeName();
                boolean indexEnabled = geomColumn.isSpatialIndexEnabled == 1 ? true : false;
                return columnName + " [" + gType + ",EPSG:" + geomColumn.srid + ",idx:" + indexEnabled + "]";
            }
        }
    }

}
