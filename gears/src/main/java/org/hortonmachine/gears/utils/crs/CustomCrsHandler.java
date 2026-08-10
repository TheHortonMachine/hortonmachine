/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) G-ANT - www.g-ant.eu 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.utils.crs;

import java.util.HashMap;
import java.util.Map;

public class CustomCrsHandler {
	// EXAMPLE OF HOW TO ADD A CUSTOM CRS
	
//		static String wkt6933 = """
//				PROJCS["WGS 84 / NSIDC EASE-Grid 2.0 Global",
//				    GEOGCS["WGS 84",
//				        DATUM["WGS_1984",
//				            SPHEROID["WGS 84",6378137,298.257223563,
//				                AUTHORITY["EPSG","7030"]],
//				            AUTHORITY["EPSG","6326"]],
//				        PRIMEM["Greenwich",0,
//				            AUTHORITY["EPSG","8901"]],
//				        UNIT["degree",0.0174532925199433,
//				            AUTHORITY["EPSG","9122"]],
//				        AUTHORITY["EPSG","4326"]],
//				    PROJECTION["Cylindrical_Equal_Area"],
//				    PARAMETER["standard_parallel_1",30],
//				    PARAMETER["central_meridian",0],
//				    PARAMETER["false_easting",0],
//				    PARAMETER["false_northing",0],
//				    UNIT["metre",1,
//				        AUTHORITY["EPSG","9001"]],
//				    AXIS["Easting",EAST],
//				    AXIS["Northing",NORTH],
//				    AUTHORITY["EPSG","6933"]]
//				""";

	static final Map<String, String> addedCrsWkts = new HashMap<>() {
//		{
//			 put("6933", wkt6933);
//		}
	};
}
