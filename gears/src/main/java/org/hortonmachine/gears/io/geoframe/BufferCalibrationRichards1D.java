
/*
 * GNU GPL v3 License
 *
 * Copyright 2020 Niccolo` Tubini
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
import java.util.HashMap;

import oms3.annotations.*;

@Description("Extract values in the calibration points for 1D Richards simulation.")
@Documentation("")
@Author(name = "Niccolo' Tubini, Riccardo Rigon", contact = "tubini.niccolo@gmail.com")
@Keywords("Hydrology, Richards, Infiltration")
//@Label(JGTConstants.HYDROGEOMORPHOLOGY)
//@Name("shortradbal")
//@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class BufferCalibrationRichards1D {
	
	@Description("Index of measurament control volumes.")
	@In 
	@Unit ("-")
	public int[] controlVolumeIndex;
	
	@Description("Variable to store")
	@In 
	@Unit ("-")
	public ArrayList<double[]> inputVariable;
	
	@Description("Simulated values for water suction in the measuraments points.")
	@Out 
	@Unit ("m")
	public HashMap<Integer, double[]> simulatedPsi = null;
	
	@Description("Simulated values for the adimensional water content in the measuraments points.")
	@Out 
	@Unit ("-")
	public HashMap<Integer, double[]> simulatedTheta = null;
	
	private double[] tmp;
	private int step = 0;
	
	@Execute
	public void solve() {

		if(step==0) {
			
			simulatedPsi = new HashMap<Integer, double[]>();
			simulatedTheta = new HashMap<Integer, double[]>();
			
		}
		
		simulatedPsi.clear();
		simulatedTheta.clear();
		
		tmp = inputVariable.get(0).clone();
		
		for(int i=0; i<controlVolumeIndex.length; i++) {
			if(controlVolumeIndex[i]<0) {
				simulatedPsi.put(-9999, new double[] { -9999 });	
			} else {
				simulatedPsi.put(controlVolumeIndex[i], new double[] { tmp[controlVolumeIndex[i]] });
			}
		}
		
		
		
		tmp = inputVariable.get(1).clone();
		
		for(int i=0; i<controlVolumeIndex.length; i++) {
			if(controlVolumeIndex[i]<0) {
				simulatedTheta.put(-9999, new double[] { -9999 });	
			} else {
				simulatedTheta.put(controlVolumeIndex[i], new double[] { tmp[controlVolumeIndex[i]] });
			}
		}


		step++;
		
	}
	
	

}