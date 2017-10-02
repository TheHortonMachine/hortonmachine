/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.etp;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_UI;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_inNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_inPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_inRh_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_inShortradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_inSwe_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_inTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_inVegetation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_inWind_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_outEtp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPENMANETP_tCurrent_DESCRIPTION;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.hortonmachine.gears.io.adige.VegetationLibraryRecord;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description(OMSPENMANETP_DESCRIPTION)
@Author(name = OMSPENMANETP_AUTHORNAMES, contact = OMSPENMANETP_AUTHORCONTACTS)
@Keywords(OMSPENMANETP_KEYWORDS)
@Label(OMSPENMANETP_LABEL)
@Name(OMSPENMANETP_NAME)
@Status(OMSPENMANETP_STATUS)
@License(OMSPENMANETP_LICENSE)
@UI(OMSPENMANETP_UI)
public class OmsPenmanEtp extends HMModel {

    // @Description("Baricenter elevation of the HillSlope for every basin on which to calculate.")
    // @Unit("m")
    // @In
    // public HashMap<Integer, double[]> inElevations;

    @Description(OMSPENMANETP_inVegetation_DESCRIPTION)
    @In
    public HashMap<Integer, VegetationLibraryRecord> inVegetation;

    @Description(OMSPENMANETP_inNetradiation_DESCRIPTION)
    @Unit("W/m2")
    @In
    public HashMap<Integer, double[]> inNetradiation;

