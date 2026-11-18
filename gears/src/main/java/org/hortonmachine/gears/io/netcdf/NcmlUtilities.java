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
package org.hortonmachine.gears.io.netcdf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * Utilities for NcML files, the xml files of netcdf-java that describe virtual datasets.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NcmlUtilities {

    private NcmlUtilities() {
    }

    /**
     * Write a NcML file that joins NetCDF files along an existing dimension, as files with consecutive timesteps
     * along time. The NcML file can be given to the NetCDF modules as a single file.
     *
     * @param files the files, in the order of the dimension.
     * @param dimension the name of the dimension to join along, as <code>time</code>.
     * @param ncmlFile the NcML file to write, with extension <code>.ncml</code>.
     * @return the NcML file.
     * @throws IOException if the file can't be written.
     */
    public static File joinExisting( List<File> files, String dimension, File ncmlFile ) throws IOException {
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No files to join.");
        }
        StringBuilder ncml = new StringBuilder();
        ncml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        ncml.append("<netcdf xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">\n");
        ncml.append("  <aggregation dimName=\"").append(escape(dimension)).append("\" type=\"joinExisting\">\n");
        for( File file : files ) {
            ncml.append("    <netcdf location=\"").append(escape(file.getAbsolutePath())).append("\"/>\n");
        }
        ncml.append("  </aggregation>\n");
        ncml.append("</netcdf>\n");
        FileUtilities.writeFile(ncml.toString(), ncmlFile);
        return ncmlFile;
    }

    private static String escape( String text ) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
