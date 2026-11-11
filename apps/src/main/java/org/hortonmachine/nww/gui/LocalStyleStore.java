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
package org.hortonmachine.nww.gui;

import java.io.File;
import java.net.URI;

import org.geotools.api.style.Style;
import org.hortonmachine.dbs.log.PreferencesDb;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.nww.tools.LayerSource;

/**
 * The styles of the layers whose data can't hold a style: the tables of database connections,
 * whose styles are not saved in the databases, and the layers of web services, as WFS. They are
 * saved in local .sld files, in the folder of the HortonMachine preferences:
 * <code>.hortonmachine/styles/&lt;source&gt;/&lt;layer&gt;.sld</code>, where the source is the
 * label of the connection or the host and path of the service.
 *
 * <p>Renaming a connection in the database viewer leaves its styles behind.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class LocalStyleStore {

    private LocalStyleStore() {
    }

    /**
     * @param source the source of a layer.
     * @return <code>true</code> if the styles of the layer are saved here.
     */
    public static boolean handles( LayerSource source ) {
        return source != null && (source.isDatabaseTable() || source.isWebService()) && source.table() != null;
    }

    /**
     * @return the folder of the styles, next to the preferences database.
     */
    public static File getStylesFolder() {
        File prefsFile = PreferencesDb.INSTANCE.isValid() ? PreferencesDb.INSTANCE.getDbFile() : null;
        File baseFolder = prefsFile != null && prefsFile.getParentFile() != null
                ? prefsFile.getParentFile()
                : PreferencesDb.getBaseFolder();
        return new File(new File(baseFolder, ".hortonmachine"), "styles");
    }

    /**
     * @param source the source of a layer read from a table of a database connection or from a
     *          web service.
     * @return the style file of the layer, which may not exist.
     */
    public static File getStyleFile( LayerSource source ) {
        if (!handles(source)) {
            throw new IllegalArgumentException("Not a table of a database connection or a web service layer: " + source.path());
        }
        String folderName = source.isDatabaseTable() ? source.connection().connectionLabel : serviceFolderName(source.service());
        File sourceFolder = new File(getStylesFolder(), safeName(folderName));
        return new File(sourceFolder, safeName(source.table()) + "." + SldUtilities.SLD_EXTENSION);
    }

    /**
     * @return the host and path of a service, which tell apart the services of a server, as
     *          the workspaces of a GeoServer, without the parameters of the request.
     */
    private static String serviceFolderName( String service ) {
        try {
            URI uri = new URI(service.trim());
            if (uri.getHost() != null) {
                return uri.getHost() + (uri.getPath() != null ? uri.getPath() : "");
            }
        } catch (Exception e) {
            // the whole address
        }
        return service;
    }

    /**
     * @param source the source of a layer.
     * @return the saved style of the layer, or <code>null</code> if there is none.
     */
    public static Style read( LayerSource source ) {
        if (!handles(source)) {
            return null;
        }
        File styleFile = getStyleFile(source);
        if (!styleFile.exists()) {
            return null;
        }
        try {
            return SldUtilities.getStyleFromSldString(FileUtilities.readFile(styleFile));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save the style of a layer, replacing the one saved before.
     *
     * @param source the source of a layer read from a table of a database connection or from a
     *          web service.
     * @param style the style.
     * @return the file written.
     * @throws Exception if the file can't be written.
     */
    public static File save( LayerSource source, Style style ) throws Exception {
        File styleFile = getStyleFile(source);
        File folder = styleFile.getParentFile();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalArgumentException("Unable to create the folder of the styles: " + folder);
        }
        FileUtilities.writeFile(SldUtilities.styleToString(style), styleFile);
        return styleFile;
    }

    /**
     * @return the name with the characters that are not safe in file names replaced by
     *          underscores.
     */
    static String safeName( String name ) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
