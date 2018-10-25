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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.energybalance;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.tan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.io.eicalculator.EIAreas;
import org.hortonmachine.gears.io.eicalculator.EIEnergy;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;

import static org.hortonmachine.gears.libs.modules.HMConstants.C_ice;
import static org.hortonmachine.gears.libs.modules.HMConstants.C_liq;
import static org.hortonmachine.gears.libs.modules.HMConstants.Isc;
import static org.hortonmachine.gears.libs.modules.HMConstants.Lf;
import static org.hortonmachine.gears.libs.modules.HMConstants.Lv;
import static org.hortonmachine.gears.libs.modules.HMConstants.Tf;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.ka;
import static org.hortonmachine.gears.libs.modules.HMConstants.omega;
import static org.hortonmachine.gears.libs.modules.HMConstants.rho_i;
import static org.hortonmachine.gears.libs.modules.HMConstants.rho_w;
import static org.hortonmachine.gears.libs.modules.HMConstants.sigma;
import static org.hortonmachine.gears.libs.modules.HMConstants.tk;
import static org.hortonmachine.hmachine.i18n.HortonMessages.*;

@Description(OMSENERGYBALANCE_DESCRIPTION)
@Author(name = OMSENERGYBALANCE_AUTHORNAMES, contact = OMSENERGYBALANCE_AUTHORCONTACTS)
@Keywords(OMSENERGYBALANCE_KEYWORDS)
@Label(OMSENERGYBALANCE_LABEL)
@Name(OMSENERGYBALANCE_NAME)
@Status(OMSENERGYBALANCE_STATUS)
@License(OMSENERGYBALANCE_LICENSE)
public class OmsEnergyBalance extends HMModel {

    private static final int GLACIER_SWE = 2000;

    @Description(OMSENERGYBALANCE_inBasins_DESCRIPTION)
    @In
    public SimpleFeatureCollection inBasins;

    @Description(OMSENERGYBALANCE_fBasinid_DESCRIPTION)
    @In
    public String fBasinid = null;

    @Description(OMSENERGYBALANCE_fBasinlandcover_DESCRIPTION)
    @In
    public String fBasinlandcover = null;

    @Description(OMSENERGYBALANCE_pTrain_DESCRIPTION)
    @Unit("degrees")
    @In
    public double pTrain = 2.0;

    @Description(OMSENERGYBALANCE_pTsnow_DESCRIPTION)
    @Unit("degrees")
    @In
    public double pTsnow = 0.0;

    @Description(OMSENERGYBALANCE_pInternaltimestep_DESCRIPTION)
    @In
    public double pInternaltimestep = 1;

    @Description(OMSENERGYBALANCE_pRhosnow_DESCRIPTION)
    @Unit("kg/m3")
    @In
    public double pRhosnow = 400.0;

    @Description(OMSENERGYBALANCE_pInitswe_DESCRIPTION)
    @In
    public double pInitswe = -9999.0;

    @Description(OMSENERGYBALANCE_pCanopyh_DESCRIPTION)
    @In
    public double pCanopyh = 0.0;

    @Description(OMSENERGYBALANCE_pGlacierid_DESCRIPTION)
    @In
    public double pGlacierid = -9999.0;

    @Description(OMSENERGYBALANCE_pSnowrefv_DESCRIPTION)
    @In
    public double pSnowrefv = 0.85;

    @Description(OMSENERGYBALANCE_pSnowrefir_DESCRIPTION)
    @In
    public double pSnowrefir = 0.65;

    @Description(OMSENERGYBALANCE_tTimestep_DESCRIPTION)
    @In
    public int tTimestep;

    @Description(OMSENERGYBALANCE_tCurrent_DESCRIPTION)
    @In
    public String tCurrent = null;

