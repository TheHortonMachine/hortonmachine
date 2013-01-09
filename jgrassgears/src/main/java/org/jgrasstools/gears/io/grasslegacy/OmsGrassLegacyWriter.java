/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.io.grasslegacy;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_UI;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_geodata_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGRASSLEGACYWRITER_inWindow_DESCRIPTION;

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
import org.jgrasstools.gears.io.grasslegacy.io.GrassRasterWriter;
import org.jgrasstools.gears.io.grasslegacy.utils.Window;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSGRASSLEGACYWRITER_DESCRIPTION)
@Author(name = OMSGRASSLEGACYWRITER_AUTHORNAMES, contact = OMSGRASSLEGACYWRITER_AUTHORCONTACTS)
@Keywords(OMSGRASSLEGACYWRITER_KEYWORDS)
@Label(OMSGRASSLEGACYWRITER_LABEL)
@Name(OMSGRASSLEGACYWRITER_NAME)
@Status(OMSGRASSLEGACYWRITER_STATUS)
@License(OMSGRASSLEGACYWRITER_LICENSE)
@UI(OMSGRASSLEGACYWRITER_UI)
public class OmsGrassLegacyWriter extends JGTModel {

    @Description(OMSGRASSLEGACYWRITER_geodata_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public double[][] geodata = null;

    @Description(OMSGRASSLEGACYWRITER_inWindow_DESCRIPTION)
    @In
    public Window inWindow = null;

    @Description(OMSGRASSLEGACYWRITER_file_DESCRIPTION)
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
