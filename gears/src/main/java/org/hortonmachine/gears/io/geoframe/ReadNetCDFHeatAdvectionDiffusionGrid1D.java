
/*
// * GNU GPL v3 License
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
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D1;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

@Description("This class reads a NetCDF containing 1D grid data.")
@Documentation("")
@Author(name = "Niccolo' Tubini", contact = "tubini.niccolo@gmail.com")
@Keywords("Soil heat conduction, phase change, frozen soil, GEOframe, Permafrostnet")
@Label("GEOframe.NETCDF")
@Name("readFreezingThawing1Dgrid")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class ReadNetCDFHeatAdvectionDiffusionGrid1D {

	@Description("File name of NetCDF containing grid data")
	@In
	public String gridFilename;

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

	@Description("Water suction profile")
	@Out
	@Unit("m")
	public double[] psi;
	
	@Description("Initial condition for temperature")
	@Out
	@Unit("K")
	public double[] temperatureIC;

	@Description("Distance between consecutive controids, is used to compute gradients")
	@Out
	@Unit("m")
	public double[] spaceDelta;

	@Description("Dimension of the control volume. It is used to integrate enthalpy function")
	@Out
	@Unit("m")
	public double[] controlVolume;

	@Description("Control volume label identifying the equation state")
	@Out
	@Unit("-")
	public int[] equationStateID;

	@Description("Control volume label identifying the set of paremeters describing the soil")
	@Out
	@Unit("-")
	public int[] parameterID;
	
	@Description("Soil particles density")
	@Out
	@Unit("kg m-3")
	public double[] soilParticlesDensity;
	
	@Description("Soil particles thermal conductivity")
	@Out
	@Unit("W m-2 K-1")
	public double[] soilParticlesThermalConductivity;
	
	@Description("Soil particles specific heat capacity")
	@Out
	@Unit("J kg-1 m-3")
	public double[] soilParticlesSpecificHeatCapacity;
	
	@Description("Adimensional water content at saturation")
	@Out
	@Unit("-")
	public double[] thetaS;
	
	@Description("Adimensional residual water content")
	@Out
	@Unit("-")
	public double[] thetaR;
	
	@Description("Melting temperature")
	@Out
	@Unit("K")
	public double[] meltingTemperature;
	
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
	
	@Description("SWRC parameter 1. This depends on the SWRC used, look at the documentation.")
	@Out
	@Unit("-")
	public double[] par1SWRC;
	
	@Description("SWRC parameter 2. This depends on the SWRC used, look at the documentation.")
	@Out
	@Unit("-")
	public double[] par2SWRC;
	
	@Description("SWRC parameter 3. This depends on the SWRC used, look at the documentation.")
	@Out
	@Unit("-")
	public double[] par3SWRC;
	
	@Description("SWRC parameter 4. This depends on the SWRC used, look at the documentation.")
	@Out
	@Unit("-")
	public double[] par4SWRC;

	@Description("Fifth SWRC parameter. This depends on the SWRC used, look at the documentation.")
	@Out
	@Unit(" ")
	public double[] par5SWRC;
	
	private int[] size;
	private int[] size1;
	private int[] sizeParameter;
//	private int[] sizeCellSize;
	private int step = 0;

	@Execute
	/**
	 * Read the computational grid.
	 **/
	public void read() throws IOException {

		if (step == 0) {

			// Open the file. The ReadOnly parameter tells netCDF we want
			// read-only access to the file.
			NetcdfFile dataFile = null;
			String filename = gridFilename;
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
				                                                           
				Variable dataSoilParticlesDensity = dataFile.findVariable("soilParticlesDensity");
				Variable dataThermalConductivitySoilParticles = dataFile.findVariable("thermalConductivitySoilParticles");
				Variable dataSpecificThermalCapacitySoilParticles = dataFile.findVariable("specificThermalCapacitySoilParticles");
				Variable dataThetaS = dataFile.findVariable("thetaS");
				Variable dataThetaR = dataFile.findVariable("thetaR");
				Variable dataMeltingTemperature = dataFile.findVariable("meltingTemperature");
				Variable dataKs = dataFile.findVariable("ks");
				Variable dataAlphaSS = dataFile.findVariable("alphaSpecificStorage");
				Variable dataBetaSS = dataFile.findVariable("betaSpecificStorage");
				Variable dataPar1SWRC = dataFile.findVariable("par1SWRC");
				Variable dataPar2SWRC = dataFile.findVariable("par2SWRC");
				Variable dataPar3SWRC = dataFile.findVariable("par3SWRC");
				Variable dataPar4SWRC = dataFile.findVariable("par4SWRC");
				Variable dataPar5SWRC = dataFile.findVariable("par5SWRC");


				size = dataEta.getShape();

				KMAX = 0;
				eta = new double[size[0]];
				etaDual = new double[size[0]];
				z = new double[size[0]];
				zDual = new double[size[0]];
				controlVolume = new double[size[0]];
				psi = new double[size[0]];
				temperatureIC = new double[size[0]];
				equationStateID = new int[size[0]];
				parameterID = new int[size[0]];
				
				size1 = dataSpaceDelta.getShape();
				spaceDelta = new double[size1[0]];
					
				
				sizeParameter = dataPar1SWRC.getShape();
				soilParticlesDensity = new double[sizeParameter[0]];
				soilParticlesThermalConductivity = new double[sizeParameter[0]];
				soilParticlesSpecificHeatCapacity = new double[sizeParameter[0]];
				thetaS  = new double[sizeParameter[0]];
				thetaR = new double[sizeParameter[0]];
				meltingTemperature = new double[sizeParameter[0]];
				par1SWRC = new double[sizeParameter[0]];
				par2SWRC = new double[sizeParameter[0]];
				par3SWRC = new double[sizeParameter[0]];
				par4SWRC = new double[sizeParameter[0]];
				par5SWRC = new double[sizeParameter[0]];
				Ks = new double[sizeParameter[0]];
				alphaSS = new double[sizeParameter[0]];
				betaSS = new double[sizeParameter[0]];
				
				ArrayDouble.D1 dataArrayEta = (ArrayDouble.D1) dataEta.read(null, size);
				ArrayDouble.D1 dataArrayEtaDual= (ArrayDouble.D1) dataEtaDual.read(null, size);
				ArrayDouble.D1 dataArrayZ = (ArrayDouble.D1) dataZ.read(null, size);
				ArrayDouble.D1 dataArrayZDual = (ArrayDouble.D1) dataZDual.read(null, size);
				ArrayDouble.D1 dataArrayPsiIC = (ArrayDouble.D1) dataPsiIC.read(null, size);
				ArrayDouble.D1 dataArrayTemperatureIC = (ArrayDouble.D1) dataTemperatureIC.read(null, size);
				ArrayDouble.D1 dataArraySpaceDelta = (ArrayDouble.D1) dataSpaceDelta.read(null, size1);
				ArrayDouble.D1 dataArrayControlVolume = (ArrayDouble.D1) dataControlVolume.read(null, size);
				ArrayInt.D1 dataArrayEquationStateID = (ArrayInt.D1) dataEquationStateID.read(null, size);
				ArrayInt.D1 dataArrayParameterID= (ArrayInt.D1) dataParameterID.read(null, size);
				ArrayDouble.D1 dataArraySoilParticlesDensity = (ArrayDouble.D1) dataSoilParticlesDensity.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayThermalConductivitySoilParticles = (ArrayDouble.D1) dataThermalConductivitySoilParticles.read(null, sizeParameter);
				ArrayDouble.D1 dataArraySpecificThermalCapacitySoilParticles = (ArrayDouble.D1) dataSpecificThermalCapacitySoilParticles.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayThetaS = (ArrayDouble.D1) dataThetaS.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayThetaR = (ArrayDouble.D1) dataThetaR.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayMeltingTemperature = (ArrayDouble.D1) dataMeltingTemperature.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayKs = (ArrayDouble.D1) dataKs.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayAlphaSS = (ArrayDouble.D1) dataAlphaSS.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayBetaSS = (ArrayDouble.D1) dataBetaSS.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayPar1SWRC = (ArrayDouble.D1) dataPar1SWRC.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayPar2SWRC = (ArrayDouble.D1) dataPar2SWRC.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayPar3SWRC = (ArrayDouble.D1) dataPar3SWRC.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayPar4SWRC = (ArrayDouble.D1) dataPar4SWRC.read(null, sizeParameter);
				ArrayDouble.D1 dataArrayPar5SWRC = (ArrayDouble.D1) dataPar5SWRC.read(null, sizeParameter);

				
				



				KMAX = dataKMAX.readScalarInt();
				for (int i = 0; i < size[0]; i++) {

					eta[i] = dataArrayEta.get(i);
					etaDual[i] = dataArrayEtaDual.get(i);
					z[i] = dataArrayZ.get(i);
					zDual[i] = dataArrayZDual.get(i);
					psi[i] = dataArrayPsiIC.get(i);
					temperatureIC[i] = dataArrayTemperatureIC.get(i);
					controlVolume[i] = dataArrayControlVolume.get(i);
					equationStateID[i] = (int) dataArrayEquationStateID.get(i);
					parameterID[i] = (int) dataArrayParameterID.get(i);

				}

				for (int i = 0; i < size1[0]; i++) {
					
					spaceDelta[i] = dataArraySpaceDelta.get(i);

				}

				for (int i = 0; i < sizeParameter[0]; i++) {
					
					soilParticlesDensity[i] = dataArraySoilParticlesDensity.get(i);
					soilParticlesThermalConductivity[i] = dataArrayThermalConductivitySoilParticles.get(i);
					soilParticlesSpecificHeatCapacity[i] = dataArraySpecificThermalCapacitySoilParticles.get(i);
					thetaS[i] = dataArrayThetaS.get(i);
					thetaR[i] = dataArrayThetaR.get(i);
					meltingTemperature[i] = dataArrayMeltingTemperature.get(i);
					Ks[i] = dataArrayKs.get(i);
					alphaSS[i] = dataArrayAlphaSS.get(i);
					betaSS[i] = dataArrayBetaSS.get(i);
					par1SWRC[i] = dataArrayPar1SWRC.get(i);
					par2SWRC[i] = dataArrayPar2SWRC.get(i);
					par3SWRC[i] = dataArrayPar3SWRC.get(i);
					par4SWRC[i] = dataArrayPar4SWRC.get(i);
					par5SWRC[i] = dataArrayPar5SWRC.get(i);
					
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

			System.out.println("*** SUCCESS reading file " + gridFilename);

		}
		step++;

	}
}