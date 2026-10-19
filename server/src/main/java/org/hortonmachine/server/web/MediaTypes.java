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
package org.hortonmachine.server.web;

import java.util.Locale;
import java.util.Map;

import org.hortonmachine.cli.ModuleDescriptor.DataType;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;

/**
 * The media types of the files, as given to the clients of the api.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class MediaTypes {
    public static final String GEOTIFF = "image/tiff; application=geotiff";
    public static final String OCTET_STREAM = "application/octet-stream";

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries( //
            Map.entry("tif", GEOTIFF), Map.entry("tiff", GEOTIFF), //
            Map.entry("asc", "text/plain"), Map.entry("txt", "text/plain"), Map.entry("csv", "text/csv"), //
            Map.entry("nc", "application/netcdf"), //
            Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"), //
            Map.entry("gpkg", "application/geopackage+sqlite3"), //
            Map.entry("geojson", "application/geo+json"), Map.entry("json", "application/json"), //
            Map.entry("shp", "application/vnd.shp"), Map.entry("dbf", "application/vnd.dbf"), //
            Map.entry("kml", "application/vnd.google-earth.kml+xml"), Map.entry("gml", "application/gml+xml"), //
            Map.entry("las", "application/vnd.las"), Map.entry("laz", "application/vnd.laszip"), //
            // the styles, as the STAC clients of HortonMachine recognize them
            Map.entry("sld", "application/vnd.ogc.sld+xml"), Map.entry("qml", "application/vnd.qgis.qml+xml"), //
            Map.entry("zip", "application/zip"));

    private MediaTypes() {
    }

    /**
     * @return the media type of a file by its extension, {@value #OCTET_STREAM} if not known.
     */
    public static String forFile( String name ) {
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return BY_EXTENSION.getOrDefault(extension, OCTET_STREAM);
    }

    /**
     * @return the media type of the files of a path parameter, or <code>null</code> if not known.
     */
    public static String forParameter( Parameter parameter ) {
        if (parameter.extension != null) {
            return forFile("x." + parameter.extension);
        }
        if (parameter.dataType == DataType.RASTER) {
            return GEOTIFF;
        }
        if (parameter.dataType == DataType.VECTOR) {
            return forFile("x.gpkg");
        }
        if (parameter.dataType == DataType.CSV) {
            return forFile("x.csv");
        }
        if (parameter.dataType == DataType.LAS) {
            return forFile("x.las");
        }
        return null;
    }
}
