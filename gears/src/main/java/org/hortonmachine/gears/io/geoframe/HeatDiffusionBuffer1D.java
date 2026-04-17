/*
 * GNU GPL v3 License
 *
 * Copyright 2019 Niccolò Tubini
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

@Description("Buffer for 1D heat diffusion simulation. This component temporarily the output and then passes them to "
		+ "the writer component")
@Documentation("")
@Author(name = "Niccolo' Tubini, Riccardo Rigon", contact = "tubini.niccolo@gmail.com")
@Keywords("Diffusion, heat equation, 1D problem")
//@Label("GEOframe.B")
@Name("HeatDiffusionBuffer1D")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class HeatDiffusionBuffer1D {

	@Description("Output variables of the current time step")
	@In 
	@Unit ("-")
	public ArrayList<double[]> inputVariable;

	@Description("Date of the current time step")
	@In 
	@Unit ("YYYY-MM-DD HH:mm")
	public String inputDate;

	@Description("Boolean value controlling the buffer component")
	@In 
	@Unit ("-")
	public boolean doProcessBuffer;
	
	@Description("Numeber of time step every which the ouptut is written to the disk."
			+ "Default is 1")
	@In 
	public int writeFrequency = 1;


	@Description("Output variable. This variable is passed to the writer component")
	@Out
	@Unit ()
	public LinkedHashMap<String,ArrayList<double[]>> myVariable = new LinkedHashMap<String,ArrayList<double[]>>();



	private int step=0;
	private ArrayList<double[]> tempVariable;


	/**
	 * Store the output in LinkedHashMap<String,ArrayList<double[]>>
	 */
	@Execute
	public void solve() {

		if(step==0){

			tempVariable = new ArrayList<double[]>();

		}
		

		if( ((step-1)%writeFrequency) == 0 || step == 1) {

			myVariable.clear();

		}

		if(doProcessBuffer== true) {

			// temperature
			tempVariable.add(inputVariable.get(0).clone());

			// theta_w
			tempVariable.add(inputVariable.get(1).clone());
			
			// internal energy
			tempVariable.add(inputVariable.get(2).clone());
			
			// diffusion heat flux
			tempVariable.add(inputVariable.get(3).clone());
			
			// errorEnergy
			tempVariable.add(inputVariable.get(4).clone());

			// heat flux at the top of the  domain 
			tempVariable.add(inputVariable.get(5).clone());

			// heat flux at the bottom of the  domain
			tempVariable.add(inputVariable.get(6).clone());


			myVariable.put(inputDate,(ArrayList<double[]>) tempVariable.clone());

			tempVariable.clear();
		}
		step++;
	

	}


}
