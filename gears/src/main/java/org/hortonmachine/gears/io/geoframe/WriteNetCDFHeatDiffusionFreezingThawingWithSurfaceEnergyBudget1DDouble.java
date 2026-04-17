/*
 * GNU GPL v3 License
 *
 * Copyright 2018 Niccolo` Tubini
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Map.Entry;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Unit;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.ArrayDouble.D1;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

@Description("This class writes a NetCDF with heat diffusion equation considering the surface energy budget outputs. Before writing, outputs are stored in a buffer writer"
		+ " and as simulation is ended they are written in a NetCDF file.")
@Documentation("")
@Author(name = "Niccolo' Tubini, Riccardo Rigon", contact = "tubini.niccolo@gmail.com")
@Keywords("Hydrology, heat equation, diffusion")
//@Label(JGTConstants.HYDROGEOMORPHOLOGY)
//@Name("shortradbal")
//@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")


public class WriteNetCDFHeatDiffusionFreezingThawingWithSurfaceEnergyBudget1DDouble {

	@Description()
	@In
	@Unit ()
	public String timeUnits = "Minutes since 01/01/1970 00:00:00 UTC";
	
	@Description("Time zone used to convert dates in UNIX time")
	@In
	@Unit()
	public String timeZone = "UTC";

	@Description()
	@In
	@Unit ()
	public LinkedHashMap<String,ArrayList<double[]>> variables; // consider the opportunity to save varibale as float instead of double

	@Description()
	@In
	@Unit ()
	public double[] spatialCoordinate;

	@Description()
	@In
	@Unit ()
	public double[] dualSpatialCoordinate;

	@Description("Dimension of each control volume.")
	@In
	@Unit ()
	public double[] controlVolume;
	
	@Description("Water suction profile.")
	@In
	@Unit ()
	public double[] psi;
	
	@Description("Initial condition for temperature profile.")
	@In
	@Unit ()
	public double[] temperatureIC;
	
	@In
	public int writeFrequency = 1;

	@Description()
	@In
	@Unit ()
	public String fileName;

	@Description("Brief descritpion of the problem")
	@In
	@Unit ()
	public String briefDescritpion;
	@In
	public String topBC = " ";
	@In
	public String bottomBC = " ";
	@In
	public String pathTopBC = " ";
	@In
	public String pathBottomBC = " ";
	@In
	public String pathGrid = " ";
	@In
	public String timeDelta = " ";
	@In
	public String swrcModel = " ";
	@In
	public String soilThermalConductivityModel = " ";
	@In
	public String interfaceConductivityModel = " ";


	@Description("Boolean variable to print output file only at the end of the simulation")
	@In
	@Unit ()
	public boolean doProcess;
	
	@Description("Maximum allowed file size")
	@In
	@Unit ("MB")
	public double fileSizeMax = 10000;
	
	@Description("Name of the variables to save")
	@In
	@Unit ()
	public String [] outVariables = new String[]{""};

	double[] tempVariable;
	Iterator it;
	DateFormat dateFormat;
	Date date = null;
	String fileNameToSave;
	String filename;
	NetcdfFileWriter dataFile;
	int KMAX;
	int DUALKMAX;
	int NREC;
	int[] origin;
	int[] dual_origin;
	int[] time_origin;
	int origin_counter;
	int i;
	int fileNumber = 0;
	double fileSizeMB;
	Dimension kDim;
	Dimension dualKDim;
	Dimension timeDim;
	D1 depth;
	D1 dualDepth;
	Array times;
	String dims;
	List<String> outVariablesList;

	Variable timeVar;
	Variable depthVar;
	Variable dualDepthVar;
	Variable psiVar;
	Variable temperatureICVar;
	Variable temperatureVar;
	Variable thetaVar;
	Variable iceContentVar;
	Variable internalEnergyVar;
	Variable diffusionHeatFluxVar;
	Variable errorVar;
	Variable airTemperatureVar;
	Variable shortWaveOutVar;
	Variable shortWaveInVar;
	Variable longWaveOutVar;
	Variable longWaveInVar;
	Variable sensibleHeatFluxVar;
	Variable actualLatentHeatFluxVar;
	Variable bottomBCVar;
	Variable controlVolumeVar;

	ArrayDouble.D1 dataPsi;
	ArrayDouble.D1 dataTemperatureIC;
	ArrayDouble.D1 dataError;
	ArrayDouble.D1 dataAirTemperature;
	ArrayDouble.D1 dataShortWaveOut;
	ArrayDouble.D1 dataShortWaveIn;
	ArrayDouble.D1 dataLongWaveOut;
	ArrayDouble.D1 dataLongWaveIn;
	ArrayDouble.D1 dataSensibleHeatFlux;
	ArrayDouble.D1 dataActualLatentHeatFlux;
	ArrayDouble.D1 dataBottomBC;
	ArrayDouble.D1 dataControlVolume;
	
	ArrayDouble.D2 dataTemperature;
	ArrayDouble.D2 dataTheta;
	ArrayDouble.D2 dataIceContent;
	ArrayDouble.D2 dataInternalEnergy;
	ArrayDouble.D2 dataDiffusionHeatFlux;


	int step = 0;
	int stepCreation = 0;

	@Execute
	public void writeNetCDF() throws IOException {


		/*
		 * Create a new file
		 */
		if(stepCreation == 0) {
			
			origin_counter = 0;
			outVariablesList = Arrays.asList(outVariables);
			
			//			System.out.println("WriterNetCDF step:" + step);
			KMAX = spatialCoordinate.length;
			DUALKMAX = dualSpatialCoordinate.length;
			//			NREC = myVariables.keySet().size();

			dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
			date = null;

			origin = new int[]{0, 0};
			dual_origin = new int[]{0, 0};
			time_origin = new int[]{0};

			dataFile = null;

			fileNameToSave = fileName.substring(0,fileName.length()-3) + '_' + String.format("%04d", fileNumber) + fileName.substring(fileName.length()-3,fileName.length());

			try {
				// Create new netcdf-3 file with the given filename
				dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fileNameToSave);
				// add a general attribute describing the problem and containing other relevant information for the user
				dataFile.addGroupAttribute(null, new Attribute("Description of the problem",briefDescritpion));
				dataFile.addGroupAttribute(null, new Attribute("Top boundary condition",topBC));
				dataFile.addGroupAttribute(null, new Attribute("Bottom boundary condition",bottomBC));
				dataFile.addGroupAttribute(null, new Attribute("path top boundary condition",pathTopBC));
				dataFile.addGroupAttribute(null, new Attribute("path bottom boundary condition",pathBottomBC));
				dataFile.addGroupAttribute(null, new Attribute("path grid",pathGrid));			
				dataFile.addGroupAttribute(null, new Attribute("time delta",timeDelta));
				dataFile.addGroupAttribute(null, new Attribute("swrc model",swrcModel));
				dataFile.addGroupAttribute(null, new Attribute("soil thermal conductivity model",soilThermalConductivityModel));
				dataFile.addGroupAttribute(null, new Attribute("interface conductivity model",interfaceConductivityModel));

				//add dimensions  where time dimension is unlimit
				// the spatial dimension is defined using just the indexes 
				kDim = dataFile.addDimension(null, "depth", KMAX);
				dualKDim = dataFile.addDimension(null, "dualDepth", DUALKMAX);
				timeDim = dataFile.addUnlimitedDimension("time");

				// Define the coordinate variables.
				depthVar = dataFile.addVariable(null, "depth", DataType.DOUBLE, "depth");
				dualDepthVar = dataFile.addVariable(null, "dualDepth", DataType.DOUBLE, "dualDepth");
				timeVar = dataFile.addVariable(null, "time", DataType.INT, "time");

				// Define units attributes for data variables.
				// Define units attributes for data variables.
				dataFile.addVariableAttribute(timeVar, new Attribute("units", timeUnits));
				dataFile.addVariableAttribute(timeVar, new Attribute("long_name", "Time."));

				dataFile.addVariableAttribute(depthVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(depthVar, new Attribute("long_name", "Soil depth."));

				dataFile.addVariableAttribute(dualDepthVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(dualDepthVar, new Attribute("long_name", "Dual soil depth."));

				// Define the netCDF variables and their attributes.
				String dims = "time depth";
				String dualDims = "time dualDepth";

				psiVar = dataFile.addVariable(null, "psi", DataType.DOUBLE, "depth");
				dataFile.addVariableAttribute(psiVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(psiVar, new Attribute("long_name", "Water suction."));
				
				temperatureICVar = dataFile.addVariable(null, "temperatureIC", DataType.DOUBLE, "depth");
				dataFile.addVariableAttribute(temperatureICVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(temperatureICVar, new Attribute("long_name", "Initial condition for temperature."));
				
				temperatureVar = dataFile.addVariable(null, "T", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(temperatureVar, new Attribute("units", "K"));
				dataFile.addVariableAttribute(temperatureVar, new Attribute("long_name", "Temperature."));
				
				thetaVar = dataFile.addVariable(null, "waterContent", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(thetaVar, new Attribute("units", " "));
				dataFile.addVariableAttribute(thetaVar, new Attribute("long_name", "Adimensional water content."));
				
				iceContentVar = dataFile.addVariable(null, "iceContent", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(iceContentVar, new Attribute("units", " "));
				dataFile.addVariableAttribute(iceContentVar, new Attribute("long_name", "Adimensional ice content."));
				
				internalEnergyVar = dataFile.addVariable(null, "internalEnergy", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(internalEnergyVar, new Attribute("units", "J"));
				dataFile.addVariableAttribute(internalEnergyVar, new Attribute("long_name", "Internal energy"));
				
				diffusionHeatFluxVar = dataFile.addVariable(null, "diffusionHeatFlux", DataType.DOUBLE, dualDims);
				dataFile.addVariableAttribute(diffusionHeatFluxVar, new Attribute("units", "W m-2"));
				dataFile.addVariableAttribute(diffusionHeatFluxVar, new Attribute("long_name", "Diffusion heat flux."));

				
				errorVar = dataFile.addVariable(null, "error", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(errorVar, new Attribute("units", "J"));
				dataFile.addVariableAttribute(errorVar, new Attribute("long_name", "Internal energy error at each time step."));
				
				airTemperatureVar  = dataFile.addVariable(null, "airT", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(airTemperatureVar, new Attribute("units", "K"));                   
				dataFile.addVariableAttribute(airTemperatureVar, new Attribute("long_name", "Air temperature")); 
				
				shortWaveOutVar  = dataFile.addVariable(null, "swOut", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(shortWaveOutVar, new Attribute("units", "W m-2"));                   
				dataFile.addVariableAttribute(shortWaveOutVar, new Attribute("long_name", "Outcoming short-wave radiation")); 
				
				shortWaveInVar  = dataFile.addVariable(null, "swIn", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(shortWaveInVar, new Attribute("units", "W m-2"));                   
				dataFile.addVariableAttribute(shortWaveInVar, new Attribute("long_name", "Incoming short-wave radiation")); 

				longWaveOutVar  = dataFile.addVariable(null, "lwOut", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(longWaveOutVar, new Attribute("units", "W m-2"));                   
				dataFile.addVariableAttribute(longWaveOutVar, new Attribute("long_name", "Outcoming long-wave radiation")); 
				
				longWaveInVar  = dataFile.addVariable(null, "lwIn", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(longWaveInVar, new Attribute("units", "W m-2"));                   
				dataFile.addVariableAttribute(longWaveInVar, new Attribute("long_name", "Incoming long-wave radiation")); 
				
				sensibleHeatFluxVar  = dataFile.addVariable(null, "H", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(sensibleHeatFluxVar, new Attribute("units", "W m-2"));                   
				dataFile.addVariableAttribute(sensibleHeatFluxVar, new Attribute("long_name", "Sensible heat flux")); 
				
				actualLatentHeatFluxVar  = dataFile.addVariable(null, "LE", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(actualLatentHeatFluxVar, new Attribute("units", "W m-2"));                   
				dataFile.addVariableAttribute(actualLatentHeatFluxVar, new Attribute("long_name", "Actual latent heat flux")); 
				
				bottomBCVar = dataFile.addVariable(null, "bottomBC", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(bottomBCVar, new Attribute("units", ""));                 //?????
				dataFile.addVariableAttribute(bottomBCVar, new Attribute("long_name", "")); //?????
								
				controlVolumeVar = dataFile.addVariable(null, "controlVolume", DataType.DOUBLE, "depth");
				dataFile.addVariableAttribute(controlVolumeVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(controlVolumeVar, new Attribute("long_name", "dimension of each control volumes"));


				depth = new ArrayDouble.D1(kDim.getLength());
				dualDepth = new ArrayDouble.D1(dualKDim.getLength());
				dataControlVolume = new ArrayDouble.D1(kDim.getLength());
				dataPsi = new ArrayDouble.D1(kDim.getLength());
				dataTemperatureIC = new ArrayDouble.D1(kDim.getLength());

				for (int k = 0; k < kDim.getLength(); k++) {
					depth.set(k, spatialCoordinate[k]);
					dataControlVolume.set(k, controlVolume[k]);
					dataPsi.set(k, psi[k]);
					dataTemperatureIC.set(k, temperatureIC[k]);	
				}
				
				for (int k = 0; k < kDim.getLength()-1; k++) {
					
				}

				for (int k = 0; k < dualKDim.getLength(); k++) {
					dualDepth.set(k, dualSpatialCoordinate[k]);
				}

				//Create the file. At this point the (empty) file will be written to disk
				dataFile.create();
				dataFile.write(depthVar, depth);
				dataFile.write(dualDepthVar, dualDepth);
				dataFile.write(controlVolumeVar, dataControlVolume);
				dataFile.write(psiVar, dataPsi);
				dataFile.write(temperatureICVar, dataTemperatureIC);
				stepCreation = 1;

				System.out.println("\n\t***Created NetCDF " + fileNameToSave +"\n\n");
			} catch (InvalidRangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (dataFile != null)
					try {
						dataFile.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
			}

		}


		/*
		 * Write data
		 */

		if( step%writeFrequency==0 || doProcess == false) {


			try {

				dataFile = NetcdfFileWriter.openExisting(fileNameToSave);

				// number of time record that will be saved
				NREC = variables.keySet().size();

				times = Array.factory(DataType.INT, new int[] {NREC});

				dataTemperature = new ArrayDouble.D2(NREC, KMAX);
				dataTheta = new ArrayDouble.D2(NREC, KMAX);
				dataIceContent = new ArrayDouble.D2(NREC, KMAX);
				dataInternalEnergy = new ArrayDouble.D2(NREC, KMAX);
				dataDiffusionHeatFlux = new ArrayDouble.D2(NREC, DUALKMAX);
				dataError = new ArrayDouble.D1(NREC);
				dataAirTemperature = new ArrayDouble.D1(NREC);
				dataShortWaveOut = new ArrayDouble.D1(NREC);
				dataShortWaveIn = new ArrayDouble.D1(NREC);
				dataLongWaveOut = new ArrayDouble.D1(NREC);
				dataLongWaveIn = new ArrayDouble.D1(NREC);
				dataSensibleHeatFlux = new ArrayDouble.D1(NREC);
				dataActualLatentHeatFlux = new ArrayDouble.D1(NREC);
				dataBottomBC = new ArrayDouble.D1(NREC);

				int i=0;
				it = variables.entrySet().iterator();
				while (it.hasNext()) {

					@SuppressWarnings("unchecked")
					Entry<String, ArrayList<double[]>> entry = (Entry<String, ArrayList<double[]>>) it.next();

					try {
						date = dateFormat.parse(entry.getKey());
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					times.setLong(i, (long) date.getTime()/(60*1000));


					tempVariable =  entry.getValue().get(0);
					for (int k = 0; k < KMAX; k++) {

						dataTemperature.set(i, k, tempVariable[k]);

					}


					tempVariable =  entry.getValue().get(1);
					for (int k = 0; k < KMAX; k++) {

						dataTheta.set(i, k, tempVariable[k]);

					}
					
					tempVariable =  entry.getValue().get(2);
					for (int k = 0; k < KMAX; k++) {

						dataIceContent.set(i, k, tempVariable[k]);

					}
					
					tempVariable =  entry.getValue().get(3);
					for (int k = 0; k < KMAX; k++) {

						dataInternalEnergy.set(i, k, tempVariable[k]);

					}
					
					
					tempVariable =  entry.getValue().get(4);
					for (int k = 0; k < DUALKMAX; k++) {

						dataDiffusionHeatFlux.set(i, k, tempVariable[k]);

					}
					
					
					dataError.set(i, entry.getValue().get(5)[0]);

					dataAirTemperature.set(i, entry.getValue().get(6)[0]);

					dataShortWaveOut.set(i, entry.getValue().get(7)[0]);
					
					dataShortWaveIn.set(i, entry.getValue().get(8)[0]);
					
					dataLongWaveOut.set(i, entry.getValue().get(9)[0]);
					
					dataLongWaveIn.set(i, entry.getValue().get(10)[0]);
					
					dataSensibleHeatFlux.set(i, entry.getValue().get(11)[0]);
					
					dataActualLatentHeatFlux.set(i, entry.getValue().get(12)[0]);

					dataBottomBC.set(i, entry.getValue().get(13)[0]);


					i++;
				}				

				// A newly created Java integer array to be initialized to zeros.
//				origin[0] = dataFile.findVariable("psi").getShape()[0];
//				dual_origin[0] = dataFile.findVariable("darcy_velocity").getShape()[0];
//				time_origin[0] = dataFile.findVariable("time").getShape()[0];

				origin[0] = origin_counter;
				time_origin[0] = origin_counter;
				
				//				dataFile.write(kIndexVar, kIndex);
				dataFile.write(dataFile.findVariable("time"), time_origin, times);
				dataFile.write(dataFile.findVariable("T"), origin, dataTemperature);
				dataFile.write(dataFile.findVariable("waterContent"), origin, dataTheta);
				dataFile.write(dataFile.findVariable("iceContent"), origin, dataIceContent);
				dataFile.write(dataFile.findVariable("internalEnergy"), origin, dataInternalEnergy);
				dataFile.write(dataFile.findVariable("diffusionHeatFlux"), origin, dataDiffusionHeatFlux);

				dataFile.write(dataFile.findVariable("error"), time_origin, dataError);
				dataFile.write(dataFile.findVariable("airT"), time_origin, dataAirTemperature);
				dataFile.write(dataFile.findVariable("swOut"), time_origin, dataShortWaveOut);	
				dataFile.write(dataFile.findVariable("swIn"), time_origin, dataShortWaveIn);
				dataFile.write(dataFile.findVariable("lwOut"), time_origin, dataLongWaveOut);
				dataFile.write(dataFile.findVariable("lwIn"), time_origin, dataLongWaveIn);
				dataFile.write(dataFile.findVariable("H"), time_origin, dataSensibleHeatFlux);
				dataFile.write(dataFile.findVariable("LE"), time_origin, dataActualLatentHeatFlux);
				dataFile.write(dataFile.findVariable("bottomBC"), time_origin, dataBottomBC);

				origin_counter = origin_counter + NREC;
				
				
				fileSizeMB = (4*KMAX + DUALKMAX + 9)*8*origin_counter/1000000;
//				System.out.println("\t\tfileSizeMB: " + fileSizeMB);
				stepCreation ++;
				if(fileSizeMB>fileSizeMax) {
					stepCreation = 0;
					fileNumber++;
					NREC = 0;
				}
					
				if(!variables.isEmpty()) {
					System.out.println("\t\t*** " + variables.keySet().toArray()[i-1].toString() +", writing output file: " + fileNameToSave + "\n");
				}


			} catch (IOException e) {
				e.printStackTrace(System.err);

			} catch (InvalidRangeException e) {
				e.printStackTrace(System.err);

			} finally {
				if (dataFile != null)
					try {
						dataFile.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
			}

		}

		step++;
	}


}


