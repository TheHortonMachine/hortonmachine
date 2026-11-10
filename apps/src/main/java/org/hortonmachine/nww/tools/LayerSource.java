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
package org.hortonmachine.nww.tools;

import java.io.File;

import org.hortonmachine.gears.libs.modules.HMConstants;

import gov.nasa.worldwind.layers.Layer;

/**
 * Where the data of a layer come from: a file and, for databases as geopackages, a table.
 *
 * <p>It is kept in the attributes of the layer, so that the {@link INwwLayerTool}s can read
 * more of the data than the layer holds.</p>
 *
 * @param file the file of the data.
 * @param table the table in the file, or <code>null</code> for single layer files.
 * @param filter the filter of the features shown by the layer, as a SQL WHERE condition for
 *          the database tables and as ECQL for the shapefiles, or <code>null</code> if all are
 *          shown.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record LayerSource( File file, String table, String filter ) {

    /** The key of the source in the attributes of the layers. */
    public static final String KEY = "org.hortonmachine.nww.tools.LayerSource";

    /**
     * The source of a layer showing all the features.
     *
     * @param file the file of the data.
     * @param table the table in the file, or <code>null</code> for single layer files.
     */
    public LayerSource( File file, String table ) {
        this(file, table, null);
    }

    /**
     * @param layer the layer.
     * @return the source of the layer, or <code>null</code> if it is not known.
     */
    public static LayerSource of( Layer layer ) {
        Object value = layer == null ? null : layer.getValue(KEY);
        return value instanceof LayerSource ? (LayerSource) value : null;
    }

    /**
     * Keep this source in the attributes of a layer.
     *
     * @param layer the layer.
     */
    public void attachTo( Layer layer ) {
        layer.setValue(KEY, this);
    }

    /**
     * @return the extension of the file, lower case and without the dot.
     */
    public String extension() {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    /**
     * @return <code>true</code> for a table of a geopackage.
     */
    public boolean isGeopackageTable() {
        return table != null && extension().equals(HMConstants.GPKG);
    }

    /**
     * @return <code>true</code> for a shapefile.
     */
    public boolean isShapefile() {
        return extension().equals("shp");
    }

    /**
     * @return the path of the data as the readers of HortonMachine take it: the file, followed
     *          by the table if there is one.
     */
    public String path() {
        return table == null ? file.getAbsolutePath() : file.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + table;
    }
}
