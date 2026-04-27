
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

@Description("This class writes a NetCDF with Richards solute advection-dispersion equation outputs. Before writing, outputs are stored in a buffer writer"
		+ " and as simulation is ended they are written in a NetCDF file as float type.")
@Documentation("")
@Author(name = "Concetta D'Amato, Niccolo' Tubini, Riccardo Rigon", contact = "concettadamato94@gmail.com")
@Keywords("Hydrology, solute transport equation, advection-dispersion")
//@Label(JGTConstants.HYDROGEOMORPHOLOGY)
//@Name("shortradbal")
//@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")


public class WriteNetCDFRichardsSoluteADE1DFloat {

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
	
	@Description("Initial condition for concentration profile.")
	@In
	@Unit ()
	public double[] concentrationIC;
	
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
	public String topSoluteBC = " ";
	@In
	public String bottomSoluteBC = " ";
	
	public String topRichardsBC = " ";
	@In
	public String bottomRichardsBC = " ";
	@In
	public String pathSoluteTopBC = " ";
	@In
	public String pathSoluteBottomBC = " ";
	@In
	public String pathRichardsTopBC = " ";
	@In
	public String pathRichardsBottomBC = " ";
	@In
	public String pathGrid = " ";
	@In
	public String timeDelta = " ";
	@In
	public String swrcModel = " ";
	@In
	public String soilHydraulicConductivityModel = " ";
	@In
	public String interfaceHydraulicConductivityModel = " ";
	//@In
	//public String soilThermalConductivityModel = " ";
	@In
	public String interfaceDispersionCoefficientModel = " ";


	@Description("Boolean variable to print output file only at the end of the simulation")
	@In
	@Unit ()
	public boolean doProcess;
	
	@Description("Maximum allowed file size")
	@In
	@Unit ()
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
	Variable psiICVar;
	Variable thetaVar;
	Variable waterVolumeVar;
	Variable darcyVelocitiesVar;
	//Variable ETsVar;
	
	Variable concentrationICVar;
	Variable concentrationsVar;
	Variable waterVolumeConcentrationsVar;
	Variable averageSoluteConcentrationVar;
	Variable averageWaterVolumeSoluteConcentrationVar;
	
	//Variable soluteFluxesVar;
	Variable dispersionSoluteFluxesVar;
	Variable advectionSoluteFluxesVar;
	Variable errorWaterVolumeConcentrationVar;
	Variable errorVolumeVar;
	Variable controlVolumeVar; 

	ArrayDouble.D1 dataPsiIC;
	//ArrayDouble.D1 dataRootIC;
	ArrayDouble.D1 dataConcentrationIC;
	ArrayDouble.D1 dataErrorWaterVolumeConcentration;
	ArrayDouble.D1 dataErrorVolume;
	ArrayDouble.D1 dataControlVolume;
	ArrayDouble.D1 dataAverageSoluteConcentration;
	ArrayDouble.D1 dataAverageWaterVolumeSoluteConcentration;
	
	ArrayDouble.D2 dataPsi;
	ArrayDouble.D2 dataTheta;
	ArrayDouble.D2 dataWaterVolume;
	ArrayDouble.D2 dataDarcyVelocities;
	//ArrayDouble.D2 dataETs;
	ArrayDouble.D2 dataConcentrations;
	ArrayDouble.D2 dataWaterVolumeConcentrations;
	//ArrayDouble.D2 dataSoluteFluxes;
	ArrayDouble.D2 dataDispersionSoluteFluxes;
	ArrayDouble.D2 dataAdvectionSoluteFluxes;
	
	
	


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
				dataFile.addGroupAttribute(null, new Attribute("Top boundary condition for solute equation",topSoluteBC));
				dataFile.addGroupAttribute(null, new Attribute("Bottom boundary condition for solute equation",bottomSoluteBC));
				dataFile.addGroupAttribute(null, new Attribute("Top boundary condition for Richards equation",topRichardsBC));
				dataFile.addGroupAttribute(null, new Attribute("Bottom boundary condition for Richards equation",bottomRichardsBC));
				dataFile.addGroupAttribute(null, new Attribute("path top boundary condition for solute equation",pathSoluteTopBC));
				dataFile.addGroupAttribute(null, new Attribute("path bottom boundary condition for heat equation",pathSoluteBottomBC));
				dataFile.addGroupAttribute(null, new Attribute("path top boundary condition for Richards equation",pathRichardsTopBC));
				dataFile.addGroupAttribute(null, new Attribute("path bottom boundary condition for Richards equation",pathRichardsBottomBC));
				dataFile.addGroupAttribute(null, new Attribute("path grid",pathGrid));			
				dataFile.addGroupAttribute(null, new Attribute("time delta",timeDelta));
				dataFile.addGroupAttribute(null, new Attribute("swrc model",swrcModel));
				dataFile.addGroupAttribute(null, new Attribute("soil hydraulic conductivity model",soilHydraulicConductivityModel));
				dataFile.addGroupAttribute(null, new Attribute("interface hydraulic conductivity model",interfaceHydraulicConductivityModel));
				//dataFile.addGroupAttribute(null, new Attribute("soil thermal conductivity model",soilThermalConductivityModel));
				dataFile.addGroupAttribute(null, new Attribute("interface dispersion coefficient model",interfaceDispersionCoefficientModel));

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

