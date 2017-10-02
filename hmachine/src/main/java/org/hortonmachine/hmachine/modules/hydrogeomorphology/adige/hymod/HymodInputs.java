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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.hymod;

import org.hortonmachine.gears.libs.modules.HMModel;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

@Description("The HyMod model.")
@Author(name = "Silvia Franceschi, Andrea Antonello, Giuseppe Formetta", contact = "www.hydrologis.com")
@Keywords("Hydrology")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class HymodInputs extends HMModel {
    @Description("The maximum storage capacity")
    @In
    @Unit("mm")
    public Double pCmax = null;

    @Description("Degree of spatial variability of the soil moisture capacity.")
    @In
    @Unit("-")
    public Double pB = null;

    @Description("Factor distributing the flow between slow and quick reservoirs.")
    @In
    @Unit("-")
    public Double pAlpha = null;

    @Description("Part of slow tank that empties each time step.")
    @In
    @Unit("t-1")
    public Double pRs = null;

    @Description("Part of quick tank that empties each time step.")
    @In
    @Unit("t-1")
    public Double pRq = null;

    @Description("The first value of measured discharge")
    @In
    @Unit("m3/s")
    public Double pQ0 = null;
    
    @Description("The inputs instance for linking.")
    @Out
    public HymodInputs outInputs;

    @Execute
    public void process() throws Exception {
        outInputs = this;
    }

}
