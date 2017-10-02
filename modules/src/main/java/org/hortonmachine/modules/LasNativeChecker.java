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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import org.hortonmachine.gears.io.las.core.Las;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

@Description("Cheker for las native liblas installation.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("las, native")
@Label(HMConstants.VECTORPROCESSING)
@Name("lasnativechecker")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class LasNativeChecker extends HMModel {

    @Execute
    public void process() throws Exception {
        // LiblasWrapper.loadNativeLibrary("/usr/local/lib/", "las_c");

        boolean supportsNative = Las.supportsNative();

        if (supportsNative) {
            pm.message("This installation supports native las\nreading and writing through the liblas project.");
        } else {
            pm.message("This installation does not support native las\nreading and writing through the liblas project.");
            pm.message("No native libraries could be found.");
            pm.message("The library path contains:");
            String[] libraryPaths = Las.getLibraryPaths();
            for( String path : libraryPaths ) {
                if (path.trim().length() > 0) {
                    pm.message(" - " + path);
                }
            }
        }
    }

    public static void main( String[] args ) throws Exception {
        new LasNativeChecker().process();
    }

}