				psiICVar = dataFile.addVariable(null, "psiIC", DataType.FLOAT, "depth");
				dataFile.addVariableAttribute(psiICVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(psiICVar, new Attribute("long_name", "Initial condition for water suction."));
				
				psiVar = dataFile.addVariable(null, "psi", DataType.FLOAT, dims);
				dataFile.addVariableAttribute(psiVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(psiVar, new Attribute("long_name", "Water suction."));
			
				thetaVar = dataFile.addVariable(null, "theta", DataType.FLOAT, dims);
				dataFile.addVariableAttribute(thetaVar, new Attribute("units", " "));
				dataFile.addVariableAttribute(thetaVar, new Attribute("long_name", "theta for within soil and water depth."));
				
				waterVolumeVar  = dataFile.addVariable(null, "waterVolume", DataType.FLOAT, dims);
				dataFile.addVariableAttribute(waterVolumeVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(waterVolumeVar, new Attribute("long_name", "Water volume in each control volume"));

				darcyVelocitiesVar = dataFile.addVariable(null, "darcyVelocity", DataType.FLOAT, dualDims);
				dataFile.addVariableAttribute(darcyVelocitiesVar, new Attribute("units", "m s-1"));
				dataFile.addVariableAttribute(darcyVelocitiesVar, new Attribute("long_name", "Darcy flux."));
				
				//ETsVar  = dataFile.addVariable(null, "ets", DataType.FLOAT, dims);
				//dataFile.addVariableAttribute(ETsVar, new Attribute("units", "m"));
				//dataFile.addVariableAttribute(ETsVar, new Attribute("long_name", "Transpired stressed water."));
				
				concentrationICVar  = dataFile.addVariable(null, "concentrationIC", DataType.FLOAT, "depth");
				dataFile.addVariableAttribute(concentrationICVar, new Attribute("units", "ML-3"));
				dataFile.addVariableAttribute(concentrationICVar, new Attribute("long_name", "Initial condition for solute concentration."));

				concentrationsVar  = dataFile.addVariable(null, "concentrations", DataType.FLOAT, dims);
				dataFile.addVariableAttribute(concentrationsVar, new Attribute("units", "ML-3"));
				dataFile.addVariableAttribute(concentrationsVar, new Attribute("long_name", "Solute concentration in each control volume."));
				
				waterVolumeConcentrationsVar  = dataFile.addVariable(null, "waterVolumeConcentrations", DataType.FLOAT, dims);
				dataFile.addVariableAttribute(waterVolumeConcentrationsVar, new Attribute("units", "ML-2"));
				dataFile.addVariableAttribute(waterVolumeConcentrationsVar, new Attribute("long_name", "waterVolumeSolute concentration in each control volume."));
				
				averageSoluteConcentrationVar = dataFile.addVariable(null, "averageSoluteConcentration", DataType.FLOAT, "time");
				dataFile.addVariableAttribute(averageSoluteConcentrationVar, new Attribute("units", "ML-3"));
				dataFile.addVariableAttribute(averageSoluteConcentrationVar, new Attribute("long_name", "Average solute concentration."));
				
				averageWaterVolumeSoluteConcentrationVar = dataFile.addVariable(null, "averageWaterVolumeSoluteConcentration", DataType.FLOAT, "time");
				dataFile.addVariableAttribute(averageWaterVolumeSoluteConcentrationVar, new Attribute("units", "ML-2"));
				dataFile.addVariableAttribute(averageWaterVolumeSoluteConcentrationVar, new Attribute("long_name", "Average water volume solute concentration."));
				
				//soluteFluxesVar = dataFile.addVariable(null, "soluteFluxes", DataType.FLOAT, dualDims);
				//dataFile.addVariableAttribute(soluteFluxesVar, new Attribute("units", "")); //?????
				//dataFile.addVariableAttribute(soluteFluxesVar, new Attribute("long_name", "Solute Flux."));
				
				dispersionSoluteFluxesVar = dataFile.addVariable(null, "dispersionSoluteFluxes", DataType.FLOAT, dualDims);
				dataFile.addVariableAttribute(dispersionSoluteFluxesVar, new Attribute("units", ""));  //?????
				dataFile.addVariableAttribute(dispersionSoluteFluxesVar, new Attribute("long_name", "Dispersion Flux."));
				
				advectionSoluteFluxesVar = dataFile.addVariable(null, "advectionSoluteFluxes", DataType.FLOAT, dualDims);
				dataFile.addVariableAttribute(advectionSoluteFluxesVar, new Attribute("units", ""));  //?????
				dataFile.addVariableAttribute(advectionSoluteFluxesVar, new Attribute("long_name", "Advection Flux."));
				
				errorWaterVolumeConcentrationVar = dataFile.addVariable(null, "errorWaterVolumeConcentration", DataType.FLOAT, "time");
				dataFile.addVariableAttribute(errorWaterVolumeConcentrationVar, new Attribute("units", "")); //?????
				dataFile.addVariableAttribute(errorWaterVolumeConcentrationVar, new Attribute("long_name", "Water volume concentration error at each time step."));
				
				errorVolumeVar = dataFile.addVariable(null, "errorVolume", DataType.FLOAT, "time");
				dataFile.addVariableAttribute(errorVolumeVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(errorVolumeVar, new Attribute("long_name", "Volume error at each time step."));
				
				controlVolumeVar = dataFile.addVariable(null, "controlVolume", DataType.FLOAT, "depth");
				dataFile.addVariableAttribute(controlVolumeVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(controlVolumeVar, new Attribute("long_name", "dimension of each control volumes"));


				depth = new ArrayDouble.D1(kDim.getLength());
				dualDepth = new ArrayDouble.D1(dualKDim.getLength());
				dataControlVolume = new ArrayDouble.D1(kDim.getLength());
				dataPsiIC = new ArrayDouble.D1(kDim.getLength());
				dataConcentrationIC = new ArrayDouble.D1(kDim.getLength());
				//dataRootIC = new ArrayDouble.D1(kDim.getLength());

				for (int k = 0; k < kDim.getLength(); k++) {
					depth.set(k, spatialCoordinate[k]);
					dataControlVolume.set(k, controlVolume[k]);
					dataPsiIC.set(k, psi[k]);
					dataConcentrationIC.set(k, concentrationIC[k]);	
				}
				

				for (int k = 0; k < dualKDim.getLength(); k++) {
					dualDepth.set(k, dualSpatialCoordinate[k]);
				}

				//Create the file. At this point the (empty) file will be written to disk
				dataFile.create();
				dataFile.write(depthVar, depth);
				dataFile.write(dualDepthVar, dualDepth);
				dataFile.write(controlVolumeVar, dataControlVolume);
				dataFile.write(psiICVar, dataPsiIC);
				dataFile.write(concentrationICVar, dataConcentrationIC);
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

				dataPsi 	= new ArrayDouble.D2(NREC, KMAX);
				dataTheta 	= new ArrayDouble.D2(NREC, KMAX);
				//dataETs 	= new ArrayDouble.D2(NREC, KMAX);
				dataWaterVolume 		= new ArrayDouble.D2(NREC, KMAX);
				dataDarcyVelocities 	= new ArrayDouble.D2(NREC, DUALKMAX);
				dataConcentrations 		= new ArrayDouble.D2(NREC, KMAX);
				dataWaterVolumeConcentrations 	= new ArrayDouble.D2(NREC, KMAX);
				//dataSoluteFluxes 				= new ArrayDouble.D2(NREC, DUALKMAX);
				dataDispersionSoluteFluxes 		= new ArrayDouble.D2(NREC, DUALKMAX);
				dataAdvectionSoluteFluxes 		= new ArrayDouble.D2(NREC, DUALKMAX);
				
				dataErrorVolume = new ArrayDouble.D1(NREC);
				dataErrorWaterVolumeConcentration = new ArrayDouble.D1(NREC);
				dataAverageSoluteConcentration = new ArrayDouble.D1(NREC);
				dataAverageWaterVolumeSoluteConcentration = new ArrayDouble.D1(NREC);
			

				
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

						dataPsi.set(i, k, tempVariable[k]);

					}

					tempVariable =  entry.getValue().get(1);
					for (int k = 0; k < KMAX; k++) {

						dataTheta.set(i, k, tempVariable[k]);

					}


					tempVariable =  entry.getValue().get(2);
					for (int k = 0; k < KMAX; k++) {

						dataWaterVolume.set(i, k, tempVariable[k]);

					}
					
					tempVariable =  entry.getValue().get(3);
					for (int k = 0; k < DUALKMAX; k++) {

						dataDarcyVelocities.set(i, k, tempVariable[k]);

					}
					
					
					/*tempVariable =  entry.getValue().get(4);
					for (int k = 0; k < KMAX; k++) {

						dataETs.set(i, k, tempVariable[k]);

					}*/
					
					tempVariable =  entry.getValue().get(4);
					for (int k = 0; k < KMAX; k++) {

						dataConcentrations.set(i, k, tempVariable[k]);

					}
					
					tempVariable =  entry.getValue().get(5);
					for (int k = 0; k < KMAX; k++) {

						dataWaterVolumeConcentrations.set(i, k, tempVariable[k]);

					}
					
					/*tempVariable =  entry.getValue().get(7);
					for (int k = 0; k < DUALKMAX; k++) {

						dataSoluteFluxes.set(i, k, tempVariable[k]);

					}*/
					
					tempVariable =  entry.getValue().get(6);
					for (int k = 0; k < DUALKMAX; k++) {

						dataDispersionSoluteFluxes.set(i, k, tempVariable[k]);

					}
					
					tempVariable =  entry.getValue().get(7);
					for (int k = 0; k < DUALKMAX; k++) {

						dataAdvectionSoluteFluxes.set(i, k, tempVariable[k]);

					}
					
					
					dataErrorWaterVolumeConcentration.set(i, entry.getValue().get(8)[0]);

					dataErrorVolume.set(i, entry.getValue().get(9)[0]);
					
					dataAverageSoluteConcentration.set(i, entry.getValue().get(10)[0]);
					
					dataAverageWaterVolumeSoluteConcentration.set(i, entry.getValue().get(11)[0]);



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
				dataFile.write(dataFile.findVariable("psi"), origin, dataPsi);
				dataFile.write(dataFile.findVariable("theta"), origin, dataTheta);
				dataFile.write(dataFile.findVariable("waterVolume"), origin, dataWaterVolume);
				dataFile.write(dataFile.findVariable("darcyVelocity"), origin, dataDarcyVelocities);		
				//dataFile.write(dataFile.findVariable("ets"), origin, dataETs);
				
				dataFile.write(dataFile.findVariable("concentrations"), origin, dataConcentrations);
				dataFile.write(dataFile.findVariable("waterVolumeConcentrations"), origin, dataWaterVolumeConcentrations);
				//dataFile.write(dataFile.findVariable("soluteFluxes"), origin, dataSoluteFluxes);
				dataFile.write(dataFile.findVariable("dispersionSoluteFluxes"), origin, dataDispersionSoluteFluxes);
				dataFile.write(dataFile.findVariable("advectionSoluteFluxes"), origin, dataAdvectionSoluteFluxes);
				

				dataFile.write(dataFile.findVariable("errorWaterVolumeConcentration"), time_origin, dataErrorWaterVolumeConcentration);
				dataFile.write(dataFile.findVariable("errorVolume"), time_origin, dataErrorVolume);
				dataFile.write(dataFile.findVariable("averageSoluteConcentration"), time_origin, dataAverageSoluteConcentration);
				dataFile.write(dataFile.findVariable("averageWaterVolumeSoluteConcentration"), time_origin, dataAverageWaterVolumeSoluteConcentration);

				origin_counter = origin_counter + NREC;
				
				
				fileSizeMB = 1*KMAX*8*origin_counter/1000000;
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

