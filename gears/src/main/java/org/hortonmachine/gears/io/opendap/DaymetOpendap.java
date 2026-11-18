/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.io.opendap;

import java.util.List;

/**
 * The OPeNDAP urls of Daymet V4R1, the daily weather on a 1 km grid of North America, Hawaii and Puerto Rico, served
 * by NASA Earthdata (an Earthdata login is needed, see {@link OpendapSession#fromNetrc(String)}).
 *
 * <p>Every file holds one variable of one year and region, with 365 days: leap years drop December 31. The grid is a
 * Lambert conformal conic projection with 1 km cells.</p>
 *
 * <p>Citation: Thornton, M.M., R. Shrestha, Y. Wei, P.E. Thornton, S-C. Kao, and B.E. Wilson. 2022. Daymet: Daily
 * Surface Weather Data on a 1-km Grid for North America, Version 4 R1. ORNL DAAC, Oak Ridge, Tennessee, USA.
 * https://doi.org/10.3334/ORNLDAAC/2129</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class DaymetOpendap {

    /** The granules of the daily collection on the Earthdata OPeNDAP server. */
    public static final String DAILY_GRANULES_URL = "https://opendap.earthdata.nasa.gov/collections/C2532426483-ORNL_CLOUD/granules/";

    /** Daily maximum temperature, degrees C. */
    public static final String TMAX = "tmax";
    /** Daily minimum temperature, degrees C. */
    public static final String TMIN = "tmin";
    /** Daily total precipitation, mm/day. */
    public static final String PRCP = "prcp";
    /** Shortwave radiation averaged over the daylight period, W/m2: the daily total is srad * dayl / 1E6 MJ/m2/day. */
    public static final String SRAD = "srad";
    /** Daily average water vapor pressure, Pa. */
    public static final String VP = "vp";
    /** Snow water equivalent, kg/m2. */
    public static final String SWE = "swe";
    /** Duration of the daylight period, s/day. */
    public static final String DAYL = "dayl";

    public static final List<String> VARIABLES = List.of(TMAX, TMIN, PRCP, SRAD, VP, SWE, DAYL);

    /** North America. */
    public static final String NORTH_AMERICA = "na";
    /** Hawaii. */
    public static final String HAWAII = "hi";
    /** Puerto Rico. */
    public static final String PUERTO_RICO = "pr";

    public static final List<String> REGIONS = List.of(NORTH_AMERICA, HAWAII, PUERTO_RICO);

    /** The first year of the data. */
    public static final int FIRST_YEAR = 1980;

    private DaymetOpendap() {
    }

    /**
     * @param variable one of {@link #VARIABLES}.
     * @param region one of {@link #REGIONS}.
     * @param year the year, from {@link #FIRST_YEAR}.
     * @return the OPeNDAP url of the daily file, for {@link OpendapGridReader}.
     */
    public static String dailyUrl( String variable, String region, int year ) {
        if (!VARIABLES.contains(variable)) {
            throw new IllegalArgumentException("Unknown Daymet variable " + variable + ", use one of " + VARIABLES);
        }
        if (!REGIONS.contains(region)) {
            throw new IllegalArgumentException("Unknown Daymet region " + region + ", use one of " + REGIONS);
        }
        if (year < FIRST_YEAR) {
            throw new IllegalArgumentException("Daymet starts in " + FIRST_YEAR + ".");
        }
        return DAILY_GRANULES_URL + "Daymet_Daily_V4R1.daymet_v4_daily_" + region + "_" + variable + "_" + year + ".nc";
    }
}
