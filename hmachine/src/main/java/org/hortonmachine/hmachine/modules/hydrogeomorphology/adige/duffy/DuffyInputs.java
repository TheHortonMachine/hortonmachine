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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.duffy;

import java.util.HashMap;

import org.hortonmachine.gears.io.adige.AdigeBoundaryCondition;
import org.hortonmachine.gears.libs.modules.HMModel;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description("Inputs for the duffy model")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Hydrology")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class DuffyInputs extends HMModel {

    @Description("The a field name of the avg_sub attribute in the hillslope data.")
    @In@Out
    public String fAvg_sub = null;

    @Description("The a field name of the var_sub attribute in the hillslope data.")
    @In@Out
    public String fVar_sub = null;

    @Description("The a field name of the avg_sup_10 attribute in the hillslope data.")
    @In@Out
    public String fAvg_sup_10 = null;

    @Description("The a field name of the var_sup_10 attribute in the hillslope data.")
    @In@Out
    public String fVar_sup_10 = null;

    @Description("The a field name of the avg_sup_30 attribute in the hillslope data.")
    @In@Out
    public String fAvg_sup_30 = null;

    @Description("The a field name of the var_sup_30 attribute in the hillslope data.")
    @In@Out
    public String fVar_sup_30 = null;

    @Description("The a field name of the avg_sup_60 attribute in the hillslope data.")
    @In@Out
    public String fAvg_sup_60 = null;

    @Description("The a field name of the var_sup_60 attribute in the hillslope data.")
    @In@Out
    public String fVar_sup_60 = null;

    @Description("The average speed for superficial runoff.")
    @In@Out
    public double pV_sup = -1;

    @Description("The average speed for sub-superficial runoff.")
    @In@Out
    public double pV_sub = -1;

    @Description("Saturated hydraulic conductivity.")
    @In@Out
    public double pKs = 3.0;

    @Description("Mstexp")
    @In@Out
    public double pMstexp = 11.0;

    @Description("Mstexp")
    @In@Out
    public double pDepthmnsat = 2.0;

    @Description("Specyield")
    @In@Out
    public double pSpecyield = 0.01;

    @Description("Porosity")
    @In@Out
    public double pPorosity = 0.41;

    @Description("Etrate")
    @In@Out
    public Double pEtrate = null;

    @Description("Satconst")
    @In@Out
    public double pSatconst = 0.3;

    @Description("The routing model type to use.")
    @In@Out
    public int pRouting = 3;

    @Description("Switch to write final boundary conditions.")
    @In@Out
    public boolean doBoundary = false;

    @Description("The initial conditions of the model.")
    @In@Out
    public HashMap<Integer, AdigeBoundaryCondition> inInitialconditions = null;

    @Description("Start discharge per unit area")
    @In@Out
    public double pDischargePerUnitArea = 0.01; // m3/s per km2 of upstream drainage area

    @Description("Start superficial discharge fraction")
    @In@Out
    public double pStartSuperficialDischargeFraction = 0.3;

    @Description("Start saturated volume fraction")
    @In@Out
    public double pMaxSatVolumeS1 = 0.2;

    @Description("Start unsaturated volume fraction")
    @In@Out
    public double pMaxSatVolumeS2 = 0.25;

    @Description("The water content in non saturated soil for every basin id.")
    @In@Out
    public HashMap<Integer, double[]> outS1;

    @Description("The water content in saturated soil for every basin id.")
    @In@Out
    public HashMap<Integer, double[]> outS2;

    @Description("The final conditions of the model to persist.")
    @In@Out
    public HashMap<Integer, AdigeBoundaryCondition> outFinalconditions = null;

    @Execute
    public void process() throws Exception {
    }

}
