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


public class Model8Konzelman implements Model{

	/** The x parameter of the model  */
	double X;

	/** The y parameter of the model */
	double Y;

	/** The input air temperature */
	double airTemperature;

	/** The input e */
	double e;


	public Model8Konzelman(double X, double Y, double airTemperature, double e){

		this.X=X;
		this.Y=Y;
		this.airTemperature=airTemperature;
		this.e=e;

	}

	/**
	 * Konzelman  [1994].
	 *
	 * @return the double value of the clear sky emissivity
	 */
	public double epsilonCSValues() {
		
		/** e must be converted in Pascal from kPa*/
		return  X + Y * Math.pow((e *1000) / airTemperature, (1.0 / 8.0));
	}

}
