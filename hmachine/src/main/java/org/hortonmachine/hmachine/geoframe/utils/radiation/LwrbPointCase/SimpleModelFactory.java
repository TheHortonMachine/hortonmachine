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

// TODO: Auto-generated Javadoc
/**
 * A simple design factory for creating Model objects.
 */
public class SimpleModelFactory {

	/**
	 * Creates a new Model object.
	 *
	 * @param type: the string containing the number of the model
	 * @param X: the x parameter of the formulation
	 * @param Y: the y parameter of the formulation
	 * @param Z: the z parameter of the formulation
	 * @param airTemperature: the input air temperature
	 * @param e: the input e screen-level water-vapor pressure
	 * @return the model chosen
	 */
	public static Model createModel(String type,double X, double Y, double Z, 
			double airTemperature, double e){
		Model model=null;
		
		    /**Angstrom [1918]*/
		if (type.equals("1")){
			model=new Model1Angstrom(X,Y,Z,airTemperature,e);
			
			/**Brunt's [1932]*/
		}else if (type.equals("2")){
			model=new Model2Brunts(X,Y,airTemperature,e);
			
			/**Swinbank [1963]*/
		}else if (type.equals("3")){
			model=new Model3Swinbank(X,airTemperature);			
			
			/**Idso and Jackson [1969]*/
		}else if (type.equals("4")){
			model=new Model4IdsoJackson(X,Y,airTemperature);
			
			/**Brutsaert [1975]*/
		}else if (type.equals("5")){
			model=new Model5Brutsaert(X,Y,airTemperature,e);
			
			/**Idso [1981]*/
		}else if (type.equals("6")){
			model=new Model6Idso(X,Y,airTemperature,e);
			
			/**Monteith and Unsworth [1990]*/
		}else if (type.equals("7")){
			model=new Model7MontheithUnsworth(X,Y,airTemperature);
			
			/**Konzelman [1994]*/
		}else if (type.equals("8")){
			model=new Model8Konzelman(X,Y,airTemperature,e);
			
			/**Prata [1996]*/
		}else if (type.equals("9")){
			model=new Model9Prata(X,Y,Z,airTemperature,e);
			
			/**Dilley and O'Brien [1998]*/
		}else if (type.equals("10")){
			model=new Model10DilleyObrien(X,Y,Z, airTemperature,e);
			
			/**to be implemented*/
		}else if (type.equals("11")){
			model=new ModelNotImplemented();		
		}

		return model;

	}

}
