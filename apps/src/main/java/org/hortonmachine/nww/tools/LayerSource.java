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
import java.net.URI;

import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.layers.Layer;

/**
 * Where the data of a layer come from: a file and, for databases as geopackages, a table, a
 * table of a database connection, as the ones saved in the database viewer, or a layer of a web
 * service, as WMS or WFS.
 *
 * <p>It is kept in the attributes of the layer, so that the {@link INwwLayerTool}s can read
 * more of the data than the layer holds.</p>
 *
 * @param file the file of the data, <code>null</code> for the tables of a database connection
 *          and the web services.
 * @param table the table in the file or in the database, or the layer of the web service, or
 *          <code>null</code> for single layer files.
 * @param filter the filter of the features shown by the layer, as a SQL WHERE condition for
 *          the database tables and as ECQL for the shapefiles, or <code>null</code> if all are
 *          shown.
 * @param connection the database connection of the table, <code>null</code> for the others.
 * @param service the address of the web service of the layer, <code>null</code> for the others.
 * @param extent the WGS84 lon/lat extent the features were read in, as the map view, for the
 *          tables of database connections and the WFS layers, <code>null</code> if all were read.
 *          Reading the data again, as with a new filter, keeps the extent.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record LayerSource( File file, String table, String filter, ConnectionData connection, String service,
        Envelope extent ) {

    /** The key of the source in the attributes of the layers. */
    public static final String KEY = "org.hortonmachine.nww.tools.LayerSource";

    /**
     * The source of a layer read from a file.
     *
     * @param file the file of the data.
     * @param table the table in the file, or <code>null</code> for single layer files.
     * @param filter the filter of the features shown by the layer, or <code>null</code> if all
     *          are shown.
     */
    public LayerSource( File file, String table, String filter ) {
        this(file, table, filter, null, null, null);
    }

    /**
     * The source of a layer read from a file, showing all the features.
     *
     * @param file the file of the data.
     * @param table the table in the file, or <code>null</code> for single layer files.
     */
    public LayerSource( File file, String table ) {
        this(file, table, null, null, null, null);
    }

    /**
     * The source of a layer read from a table of a database connection.
     *
     * @param connection the connection.
     * @param table the table, with its schema if not the default one.
     * @param filter the filter of the features as a SQL WHERE condition, or <code>null</code>
     *          if all are shown.
     * @param extent the WGS84 lon/lat extent the features were read in, <code>null</code> if
     *          all were read.
     * @return the source.
     */
    public static LayerSource ofDatabase( ConnectionData connection, String table, String filter, Envelope extent ) {
        return new LayerSource(null, table, filter, connection, null, extent);
    }

    /**
     * The source of a layer read from a web service.
     *
     * @param service the address of the service.
     * @param layerName the name of the layer in the service.
     * @param extent the WGS84 lon/lat extent the features were read in, <code>null</code> if
     *          all were read or for the services of images, as WMS.
     * @return the source.
     */
    public static LayerSource ofWebService( String service, String layerName, Envelope extent ) {
        return new LayerSource(null, layerName, null, null, service, extent);
    }

    /**
     * @param newFilter the new filter, <code>null</code> to show all the features.
     * @return the same source, with another filter.
     */
    public LayerSource withFilter( String newFilter ) {
        return new LayerSource(file, table, newFilter, connection, service, extent);
    }

    /**
     * @return <code>true</code> for a table of a database connection.
     */
    public boolean isDatabaseTable() {
        return connection != null;
    }

    /**
     * @return <code>true</code> for a layer of a web service.
     */
    public boolean isWebService() {
        return service != null;
    }

    /**
     * @param url the address of a web service.
     * @return the host of the service, or the address if it has none.
     */
    public static String serviceHost( String url ) {
        try {
            String host = new URI(url.trim()).getHost();
            return host != null ? host : url;
        } catch (Exception e) {
            return url;
        }
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
     * @return the extension of the file, lower case and without the dot, empty if there is no
     *          file.
     */
    public String extension() {
        if (file == null) {
            return "";
        }
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
     *          by the table if there is one. For the database connections, which have no file,
     *          the label of the connection followed by the table, for the web services the host
     *          of the service followed by the layer.
     */
    public String path() {
        if (file == null) {
            String label = connection != null ? connection.connectionLabel : service != null ? serviceHost(service) : "";
            return table == null ? label : label + HMConstants.DB_TABLE_PATH_SEPARATOR + table;
        }
        return table == null ? file.getAbsolutePath() : file.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + table;
    }
}
