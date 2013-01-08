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
package org.jgrasstools.modules;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.geomorphology.aspect.OmsAspect;

@Description("Calculates the aspect considering the zero toward the north and the rotation angle counterclockwise.")
@Documentation("Aspect.html")
@Author(name = "Andrea Antonello, Erica Ghesla, Rigon Riccardo, Pisoni Silvano, Andrea Cozzini", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Geomorphology, OmsDrainDir, OmsFlowDirections")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("_aspect")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Aspect extends JGTModel {
    @Description("The map of the digital elevation model (DEM).")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev = null;

    @Description("Switch to define whether create the output map in degrees (default) or radiants.")
    @In
    public boolean doRadiants = false;

    @Description("Switch to define whether the output map values should be rounded (might make sense in the case of degree maps).")
    @In
    public boolean doRound = false;

    @Description("The map of aspect.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outAspect = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outAspect == null, doReset)) {
            return;
        }
        checkNull(inElev);

        GridCoverage2D inElevGC = getRaster(inElev);

        OmsAspect aspect = new OmsAspect();
        aspect.inElev = inElevGC;
        aspect.doRound = true;
        aspect.pm = pm;
        aspect.process();
        GridCoverage2D aspectCoverage = aspect.outAspect;

        dumpRaster(aspectCoverage, outAspect);
    }

}
