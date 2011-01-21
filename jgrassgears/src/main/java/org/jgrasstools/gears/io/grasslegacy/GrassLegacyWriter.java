/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Grass, Raster, Writing")
@Label(JGTConstants.RASTERWRITER)
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class GrassLegacyWriter extends JGTModel {
    @Description("The map that needs to be written.")
    @UI(JGTConstants.FILE_UI_HINT)
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
