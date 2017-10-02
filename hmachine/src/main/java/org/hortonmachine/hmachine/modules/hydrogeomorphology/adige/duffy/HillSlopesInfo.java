/*
CUENCAS is a River Network Oriented GIS
Copyright (C) 2005  Ricardo Mantilla

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/*
 * hillSlopesInfo.java
 *
 * Created on November 11, 2001, 10:34 AM
 */

package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.duffy;

import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope;

/**
 * The purpose of this class is to be a centralized database for all the
 * information related to the system of hillslopes that compose the basin. In
 * the current implementation this class produces aggregated output for the two
 * hillslopes draining into a stream link. Future implementations will consider
 * the variability of the two hillslopes. Information such as precipitation,
 * evaporation, soil parameters can be requested to this class. Note: In order
 * to implement new hillslope models this class must be updated to provide the
 * information for the hillslopes
 * 
 * @author Ricardo Mantilla
 */
public class HillSlopesInfo {

    public double So( IHillSlope hillSlope ) {
        return 1.0; // So is max storage in the hillslope and i is the i-th link
    }

    public double Ts( IHillSlope hillSlope ) {
        return 10.0;
    }

    public double Te( IHillSlope hillSlope ) {
        return 1e20;
    }

    /* PF ADDITION - START ... */
    /* Working units are m, hr, .... */
    public double depthMnSat( IHillSlope hillSlope ) {
        double depth_m = 2.5; // meters
        return depth_m;
    }

    public double ks( IHillSlope hillSlope ) {
        // double ks_mpd = 1.023 * 10000.0;
        // double ks_mphr = ks_mpd * (1. / 24.);
        double ks_mphr = 0.01;
        return ks_mphr;
    }

    public double mstExp( IHillSlope hillSlope ) {
        return 11.0; // 11.0 this value dimensionless
    }

    public double recParam( IHillSlope hillSlope ) {
        double area_m2 = hillSlope.getHillslopeArea();
        double spec_yield = 0.01; // dimensionless
        double d3_phr = (3.0 * ks(hillSlope) * depthMnSat(hillSlope)) / (spec_yield * area_m2);
        return d3_phr; // [1/T]
    }

    public double s2Param( IHillSlope hillSlope ) {
        double area_m2 = hillSlope.getHillslopeArea();
        double porosity = 0.46; // 0.41; dimensionless
        double d4_pm3 = 0.905 * (1. / (porosity * depthMnSat(hillSlope) * area_m2));
        return d4_pm3; // [1/L^3]
    }

    public double s2max( IHillSlope hillSlope ) {
        double s2max_m3 = (1.0 / s2Param(hillSlope));
        return s2max_m3;
    }

    public double eTrate( IHillSlope hillSlope ) {
        double etrate_mpd = 0.34; // 0.0034;
        double etrate_mphr = etrate_mpd * (1. / 24.);
        return etrate_mphr;
    }
    /* PF ADDITION - ... END */

}
