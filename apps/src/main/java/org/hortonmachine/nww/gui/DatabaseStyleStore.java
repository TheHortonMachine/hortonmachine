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

import org.geotools.api.style.Style;
import org.hortonmachine.dbs.log.PreferencesDb;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.nww.tools.LayerSource;

/**
 * The styles of the tables of database connections, which are not saved in the databases but in
 * local .sld files, one folder per connection, in the folder of the HortonMachine preferences:
 * <code>.hortonmachine/styles/&lt;connection&gt;/&lt;table&gt;.sld</code>.
 *
 * <p>The files are named after the label of the connection, so renaming the connection in the
 * database viewer leaves its styles behind.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class DatabaseStyleStore {

    private DatabaseStyleStore() {
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
     * @param source the source of a layer read from a table of a database connection.
     * @return the style file of the table, which may not exist.
     */
    public static File getStyleFile( LayerSource source ) {
        if (!source.isDatabaseTable()) {
            throw new IllegalArgumentException("Not a table of a database connection: " + source.path());
        }
        File connectionFolder = new File(getStylesFolder(), safeName(source.connection().connectionLabel));
        return new File(connectionFolder, safeName(source.table()) + "." + SldUtilities.SLD_EXTENSION);
    }

    /**
     * @param source the source of a layer read from a table of a database connection.
     * @return the saved style of the table, or <code>null</code> if there is none.
     */
    public static Style read( LayerSource source ) {
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
     * Save the style of a table, replacing the one saved before.
     *
     * @param source the source of a layer read from a table of a database connection.
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
