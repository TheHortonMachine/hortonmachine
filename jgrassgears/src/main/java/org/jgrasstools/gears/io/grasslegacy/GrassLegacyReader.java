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
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.gce.grassraster.JGrassMapEnvironment;
import org.geotools.gce.grassraster.JGrassRegion;
import org.jgrasstools.gears.io.grasslegacy.io.GrassRasterReader;
import org.jgrasstools.gears.io.grasslegacy.io.MapReader;
import org.jgrasstools.gears.io.grasslegacy.utils.Window;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

@Description("Legacy class for reading grass data the old way.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Grass, Raster, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class GrassLegacyReader extends JGTModel {
    @Description("The file to the map to be read (the cell file).")
    @In
    public String file = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("Flag that defines if the map should be read as a whole (false) or"
            + " on the active region (true and default).")
    @In
    public boolean doActive = true;

    @Description("The read output map.")
    @Out
    public double[][] geodata = null;

    @Execute
    public void readCoverage() throws Exception {
        if (!concatOr(geodata == null, doReset)) {
            return;
        }
        JGrassMapEnvironment mapEnvironment = new JGrassMapEnvironment(new File(file));
        JGrassRegion jGrassRegion = null;
        if (doActive) {
            jGrassRegion = mapEnvironment.getActiveRegion();
        } else {
            jGrassRegion = mapEnvironment.getFileRegion();
        }

        GrassRasterReader reader = new GrassRasterReader();
        try {
            reader.setReaderType(MapReader.RASTER_READER);
            reader.setOutputDataObject(new double[0][0]);
            reader.setDataWindow(new Window(jGrassRegion.getWest(), jGrassRegion.getEast(), jGrassRegion.getSouth(), jGrassRegion
                    .getNorth(), jGrassRegion.getWEResolution(), jGrassRegion.getNSResolution()));

            reader.open(mapEnvironment.getCELL().getAbsolutePath());
            if (reader.hasMoreData(pm)) {
                geodata = (double[][]) reader.getNextData();
            }
        } finally {
            reader.close();
        }
    }
}
