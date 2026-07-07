/*
 * GNU GPL v3 License
 *
 * Copyright 2015 Marialaura Bancheri
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
package org.hortonmachine.hmachine.geoframe.utils.radiation.LwrbPointCase;


import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.Position2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

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



@Description("The component computes the longwave solar radiation, both upwelling and downwelling.")
@Documentation("")
@Author(name = "Marialaura Bancheri and Giuseppe Formetta", contact = "maryban@hotmail.it")
@Keywords("Hydrology, Radiation, Downwelling , upwelling")
@Label(HMConstants.HYDROGEOMORPHOLOGY)
@Name("lwrb")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class Lwrb extends HMModel {


	@Description("Air temperature input value")
	@Unit("°C")
	double airTemperature;

	@Description("Air temperature input Hashmap")
	@In
	public HashMap<Integer, double[]> inAirTemperatureValues;

	@Description("Soil temperature input value") 
	@Unit("°C")
	double soilTemperature;

	@Description("Soil temprature input Hashmap")
	@In
	public HashMap<Integer, double[]> inSoilTempratureValues;

	@Description("Humidity input value") 
	@Unit("%")
	double humidity;

	@Description("Reference humidity")
	private static final double pRH = 0.7;

	@Description("Humidity input Hashmap")
	@In
	public HashMap<Integer, double[]> inHumidityValues;

	@Description("Clearness index input value") 
	@Unit("[0,1]")
	double clearnessIndex;

	@Description("Clearness index input Hashmap")
	@In
	public HashMap<Integer, double[]> inClearnessIndexValues;

	@Description("The map of the skyview factor")
	@In
	public GridCoverage2D inSkyview;
	WritableRaster skyviewfactorWR;

	@Description("X parameter of the literature formulation")
	@In
	public double X; 

	@Description("Y parameter of the literature formulation")
	@In
	public double Y ;

	@Description("Z parameter of the literature formulation")
	@In
	public double Z;

	@Description("the linked HashMap with the coordinate of the stations")
	LinkedHashMap<Integer, Coordinate> stationCoordinates;

	@Description("Soil emissivity")
	@Unit("-")
	@In
	public double epsilonS;	

	@Description("String containing the number of the model: "
			+ " 1: Angstrom [1918];"
			+ " 2: Brunt's [1932];"
			+ " 3: Swinbank [1963];"
			+ " 4: Idso and Jackson [1969];"
			+ " 5: Brutsaert [1975];"
			+ " 6: Idso [1981];"
			+ " 7: Monteith and Unsworth [1990];"
			+ " 8: Konzelman [1994];"
			+ " 9: Prata [1996];"
			+ " 10: Dilley and O'Brien [1998];"
			+ " 11: To be implemented")
	@In
	public String model;

	@Description("Coefficient to take into account the cloud cover,"
			+ "set equal to 0 for clear sky conditions ")
	@In
	public double A_Cloud;

	@Description("Exponent  to take into account the cloud cover,"
			+ "set equal to 1 for clear sky conditions")
	@In
	public double B_Cloud;

	@Description("It is needed as index of the time step")
	int step;

	@Description("The shape file with the station measuremnts")
	@In
	public SimpleFeatureCollection inStations;

	@Description("The name of the field containing the ID of the station in the shape file")
	@In
	public String fStationsid;
	
	@Description("List of the indeces of the columns of the station in the map")
	ArrayList <Integer> columnStation= new ArrayList <Integer>();

	@Description("List of the indeces of the rows of the station in the map")
	ArrayList <Integer> rowStation= new ArrayList <Integer>();
	
	@Description(" The vetor containing the id of the station")
	Object []idStations;

	@Description("Stefan-Boltzaman costant")
	private static final double ConstBoltz = 5.670373 * Math.pow(10, -8);

	@Description("The output downwelling Hashmap")
	@Out
	public HashMap<Integer, double[]> outHMlongwaveDownwelling= new HashMap<Integer, double[]>();;

	@Description("The output upwelling Hashmap")
	@Out
	public HashMap<Integer, double[]> outHMlongwaveUpwelling= new HashMap<Integer, double[]>();;


	Model modelCS;



	/**
	 * Process.
	 *
	 * @throws Exception the exception
	 */
	@Execute
	public void process() throws Exception {

		if(step==0){
			RenderedImage inSkyviewRenderedImage = inSkyview.getRenderedImage();
			skyviewfactorWR = CoverageUtilities.replaceNovalue(inSkyviewRenderedImage, -9999.0);
			inSkyviewRenderedImage = null;

			// starting from the shp file containing the stations, get the coordinate
			//of each station
			stationCoordinates = getCoordinate(inStations, fStationsid);
		}

		// computing the reference system of the input DEM
		CoordinateReferenceSystem sourceCRS = inSkyview.getCoordinateReferenceSystem2D();
		//  from pixel coordinates (in coverage image) to geographic coordinates (in coverage CRS)
		MathTransform transf = inSkyview.getGridGeometry().getCRSToGrid2D();


		//create the set of the coordinate of the station, so we can 
		//iterate over the set			
		Iterator<Integer> idIterator = stationCoordinates.keySet().iterator();
		
		// trasform the list of idStation into an array
		idStations= stationCoordinates.keySet().toArray();


		// iterate over the list of the stations and detect their position in the map
		for (int i=0;i<idStations.length;i++){

			// compute the coordinate of the station from the linked hashMap
			Coordinate coordinate = (Coordinate) stationCoordinates.get(idIterator.next());

			// define the position, according to the CRS, of the station in the map
			org.geotools.api.geometry.Position point = new Position2D(sourceCRS, coordinate.x, coordinate.y);

			// trasform the position in two the indices of row and column 
			org.geotools.api.geometry.Position gridPoint = transf.transform(point, null);

			// add the indices to a list
			columnStation.add((int) gridPoint.getCoordinate()[0]);
			rowStation.add((int) gridPoint.getCoordinate()[1]);

			airTemperature=inAirTemperatureValues.get(idStations[i])[0];

			soilTemperature = inSoilTempratureValues.get(idStations[i])[0];

			humidity= pRH;
			if (inHumidityValues != null)
				humidity = inHumidityValues.get(idStations[i])[0];

			clearnessIndex = 1;
			if (inClearnessIndexValues != null) clearnessIndex = inClearnessIndexValues.get(idStations[i])[0];
			if (isNovalue(clearnessIndex )) clearnessIndex = 1;

			double skyviewvalue=skyviewfactorWR.getSampleDouble(columnStation.get(i), rowStation.get(i),0);

			/**Computation of the downwelling, upwelling and longwave:
			 * if there is no value in the input data, there will be no value also in
			 * the output*/
			
			double upwelling=(isNovalue(soilTemperature))? Double.NaN:computeUpwelling(soilTemperature);
			upwelling=(upwelling<0)? Double.NaN:upwelling;
			upwelling=(upwelling>2000)? Double.NaN:upwelling;
			
			
			double downwellingALLSKY=(isNovalue(airTemperature))? Double.NaN:
				computeDownwelling(model,airTemperature,humidity/100,skyviewvalue,upwelling);
			
			downwellingALLSKY=(downwellingALLSKY<0)? Double.NaN:downwellingALLSKY;
			downwellingALLSKY=(downwellingALLSKY>2000)? Double.NaN:downwellingALLSKY;
			


			/**Store results in Hashmaps*/
			storeResult((Integer)idStations[i],downwellingALLSKY,upwelling);
		}
		step++;
	}


	/**
	 * Gets the coordinate given the shp file and the field name in the shape with the coordinate of the station.
	 *
	 * @param collection is the shp file with the stations
	 * @param idField is the name of the field with the id of the stations 
	 * @return the coordinate of each station
	 * @throws Exception the exception in a linked hash map
	 */
	private LinkedHashMap<Integer, Coordinate> getCoordinate(SimpleFeatureCollection collection, String idField)
			throws Exception {
		LinkedHashMap<Integer, Coordinate> id2CoordinatesMap = new LinkedHashMap<Integer, Coordinate>();
		FeatureIterator<SimpleFeature> iterator = collection.features();
		Coordinate coordinate = null;
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				int stationNumber = ((Number) feature.getAttribute(idField)).intValue();
				coordinate = ((Geometry) feature.getDefaultGeometry()).getCentroid().getCoordinate();
				id2CoordinatesMap.put(stationNumber, coordinate);
			}
		} finally {
			iterator.close();
		}

		return id2CoordinatesMap;
	}

	/**
	 * Compute upwelling longwave radiation .
	 *
	 * @param soilTemperature: the soil temperature input
	 * @return the double value of the upwelling
	 */
	private double computeUpwelling( double soilTemperature){

		/**compute the upwelling*/
		return epsilonS * ConstBoltz * Math.pow(soilTemperature+ 273.15, 4);
	}

	/**
	 * Compute downwelling longwave radiation.
	 *
	 * @param model: the string containing the number of the model
	 * @param airTemperature:  the air temperature input
	 * @param humidity: the humidity input
	 * @param clearnessIndex: the clearness index input
	 * @return the double value of the all sky downwelling
	 */
	private double computeDownwelling(String model,double airTemperature, 
			double humidity, double skyviewvalue, double upwelling){

		/**e is the screen-level water-vapor pressure in kPa*/
		double e = humidity *6.11 * Math.pow(10, (7.5 * airTemperature) / (237.3 + airTemperature)) / 10;

		/**compute the clear sky emissivity*/
		modelCS=SimpleModelFactory.createModel(model,X,Y,Z,airTemperature+ 273.15,e);
		double epsilonCS=modelCS.epsilonCSValues();

		/**compute the downwelling in clear sky conditions*/
		double downwellingCS=epsilonCS* ConstBoltz* Math.pow(airTemperature+ 273.15, 4);
		
		/**correct downwelling clear sky for sloping terrain*/
		downwellingCS=downwellingCS*skyviewvalue+upwelling*(1-skyviewvalue);

		/**compute the cloudness index*/
		double cloudnessIndex = 1 + A_Cloud* Math.pow((1-clearnessIndex), B_Cloud);

		/**compute the downwelling in all-sky conditions*/
		return downwellingCS * cloudnessIndex;

	}


	/**
	 * Store result in given hashpmaps.
	 *
	 * @param downwellingALLSKY: the downwelling radiation in all sky conditions
	 * @param upwelling: the upwelling radiation
	 * @param longwave: the longwave radiation
	 * @throws SchemaException 
	 */
	private void storeResult(int ID,double downwellingALLSKY, double upwelling) 
			throws SchemaException {

		outHMlongwaveDownwelling.put(ID, new double[]{downwellingALLSKY});
		outHMlongwaveUpwelling.put(ID, new double[]{upwelling});
	}


}