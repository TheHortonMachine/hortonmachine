
/*
 * GNU GPL v3 License
 *
 * Copyright 2019 Niccolo` Tubini
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

import java.io.IOException;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Unit;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

@Description("This class reads a NetCDF containing Richards 1D output.")
@Documentation("")
@Author(name = "Niccolo' Tubini, Riccardo Rigon", contact = "tubini.niccolo@gmail.com")
@Keywords("Hydrology, Richards, Infiltration")
// @Label(JGTConstants.HYDROGEOMORPHOLOGY)
// @Name("shortradbal")
// @Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class ReadNetCDFRichardsOutput1D {

	@Description("File name of NetCDF containing output")
	@In
	public String richardsOutputFilename;

	@Description("Water suction")
	@Out
	@Unit("m")
	public double[][] psi;

	@Description("Dimensional water content.")
	@Out
	@Unit("m")
	public double[][] theta;

	private int[] size;

	@Execute
	public void read() throws IOException {


		// Open the file. The ReadOnly parameter tells netCDF we want
		// read-only access to the file.
		NetcdfFile dataFile = null;
		String filename = richardsOutputFilename;
		// Open the file.
		try {

			dataFile = NetcdfFile.open(filename, null);

			// Retrieve the variables named "___"
			Variable dataPsi = dataFile.findVariable("psi");
			Variable dataTheta = dataFile.findVariable("theta");

			size = dataPsi.getShape();

			psi = new double[size[0]][size[1]];
			theta = new double[size[0]][size[1]];

			ArrayDouble.D2 dataArrayPsi = (ArrayDouble.D2) dataPsi.read(null, size);
			ArrayDouble.D2 dataArrayTheta = (ArrayDouble.D2) dataTheta.read(null, size);

			for (int i = 0; i < size[0]; i++) {
				
				for (int j = 0; j < size[1]; j++) {
					
					psi[i][j] = dataArrayPsi.get(i,j);
					theta[i][j] = dataArrayTheta.get(i,j);

				}

			}

		} catch (InvalidRangeException e) {
			e.printStackTrace();

		} finally {
			if (dataFile != null)
				try {
					dataFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}

		System.out.println("*** SUCCESS reading " + richardsOutputFilename);

	}

	public static void main(String[] args)  throws Exception {

		ReadNetCDFRichardsOutput1D myReader = new ReadNetCDFRichardsOutput1D();

		myReader.richardsOutputFilename = "C:/Users/Niccolo/eclipse-workspace/richards1D/resources/Output/test1.nc";

		myReader.read();
	}
}