    @Description(OMSPENMANETP_inShortradiation_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inShortradiation;

    @Description(OMSPENMANETP_inTemp_DESCRIPTION)
    @Unit("C")
    @In
    public HashMap<Integer, double[]> inTemp;

    @Description(OMSPENMANETP_inRh_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inRh;

    @Description(OMSPENMANETP_inWind_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inWind;

    @Description(OMSPENMANETP_inPressure_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inPressure;

    @Description(OMSPENMANETP_inSwe_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inSwe;

    @Description(OMSPENMANETP_tCurrent_DESCRIPTION)
    @In
    public String tCurrent;

    @Description(OMSPENMANETP_outEtp_DESCRIPTION)
    @Unit("mm/day")
    @Out
    public HashMap<Integer, double[]> outEtp;

    private DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;

    private static final double Z0_SNOW = 0.010;
    private static final double CLOSURE = 4000.0;
    private static final double RSMAX = 5000.0; // Pa
    private static final double VPDMINFACTOR = 0.1;
    private static final double A_SVP = 0.61078;
    private static final double B_SVP = 17.269;
    private static final double C_SVP = 237.3;
    private static final double CP_PM = 1013.0; /* specific heat of moist air J/kg/C (Handbook of Hydrology) */
    // private static final double PS_PM = 101300.0; /* sea level air pressure in Pa */
    // private static final double LAPSE_PM = -0.006; /* environmental lapse rate in C/m */
    // private static final int SECPHOUR = 3600; /* seconds per hour */
    private static final int SEC_PER_DAY = 86400; /* seconds per day */
    private static final double HUGE_RESIST = 1.e20; /* largest allowable double number */
    private static final double VON_K = 0.41; /* Von Karman constant for evapotranspiration */
    private static final double ZREF = 2.0; // reference height for wind speed

    @Execute
    public void penman() {

        checkNull(inPressure, inTemp, inRh, inWind, inSwe, inVegetation, inShortradiation, inNetradiation);

        outEtp = new HashMap<Integer, double[]>();

        DateTime currentTimestamp = formatter.parseDateTime(tCurrent);
        int monthOfYear = currentTimestamp.getMonthOfYear();

        Set<Entry<Integer, double[]>> elevSet = inTemp.entrySet();
        for( Entry<Integer, double[]> entry : elevSet ) {
            Integer basinId = entry.getKey();
            // double elevation = inElevations.get(basinId)[0];
            double tair = entry.getValue()[0];
            double pressure = inPressure.get(basinId)[0];
            double relativeHumidity = inRh.get(basinId)[0];
            double wind = inWind.get(basinId)[0];
            double snowWaterEquivalent = inSwe.get(basinId)[0];
            double shortRadiation = inShortradiation.get(basinId)[0];
            double netRadiation = inNetradiation.get(basinId)[0];

            VegetationLibraryRecord vegetation = inVegetation.get(basinId);
            double displacement = vegetation.getDisplacement(monthOfYear);
            double roughness = vegetation.getRoughness(monthOfYear);
            double rs = vegetation.getMinStomatalResistance();
            double RGL = vegetation.getRgl();
            double lai = vegetation.getLai(monthOfYear);
            double rarc = vegetation.getArchitecturalResistance();

            /*
             * set the pressure in Pascal instead of in hPa as the input link gives 
             */
            pressure = pressure / 100;
            double vpd = svp(tair) - (relativeHumidity * 100 / svp(tair)); // vpd in KPa
            double ra = calcAerodynamic(displacement, roughness, ZREF, wind, snowWaterEquivalent);

            // CONSIDER THE SOIL RESISTANCE NULL
            // // calculate gsm_inv: soil moisture stress factor
            // double criticalSoilMoisture = 0.33; // fraction of soil moisture content at the
            // critical
            // // point
            // double wiltingPointSoilMoisture = 0.133;
            // double waterContentCriticalPoint = criticalSoilMoisture * maxMoisture;
            // double waterContentWiltingPoint = wiltingPointSoilMoisture * maxMoisture;
            // double gsm_inv; // soil moisture stress factor
            // if (soilMoisture >= waterContentCriticalPoint) {
            // gsm_inv = 1.0;
            // } else if (soilMoisture >= waterContentWiltingPoint) {
            // gsm_inv = (soilMoisture - waterContentWiltingPoint) / (waterContentCriticalPoint -
            // waterContentWiltingPoint);
            // } else {
            // gsm_inv = 0.0;
            // }
            /* calculate the slope of the saturated vapor pressure curve in Pa/K */
            double slope = svp_slope(tair) * 1000.0;

            /* factor for canopy resistance based on photosynthesis */
            double dayFactor;
            /* calculate resistance factors (Wigmosta et al., 1994) */
            double f = 0.0;
            if (rs > 0.) {
                if (RGL < 0) {
                    throw new ModelsIllegalargumentException("Invalid value of RGL for the current class.", this, pm);
                } else if (RGL == 0) {
                    f = shortRadiation;
                } else {
                    f = shortRadiation / RGL;
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
            // double rc = rs / (lai * gsm_inv * tFactor * vpdFactor) * dayFactor;
            double rc = rs / (lai * tFactor * vpdFactor) * dayFactor;
            rc = (rc > RSMAX) ? RSMAX : rc;

            // double h; /* scale height in the atmosphere (m) */
            // /* calculate scale height based on average temperature in the column */
            // h = 287 / 9.81 * ((tair + 273.15) + 0.5 * (double) elevation * LAPSE_PM);
            //
            // /* use hypsometric equation to calculate p_z, assume that virtual temperature is
            // equal
            // air_temp */
            // pz = PS_PM * Math.exp(-(double) elevation / h);

            // instead of calculating the pressure in h.adige it is possible to read the
            // interpolated
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
            double evap = ((slope * netRadiation + r_air * CP_PM * vpd / ra) / (lv * (slope + gamma * (1 + (rc + rarc) / ra))) * SEC_PER_DAY) / 24.0;

            if (vpd >= 0.0 && evap < 0.0)
                evap = 0.0;

            outEtp.put(basinId, new double[]{evap});
        }
    }

    /**
     * This routine computes the gradient of d(svp)/dT using Handbook of Hydrology eqn 4.2.3
     * @param tair
     * @return saturated vapor pressure slope
     */
    private double svp_slope( double temp ) {
        double satVaporPressureSlope = (B_SVP * C_SVP) / ((C_SVP + temp) * (C_SVP + temp)) * svp(temp);

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
    private double calcAerodynamic( double displacement, double roughness, double Zref, double windSpeed,
            double snowWaterEquivalent ) {

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

        if (displacement > Zref)
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
            windSpeed = Math.log((2. + Z0_SNOW) / Z0_SNOW) / Math.log(Zref / Z0_SNOW);
            ra = Math.log((2. + Z0_SNOW) / Z0_SNOW) * Math.log(Zref / Z0_SNOW) / K2;
        } else {
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
            pm.message("Aerodinamic resistance is set to the maximum value!");
        }

        return ra;
    }

}
