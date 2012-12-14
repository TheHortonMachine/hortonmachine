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

import java.io.File;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.gce.grassraster.JGrassMapEnvironment;
import org.jgrasstools.gears.io.grasslegacy.io.GrassRasterWriter;
import org.jgrasstools.gears.io.grasslegacy.utils.Window;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;

@Description("Legacy class for writing grass rasters.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Grass, Raster, Writing")
@Label(JGTConstants.RASTERWRITER)
@UI(JGTConstants.HIDE_UI_HINT)
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class GrassLegacyWriter extends JGTModel {
    @Description("The map that needs to be written.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public double[][] geodata = null;

    @Description("The region for the map to be written.")
    @In
    public Window inWindow = null;
    
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The file to the map to be written (the cell file).")
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
