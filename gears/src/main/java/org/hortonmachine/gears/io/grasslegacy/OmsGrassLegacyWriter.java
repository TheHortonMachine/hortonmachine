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
package org.hortonmachine.gears.io.grasslegacy;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_GEODATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_IN_WINDOW_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_UI;

import java.io.File;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.gce.grassraster.JGrassMapEnvironment;
import org.hortonmachine.gears.io.grasslegacy.io.GrassRasterWriter;
import org.hortonmachine.gears.io.grasslegacy.utils.Window;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

@Description(OMSGRASSLEGACYWRITER_DESCRIPTION)
@Author(name = OMSGRASSLEGACYWRITER_AUTHORNAMES, contact = OMSGRASSLEGACYWRITER_AUTHORCONTACTS)
@Keywords(OMSGRASSLEGACYWRITER_KEYWORDS)
@Label(OMSGRASSLEGACYWRITER_LABEL)
@Name(OMSGRASSLEGACYWRITER_NAME)
@Status(OMSGRASSLEGACYWRITER_STATUS)
@License(OMSGRASSLEGACYWRITER_LICENSE)
@UI(OMSGRASSLEGACYWRITER_UI)
public class OmsGrassLegacyWriter extends HMModel {

    @Description(OMSGRASSLEGACYWRITER_GEODATA_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT)
    @In
    public double[][] geodata = null;

    @Description(OMSGRASSLEGACYWRITER_IN_WINDOW_DESCRIPTION)
    @In
    public Window inWindow = null;

    @Description(OMSGRASSLEGACYWRITER_FILE_DESCRIPTION)
    @In
    public String file = null;

    private boolean hasWritten = false;

    @Execute
    public void writeRaster() throws Exception {
        if (!concatOr(!hasWritten, doReset)) {
            return;
        }
        JGrassMapEnvironment mapEnvironment = new JGrassMapEnvironment(new File(file));

        GrassRasterWriter writer = new GrassRasterWriter();
        try {
            writer.setOutputDataObject(new double[0][0]);
            writer.setDataWindow(inWindow);
            writer.open(mapEnvironment.getCELL().getAbsolutePath());
            writer.write(geodata);
        } finally {
            writer.close();
        }

        hasWritten = true;
    }

}
