/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.io.dxfdwg.libs.dwg.utils;

import java.lang.Math;

/**
 * This class allows to apply the extrusion transformation of Autocad given by an array
 * of doubles to a point given by an array of doubles too. The result of this
 * transformation is a Point2D 
 * 
 * @author jmorell
 */
public class AcadExtrusionCalculator {
    
    /**
     * Method that allows to apply the extrusion transformation of Autocad
     * 
     * @param coord_in Array of doubles that represents the input coordinates
     * @param xtru array of doubles that contanins the extrusion parameters
     * @return double[] Is the result of the application of the extrusion transformation
     * 		   to the input point
     */
	public static double[] CalculateAcadExtrusion(double[] coord_in, double[] xtru) {
		double[] coord_out;
		
        double dxt0 = 0D, dyt0 = 0D, dzt0 = 0D;
        double dvx1, dvx2, dvx3;
        double dvy1, dvy2, dvy3;
        double dmod, dxt, dyt, dzt;
        
        double aux = 1D/64D;
        double aux1 = Math.abs(xtru[0]);
        double aux2 = Math.abs(xtru[1]);
        
        dxt0 = coord_in[0];
        dyt0 = coord_in[1];
        dzt0 = coord_in[2];
        
        double xtruX, xtruY, xtruZ;
        xtruX = xtru[0];
        xtruY = xtru[1];
        xtruZ = xtru[2];

        if ((aux1 < aux) && (aux2 < aux)) {
            dmod = Math.sqrt(xtruZ*xtruZ + xtruX*xtruX);
            dvx1 = xtruZ / dmod;
            dvx2 = 0;
            dvx3 = -xtruX / dmod;
        } else {
            dmod = Math.sqrt(xtruY*xtruY + xtruX*xtruX);
            dvx1 = -xtruY / dmod;
            dvx2 = xtruX / dmod;
            dvx3 = 0;
        }

        dvy1 = xtruY*dvx3 - xtruZ*dvx2;
        dvy2 = xtruZ*dvx1 - xtruX*dvx3;
        dvy3 = xtruX*dvx2 - xtruY*dvx1;

        dmod = Math.sqrt(dvy1*dvy1 + dvy2*dvy2 + dvy3*dvy3);

        dvy1 = dvy1 / dmod;
        dvy2 = dvy2 / dmod;
        dvy3 = dvy3 / dmod;

        dxt = dvx1*dxt0 + dvy1*dyt0 + xtruX*dzt0;
        dyt = dvx2*dxt0 + dvy2*dyt0 + xtruY*dzt0;
        dzt = dvx3*dxt0 + dvy3*dyt0 + xtruZ*dzt0;

        coord_out = new double[]{dxt, dyt, dzt};

        dxt0 = 0;
        dyt0 = 0;
        dzt0 = 0;
        
		return coord_out;
	}
}
