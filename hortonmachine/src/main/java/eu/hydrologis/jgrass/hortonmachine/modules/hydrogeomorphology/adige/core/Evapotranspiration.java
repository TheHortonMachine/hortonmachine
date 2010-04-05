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
package eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core;


/**
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public class Evapotranspiration {

    private final double Z0_SNOW = 0.010;
    private final double CLOSURE = 4000.0;
    private final double RSMAX = 5000.0; // Pa
    private final double VPDMINFACTOR = 0.1;
    private final double A_SVP = 0.61078;
    private final double B_SVP = 17.269;
    private final double C_SVP = 237.3;
    private final double CP_PM = 1013.0; /* specific heat of moist air J/kg/C (Handbook of Hydrology) */
    // private final double PS_PM = 101300.0; /* sea level air pressure in Pa */
    // private final double LAPSE_PM = -0.006; /* environmental lapse rate in C/m */
    // private final int SECPHOUR = 3600; /* seconds per hour */
    private final int SEC_PER_DAY = 86400; /* seconds per day */
    private final double HUGE_RESIST = 1.e20; /* largest allowable double number */
    private final double VON_K = 0.41; /* Von Karman constant for evapotranspiration */
    private final double ZREF = 2.0; // reference height for wind speed

    public Evapotranspiration() {
    }

    /**
     * Calculates the daily evapotranspiration using the combination equation.
     * 
     * @param elevation baricenter elevation of the {@link HillSlope} (m).
     * @param rad net radiation from energy balance (W/m2).
     * @param rs minimum stomatal resistance (s/m).
     * @param rarc architectural resistance (s/m).
     * @param lai leaf area index.
     * @param RGL minimum incoming shortwave radiation at which there will be
     *                  transpiration.
     * @param displacement the vegetation displacement height.
     * @param roughness vegetation roughness.
     * @param maxMoisture maximum moisture. (getS2max)
     * @param pressure air pressure
     * @param tair air temperature (to calculate slope of saturated vapor pressure curve) (C).
     * @param net_short shortwave net radiation.
     * @param relativeHumidity moisture.
     * @param wind wind speed.
     * @return daily evapotranspiration (mm/day).
     */
    public double penman( double elevation, double rad, double rs, double rarc, double lai,
            double RGL, double displacement, double roughness, double maxMoisture, double pressure,
            double tair, double net_short, double relativeHumidity, double wind,
            double soilMoisture, double snowWaterEquivalent ) {

        /*
         * set the pressure in Pascal instead of in hPa as the input link gives 
         */
        
        pressure = pressure / 100;
        double vpd = svp(tair) - (relativeHumidity * 100 / svp(tair)); // vpd in KPa
        double ra = CalcAerodynamic(displacement, roughness, ZREF, wind, snowWaterEquivalent);

        // calculate gsm_inv: soil moisture stress factor
        double criticalSoilMoisture = 0.33; // fraction of soil moisture content at the critical
        // point
        double wiltingPointSoilMoisture = 0.133;
        double waterContentCriticalPoint = criticalSoilMoisture * maxMoisture;
        double waterContentWiltingPoint = wiltingPointSoilMoisture * maxMoisture;
        double gsm_inv; // soil moisture stress factor
        if (soilMoisture >= waterContentCriticalPoint) {
            gsm_inv = 1.0;
        } else if (soilMoisture >= waterContentWiltingPoint) {
            gsm_inv = (soilMoisture - waterContentWiltingPoint)
                    / (waterContentCriticalPoint - waterContentWiltingPoint);
        } else {
            gsm_inv = 0.0;
        }

        /* calculate the slope of the saturated vapor pressure curve in Pa/K */
        double slope = svp_slope(tair) * 1000.0;

        /* factor for canopy resistance based on photosynthesis */
        double dayFactor;
        /* calculate resistance factors (Wigmosta et al., 1994) */
        double f = 0.0;
        if (rs > 0.) {
            if (RGL < 0) {
                System.out.println("Invalid value of RGL for the current class.");
                return -1;
            }else if (RGL == 0) {
                f = net_short;
            }else {
                f = net_short / RGL;
            }
            dayFactor = (1. + f) / (f + rs / RSMAX);
        } else
            dayFactor = 1.;

        /* factor for canopy resistance based on temperature */
        double tFactor = .08 * tair - 0.0016 * tair * tair;
        tFactor = (tFactor <= 0.0) ? 1e-10 : tFactor;

        /* factor for canopy resistance based on vpd */
        double vpdFactor = 1 - vpd / CLOSURE;
        vpdFactor = (vpdFactor < VPDMINFACTOR) ? VPDMINFACTOR : vpdFactor;

        /* calculate canopy resistance in s/m */
        double rc = rs / (lai * gsm_inv * tFactor * vpdFactor) * dayFactor;
        rc = (rc > RSMAX) ? RSMAX : rc;

        // double h; /* scale height in the atmosphere (m) */
        // /* calculate scale height based on average temperature in the column */
        // h = 287 / 9.81 * ((tair + 273.15) + 0.5 * (double) elevation * LAPSE_PM);
        //
        // /* use hypsometric equation to calculate p_z, assume that virtual temperature is equal
        // air_temp */
        // pz = PS_PM * Math.exp(-(double) elevation / h);

        // instead of calculating the pressure in h.adige it is possible to read the interpolated
        // pressure from input link
        // pz is the surface air pressure

        /* calculate latent heat of vaporization. Eq. 4.2.1 in Handbook of Hydrology, assume Ts is Tair */
        double lv = 2501000 - 2361 * tair;

        /* calculate the psychrometric constant gamma (Pa/C). Eq. 4.2.28. Handbook of Hydrology */
        double gamma = 1628.6 * pressure / lv;

        /* calculate factor to be applied to rc/ra */

        /* calculate the air density (in kg/m3), using eq. 4.2.4 Handbook of Hydrology */
        double r_air = 0.003486 * pressure / (275 + tair);

        /* calculate the Penman-Monteith evaporation in mm/day (by not dividing by 
         * the density of water (~1000 kg/m3)), the result ends up being in mm instead of m */
        double evap = ((slope * rad + r_air * CP_PM * vpd / ra)
                / (lv * (slope + gamma * (1 + (rc + rarc) / ra))) * SEC_PER_DAY) / 24.0;

        if (vpd >= 0.0 && evap < 0.0)
            evap = 0.0;

        return evap;
    }

    /**
     * This routine computes the gradient of d(svp)/dT using Handbook of Hydrology eqn 4.2.3
     * @param tair
     * @return saturated vapor pressure slope
     */
    private double svp_slope( double temp ) {
        double satVaporPressureSlope = (B_SVP * C_SVP) / ((C_SVP + temp) * (C_SVP + temp))
                * svp(temp);

        return satVaporPressureSlope;
    }

    /**
     * This routine computes the saturated vapor pressure using Handbook of Hydrology eqn 4.2.2 (Pressure in kPa)
     * 
     * @param temp temperature.
     * @return saturated vapor pressure.
     */
    private double svp( double temp ) {
        double SVP;

        SVP = A_SVP * Math.exp((B_SVP * temp) / (C_SVP + temp));

        if (temp < 0)
            SVP *= 1.0 + .00972 * temp + .000042 * temp * temp;

        return (SVP);
    }

    /**
     * Calculates the aerodynamic resistance for the vegetation layer.
     * 
     * <p>Calculates the aerodynamic resistance for the vegetation layer, and 
     * the wind 2m above the layer boundary.</p>
     * <p>The values are normalized based on a reference height wind 
     * speed, Uref, of 1 m/s. To get wind speeds and aerodynamic resistances for 
     * other values of Uref, you need to multiply the here calculated wind 
     * speeds by Uref and divide the here calculated aerodynamic resistances 
     * by Uref</p>
     *
     * @param displacement
     * @param roughness
     * @param Zref reference height for windspeed.
     * @param windSpeed
     * @return the aerodynamic resistance for the vegetation layer.
     */
    private double CalcAerodynamic( double displacement, double roughness, double Zref,
            double windSpeed, double snowWaterEquivalent ) {

        double ra = 0.0;
        double d_Lower;
        double K2;
        double Z0_Lower;
        double tmp_wind;

        // only a value of these quantities are input of the method:
        // - wind speed
        // - relative humidity
        // - Zref
        // - ra
        tmp_wind = windSpeed;

        K2 = VON_K * VON_K;
        
        if(displacement > Zref) 
            Zref = displacement + Zref + roughness;

        /* No OverStory, thus maximum one soil layer */
        // for bare soil
        // if (iveg == Nveg) {
        // Z0_Lower = Z0_SOIL; //Z0_SOIL is the soil roughness
        // d_Lower = 0;
        // } else { //with vegetation
        Z0_Lower = roughness;
        d_Lower = displacement;
        // } if cycle is deleted thinking that bare soil is a vegetation type with roughness and
        // displacement


        /* With snow on the surface */
        if (snowWaterEquivalent > 0) {
            windSpeed = Math.log((2. + Z0_SNOW)/Z0_SNOW)/Math.log(Zref/Z0_SNOW);
            ra = Math.log((2. + Z0_SNOW)/Z0_SNOW) * Math.log(Zref/Z0_SNOW)/K2;
        }else{
            /* No snow on the surface*/
            windSpeed = Math.log((2. + Z0_Lower) / Z0_Lower) / Math.log((Zref - d_Lower) / Z0_Lower);
            ra = Math.log((2. + (1.0 / 0.63 - 1.0) * d_Lower) / Z0_Lower)
            * Math.log((2. + (1.0 / 0.63 - 1.0) * d_Lower) / (0.1 * Z0_Lower)) / K2;
        }
            
        if (tmp_wind > 0.) {
            windSpeed *= tmp_wind;
            ra /= tmp_wind;
        } else {
            windSpeed *= tmp_wind;
            ra = HUGE_RESIST;
            System.out.println("Aerodinamic resistance is set to the maximum value!");
        }

        return ra;
    }

}
