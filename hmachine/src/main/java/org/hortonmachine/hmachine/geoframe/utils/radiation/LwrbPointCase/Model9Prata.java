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


public class Model9Prata implements Model{


	/** The x parameter of the model  */
	double X;

	/** The y parameter of the model */
	double Y;

	/** The z parameter of the model */
	double Z;

	/** The input air temperature */
	double airTemperature;

	/** The input e */
	double e;


	public Model9Prata(double X, double Y, double Z, double airTemperature, double e){

		this.X=X;
		this.Y=Y;
		this.Z=Z;
		this.airTemperature=airTemperature;
		this.e=e;

	}

	/**
	 * Prata [1996].
	 *
	 * @return the double value of the clear sky emissivity
	 */
	public double epsilonCSValues() {
		double w= 4650 * e / airTemperature;	
		return (1.0 - (X + w) * Math.exp((-Math.pow((Y + Z * w), 0.5))));

	}

}
