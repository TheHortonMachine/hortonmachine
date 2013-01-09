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
import org.jgrasstools.hortonmachine.modules.geomorphology.ab.OmsAb;

@Description("Calculates the draining area per length unit.")
@Author(name = "Andrea Antonello, Erica Ghesla, Rigon Riccardo, Andrea Cozzini, Silvano Pisoni", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Geomorphology, OmsTca, OmsCurvatures, OmsDrainDir, OmsFlowDirections")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("_ab")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Ab extends OmsAb {
    @Description( "The map of the total contributing area.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inTca = null;

    @Description("The map of the planar curvatures.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inPlan = null;

    @Description("The map of area per length.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outAb = null;

    @Description("The map of contour line.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outB = null;

    @Execute
    public void process() throws Exception {
        GridCoverage2D inTcaGC = getRaster(inTca);
        GridCoverage2D inPlanGC = getRaster(inPlan);
        super.inTca = inTcaGC;
        super.inPlan = inPlanGC;

        // OmsAb ab = new OmsAb();
        // ab.inTca = inTcaGC;
        // ab.inPlan = inPlanGC;
        // ab.pm = pm;
        super.process();

        dumpRaster(super.outAb, outAb);
        dumpRaster(super.outB, outB);
    }


}
