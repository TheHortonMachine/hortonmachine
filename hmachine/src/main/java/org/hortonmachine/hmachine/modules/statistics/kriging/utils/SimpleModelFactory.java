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
package org.hortonmachine.hmachine.modules.statistics.kriging.utils;

// TODO: Auto-generated Javadoc
/**
 * A simple design factory for creating Model objects.
 */
public class SimpleModelFactory {


	/**
	 * Creates a new SimpleModel object.
	 *
	 * @param distanceVector the distance vector
	 * @param inNumCloserStations the in number of closer stations
	 * @param maxdist the max distance
	 * @return the model
	 */
	public static Model createModel(double [] distanceVector, int inNumCloserStations, double maxdist){
		Model model=null;
		

		if (maxdist>0){
			model=new MaxDistance(distanceVector,maxdist);
			

		}else if (inNumCloserStations>0){
			model=new NumberOfStations(inNumCloserStations);			
		}

		return model;

	}
}