    @Description(OMSENERGYBALANCE_inRain_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inRain;

    @Description(OMSENERGYBALANCE_inTemp_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inTemp;

    @Description(OMSENERGYBALANCE_inWind_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inWind;

    @Description(OMSENERGYBALANCE_inPressure_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inPressure;

    @Description(OMSENERGYBALANCE_inRh_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inRh;

    @Description(OMSENERGYBALANCE_inDtday_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inDtday;

    @Description(OMSENERGYBALANCE_inDtmonth_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inDtmonth;

    @Description(OMSENERGYBALANCE_inEnergy_DESCRIPTION)
    @In
    public List<EIEnergy> inEnergy = null;

    @Description(OMSENERGYBALANCE_inAreas_DESCRIPTION)
    @In
    public List<EIAreas> inAreas = null;

    @Description(OMSENERGYBALANCE_pInitsafepoint_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String pInitsafepoint;

    @Description(OMSENERGYBALANCE_pEndsafepoint_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String pEndsafepoint;

    @Description(OMSENERGYBALANCE_outPnet_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> outPnet;

    @Description(OMSENERGYBALANCE_outPrain_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> outPrain;

    @Description(OMSENERGYBALANCE_outPsnow_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> outPsnow;

    @Description(OMSENERGYBALANCE_outSwe_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> outSwe;

    @Description(OMSENERGYBALANCE_outNetradiation_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> outNetradiation;

    @Description(OMSENERGYBALANCE_outNetshortradiation_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> outNetshortradiation;

    /*
     * Model's variables definition
     */
    private double[] averageTemperature;
    // public double[] fullAdigeData;

    private double defaultTollU0 = 1000.0;
    private double defaultTollU = 500.0;
    private double defaultTollW0 = 10.0;
    private double defaultTollW = 5.0;
    private double defaultTollTs = 1.0;
    private double defaultUWiter = 10.0;
    private double defaultTsiter = 5.0;

    private int num_ES = -9999;
    private int num_EI = -9999;

    /**
     * latitude latitude in rad.
     */
    private double latitude;

    /**
     * longitude longitude in rad.
     */
    private double longitude;

    /**
     * standard_time difference from the UTM [hour].
     */
    private double standard_time;
    private double E0;
    private double alpha;
    private int basinNum = -1;

    private SafePoint safePoint;
    private ArrayList<SimpleFeature> basinsFeatures;
    private double[] Abasin;

    private HashMap<Integer, Integer> basinid2BasinindexMap;
    private HashMap<Integer, Integer> basinindex2BasinidMap;

    private double[][][] EI;
    private double[][][] A;

    /*
     * Full adige vector data contains for every basin in the following order:
     * 1. net precipitation
     * 2. net radiation
     * 3. net short radiation
     * 4. temperature
     * 5. relative humidity
     * 6. wind speed
     * 7. air pressure
     */
    // private double[] fullAdigeData;

    private int usoFieldIndex = -1;
    private List<Integer> usoList;

    private DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;

    @Execute
    public void process() throws Exception {
        outPnet = new HashMap<Integer, double[]>();
        outPrain = new HashMap<Integer, double[]>();
        outPsnow = new HashMap<Integer, double[]>();
        outSwe = new HashMap<Integer, double[]>();
        outNetradiation = new HashMap<Integer, double[]>();
        outNetshortradiation = new HashMap<Integer, double[]>();

        if (safePoint == null)
            safePoint = new SafePoint();
        // retrieve number of bands
        num_EI = 0;
        for( EIEnergy energy : inEnergy ) {
            int j = energy.energeticBandId;
            if (j > num_EI) {
                num_EI = j;
            }
        }
        num_EI++;
        num_ES = 0;
        for( EIAreas area : inAreas ) {
            int j = area.altimetricBandId;
            if (j > num_ES) {
                num_ES = j;
            }
        }
        num_ES++;

        if (basinid2BasinindexMap == null) {
            // get basin features from feature link
            basinsFeatures = new ArrayList<SimpleFeature>();
            FeatureIterator<SimpleFeature> featureIterator = inBasins.features();

            basinNum = inBasins.size();
            SimpleFeatureType featureType = inBasins.getSchema();

            int basinIdFieldIndex = featureType.indexOf(fBasinid);
            if (basinIdFieldIndex == -1) {
                throw new IllegalArgumentException("The field of the basin id couldn't be found in the supplied basin data.");
            }
            if (fBasinlandcover != null) {
                usoFieldIndex = featureType.indexOf(fBasinlandcover);
                if (usoFieldIndex == -1) {
                    throw new IllegalArgumentException(
                            "The field of the soil type (usofield) couldn't be found in the supplied basin data.");
                }
            }
            basinid2BasinindexMap = new HashMap<Integer, Integer>();
            basinindex2BasinidMap = new HashMap<Integer, Integer>();

            pm.beginTask("Read basins data.", inBasins.size());
            int index = 0;
            Abasin = new double[basinNum];
            while( featureIterator.hasNext() ) {
                pm.worked(1);
                SimpleFeature feature = featureIterator.next();
                basinsFeatures.add(feature);
                basinid2BasinindexMap.put(((Number) feature.getAttribute(basinIdFieldIndex)).intValue(), index);
                basinindex2BasinidMap.put(index, ((Number) feature.getAttribute(basinIdFieldIndex)).intValue());
                Geometry basinGeometry = (Geometry) feature.getDefaultGeometry();
                Abasin[index] = basinGeometry.getArea() / 1000000.0; // area in km2 as the input
                // area for energetic and
                // altimetric bands
                index++;

                // read land cover if requested
                if (usoFieldIndex != -1) {
                    if (usoList == null) {
                        usoList = new ArrayList<Integer>();
                    }
                    int uso = ((Number) feature.getAttribute(usoFieldIndex)).intValue();
                    usoList.add(uso);
                }

            }
            featureIterator.close();
            pm.done();
        }

        // get rain from scalar link
        double[] rain = new double[basinNum];
        Set<Integer> basinIdSet = inRain.keySet();
        pm.beginTask("Read rain data.", basinIdSet.size());
        for( Integer basinId : basinIdSet ) {
            pm.worked(1);
            Integer index = basinid2BasinindexMap.get(basinId);
            if (index == null) {
                continue;
            }
            double[] value = inRain.get(basinId);
            if (!isNovalue(value[0])) {
                rain[index] = value[0];
            } else {
                rain[index] = 0.0;
            }
        }
        pm.done();

        // get energy values from scalar link ([12][num_EI][basinNum]) 12 ==
        // 0,1,2,3,4,5,5,4,3,2,1,0 ones at the beginning of the simulation
        if (EI == null) {
            EI = new double[12][num_EI][basinNum];
            pm.beginTask("Read energy index data.", inEnergy.size());
            for( EIEnergy energy : inEnergy ) {
                pm.worked(1);
                int tempId = energy.basinId;
                Integer index = basinid2BasinindexMap.get(tempId);
                if (index == null) {
                    // basinid2BasinindexMap.remove(tempId);
                    continue;
                }
                int j = energy.energeticBandId;
                int k = energy.virtualMonth;
                int kInverse = 11 - k;

                EI[k][j][index] = energy.energyValue;
                EI[kInverse][j][index] = energy.energyValue;
            }
            pm.done();
        }
        // get area bande fascie from scalar link ([num_ES][num_EI][basinNum]) ones at the
        // beginning of the simulation
        if (A == null) {
            A = new double[num_ES][num_EI][basinNum];

            pm.beginTask("Read area per heigth and band data.", inAreas.size());

            HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>> idbasinMap = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>>();
            for( EIAreas area : inAreas ) {
                int idBasin = area.basinId;
                HashMap<Integer, HashMap<Integer, Double>> idfasceMap = idbasinMap.get(idBasin);
                if (idfasceMap == null) {
                    idfasceMap = new HashMap<Integer, HashMap<Integer, Double>>();
                    idbasinMap.put(idBasin, idfasceMap);
                }
                int idAltimetricBand = area.altimetricBandId;
                HashMap<Integer, Double> idbandeMap = idfasceMap.get(idAltimetricBand);
                if (idbandeMap == null) {
                    idbandeMap = new HashMap<Integer, Double>();
                    idfasceMap.put(idAltimetricBand, idbandeMap);
                }
                int idEnergeticBand = area.energyBandId;
                double areaValue = area.areaValue;
                idbandeMap.put(idEnergeticBand, areaValue);
                pm.worked(1);
            }
            pm.done();

            for( int i = 0; i < basinNum; i = i + 1 ) {
                Integer index = basinindex2BasinidMap.get(i);
                if (index == null) {
                    basinid2BasinindexMap.remove(i);
                    continue;
                }
                HashMap<Integer, HashMap<Integer, Double>> fasceMap = idbasinMap.get(index);

                for( int j = 0; j < num_ES; j++ ) {
                    HashMap<Integer, Double> bandeMap = fasceMap.get(j);
                    for( int k = 0; k < num_EI; k++ ) {
                        A[j][k][i] = bandeMap.get(k);
                    }
                }
            }
        }

        // get T (temperatures per basin per band) from scalar input link at each time step
        double[][] T = null;
        if (inTemp != null) {
            T = new double[basinNum][num_ES];
            pm.beginTask("Read temperature data.", inTemp.size());
            Set<Integer> basinIdsSet = inTemp.keySet();
            for( Integer basinId : basinIdsSet ) {
                pm.worked(1);
                Integer index = basinid2BasinindexMap.get(basinId);
                if (index == null) {
                    // data for a basin that is not considered, ignore it
                    continue;
                }
                double[] values = inTemp.get(basinId);
                T[index] = values;
            }
            pm.done();
        }

        // get V (wind speed per basin per band) from scalar link at each time step
        double[][] V = null;
        if (inWind != null) {
            V = new double[basinNum][num_ES];
            pm.beginTask("Read wind speed data.", inWind.size());
            Set<Integer> basinIdsSet = inWind.keySet();
            for( Integer basinId : basinIdsSet ) {
                pm.worked(1);
                Integer index = basinid2BasinindexMap.get(basinId);
                if (index == null) {
                    // data for a basin that is not considered, ignore it
                    continue;
                }
                double[] values = inWind.get(basinId);
                V[index] = values;
            }
        }

        // get P (pressure per basin per band) from scalar link at each time step
        double[][] P = null;
        if (inPressure != null) {
            P = new double[basinNum][num_ES];
            pm.beginTask("Read pressure data.", inPressure.size());
            Set<Integer> basinIdsSet = inPressure.keySet();
            for( Integer basinId : basinIdsSet ) {
                pm.worked(1);
                Integer index = basinid2BasinindexMap.get(basinId);
                if (index == null) {
                    // data for a basin that is not considered, ignore it
                    continue;
                }
                double[] values = inPressure.get(basinId);
                P[index] = values;
            }
            pm.done();
        }

        // get RH (relative humidity per basin per band) from scalar link at each time step
        double[][] RH = null;
        if (inRh != null) {
            RH = new double[basinNum][num_ES];
            pm.beginTask("Read humidity data.", inRh.size());
            Set<Integer> basinIdsSet = inRh.keySet();
            for( Integer basinId : basinIdsSet ) {
                pm.worked(1);
                Integer index = basinid2BasinindexMap.get(basinId);
                if (index == null) {
                    // data for a basin that is not considered, ignore it
                    continue;
                }
                double[] values = inRh.get(basinId);
                RH[index] = values;
            }
            pm.done();
        }

        // get dtday (daily temperature range per basin per band) from scalar link at each time
        // step
        double[][] DTd = null;
        if (inDtday != null) {
            DTd = new double[basinNum][num_ES];
            pm.beginTask("Read dtday data.", inDtday.size());
            Set<Integer> basinIdsSet = inDtday.keySet();
            for( Integer basinId : basinIdsSet ) {
                pm.worked(1);
                Integer index = basinid2BasinindexMap.get(basinId);
                if (index == null) {
                    // data for a basin that is not considered, ignore it
                    continue;
                }
                double[] values = inDtday.get(basinId);
                DTd[index] = values;
            }
            pm.done();
        }

        // get dtmonth (monthly temperature range per basin per band) from scalar link at each
        // time step
        double[][] DTm = null;
        if (inDtmonth != null) {
            DTm = new double[basinNum][num_ES];
            pm.beginTask("Read dtday data.", inDtmonth.size());
            Set<Integer> basinIdsSet = inDtmonth.keySet();
            for( Integer basinId : basinIdsSet ) {
                pm.worked(1);
                Integer index = basinid2BasinindexMap.get(basinId);
                if (index == null) {
                    // data for a basin that is not considered, ignore it
                    continue;
                }
                double[] values = inDtmonth.get(basinId);
                DTm[index] = values;
            }
            pm.done();
        }

        /*
         * set the current time: day, month and hour
         */
        DateTime currentDatetime = formatter.parseDateTime(tCurrent);
        int currentMonth = currentDatetime.getMonthOfYear();
        int currentDay = currentDatetime.getDayOfMonth();
        int currentMinute = currentDatetime.getMinuteOfDay();
        double hour = currentMinute / 60.0;
        System.out.println("ora: " + hour);

        if (averageTemperature == null) {
            averageTemperature = new double[2 * basinNum];
        } else {
            Arrays.fill(averageTemperature, 0.0);
        }
        /*
         * these have to be taken from initial values 
         */
        if (safePoint.SWE == null) {
            if (pInitsafepoint != null && new File(pInitsafepoint).exists()) {
                safePoint = getSafePointData();
            } else {
                safePoint.SWE = new double[num_ES][num_EI][basinNum];
                if (pInitswe == -9999.0) {
                    pInitswe = 0.0;
                }
                for( int i = 0; i < basinNum; i++ ) {
                    double sweTmp = pInitswe;
                    if (usoList != null) {
                        int usoTmp = usoList.get(i);
                        if (usoTmp == pGlacierid) {
                            sweTmp = GLACIER_SWE;
                        }
                    }
                    for( int k = 0; k < num_ES; k++ ) {
                        for( int j = 0; j < num_EI; j++ ) {
                            safePoint.SWE[j][k][i] = sweTmp;
                        }
                    }
                }
                safePoint.U = new double[num_ES][num_EI][basinNum];
                safePoint.SnAge = new double[num_ES][num_EI][basinNum];
                safePoint.Ts = new double[num_ES][num_EI][basinNum];
            }
        }

        // this has to be taken from a file, scalarreader
        // TODO add the input canopyLink for the canopy height for each altimetric band
        /*
         * if there is no canopy input matrix for the model create an empty canopy matrix for each elevation band and for each basin
         */
        double[][] canopy = new double[num_ES][basinNum];
        for( int i = 0; i < canopy.length; i++ ) {
            for( int j = 0; j < canopy[0].length; j++ ) {
                canopy[i][j] = pCanopyh;
            }
        }
        checkParametersAndRunEnergyBalance(rain, T, V, P, RH, currentMonth, currentDay, hour, Abasin, A, EI, DTd, DTm, canopy);

    }

    private SafePoint getSafePointData() {
        FileInputStream fis = null;
        ObjectInputStream in = null;
        try {
            fis = new FileInputStream(pInitsafepoint);
            in = new ObjectInputStream(fis);
            SafePoint readSafePoint = (SafePoint) in.readObject();
            in.close();
            return readSafePoint;
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Finalize
    public void writeSafePoint() {
        if (pEndsafepoint != null && new File(pEndsafepoint).getParentFile() != null) {
            FileOutputStream fos = null;
            ObjectOutputStream out = null;
            try {
                fos = new FileOutputStream(pEndsafepoint);
                out = new ObjectOutputStream(fos);
                out.writeObject(safePoint);
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Method to check the input parameters.
     * 
     * <p>
     * Due to the huge amount of parameters, this method is used to do 
     * necessary checks and set default values. This is made so the initialize 
     * method doesn't get flooded.
     * </p> 
     *
     * @param dtData
     * @param rain
     * @param T matrix of temperatures of every altimetric band for every basin.[basin][altim. band]
     * @param V
     * @param P
     * @param RH
     * @param month
     * @param day
     * @param hour
     * @param Abasin area of the different basins (as defined through the features/geometries)
     * @param A area for altim and energetic bands. Coming from eicalculator.
     * @param EI energy index matrix, coming from eicalculator.
     * @param DTd daily temperature range. 
     * @param DTm monthly temperature range.
     * @param canopy
     */
    private void checkParametersAndRunEnergyBalance( double[] rain, double[][] T, double[][] V, double[][] P, double[][] RH,
            double month, double day, double hour, double[] Abasin, double[][][] A, double[][][] EI, double[][] DTd,
            double[][] DTm, double[][] canopy ) {

        double Dt = ((double) tTimestep / (double) pInternaltimestep) * 60.0;

        /*
         * some hardcoded variables
         */
        boolean hasNoStations = false;
        double zmes_T = 2.0; // quota misura temperatura,pressione e umidita'
        double zmes_U = 2.0; // quota misura velocita' vento [m]
        double z0T = 0.005; // [m] roughness length della temperatura
        double z0U = 0.05; // [m] roughness length del vento
        double K = ka * ka / ((log(zmes_U / z0U)) * (log(zmes_T / z0T)));
        double eps = 0.98; // emissivita' neve
        double Lc = 0.05; // ritenzione capillare
        double Ksat = 3.0; // 5.55; // conducibilita' idraulica della neve a saturazione
        double Ks = 5.55E-5; // conducibilita' termica superficiale della neve
        double aep = 50.0; // albedo extinction parameter (kg/m2==mm)
        double rho_g = 1600; // densita' del suolo [kg/m3]
        double De = 0.4; // suolo termicamente attivo
        double C_g = 890.0; // capacita' termica del suolo [J/(kg K)]
        double albedo_land = 0.2;
        double Ts_min = -20.0;
        double Ts_max = 20.0;

        // TODO check parameters and add to the model parameter
        latitude = 46.6 * Math.PI / 180.0; // [rad]
        longitude = 10.88 * Math.PI / 180.0; // [rad]
        standard_time = -1.0; // differenza rispetto a UMT [h]

        /*
         * start calculations
         */
        sun(hour, day);

        for( int i = 0; i < basinNum; i++ ) {
            calculateEnergyBalance(i, month, hasNoStations, V[i], canopy, T[i], P[i], RH[i], rain, pTrain, pTsnow, Dt, A, Abasin,
                    EI, DTd[i], DTm[i], K, eps, Lc, pRhosnow, Ksat, rho_g, De, C_g, aep, albedo_land, Ks, Ts_min, Ts_max);
        }
    }

    /**
     * @param i index for the basins list.
     * @param month 
     * @param hasNoStations
     * @param windSpeed vettore della velocita' del vento sulle fasce altimetriche per il bacino considerato.
     * @param canopy 
     * @param T vettore della temperatura sulle fasce altimetriche per il bacino considerato.
     * @param P vettore della pressione sulle fasce altimetriche per il bacino considerato.
     * @param RH vettore dell'umidita' relativa sulle fasce altimetriche per il bacino considerato.
     * @param rain vettore della pioggia
     * @param Train 
     * @param Tsnow 
     * @param Dt 
     * @param A 
     * @param Abasin 
     * @param EI 
     * @param DTd 
     * @param DTm 
     * @param K 
     * @param eps snow emissivity.
     * @param Lc capillar ritention. 
     * @param rho_sn snow density [kg/m3]
     * @param Ksat conducibilita' idraulica della neve a saturazione [kg/(m2 s)].
     * @param rho_g densita' del suolo [kg/m3].
     * @param De suolo termicamente attivo [m].
     * @param C_g capacita' termica del suolo [J/(kg K)].
     * @param aep albedo extinction parameter (kg/m2==mm).
     * @param albedo_land 
     * @param Ks conducibilita' superficiale della neve [m/s].
     * @param Ts_min 
     * @param Ts_max 
     * @param SWE snow water equivalent della [banda altimetrica][banda energetica][bacino].
     * @param U 
     * @param SnAge snow age [banda altimetrica][banda energetica][bacino]
     * @param Ts surface temperature
     */
    private void calculateEnergyBalance( int i, double month, boolean hasNoStations, double[] windSpeed, double[][] canopy,
            double[] T, double[] P, double[] RH, double[] rain, double Train, double Tsnow, double Dt, double[][][] A,
            double[] Abasin, double[][][] EI, double[] DTd, double[] DTm, double K, double eps, double Lc, double rho_sn,
            double Ksat, double rho_g, double De, double C_g, double aep, double albedo_land, double Ks, double Ts_min,
            double Ts_max ) {

        double rho, cp, ea, Psnow, T_snow, Prain, T_rain, Qp, Pnet;
        double[] tausn = new double[1];
        double[] Rsw = new double[1];
        double[] Rlwin = new double[1];
        double[] netRadiation = new double[1];
        double[] netShortRadiation = new double[1];
        double[] Wice = new double[1];
        double[] Tin = new double[1];
        double[] Fliq = new double[1];
        double Tsur, Se, H0, L0, R0, M0, U0, W0, H1, L1, R1, M1, U1, W1, U2, W2;
        int tol, cont;
        int[] conv = new int[1];

        double[][][] SWE = safePoint.SWE;
        double[][][] SnAge = safePoint.SnAge;
        double[][][] Ts = safePoint.Ts;
        double[][][] U = safePoint.U;

        /*
         * set first value to the id of the basin
         */
        Integer basinId = basinindex2BasinidMap.get(i);
        averageTemperature[2 * i] = basinId;

        // valori medi per bacino
        // creo un vettore contenente un valore di Snow Water Equivalent per
        // ogni bacino

        double tmpPnet = 0.0;
        double tmpPrain = 0.0;
        double tmpPsnow = 0.0;
        double tmpSwe = 0.0;
        double tmpNetradiation = 0.0;
        double tmpNetShortRadiation = 0.0;
        for( int j = 0; j < num_ES; j++ ) { // per tutte le BANDE
            // ALTIMETRICHE

            // riduzione della velocita' del vento dovuta alla canopy
            windSpeed[j] *= (1.0 - 0.8 * canopy[j][i]);

            // printf("\n j=%4ld T=%10.5f",j,V[j]);

            // densita' dell'aria (kg/m3)
            rho = 1.2922 * (tk / (T[j] + tk)) * (P[j] / 1013.25);

            // calore specifico a pressione costante (J/(kg K))
            cp = 1005.00 + (T[j] + 23.15) * (T[j] + 23.15) / 3364.0;

            // pressione di vapore dell'aria (mbar)
            // BEFORE ea = 0.01 * RH[j] * pvap(T[j]);
            ea = 0.01 * RH[j] * pVap(T[j], P[j]);

            // precipitazione liquida e solida
            // se la temperatura della fascia altimetrica è maggiore di Train
            // (parameters)
            // tutta la precipitazione è pioggia
            if (T[j] > Train) {
                Prain = rain[i];
                Psnow = 0.0;
                // se la temperatura della fascia altimetrica è compresa tra
                // Train e Tsnow
                // ci sarà una parte di pioggia e una di neve
                // si trascura l'effetto del vento
            } else if (T[j] <= Train && T[j] >= Tsnow) {
                Prain = rain[i] * (T[j] - Tsnow) / (Train - Tsnow);
                Psnow = rain[i] - Prain;
                // se la temperatura della fascia altimetrica è minore di Tsnow
                // tutto è neve
            } else {
                Prain = 0.0;
                Psnow = rain[i];
            }
            // temperatura della neve
            T_snow = T[j];
            // se la temperatura della neve e' maggiore dello zero assegno a
            // T_snow lo zero
            if (T_snow > Tf)
                T_snow = Tf;
            T_rain = T[j];
            // se la temperatura dell'acqua è minore dello zero assegno a T_rain
            // lo zero
            if (T_rain < Tf)
                T_rain = Tf;

            // calore trasportato dalla precipitazione
            Qp = (Psnow * C_ice * T_snow + Prain * (Lf + C_liq * T_rain)) / Dt; // Qp[W/m^2]
            // P[kg/m^2]
            // c[J/(kg*K)]
            // Lf[J/kg]

            for( int k = 0; k < num_EI; k++ ) { // per tutte le BANDE
                // ENERGETICHE

                // se il SWE della banda altimetrica, banda energetica del
                // bacino i o la prec
                // nevosa sono maggiori di zero
                if (SWE[j][k][i] > 0 || Psnow > 0) {

                    // calcolo contenuto di ghiaccio e temperatura della neve da
                    // U
                    calculateTemp(Wice, Tin, Fliq, 1.0E3 * U[j][k][i], SWE[j][k][i], rho_g, De, C_g);

                    // radiazione e albedo
                    tausn[0] = SnAge[j][k][i]; // età della neve
                    // adimensionale
                    // BEFORE radiation(parameters, EI[month][k][i],
                    // alpha, E0,
                    // Ts[j][k][i], Wice[0], T[j], ea, Psnow,
                    // DTd[j], DTm[j], tausn, Rsw, Rlwin);
                    calculateRadiation(EI[(int) month][k][i], Ts[j][k][i], Wice[0], T[j], ea, P[j], Psnow, DTd[j], DTm[j], tausn,
                            Rsw, Rlwin, Dt, aep, albedo_land, netRadiation, netShortRadiation, pSnowrefv, pSnowrefir);

                    // double Dt, double aep, double albedo_land

                    SnAge[j][k][i] = tausn[0];

                    // riduzione della canopy sui flussi radiativi
                    Rsw[0] *= (1.0 - canopy[j][i]);
                    Rlwin[0] *= (1.0 - canopy[j][i]);

                    // PREDICTOR
                    // temperatura della superficie
                    Tsur = calculateSurfaceTemperature(conv, Ts[j][k][i], Tin[0], T[j], P[j], windSpeed[j], ea, Rsw[0], Rlwin[0],
                            Qp, rho, cp, canopy[j][i], K, rho_sn, Ks, eps, Ts_min, Ts_max);

                    if (Double.isNaN(Tsur) || conv[0] != 1)
                        Tsur = T[j]; // controllo in caso di non
                    // convergenza
                    if (Tsur > Tf)
                        Tsur = Tf; // vedi appunti OK

                    // calore sensibile
                    H0 = K * windSpeed[j] * rho * cp * (T[j] - Tsur);

                    // calore latente
                    L0 = Lv * K * windSpeed[j] * rho * 0.622 * (ea - pVap(Tsur, P[j])) / P[j];

                    // radiazione onde lunghe uscente
                    R0 = (1.0 - canopy[j][i]) * eps * sigma * pow(Tsur + tk, 4.0);

                    // scioglimento
                    Se = (Fliq[0] / (1.0 - Fliq[0]) - Lc) / (rho_w / rho_sn - rho_w / rho_i - Lc);
                    if (Se < 0)
                        Se = 0.0;
                    M0 = Ksat * pow(Se, 3.0); // flusso di acqua uscente
                    if (Double.isNaN(Se))
                        M0 = 0.0;
                    if (M0 * Dt > SWE[j][k][i] - Wice[0])
                        M0 = (SWE[j][k][i] - Wice[0]) / Dt;

                    // aggiornamento
                    W1 = SWE[j][k][i] + rain[i] + (L0 / Lv - M0) * Dt;
                    U1 = U[j][k][i] + 1.0E-3 * (Rsw[0] + Rlwin[0] + Qp - Lf * M0 - R0 + H0 + L0) * Dt;

                    U0 = U1; // aggiorno i parametri di U e W con i valori
                    // calcolati
                    W0 = W1; // di primo tentativo

                    // contatori e controlli
                    cont = 0;
                    tol = 0;

                    // CORRECTOR
                    do {
                        // calcolo Wice, Fliq e Tin da U1
                        calculateTemp(Wice, Tin, Fliq, 1.0E3 * U1, W1, rho_g, De, C_g);

                        // temperatura della superficie
                        Tsur = calculateSurfaceTemperature(conv, Tsur, Tin[0], T[j], P[j], windSpeed[j], ea, Rsw[0], Rlwin[0],
                                Qp, rho, cp, canopy[j][i], K, rho_sn, Ks, eps, Ts_min, Ts_max);
                        if (Double.isNaN(Tsur) || conv[0] != 1)
                            Tsur = T[j]; // controllo in caso di non
                        // convergenza
                        if (Tsur > Tf)
                            Tsur = Tf;

                        // calore sensibile
                        H1 = K * windSpeed[j] * rho * cp * (T[j] - Tsur);

                        // calore latente
                        L1 = Lv * K * windSpeed[j] * rho * 0.622 * (ea - pVap(Tsur, P[j])) / P[j];

                        // radiazione onde lunghe uscente
                        R1 = (1.0 - canopy[j][i]) * eps * sigma * pow(Tsur + tk, 4.0);

                        // scioglimento
                        if (Fliq[0] == 1) {
                            M1 = W1 / Dt;
                            U2 = 0.0;
                            W2 = 0.0;
                            tol = 3;
                        } else {
                            Se = (Fliq[0] / (1.0 - Fliq[0]) - Lc) / (rho_w / rho_sn - rho_w / rho_i - Lc);
                            if (Se < 0)
                                Se = 0.0;
                            M1 = Ksat * pow(Se, 3.0);
                            if (Double.isNaN(Se))
                                M1 = 0.0;
                            if (M1 * Dt > W1 - Wice[0])
                                M1 = (W1 - Wice[0]) / Dt;

                            // aggiornamento
                            W2 = SWE[j][k][i] + rain[i] + (0.5 * (L0 / Lv - M0) + 0.5 * (L1 / Lv - M1)) * Dt;
                            U2 = U[j][k][i]
                                    + 1.0E-3
                                    * (Rsw[0] + Rlwin[0] + Qp + 0.5 * (-Lf * M0 - R0 + H0 + L0) + 0.5 * (-Lf * M1 - R1 + H1 + L1))
                                    * Dt;

                            cont += 1;

                            // controllo convergenza
                            if (cont == 1 && abs(U2 - U1) < defaultTollU0 && abs(W2 - W1) < defaultTollW0)
                                tol = 1;
                            if (cont > 1 && abs(U2 - U1) < defaultTollU && abs(W2 - W1) < defaultTollW)
                                tol = 2;
                            if (conv[0] != 1)
                                tol = 0; // se Tsur non converge, non va bene
                            // la soluzione

                            // aggiornamento
                            U1 = U2;
                            W1 = W2;
                        }

                    } while( tol == 0 && cont <= defaultUWiter );

                    // se non c'e' convergenza cerco una soluzione esplicita
                    // (approssimata)
                    if (tol == 0) { // solo frazione liquida: ho trovato i
                        // valori e aggiorno i dati
                        U[j][k][i] = U0;
                        SWE[j][k][i] = W0;
                        Ts[j][k][i] = Tsur;
                        Pnet = M0 * Dt;
                    } else if (tol == 3) { // non ho convergenza e setto a zero
                        // tutto
                        U[j][k][i] = 0.0;
                        SWE[j][k][i] = 0.0;
                        Ts[j][k][i] = 0.0;
                        Pnet = M1 * Dt;
                    } else { // frazione liquida e solida: ho trovato i
                        // valori e aggiorno i dati
                        U[j][k][i] = U1;
                        SWE[j][k][i] = W1;
                        Ts[j][k][i] = Tsur;
                        Pnet = (0.5 * M0 + 0.5 * M1) * Dt;
                    }

                    // se tutto lo SWE e' liquido o se c'è troppo poco SWE
                    // metto tutto lo SWE nella Pn
                    if (1.0E3 * U[j][k][i] >= Lf * SWE[j][k][i] || SWE[j][k][i] <= 0) {
                        if (SWE[j][k][i] < 0)
                            SWE[j][k][i] = 0.0;
                        Pnet += SWE[j][k][i];
                        if (Pnet < 0)
                            Pnet = 0.0;
                        SWE[j][k][i] = 0.0;
                        U[j][k][i] = 0.0;
                    }
                } else {
                    Pnet = Prain;

                    Tsur = T[j];
                    Ts[j][k][i] = Tsur;
                    tausn[0] = 0.0;
                    calculateRadiation(EI[(int) month][k][i], Ts[j][k][i], 0.0, T[j], ea, P[j], Psnow, DTd[j], DTm[j], tausn,
                            Rsw, Rlwin, Dt, aep, albedo_land, netRadiation, netShortRadiation, pSnowrefv, pSnowrefir);
                    // for( m = 2; m <= 40; m++ ) {
                    // logg[m] = -1.0;
                    // }
                }
                safePoint.SWE[j][k][i] = SWE[j][k][i];
                safePoint.U[j][k][i] = U[j][k][i];
                safePoint.Ts[j][k][i] = Ts[j][k][i];
                safePoint.SnAge[j][k][i] = SnAge[j][k][i];

                // calcolo i valori medi per bacino di SWE, Pnet, Prain, Psnow
                tmpSwe = tmpSwe + SWE[j][k][i] * (A[j][k][i] / Abasin[i]);
                tmpPnet = tmpPnet + Pnet * (A[j][k][i] / Abasin[i]);
                tmpPrain = tmpPrain + Prain * (A[j][k][i] / Abasin[i]);
                tmpPsnow = tmpPsnow + Psnow * (A[j][k][i] / Abasin[i]);
                tmpNetradiation = tmpNetradiation + netRadiation[0] * (A[j][k][i] / Abasin[i]);
                tmpNetShortRadiation = tmpNetShortRadiation + netShortRadiation[0] * (A[j][k][i] / Abasin[i]);
                // System.out.println("swe = " + fullAdigeData[8 * i + 8]);
            }
            averageTemperature[2 * i + 1] += T[j];
        }

        outSwe.put(basinId, new double[]{tmpSwe});
        outPnet.put(basinId, new double[]{tmpPnet});
        outPrain.put(basinId, new double[]{tmpPrain});
        outPsnow.put(basinId, new double[]{tmpPsnow});
        outNetradiation.put(basinId, new double[]{tmpNetradiation});
        outNetshortradiation.put(basinId, new double[]{tmpNetShortRadiation});

        // System.out.println("rad media= " + fullAdigeData[8 * i + 2]);
        // System.out.println("short media= " + fullAdigeData[8 * i + 3]);
        averageTemperature[2 * i + 1] /= num_ES;
    }

    private void calculateTemp( double[] Wice, double[] Tin, double[] Fliq, double U, double SWE, double rho_g, double De,
            double C_g ) {
        if (U <= 0) {
            Wice[0] = SWE;
            Tin[0] = U / (SWE * C_ice + rho_g * De * C_g);
            Fliq[0] = 0.0;
        } else if (U > 0 && U <= Lf * SWE) {
            Wice[0] = SWE - U / Lf;
            Tin[0] = 0.0;
            Fliq[0] = (SWE - Wice[0]) / SWE;
        } else {
            Wice[0] = 0.0;
            Tin[0] = (U - Lf * SWE) / (rho_g * De * C_g + SWE * C_liq);
            Fliq[0] = 1.0;
        }

    }

    private void calculateRadiation( double EI, double Ts, double Wice, double Ta, double ea, double P, double Psnow, double DTd,
            double DTm, double[] tausn, double[] Rsw, double[] Rlwin, double Dt, double aep, double albedo_land,
            double[] netRadiation, double[] netShortRadiation, double avo, double airo ) {

        double coszen, r1, r2, r3, fzen, fage, avd, avis, aird, anir, albedo, rr, AtmTrans;
        double eps_clsky, CF, eps;
        double bb = 2.0, cv = 0.2, cr = 0.5, a = 0.8, c = 2.4, b;
        double diff2glob;

        // COSINE OF ZENITHAL ANGLE
        coszen = EI * sin(alpha);

        // ALBEDO
        // effect snow surface temperature
        r1 = exp(5000.0 * (1.0 / (Tf + tk) - 1.0 / (Ts + tk)));
        // effect melt and refreezing
        r2 = pow(r1, 10);
        if (r2 > 1.0)
            r2 = 1.0;
        // effect of dirt
        r3 = 0.03;
        // non-dimensional snow age: 10 mm of snow precipitation restore snow
        // age Dt(s)
        tausn[0] = (tausn[0] + (r1 + r2 + r3) * Dt * 1.0E-6) * (1.0 - Psnow / 10.0);
        if (tausn[0] < 0.0)
            tausn[0] = 0.0;
        // dipendence from solar angle
        if (coszen < 0.5) {
            fzen = 1.0 / bb * ((bb + 1.0) / (1.0 + 2.0 * bb * coszen) - 1.0);
        } else {
            fzen = 0.0;
        }
        // dipendence from snow age
        fage = tausn[0] / (1.0 + tausn[0]);
        // diffuse visible albedo
        avd = (1.0 - cv * fage) * avo;
        // global visible albedo
        avis = avd + 0.4 * fzen * (1.0 - avd);
        // diffuse near infared albedo
        aird = (1.0 - cr * fage) * airo;
        // global near infared albedo
        anir = aird + 0.4 * fzen * (1.0 - aird);
        // albedo is taken as average
        albedo = (avis + anir) / 2.0;
        // Linear transition from snow albedo to bare ground albedo
        if (Psnow == 0) {
            albedo = albedo_land;
        } else if (Wice < aep) {
            rr = (1.0 - Wice / aep) * exp(-Wice * 0.5 / aep);
            albedo = rr * albedo_land + (1.0 - rr) * albedo;
        }

        // NET SHORTWAVE RADIATION
        if (DTm < 0)
            DTm = 0.0;
        if (DTd < 0)
            DTd = 0.0;
        // ADDED
        a = 0.48 + 0.29 * (1013.25 / P) * sin(alpha);
        if (a > 1)
            a = 1.0;

        b = 0.036 * exp(-0.154 * DTm);
        AtmTrans = a * (1.0 - exp(-b * pow(DTd, c)));

        // ADDED
        // ratio diffuse to global radiation (Erbs et al., 1982)
        if (AtmTrans <= 0.22) {
            diff2glob = 1.0 - 0.09 * AtmTrans;
        } else if (AtmTrans > 0.22 && AtmTrans <= 0.80) {
            diff2glob = 0.9511 - 0.1604 * AtmTrans + 4.388 * pow(AtmTrans, 2.0) - 16.638 * pow(AtmTrans, 3.0) + 12.336
                    * pow(AtmTrans, 4.0);
        } else {
            diff2glob = 0.165;
        }
        // Rsw=direct+diffuse
        Rsw[0] = Isc * E0 * AtmTrans * (1.0 - albedo) * ((1.0 - diff2glob) * coszen + diff2glob * sin(alpha));

        // INCOMING LONGWAVE RADIATION
        eps_clsky = 1.08 * (1.0 - exp(-pow(ea, (Ta + tk) / 2016.0)));
        CF = 1 - AtmTrans / a;
        eps = (1.0 - CF) * eps_clsky + CF;
        Rlwin[0] = eps * sigma * pow(Ta + tk, 4.0);

        // net radiation
        netRadiation[0] = Rsw[0] + Rlwin[0] - albedo * Rsw[0] - eps * sigma * pow(Ts + tk, 4.0);
        // System.out.println("Albedo " + albedo);
        // System.out.println("Radiazione netta: " + netRadiation[0]);
        netShortRadiation[0] = (1 - albedo) * Rsw[0];
        // System.out.println("Net Short: " + netShortRadiation[0]);

    }

    /**
     * Calcola la temperatura della superficie della neve (C).
     * 
     * <p>
     * Risolve il bilancio di energia alla superficie linearizzando e
     * iterando, secondo il metodo di Tarboton.
     * </p>
     * 
     * @param conv
     * @param Ts temperatura della superficie nevosa di primo tentativo (o dell'istante precedente).
     * @param Tin temperatura della neve di primo tentativo (o dell'istante precedente).
     * @param Ta temperatura dell'aria.
     * @param P pressione (mbar).
     * @param V velocita' del vento (m/s).
     * @param ea pressione di vapore in aria (mbar).
     * @param Rsw
     * @param Rlwin
     * @param Qp
     * @param rho
     * @param cp
     * @param Fcanopy
     * @param K 
     * @param rho_sn 
     * @param Ks 
     * @param eps 
     * @param Ts_min 
     * @param Ts_max 
     * @return
     */
    private double calculateSurfaceTemperature( int[] conv, double Ts, double Tin, double Ta, double P, double V, double ea,
            double Rsw, double Rlwin, double Qp, double rho, double cp, double Fcanopy, double K, double rho_sn, double Ks,
            double eps, double Ts_min, double Ts_max ) {
        double a, b, Ts0;
        short cont;

        // coefficienti non dipendenti dalla temperatura della superficie
        a = Rsw + Rlwin + Qp + K * V * Ta * rho * cp + rho_sn * C_ice * Ks * Tin + 0.622 * K * V * Lv * rho * ea / P;
        b = rho_sn * C_ice * Ks + K * V * rho * cp;
        cont = 0;

        do {
            Ts0 = Ts;
            Ts = (a - 0.622 * K * V * Lv * rho * (pVap(Ts0, P) - Ts * dpVap(Ts0, P)) / P + 3.0 * Fcanopy * sigma * eps
                    * pow(Ts0 + tk, 4.0))
                    / (b + 0.622 * dpVap(Ts0, P) * K * V * Lv * rho / P + 4.0 * Fcanopy * eps * sigma * pow(Ts0 + tk, 3.0));
            cont += 1;
        } while( abs(Ts - Ts0) > defaultTollTs && cont <= defaultTsiter );

        // controlli
        if (abs(Ts - Ts0) > defaultTollTs) {
            conv[0] = 0; // non converge
        } else {
            conv[0] = 1; // converge
        }
        if (Ts < Ts_min || Ts > Ts_max)
            conv[0] = -1; // fuori dai limiti di ammissibilita'

        Ts0 = Ts;
        return Ts0;
    }

    /**
     * calcola la pressione di vapore [mbar] a saturazione in dipendenza
     * dalla temperatura [gradi Celsius].
     * 
     * @param T
     * @param P
     * @return la pressione di vapore [mbar] a saturazione
     */
    private double pVap( double T, double P ) {
        double A = 6.1121 * (1.0007 + 3.46E-6 * P);
        double b = 17.502;
        double c = 240.97;
        double e = A * exp(b * T / (c + T));
        return e;
    }

    /**
     * Calcola la derivata della pressione di vapore [mbar] a saturazione
     * rispetto dalla temperatura [gradi Celsius].
     *  
     * @param T
     * @param P
     * @return derivata della pressione di vapore.
     */
    private double dpVap( double T, double P ) {
        double A = 6.1121 * (1.0007 + 3.46E-6 * P);
        double b = 17.502;
        double c = 240.97;
        double De = (A * exp(b * T / (c + T))) * (b / (c + T) - b * T / pow(c + T, 2.0));

        return De;
    }

    /**
     * @param hour
     * @param day
     * @param E0 earth-sun distance correction.
     * @param alpha solar height (complementar to zenith angle), [rad].
     */
    private void sun( double hour, double day ) {
        // standard latitude according to standard time
        double lst = standard_time * PI / 12.0;

        // correction sideral time
        double G = 2.0 * PI * (day - 1.0) / 365.0;
        double Et = 0.000075 + 0.001868 * cos(G) - 0.032077 * sin(G) - 0.014615 * cos(2 * G) - 0.04089 * sin(2 * G);

        // local time
        double lh = hour + (longitude - lst) / omega + Et / omega;

        // earth-sun distance correction
        E0 = 1.00011 + 0.034221 * cos(G) + 0.00128 * sin(G) + 0.000719 * cos(2 * G) + 0.000077 * sin(2 * G);

        // solar declination
        double D = 0.006918 - 0.399912 * cos(G) + 0.070257 * sin(G) - 0.006758 * cos(2 * G) + 0.000907 * sin(2 * G) - 0.002697
                * cos(3 * G) + 0.00148 * sin(3 * G);

        // Sunrise and sunset with respect to 12pm [hour]
        double Thr = (acos(-tan(D) * tan(latitude))) / omega;

        if (lh >= 12 - Thr && lh <= 12 + Thr) {
            // alpha: solar height (complementar to zenith angle), [rad]
            alpha = asin(sin(latitude) * sin(D) + cos(latitude) * cos(D) * cos(omega * (12.0 - lh)));
        } else {
            alpha = 0.0;
        }
    }
}
