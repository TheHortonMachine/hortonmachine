package org.hortonmachine.hmachine.geoframe.utils.radiation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.geotools.feature.SchemaException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.locationtech.jts.geom.Coordinate;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.Unit;

public class NetRadiationPointCase extends HMModel {

	@Description("The Hashmap with the time series of the shortwave radiation values")
	@In
	@Unit ("W/m2")
	public HashMap<Integer, double[]> inShortwaveValues;
	



	@Description("The Hashmap with the time series of the Downwelling values")
	@In
	@Unit ("W/m2")
	public HashMap<Integer, double[]> inDownwellingValues;

	@Description("The Hashmap with the time series of the Upwelling values")
	@In
	@Unit ("W/m2")
	public HashMap<Integer, double[]> inUpwellingValues;


	@Description("albedo")
	@In
	@Unit ("-")
	public double alfa;

	@Description("the linked HashMap with the coordinate of the stations")
	LinkedHashMap<Integer, Coordinate> stationCoordinates;

	@Description("the output hashmap withe the direct radiation")
	@Out
	public HashMap<Integer, double[]> outHMnetRad= new HashMap<Integer, double[]>();

	/**
	 * Process.
	 *
	 * @throws Exception the exception
	 */
	@Execute
	public void process() throws Exception { 

		checkNull(inShortwaveValues);


		// reading the ID of all the stations 
		Set<Entry<Integer, double[]>> entrySet = inShortwaveValues.entrySet();

		for (Entry<Integer, double[]> entry : entrySet) {
			Integer ID = entry.getKey();

			double shortWave=inShortwaveValues.get(ID)[0];
			if(shortWave<0) shortWave=0;
			
			double downwelling = inDownwellingValues.get(ID)[0];
			if(downwelling<0) downwelling=0;

			double upwelling=inUpwellingValues.get(ID)[0];
			if(upwelling<0) upwelling=0;

			double netRad=(shortWave<=0)?0:(1-alfa)*(shortWave)+downwelling-upwelling;
			netRad=(netRad<0)?0:netRad;

			/**Store results in Hashmaps*/
			storeResult((Integer)ID,netRad);

		}


	}



	/**
	 * Store result in given hashpmaps.
	 *
	 * @param downwellingALLSKY: the downwelling radiation in all sky conditions
	 * @param upwelling: the upwelling radiation
	 * @param longwave: the longwave radiation
	 * @throws SchemaException 
	 */
	private void storeResult(Integer ID,double netRad) 
			throws SchemaException {

		outHMnetRad.put(ID, new double[]{netRad});

	}

}
