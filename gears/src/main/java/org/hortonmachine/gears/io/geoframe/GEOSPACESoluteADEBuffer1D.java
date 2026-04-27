
/*
 * GNU GPL v3 License
 *
 * Copyright 2016 Niccolo` Tubini
 *
 * This program is free software: you can redistribute it and/or modify
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

package org.hortonmachine.gears.io.geoframe;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import oms3.annotations.*;

@Description("Buffer for 1D Richards simulation.")
@Documentation("")
@Author(name = "Concetta D'Amato, Niccolo' Tubini, Riccardo Rigon", contact = "tubini.niccolo@gmail.com")
@Keywords("Hydrology, Richards, Infiltration")
//@Label(JGTConstants.HYDROGEOMORPHOLOGY)
//@Name("shortradbal")
//@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class GEOSPACESoluteADEBuffer1D {
	
	@Description("Variable to store from WHETGEO")
	@In 
	@Unit ("-")
	public ArrayList<double[]> inputVariableRichards;
	
	@Description("Variable to store from GEOET")
	@In 
	@Unit ("-")
	public ArrayList<double[]> inputVariableStressFactor;
	
	@Description("Variable to store from BrokerGEO")
	@In 
	@Unit ("-")
	public ArrayList<double[]> inputVariableBroker;
	
	@Description("Date at which the varible is computed")
	@In 
	@Unit ("YYYY-MM-DD HH:mm")
	public String inputDate;
	
	@Description("Boolean value controlling the buffer component")
	@In 
	@Unit ("-")
	public boolean doProcessBuffer;
	
	@In 
	public int writeFrequency = 1;
	
	@Description()
	@Out
	@Unit ()
	public LinkedHashMap<String,ArrayList<double[]>> myVariable = new LinkedHashMap<String,ArrayList<double[]>>(); // consider the opportunity to save varibale as float instead of double
	
	
	@Description("")
	private int step=0;
	
	private ArrayList<double[]> tempVariable;
	
	
	
	@Execute
	public void solve() {

		if(step==0){
			
			tempVariable = new ArrayList<double[]>();
		
		}
		
		if( ((step-1)%writeFrequency) == 0 || step == 1) {
//			System.out.println("Buffer1D clear");

			myVariable.clear();

		}
		
		if(doProcessBuffer== true) {
			//0 water suction values
			tempVariable.add(inputVariableRichards.get(0).clone());
			
			//1 thetas
			tempVariable.add(inputVariableRichards.get(1).clone());
				
			//2 water volume
			tempVariable.add(inputVariableRichards.get(2).clone());

			//3 Darcy velocities
			tempVariable.add(inputVariableRichards.get(3).clone());
				
			//4 Darcy velocities due to capillary gradient
			tempVariable.add(inputVariableRichards.get(4).clone());

			//5 Darcy velocities due to gravity gradient
			tempVariable.add(inputVariableRichards.get(5).clone());

			//6 pore velocities 
			tempVariable.add(inputVariableRichards.get(6).clone());

			//7 celerities
			tempVariable.add(inputVariableRichards.get(7).clone());

			//8 kinematic ratio
			tempVariable.add(inputVariableRichards.get(8).clone());

			//9 ETs i.e. transpired stressed water
			tempVariable.add(inputVariableRichards.get(9).clone());
				
			//10 errorVolume
			tempVariable.add(inputVariableRichards.get(10).clone());

			//11 top boundary condition value
			tempVariable.add(inputVariableRichards.get(11).clone());

			//12 bottom boundary condition value
			tempVariable.add(inputVariableRichards.get(12).clone());

			//13 surface run-off
			tempVariable.add(inputVariableRichards.get(13).clone());
			
			
			//14 Concentrations
			tempVariable.add(inputVariableRichards.get(14).clone());

			//15 WaterVolumeConcentrationsNew
			tempVariable.add(inputVariableRichards.get(15).clone());

			//16 soluteSourcesSinksTerm
			tempVariable.add(inputVariableRichards.get(16).clone());
						
			//17 sumSoluteSourceSinkTerm
			tempVariable.add(inputVariableRichards.get(17).clone());
						
			//18 timeVariationWaterVolumesConcentration
			tempVariable.add(inputVariableRichards.get(18).clone());

			//19 Dispersion solute flux
			tempVariable.add(inputVariableRichards.get(19).clone());

			//20 Advection solute fux
			tempVariable.add(inputVariableRichards.get(20).clone());
						
			//21 error in WaterVolumeConcentration
			tempVariable.add(inputVariableRichards.get(21).clone());

			//22 Average solute concentrations
			tempVariable.add(inputVariableRichards.get(22).clone());

			//23 Average WaterVolumeSoluteConcentration
			tempVariable.add(inputVariableRichards.get(23).clone());
			
			
			//24 water stress factor for each control volume g
			tempVariable.add(inputVariableStressFactor.get(0).clone());
				
			//25 water stress factor GnT[0]
			tempVariable.add(inputVariableStressFactor.get(1).clone());
							
			//26 evaporation water stress factor GnE[0]
			tempVariable.add(inputVariableStressFactor.get(2).clone());
							
			//27 stress factor sun 
			tempVariable.add(inputVariableStressFactor.get(3).clone());
							
			//28 stress factor shade 
			tempVariable.add(inputVariableStressFactor.get(4).clone());
				
			
			//29 evapotranspiration from each control volume 
			tempVariable.add(inputVariableBroker.get(0).clone());
			
			//30 root density from each control volume 
			tempVariable.add(inputVariableBroker.get(1).clone());
				
			//31 transpiration from each control volume 
			tempVariable.add(inputVariableBroker.get(2).clone());
				
			//32 evaporation from each control volume 
			tempVariable.add(inputVariableBroker.get(3).clone());
				
			

			myVariable.put(inputDate,(ArrayList<double[]>) tempVariable.clone());

			tempVariable.clear();
		}
		step++;
		
	}
	

}