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
package eu.hydrologis.jgrass.hortonmachine.io.grass;

import java.io.File;
import java.io.IOException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.ViewType;

import eu.hydrologis.jgrass.hortonmachine.libs.models.HMModel;
import eu.hydrologis.jgrass.hortonmachine.libs.monitor.DummyProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.libs.monitor.IHMProgressMonitor;

@Description("Utility class for reading grass rasters to geotools coverages.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Grass, Coverage, Raster, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class JGrassCoverageReader extends HMModel{
    @Description("The file to the map to be read (the cell file).")
    @In
    public String file = null;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Description("Flag that defines if the map should be read as a whole (false) or" + 
                 " on the active region (true and default).")
    @In
    public boolean doActive = true;

    @Description("The read output coverage map.")
    @Out
    public GridCoverage2D geodata = null;

    @Execute
    public void readCoverage() throws IOException {
        if (!concatOr(geodata == null, doReset)) {
            return;
        }
        JGrassMapEnvironment mapEnvironment = new JGrassMapEnvironment(new File(file));

        GrassCoverageReader tmp = new GrassCoverageReader(null, null, true, false, pm);
        tmp.setInput(mapEnvironment.getCELL());

        JGrassRegion jGrassRegion = null;
        if (doActive) {
            jGrassRegion = mapEnvironment.getActiveRegion();
        } else {
            jGrassRegion = mapEnvironment.getFileRegion();
        }
        GrassCoverageReadParam gcReadParam = new GrassCoverageReadParam(jGrassRegion);
        geodata = tmp.read(gcReadParam);
        geodata = geodata.view(ViewType.GEOPHYSICS);
    }
}
