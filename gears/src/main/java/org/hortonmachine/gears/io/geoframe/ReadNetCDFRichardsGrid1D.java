
/*
 * GNU GPL v3 License
 *
 * Copyright 2019 Niccolo' Tubini
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
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Unit;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

@Description("This class reads a NetCDF containing 1D grid data. The input file is created with WHETGEO1D.ipynb (Jupyter Notebook)")
@Documentation("")
@Author(name = "Niccolo' Tubini, Riccardo Rigon", contact = "tubini.niccolo@gmail.com")
@Keywords("Hydrology, Richards, Infiltration")
// @Label(JGTConstants.HYDROGEOMORPHOLOGY)
// @Name("shortradbal")
// @Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class ReadNetCDFRichardsGrid1D {

	@Description("File name of NetCDF containing grid data")
	@In
	public String richardsGridFilename;
	
	@Description("Number of control volume")
	@Out
	@Unit("-")
	public int KMAX;

	@Description("eta coordinate of volume centroids: zero is at soil surface and and positive upward")
	@Out
	@Unit("m")
	public double[] eta;

	@Description("eta coordinate of volume interfaces: zero is at soil surface and and positive upward.")
	@Out
	@Unit("m")
	public double[] etaDual;

	@Description("z coordinate  of volume centroids: zero is at the bottom of the column and and positive upward")
	@Out
	@Unit("m")
	public double[] z;

	@Description("z coordinate of volume interfaces: zero is at soil surface and and positive upward.")
	@Out
	@Unit("m")
	public double[] zDual;

	@Description("Initial condition for water suction")
	@Out
	@Unit("m")
	public double[] psiIC;

	@Description("Soil temperature profile")
	@Out
	@Unit("K")
	public double[] temperature;

	@Description("Distance between consecutive controids, is used to compute gradients")
	@Out
	@Unit("m")
	public double[] spaceDelta;

	@Description("Length of each control volume")
	@Out
	@Unit("m")
	public double[] controlVolume;

	@Description("Label describing the equation state for each control volume")
	@Out
	@Unit("-")
	public int[] equationStateID;

	@Description("Label identifying the set of parameters for each control volume")
	@Out
	@Unit("-")
	public int[] parameterID;

	@Description("Adimensional water content at saturation")
	@Out
	@Unit("-")
	public double[] thetaS;

	@Description("Adimensional residual water content")
	@Out
	@Unit("-")
	public double[] thetaR;
	
	@Description("Hydraulic conductivity at saturation")
	@Out
	@Unit("m/s")
	public double[] Ks;

	@Description("Aquitard compressibility")
	@Out
	@Unit("1/Pa")
	public double[] alphaSS;

	@Description("Water compressibility")
	@Out
	@Unit("1/Pa")
	public double[] betaSS;

	@Description("First SWRC parameter")
	@Out
	@Unit(" ")
	public double[] par1SWRC;

	@Description("Second SWRC parameter")
	@Out
	@Unit(" ")
	public double[] par2SWRC;

	@Description("Third SWRC parameter")
	@Out
	@Unit(" ")
	public double[] par3SWRC;

	@Description("Fouth SWRC parameter")
	@Out
	@Unit(" ")
	public double[] par4SWRC;

	@Description("Fifth SWRC parameter")
	@Out
	@Unit(" ")
	public double[] par5SWRC;
	
	@Description("Control volume index for calibration")
	@Out
	@Unit(" ")
	public int[] controlVolumeIndex;


	int[] size;

	int step = 0;

	@Execute
	public void read() throws IOException {

		if (step == 0) {

			// Open the file. The ReadOnly parameter tells netCDF we want
			// read-only access to the file.
			NetcdfFile dataFile = null;
			String filename = richardsGridFilename;
			// Open the file.
			try {

				dataFile = NetcdfFile.open(filename, null);

				// Retrieve the variables named "___"
				Variable dataKMAX = dataFile.findVariable("KMAX");
				Variable dataEta = dataFile.findVariable("eta");
				Variable dataEtaDual = dataFile.findVariable("etaDual");
				Variable dataZ = dataFile.findVariable("z");
				Variable dataZDual = dataFile.findVariable("zDual");
				Variable dataPsiIC = dataFile.findVariable("psi0");
				Variable dataTemperatureIC = dataFile.findVariable("T0");
				Variable dataSpaceDelta = dataFile.findVariable("spaceDelta");
				Variable dataControlVolume = dataFile.findVariable("controlVolume");
				Variable dataEquationStateID = dataFile.findVariable("equationStateID");
				Variable dataParameterID = dataFile.findVariable("parameterID");
				Variable dataThetaS = dataFile.findVariable("thetaS");
				Variable dataThetaR = dataFile.findVariable("thetaR");
				Variable dataKs = dataFile.findVariable("ks");
				Variable dataAlphaSS = dataFile.findVariable("alphaSpecificStorage");
				Variable dataBetaSS = dataFile.findVariable("betaSpecificStorage");
				Variable dataPar1SWRC = dataFile.findVariable("par1SWRC");
				Variable dataPar2SWRC = dataFile.findVariable("par2SWRC");
				Variable dataPar3SWRC = dataFile.findVariable("par3SWRC");
				Variable dataPar4SWRC = dataFile.findVariable("par4SWRC");
				Variable dataPar5SWRC = dataFile.findVariable("par5SWRC");
				Variable dataControlVolumeIndex = dataFile.findVariable("controlVolumeIndex");

				KMAX = dataKMAX.readScalarInt();
				
				size = dataEta.getShape();

				eta = new double[size[0]];
				etaDual = new double[size[0]];
				z = new double[size[0]];
				zDual = new double[size[0]];
				psiIC = new double[size[0]];
				temperature = new double[size[0]];
				controlVolume = new double[size[0]];
				equationStateID = new int[size[0]];
				parameterID = new int[size[0]];
				
				ArrayDouble.D1 dataArrayEta = (ArrayDouble.D1) dataEta.read(null, size);
				ArrayDouble.D1 dataArrayEtaDual = (ArrayDouble.D1) dataEtaDual.read(null, size);
				ArrayDouble.D1 dataArrayZ = (ArrayDouble.D1) dataZ.read(null, size);
				ArrayDouble.D1 dataArrayZDual = (ArrayDouble.D1) dataZDual.read(null, size);
				ArrayDouble.D1 dataArrayPsiIC = (ArrayDouble.D1) dataPsiIC.read(null, size);
				ArrayDouble.D1 dataArrayTemperatureIC = (ArrayDouble.D1) dataTemperatureIC.read(null, size);				
				ArrayDouble.D1 dataArrayControlVolume = (ArrayDouble.D1) dataControlVolume.read(null, size);
				ArrayInt.D1 dataArrayEquationStateID = (ArrayInt.D1) dataEquationStateID.read(null, size);
				ArrayInt.D1 dataArrayParameterID = (ArrayInt.D1) dataParameterID.read(null, size);


				for (int i = 0; i < size[0]; i++) {

					eta[i] = dataArrayEta.get(i);
					etaDual[i] = dataArrayEtaDual.get(i);
					z[i] = dataArrayZ.get(i);
					zDual[i] = dataArrayZDual.get(i);
					psiIC[i] = dataArrayPsiIC.get(i);
					temperature[i] = dataArrayTemperatureIC.get(i);
					controlVolume[i] = dataArrayControlVolume.get(i);
					equationStateID[i] = dataArrayEquationStateID.get(i);
					parameterID[i] = dataArrayParameterID.get(i);
					
				}
				
				size = dataSpaceDelta.getShape();
				
				
				spaceDelta = new double[size[0]];
				
				
				ArrayDouble.D1 dataArraySpaceDelta = (ArrayDouble.D1) dataSpaceDelta.read(null, size);

				
				for (int i = 0; i < size[0]; i++) {
					
					spaceDelta[i] = dataArraySpaceDelta.get(i);

				}
				//////////////////////////
				//////////////////////////
				//////////////////////////

				size = dataThetaS.getShape();

				thetaS = new double[size[0]];
				thetaR = new double[size[0]];
				Ks = new double[size[0]];
				alphaSS = new double[size[0]];
				betaSS = new double[size[0]];
				par1SWRC = new double[size[0]];
				par2SWRC = new double[size[0]];
				par3SWRC = new double[size[0]];
				par4SWRC = new double[size[0]];
				par5SWRC = new double[size[0]];


				ArrayDouble.D1 dataArrayThetaS = (ArrayDouble.D1) dataThetaS.read(null, size);
				ArrayDouble.D1 dataArrayThetaR = (ArrayDouble.D1) dataThetaR.read(null, size);
				ArrayDouble.D1 dataArrayKs= (ArrayDouble.D1) dataKs.read(null, size);
				ArrayDouble.D1 dataArrayAlphaSS = (ArrayDouble.D1) dataAlphaSS.read(null, size);
				ArrayDouble.D1 dataArrayBetaSS = (ArrayDouble.D1) dataBetaSS.read(null, size);
				ArrayDouble.D1 dataArrayPar1SWRC = (ArrayDouble.D1) dataPar1SWRC.read(null, size);
				ArrayDouble.D1 dataArrayPar2SWRC = (ArrayDouble.D1) dataPar2SWRC.read(null, size);
				ArrayDouble.D1 dataArrayPar3SWRC = (ArrayDouble.D1) dataPar3SWRC.read(null, size);
				ArrayDouble.D1 dataArrayPar4SWRC = (ArrayDouble.D1) dataPar4SWRC.read(null, size);
				ArrayDouble.D1 dataArrayPar5SWRC = (ArrayDouble.D1) dataPar5SWRC.read(null, size);
				
				
				for (int i = 0; i < size[0]; i++) {

					thetaS[i] = dataArrayThetaS.get(i);
					thetaR[i] = dataArrayThetaR.get(i);
					Ks[i] = dataArrayKs.get(i);
					alphaSS[i] = dataArrayAlphaSS.get(i);
					betaSS[i] = dataArrayBetaSS.get(i);
					par1SWRC[i] = dataArrayPar1SWRC.get(i);
					par2SWRC[i] = dataArrayPar2SWRC.get(i);
					par3SWRC[i] = dataArrayPar3SWRC.get(i);
					par4SWRC[i] = dataArrayPar4SWRC.get(i);
					par5SWRC[i] = dataArrayPar5SWRC.get(i);


				}

				size = dataControlVolumeIndex.getShape();

				controlVolumeIndex = new int[size[0]];
				
				ArrayInt.D1 dataArrayControlVolumeIndex = (ArrayInt.D1) dataControlVolumeIndex.read(null, size);

				
				for (int i = 0; i < size[0]; i++) {

					controlVolumeIndex[i] = dataArrayControlVolumeIndex.get(i);
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

			System.out.println("\n\n\t*** SUCCESS reading " + richardsGridFilename);

		}
		step++;

	}
}