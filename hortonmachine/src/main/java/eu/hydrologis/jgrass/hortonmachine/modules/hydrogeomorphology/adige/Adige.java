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
package eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Role;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import static eu.hydrologis.jgrass.jgrassgears.libs.modules.HMConstants.*;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.Dams;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.DischargeContributor;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.HillSlope;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.Hydrometers;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.NetBasinsManager;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.Offtakes;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.PfafstetterNumber;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.Tributaries;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.duffy.DischargeDistributor;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.duffy.DuffyModel;
import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.duffy.RungeKuttaFelberg;
import eu.hydrologis.jgrass.jgrassgears.io.grass.JGrassConstants;
import eu.hydrologis.jgrass.jgrassgears.libs.exceptions.ModelsIOException;
import eu.hydrologis.jgrass.jgrassgears.libs.exceptions.ModelsIllegalargumentException;
import eu.hydrologis.jgrass.jgrassgears.libs.modules.HMConstants;
import eu.hydrologis.jgrass.jgrassgears.libs.modules.HMModel;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.DummyProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.IHMProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;

@Description("The Adige model.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Hydrology")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Adige extends HMModel {

    @Description("The hillslope data.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inHillslope;

    @Description("The a field name of the netnum attribute in the hillslope data.")
    @In
    public String fNetnum = null;

    @Description("The a field name of the baricenter elevation attribute in the hillslope data.")
    @In
    public String fBaricenter = null;

    @Description("The a field name of the soil use attribute in the hillslope data.")
    @In
    public String fVegetation = null;

    @Description("The a field name of the avg_sub attribute in the hillslope data.")
    @In
    public String fAvg_sub = null;

    @Description("The a field name of the var_sub attribute in the hillslope data.")
    @In
    public String fVar_sub = null;

    @Description("The a field name of the avg_sup_10 attribute in the hillslope data.")
    @In
    public String fAvg_sup_10 = null;

    @Description("The a field name of the var_sup_10 attribute in the hillslope data.")
    @In
    public String fVar_sup_10 = null;

    @Description("The a field name of the avg_sup_30 attribute in the hillslope data.")
    @In
    public String fAvg_sup_30 = null;

    @Description("The a field name of the var_sup_30 attribute in the hillslope data.")
    @In
    public String fVar_sup_30 = null;

    @Description("The a field name of the avg_sup_60 attribute in the hillslope data.")
    @In
    public String fAvg_sup_60 = null;

    @Description("The a field name of the var_sup_60 attribute in the hillslope data.")
    @In
    public String fVar_sup_60 = null;

    @Description("The average speed for superficial runoff.")
    @In
    public double pV_sup = -1;

    @Description("The average speed for sub-superficial runoff.")
    @In
    public double pV_sub = -1;

    @Description("The hydrometers monitoring points.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inHydrometers;

    @Description("The hydrometers data.")
    @In
    public HashMap<Integer, Double> inHydrometerdata;

    @Description("The dams monitoring points.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inDams;

    @Description("The dams data.")
    @In
    public HashMap<Integer, Double> inDamsdata;

    @Description("The tributary monitoring points.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inTributary;

    @Description("The tributary data.")
    @In
    public HashMap<Integer, Double> inTributarydata;

    @Description("The offtakes monitoring points.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inOfftakes;

    @Description("The offtakes data.")
    @In
    public HashMap<Integer, Double> inOfftakesdata;

    @Description("Comma separated list of pfafstetter ids, in which to generate the output")
    @In
    public String pPfafids = null;

    @Description("The a field name of the monitoring point's id attribute in the monitoring points data.")
    @In
    public String fMonpointid = null;

    @Description("The network data.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inNetwork;

    @Description("The a field name of the pfafstetter enumeration attribute in the network data.")
    @In
    public String fPfaff = null;

    @Description("The a field name of the elevation of the starting point of a link in the network data.")
    @In
    public String fNetelevstart = null;

    @Description("The a field name of the elevation of the end point of a link in the network data.")
    @In
    public String fNetelevend = null;

    @Role(Role.PARAMETER)
    @Description("The routing model type to use.")
    @In
    public int pRouting = 3;

    @Role(Role.PARAMETER)
    @Description("A constant value of rain intensity.")
    @Unit("mm/h")
    @In
    public double pRainintensity;

    @Role(Role.PARAMETER)
    @Description("The duration of the constant rain in minutes.")
    @Unit("min")
    @In
    public int pRainduration;

    @Role(Role.PARAMETER)
    @Description("Switch to activate additional logging to file.")
    @In
    public boolean doLog = false;

    @Description("The timestep in minutes.")
    @In
    public int tTimestep = 0;

    @Description("The start date.")
    @In
    public String tStart = null;

    @Description("The end date.")
    @In
    public String tEnd = null;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    // public String startDateArg = null;
    // public String endDateArg = null;
    // public double deltaTArg = null;

    /*
     * ATTRIBUTES FIELDS
     */
    // private Date startDate;
    // private Date endDate;
    private double deltaTinMilliSeconds;
    private double deltaTinMinutes = -1;

    /** the running rain array */
    private double[] rainArray = null;
    private double[] radiationArray;
    private double[] netshortArray;
    private double[] temperatureArray;
    private double[] humidityArray;
    private double[] windspeedArray;
    private double[] pressureArray;
    private double[] snowWaterEquivalentArray;
    /** the running discharge array, which at the begin holds the initial conditions */
    private double[] initialConditions = null;

    private RungeKuttaFelberg rainRunoffRaining;
    private List<PfafstetterNumber> netPfaffsList;
    private DuffyModel duffyEvaluator;

    // hydrometers
    private DischargeContributor hydrometersHandler;
    private HashMap<String, Integer> hydrometer_pfaff2idMap;

    // dams
    private DischargeContributor damsHandler;
    private HashMap<String, Integer> dams_pfaff2idMap;

    // tributaries
    private DischargeContributor tributaryHandler;
    private HashMap<String, Integer> tributary_pfaff2idMap;

    // offtakes
    private DischargeContributor offtakesHandler;
    private HashMap<String, Integer> offtakes_pfaff2idMap;

    private HashMap<Integer, Integer> basinid2Index;
    private HashMap<Integer, Integer> index2Basinid;

    private int hillsSlopeNum;
    private int outletHillslopeId = -1;
    private HashMap<String, Integer> pfaff2Index;
    private int[] indexesArray;
    private List<HillSlope> orderedHillslopes;

    private DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;

    private DateTime startTimestamp;
    private DateTime endTimestamp;
    private DateTime currentTimstamp;
    private DateTime rainEndTimstamp;

    @Execute
    public void process() throws Exception {

        if (startTimestamp == null) {
            startTimestamp = formatter.parseDateTime(tStart);
            endTimestamp = formatter.parseDateTime(tEnd);
            currentTimstamp = startTimestamp;
            if (pRainintensity != -1) {
                if (pRainduration != -1) {
                    rainEndTimstamp = startTimestamp.plusMinutes(pRainduration);
                } else {
                    throw new ModelsIllegalargumentException(
                            "In the case of usage of a constant rainintensity it is necessary to define also its duration.\nCheck your arguments, probably the --rainduration flag is missing.",
                            this.getClass().getSimpleName());
                }
            }
        } else {
            currentTimstamp = currentTimstamp.plusMinutes(tTimestep);
        }

        if (fNetnum == null || fNetnum.length() < 1) {
            throw new ModelsIllegalargumentException("Missing net num attribute name.", this
                    .getClass().getSimpleName());
        }
        if (fPfaff == null || fPfaff.length() < 1) {
            throw new ModelsIllegalargumentException("Missing pfafstetter attribute name.", this
                    .getClass().getSimpleName());
        }
        if (fMonpointid == null || fMonpointid.length() < 1) {
            throw new ModelsIllegalargumentException("Missing monitoring point id attribute name.",
                    this.getClass().getSimpleName());
        }
        if (fBaricenter == null || fBaricenter.length() < 1) {
            throw new ModelsIllegalargumentException("Missing basin centroid attribute name.", this
                    .getClass().getSimpleName());
        }
        if (fNetelevstart == null || fNetelevstart.length() < 1) {
            throw new ModelsIllegalargumentException("Missing start net elevation attribute name.",
                    this.getClass().getSimpleName());
        }
        if (fNetelevend == null || fNetelevend.length() < 1) {
            throw new ModelsIllegalargumentException("Missing start net elevation attribute name.",
                    this.getClass().getSimpleName());
        }

        // hydrometers
        if (inHydrometers != null && inHydrometerdata != null) {
            if (hydrometersHandler == null) {
                pm.message("Reading hydrometers geometries and mapping them to the network...");
                hydrometer_pfaff2idMap = new HashMap<String, Integer>();
                hydrometersHandler = new Hydrometers(hydrometer_pfaff2idMap, inHydrometerdata);

                FeatureIterator<SimpleFeature> hydrometersIterator = inHydrometers.features();
                int pfaffIndex = -1;
                int monIdIndex = -1;
                while( hydrometersIterator.hasNext() ) {
                    SimpleFeature hydrometer = hydrometersIterator.next();
                    if (pfaffIndex == -1) {
                        SimpleFeatureType featureType = hydrometer.getFeatureType();
                        pfaffIndex = featureType.indexOf(fPfaff);
                        if (pfaffIndex == -1) {
                            throw new ModelsIllegalargumentException(
                                    "The hydrometer features are missing the pafaffstetter attribute field: "
                                            + fPfaff, this.getClass().getSimpleName());
                        }
                        monIdIndex = featureType.indexOf(fMonpointid);
                        if (monIdIndex == -1) {
                            throw new ModelsIllegalargumentException(
                                    "The hydrometer features are missing the id attribute field: "
                                            + fMonpointid, this.getClass().getSimpleName());
                        }
                    }

                    String pNumberStr = (String) hydrometer.getAttribute(pfaffIndex);
                    int id = ((Number) hydrometer.getAttribute(monIdIndex)).intValue();
                    hydrometer_pfaff2idMap.put(pNumberStr, id);
                }
            }
        }

        // dams
        if (inDams != null && inDamsdata != null) {
            if (damsHandler == null) {
                pm.message("Reading dams geometries and mapping them to the network...");
                dams_pfaff2idMap = new HashMap<String, Integer>();
                damsHandler = new Dams(dams_pfaff2idMap, inDamsdata);

                FeatureIterator<SimpleFeature> damsIterator = inDams.features();
                int pfaffIndex = -1;
                int monIdIndex = -1;
                while( damsIterator.hasNext() ) {
                    SimpleFeature dam = damsIterator.next();
                    if (pfaffIndex == -1) {
                        SimpleFeatureType featureType = dam.getFeatureType();
                        pfaffIndex = featureType.indexOf(fPfaff);
                        if (pfaffIndex == -1) {
                            throw new ModelsIllegalargumentException(
                                    "The dams features are missing the pfaffstetter attribute field: "
                                            + fPfaff, this.getClass().getSimpleName());
                        }
                        monIdIndex = featureType.indexOf(fMonpointid);
                        if (monIdIndex == -1) {
                            throw new ModelsIllegalargumentException(
                                    "The dams features are missing the id attribute field: "
                                            + fMonpointid, this.getClass().getSimpleName());
                        }
                    }

                    String pNumberStr = (String) dam.getAttribute(pfaffIndex);
                    int id = ((Number) dam.getAttribute(monIdIndex)).intValue();
                    dams_pfaff2idMap.put(pNumberStr, id);
                }
            }
        }

        // tributary
        if (inTributary != null && inTributarydata != null) {
            if (tributaryHandler == null) {
                pm.message("Reading tributary geometries and mapping them to the network...");
                tributary_pfaff2idMap = new HashMap<String, Integer>();
                tributaryHandler = new Tributaries(tributary_pfaff2idMap, inTributarydata);

                FeatureIterator<SimpleFeature> tributaryIterator = inTributary.features();
                int pfaffIndex = -1;
                int monIdIndex = -1;
                while( tributaryIterator.hasNext() ) {
                    SimpleFeature tributary = tributaryIterator.next();
                    if (pfaffIndex == -1) {
                        SimpleFeatureType featureType = tributary.getFeatureType();
                        pfaffIndex = featureType.indexOf(fPfaff);
                        if (pfaffIndex == -1) {
                            throw new ModelsIllegalargumentException(
                                    "The tributary features are missing the pfaffstetter attribute field: "
                                            + fPfaff, this.getClass().getSimpleName());
                        }
                        monIdIndex = featureType.indexOf(fMonpointid);
                        if (monIdIndex == -1) {
                            throw new ModelsIllegalargumentException(
                                    "The tributary features are missing the id attribute field: "
                                            + fMonpointid, this.getClass().getSimpleName());
                        }
                    }

                    String pNumberStr = (String) tributary.getAttribute(pfaffIndex);
                    int id = ((Number) tributary.getAttribute(monIdIndex)).intValue();
                    tributary_pfaff2idMap.put(pNumberStr, id);
                }
            }
        }

        // offtakes
        if (inOfftakes != null && inOfftakesdata != null) {
            if (offtakesHandler == null) {
                pm.message("Reading offtakes geometries and mapping them to the network...");
                offtakes_pfaff2idMap = new HashMap<String, Integer>();
                offtakesHandler = new Offtakes(offtakes_pfaff2idMap, inOfftakesdata, pm);

                FeatureIterator<SimpleFeature> offtakesIterator = inOfftakes.features();
                int pfaffIndex = -1;
                int monIdIndex = -1;
                while( offtakesIterator.hasNext() ) {
                    SimpleFeature offtakes = offtakesIterator.next();
                    if (pfaffIndex == -1) {
                        SimpleFeatureType featureType = offtakes.getFeatureType();
                        pfaffIndex = featureType.indexOf(fPfaff);
                        if (pfaffIndex == -1) {
                            throw new ModelsIllegalargumentException(
                                    "The offtakes features are missing the pfaffstetter attribute field: "
                                            + fPfaff, this.getClass().getSimpleName());
                        }
                        monIdIndex = featureType.indexOf(fMonpointid);
                        if (monIdIndex == -1) {
                            throw new ModelsIllegalargumentException(
                                    "The offtakes features are missing the id attribute field: "
                                            + fMonpointid, this.getClass().getSimpleName());
                        }
                    }

                    String pNumberStr = (String) offtakes.getAttribute(pfaffIndex);
                    int id = ((Number) offtakes.getAttribute(monIdIndex)).intValue();
                    offtakes_pfaff2idMap.put(pNumberStr, id);
                }
            }
        }

        hillsSlopeNum = inHillslope.size();

        if (netPfaffsList == null) {
            ScalarSet vegetationLibScalarSet = ModelsConstants.getScalarSetFromLink(
                    vegetationInputLink, time, err);
            HashMap<Integer, HashMap<Integer, Double>> vegindex2laiMap = new HashMap<Integer, HashMap<Integer, Double>>();
            HashMap<Integer, HashMap<Integer, Double>> vegindex2displacementMap = new HashMap<Integer, HashMap<Integer, Double>>();
            HashMap<Integer, HashMap<Integer, Double>> vegindex2roughnessMap = new HashMap<Integer, HashMap<Integer, Double>>();
            HashMap<Integer, Double> vegindex2RGLMap = new HashMap<Integer, Double>();
            HashMap<Integer, Double> vegindex2rsMap = new HashMap<Integer, Double>();
            HashMap<Integer, Double> vegindex2rarcMap = new HashMap<Integer, Double>();

            readVegetationLibrary(vegetationLibScalarSet, vegindex2laiMap,
                    vegindex2displacementMap, vegindex2roughnessMap, vegindex2RGLMap,
                    vegindex2rsMap, vegindex2rarcMap);

            // at the first round create the hillslopes and network hierarchy
            NetBasinsManager nbMan = new NetBasinsManager();
            orderedHillslopes = nbMan.operateOnLayers(inNetwork, hillslopeFeatureCollection,
                    fNetnum, fPfaff, fNetelevstart, fNetelevend, fBaricenter, fVegetation, out);
            HashMap<Integer, DischargeDistributor> hillslopeId2DischargeDistributor = new HashMap<Integer, DischargeDistributor>();
            outletHillslopeId = orderedHillslopes.get(0).getHillslopeId();
            netPfaffsList = new ArrayList<PfafstetterNumber>();
            pfaff2Index = new HashMap<String, Integer>();
            basinid2Index = new HashMap<Integer, Integer>();
            index2Basinid = new HashMap<Integer, Integer>();
            PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(out);
            pm.beginTask("Analaysing hillslopes and calculating distribution curves...",
                    orderedHillslopes.size());
            for( int i = 0; i < orderedHillslopes.size(); i++ ) {
                HillSlope hillSlope = orderedHillslopes.get(i);
                if (vegindex2laiMap.size() > 0)
                    hillSlope.parameters.setVegetationLibrary(vegindex2laiMap,
                            vegindex2displacementMap, vegindex2roughnessMap, vegindex2RGLMap,
                            vegindex2rsMap, vegindex2rarcMap);
                PfafstetterNumber pfafstetterNumber = hillSlope.getPfafstetterNumber();
                netPfaffsList.add(pfafstetterNumber);
                int hillslopeId = hillSlope.getHillslopeId();
                basinid2Index.put(hillslopeId, i);
                index2Basinid.put(i, hillslopeId);
                pfaff2Index.put(pfafstetterNumber.toString(), i);
                // the distributor
                HashMap<Integer, Double> params = fillParameters(hillSlope);
                System.out.println("Bacino: " + hillslopeId);
                hillslopeId2DischargeDistributor.put(hillslopeId, DischargeDistributor
                        .createDischargeDistributor(DischargeDistributor.DISTRIBUTOR_TYPE_NASH,
                                startDate.getTime(), endDate.getTime(),
                                (long) deltaTinMilliSeconds, params));
                pm.worked(1);
            }
            pm.done();

            duffyEvaluator = new DuffyModel(orderedHillslopes, pRouting, out, deltaTinMinutes,
                    doLog);
            if (hydrometersHandler != null) {
                duffyEvaluator.addDischargeContributor(hydrometersHandler);
            }
            if (damsHandler != null) {
                duffyEvaluator.addDischargeContributor(damsHandler);
            }
            if (tributaryHandler != null) {
                duffyEvaluator.addDischargeContributor(tributaryHandler);
            }
            if (offtakesHandler != null) {
                duffyEvaluator.addDischargeContributor(offtakesHandler);
            }
            duffyEvaluator.addDischargeDistributor(hillslopeId2DischargeDistributor);
            /*
             * read the initial conditions. 
             */
            initialConditions = new double[hillsSlopeNum * 4];
            ScalarSet boundaryInputSet = null;
            if (boundaryInputLink != null) {
                boundaryInputSet = ModelsConstants.getScalarSetFromLink(boundaryInputLink, time,
                        err);
            }

            if (boundaryInputSet != null) {
                int dataIndex = 0;
                for( int i = 0; i < (boundaryInputSet.size() - 1) / 5; i++ ) {
                    Double idHillslope = boundaryInputSet.get(dataIndex + 1);
                    Integer index = basinid2Index.get(idHillslope.intValue());
                    if (index == null)
                        continue;
                    initialConditions[index] = boundaryInputSet.get(dataIndex + 2);
                    initialConditions[index + hillsSlopeNum] = boundaryInputSet.get(dataIndex + 3);
                    initialConditions[index + 2 * hillsSlopeNum] = boundaryInputSet
                            .get(dataIndex + 4);
                    initialConditions[index + 3 * hillsSlopeNum] = boundaryInputSet
                            .get(dataIndex + 5);
                    dataIndex = dataIndex + 5;
                }
            } else {
                double dischargePerUnitArea = 0.01; // m3/s per km2 of upstream drainage area
                for( int i = 0; i < orderedHillslopes.size(); i++ ) {
                    HillSlope currentHillslope = orderedHillslopes.get(i);
                    // initialize with a default discharge per unit of drainage area in km2
                    double hillslopeTotalDischarge = currentHillslope.getUpstreamArea(null)
                            / 1000000.0 * dischargePerUnitArea;
                    initialConditions[i] = 0.3 * hillslopeTotalDischarge;
                    // initial subsuperficial flow is setted at a percentage of the total
                    // discharge
                    initialConditions[i + hillsSlopeNum] = 0.7 * hillslopeTotalDischarge;
                    // initial water content in the saturated hillslope volume is setted to
                    // have:
                    // saturation surface at the 10% of the total area
                    double maxSaturatedVolume = currentHillslope.parameters.getS2max();
                    // initial water content in the non saturated hillslope volume is setted to
                    initialConditions[i + 2 * hillsSlopeNum] = 0.2 * maxSaturatedVolume;
                    initialConditions[i + 3 * hillsSlopeNum] = 0.25 * maxSaturatedVolume;
                }
            }

            // print of the initial conditions values, just for check
            System.out.println("bacino\tQ\tQs\tS1\tS2");
            for( int i = 0; i < hillsSlopeNum; i++ ) {
                int currentBasinId = index2Basinid.get(i);
                System.out.println(currentBasinId + "\t" + initialConditions[i] + "\t"
                        + initialConditions[i + hillsSlopeNum] + "\t"
                        + initialConditions[i + 2 * hillsSlopeNum] + "\t"
                        + initialConditions[i + 3 * hillsSlopeNum]);
            }

            rainRunoffRaining = new RungeKuttaFelberg(duffyEvaluator, 1e-2, 10 / 60., out, doLog);
        }

        // deal with rain
        if (pRainintensity != -1) {
            /*
             * in the case of constant rain the array is build once and then used every time.
             * The only thing that changes, is that after the rainEndDate, the rain intensity is
             * set to 0.
             */
            if (runningDateInMinutes > rainEndDateInMinutes) {
                rainArray = new double[netPfaffsList.size()];
                Arrays.fill(rainArray, 0);
            } else {
                rainArray = new double[netPfaffsList.size()];
                Arrays.fill(rainArray, pRainintensity);
            }

        } else {
            // read rainfall from input link scalar set and transform into a rainfall intensity
            // [mm/h]
            ScalarSet rainfallScalarSet = ModelsConstants.getScalarSetFromLink(rainfallInputLink,
                    time, err);

            rainArray = new double[hillsSlopeNum];
            radiationArray = new double[hillsSlopeNum];
            netshortArray = new double[hillsSlopeNum];
            temperatureArray = new double[hillsSlopeNum];
            humidityArray = new double[hillsSlopeNum];
            windspeedArray = new double[hillsSlopeNum];
            pressureArray = new double[hillsSlopeNum];
            snowWaterEquivalentArray = new double[hillsSlopeNum];
            for( int i = 1; i < rainfallScalarSet.size(); i = i + 9 ) {
                // rain
                Double basinId = rainfallScalarSet.get(i);
                Integer index = basinid2Index.get(basinId.intValue());
                if (index == null) {
                    // System.out.println("Per il bacino " + basinId
                    // + " non e' stata trovata una corrispondenza tra rete e bacini.");
                    continue;
                }
                double rValue = rainfallScalarSet.get(i + 1);
                if (JGrassConstants.isNovalue(rValue)) {
                    rValue = 0.0;
                }
                rainArray[index] = rValue / (deltaTinMinutes / 60.0);
                // radiation
                rValue = rainfallScalarSet.get(i + 2);
                radiationArray[index] = rValue;
                // netshort
                rValue = rainfallScalarSet.get(i + 3);
                netshortArray[index] = rValue;
                // temperature
                rValue = rainfallScalarSet.get(i + 4);
                temperatureArray[index] = rValue;
                // humidity
                rValue = rainfallScalarSet.get(i + 5);
                humidityArray[index] = rValue;
                // windspeed
                rValue = rainfallScalarSet.get(i + 6);
                windspeedArray[index] = rValue;
                // pressure
                rValue = rainfallScalarSet.get(i + 7);
                pressureArray[index] = rValue;
                // snow water equivalent
                rValue = rainfallScalarSet.get(i + 8);
                snowWaterEquivalentArray[index] = rValue;
            }

        }

        double intervalStartTimeInMinutes = runningDateInMinutes;
        double intervalEndTimeInMinutes = runningDateInMinutes + deltaTinMinutes;

        rainRunoffRaining.solve(intervalStartTimeInMinutes, intervalEndTimeInMinutes, 1,
                initialConditions, rainArray, radiationArray, netshortArray, temperatureArray,
                humidityArray, windspeedArray, pressureArray, snowWaterEquivalentArray);
        initialConditions = rainRunoffRaining.getFinalCond();
        rainRunoffRaining.setBasicTimeStep(10 / 60.);

        // return the output link -> create a chart with average rainfall and outlet discharge
        if (linkID.equals(dischargeOutputLink.getID())) {
            // Calculate the average rain on the basin
            double avgRain = 0;
            for( int i = 0; i < rainArray.length; i++ ) {
                avgRain = avgRain + rainArray[i];
            }
            avgRain = avgRain / rainArray.length;

            ScalarSet ret = new ScalarSet();
            if (pPfafids != null) {
                int outNum = pPfafids.length;
                ret.add((double) (outNum + 1));

                if (indexesArray == null) {
                    indexesArray = new int[outNum];
                    for( int i = 0; i < pPfafids.length; i++ ) {
                        String pfaf = pPfafids[i];
                        indexesArray[i] = pfaff2Index.get(pfaf);
                    }
                }
                int pfafindex = 0;
                for( int index : indexesArray ) {
                    double dischargeToPrint = initialConditions[index]
                            + initialConditions[index + hillsSlopeNum];
                    ret.add(dischargeToPrint);
                    double supdischargetoprint = initialConditions[index];
                    ret.add(supdischargetoprint);
                    double subdischargetoprint = initialConditions[index + hillsSlopeNum];
                    ret.add(subdischargetoprint);
                    System.out.println("Bacino: " + pPfafids[pfafindex] + " Outlet Discharge "
                            + initialConditions[index] + " qsub "
                            + initialConditions[index + hillsSlopeNum] + " S1 "
                            + initialConditions[index + 2 * hillsSlopeNum] + " S2 "
                            + initialConditions[index + 3 * hillsSlopeNum] + " rain "
                            + rainArray[index]);
                    pfafindex++;
                }
            } else {
                ret.add(4.0);
                if (indexesArray == null) {
                    indexesArray = new int[1];
                    indexesArray[0] = basinid2Index.get(outletHillslopeId);
                }
                ret.add(initialConditions[indexesArray[0]]
                        + initialConditions[indexesArray[0] + hillsSlopeNum]);
                double supdischargetoprint = initialConditions[indexesArray[0]];
                ret.add(supdischargetoprint);
                double subdischargetoprint = initialConditions[indexesArray[0] + hillsSlopeNum];
                ret.add(subdischargetoprint);
                System.out.println("Outlet Discharge " + initialConditions[indexesArray[0]]
                        + " qsub " + initialConditions[indexesArray[0] + hillsSlopeNum] + " S1 "
                        + initialConditions[indexesArray[0] + 2 * hillsSlopeNum] + " S2 "
                        + initialConditions[indexesArray[0] + 3 * hillsSlopeNum] + " rain "
                        + avgRain);
            }

            // add also the average rainfall
            // ret.add(avgRain);
            return ret;
        } else if (boundaryOutputLink != null && linkID.equals(boundaryOutputLink.getID())) {
            ScalarSet outputBoundarySet = new ScalarSet();
            outputBoundarySet.add((initialConditions.length / 4.0) * 5.0);
            for( int i = 0; i < initialConditions.length / 4; i++ ) {
                Integer basinId = index2Basinid.get(i);
                outputBoundarySet.add(basinId.doubleValue());
                outputBoundarySet.add(initialConditions[i]);
                outputBoundarySet.add(initialConditions[i + hillsSlopeNum]);
                outputBoundarySet.add(initialConditions[i + 2 * hillsSlopeNum]);
                outputBoundarySet.add(initialConditions[i + 3 * hillsSlopeNum]);
            }
            return outputBoundarySet;
        } else if (s1OutputLink != null && linkID.equals(s1OutputLink.getID())) {
            ScalarSet outputS1Set = new ScalarSet();
            if (pPfafids != null) {
                int outNum = pPfafids.length;
                outputS1Set.add((double) (outNum));
                if (indexesArray == null) {
                    indexesArray = new int[outNum];
                    for( int i = 0; i < pPfafids.length; i++ ) {
                        String pfaf = pPfafids[i];
                        indexesArray[i] = pfaff2Index.get(pfaf);
                    }
                }
                for( int index : indexesArray ) {
                    outputS1Set.add(initialConditions[index + 2 * hillsSlopeNum]);
                }
            } else {
                outputS1Set.add(1.0);
                Integer outletIndex = basinid2Index.get(outletHillslopeId);
                outputS1Set.add(initialConditions[outletIndex + 2 * hillsSlopeNum]);
            }
            return outputS1Set;
        } else if (s2OutputLink != null && linkID.equals(s2OutputLink.getID())) {
            ScalarSet outputS2Set = new ScalarSet();
            if (pPfafids != null) {
                int outNum = pPfafids.length;
                outputS2Set.add((double) (outNum));
                if (indexesArray == null) {
                    indexesArray = new int[outNum];
                    for( int i = 0; i < pPfafids.length; i++ ) {
                        String pfaf = pPfafids[i];
                        indexesArray[i] = pfaff2Index.get(pfaf);
                    }
                }
                for( int index : indexesArray ) {
                    outputS2Set.add(initialConditions[index + 3 * hillsSlopeNum]);
                }
            } else {
                outputS2Set.add(1.0);
                Integer outletIndex = basinid2Index.get(outletHillslopeId);
                outputS2Set.add(initialConditions[outletIndex + 3 * hillsSlopeNum]);
            }
            return outputS2Set;
        } else if (s3OutputLink != null && linkID.equals(s3OutputLink.getID())) {
            ScalarSet outputS3Set = new ScalarSet();
            if (pPfafids != null) {
                int outNum = pPfafids.length;
                outputS3Set.add((double) (outNum));
                if (indexesArray == null) {
                    indexesArray = new int[outNum];
                    for( int i = 0; i < pPfafids.length; i++ ) {
                        String pfaf = pPfafids[i];
                        indexesArray[i] = pfaff2Index.get(pfaf);
                    }
                }
                for( int index : indexesArray ) {
                    HillSlope hillSlope = orderedHillslopes.get(index);
                    outputS3Set.add(hillSlope.parameters.getS2Param()
                            * (initialConditions[index + 3 * hillsSlopeNum]));
                }
            } else {
                outputS3Set.add(1.0);
                Integer outletIndex = basinid2Index.get(outletHillslopeId);
                HillSlope hillSlope = orderedHillslopes.get(outletIndex);
                outputS3Set.add(hillSlope.parameters.getS2Param()
                        * (initialConditions[outletIndex + 3 * hillsSlopeNum]));
            }
            return outputS3Set;
        } else if (basinrainOutputLink != null && linkID.equals(basinrainOutputLink.getID())) {
            ScalarSet basinrainSet = new ScalarSet();
            if (pPfafids != null) {
                int outNum = pPfafids.length;
                basinrainSet.add((double) (outNum));
                if (indexesArray == null) {
                    indexesArray = new int[outNum];
                    for( int i = 0; i < pPfafids.length; i++ ) {
                        String pfaf = pPfafids[i];
                        indexesArray[i] = pfaff2Index.get(pfaf);
                    }
                }
                for( int index : indexesArray ) {
                    basinrainSet.add(rainArray[index]);
                }
            } else {
                basinrainSet.add(1.0);
                Integer outletIndex = basinid2Index.get(outletHillslopeId);
                basinrainSet.add(rainArray[outletIndex + 3 * hillsSlopeNum]);
            }
            return basinrainSet;
        }

        return null;
    }

    private HashMap<Integer, Double> fillParameters( HillSlope hillSlope ) {
        HashMap<Integer, Double> params = new HashMap<Integer, Double>();
        // Double attribute = (Double)
        // hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_10);
        Double attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(fAvg_sup_10))
                .doubleValue();

        params.put(DischargeDistributor.PARAMS_AVG_SUP_10, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_30);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(fAvg_sup_30))
                .doubleValue();
        params.put(DischargeDistributor.PARAMS_AVG_SUP_30, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_60);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(fAvg_sup_60))
                .doubleValue();
        params.put(DischargeDistributor.PARAMS_AVG_SUP_60, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_10);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(fVar_sup_10))
                .doubleValue();
        params.put(DischargeDistributor.PARAMS_VAR_SUP_10, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_30);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(fVar_sup_30))
                .doubleValue();
        params.put(DischargeDistributor.PARAMS_VAR_SUP_30, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_60);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(fVar_sup_60))
                .doubleValue();
        params.put(DischargeDistributor.PARAMS_VAR_SUP_60, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUB);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(fAvg_sub)).doubleValue();
        params.put(DischargeDistributor.PARAMS_AVG_SUB, attribute);
        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUB);
        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(fVar_sub)).doubleValue();
        params.put(DischargeDistributor.PARAMS_VAR_SUB, attribute);
        try {
            double vsup = Double.parseDouble(pV_sup);
            params.put(DischargeDistributor.PARAMS_V_SUP, vsup);
            double vsub = Double.parseDouble(pV_sub);
            params.put(DischargeDistributor.PARAMS_V_SUB, vsub);
        } catch (NumberFormatException e) {
            throw new ModelsIllegalargumentException(
                    "The speed parameters need to be a valid number. Check your syntax.", this);
        }
        return params;
    }

    private void readVegetationLibrary( ScalarSet vegetationLibScalarSet,
            HashMap<Integer, HashMap<Integer, Double>> vegindex2laiMap,
            HashMap<Integer, HashMap<Integer, Double>> vegindex2displacementMap,
            HashMap<Integer, HashMap<Integer, Double>> vegindex2roughnessMap,
            HashMap<Integer, Double> vegindex2RGLMap, HashMap<Integer, Double> vegindex2rsMap,
            HashMap<Integer, Double> vegindex2rarcMap ) throws ModelsIOException {
        Double columns = vegetationLibScalarSet.get(0);
        int vegIndexesNum = 56;
        if (columns != vegIndexesNum) {
            throw new ModelsIOException(
                    "The vegetation library scalarset contains a wrong number of columns. Check your data.",
                    this);
        }
        for( int i = 1; i < vegetationLibScalarSet.size(); i = i + vegIndexesNum ) {
            // 0-id,1-architectural_resistance,2-min_stomatal_resistance,
            int id = vegetationLibScalarSet.get(i + 0).intValue();
            double archResistance = vegetationLibScalarSet.get(i + 1);
            vegindex2rarcMap.put(id, archResistance);
            double minStomatalResistance = vegetationLibScalarSet.get(i + 2);
            vegindex2rsMap.put(id, minStomatalResistance);
            // 3-lai_jan,4-lai_feb,5-lai_mar,6-lai_apr,7-lai_maj,8-lai_jun,
            // 9-lai_jul,9-lai_aug,11-lai_sep,12-lai_oct,13-lai_nov,14-lai_dec
            double laiJan = vegetationLibScalarSet.get(i + 3);
            double laiFeb = vegetationLibScalarSet.get(i + 4);
            double laiMar = vegetationLibScalarSet.get(i + 5);
            double laiApr = vegetationLibScalarSet.get(i + 6);
            double laiMay = vegetationLibScalarSet.get(i + 7);
            double laiGiu = vegetationLibScalarSet.get(i + 8);
            double laiJul = vegetationLibScalarSet.get(i + 9);
            double laiAug = vegetationLibScalarSet.get(i + 10);
            double laiSep = vegetationLibScalarSet.get(i + 11);
            double laiOct = vegetationLibScalarSet.get(i + 12);
            double laiNov = vegetationLibScalarSet.get(i + 13);
            double laiDec = vegetationLibScalarSet.get(i + 14);
            HashMap<Integer, Double> laiMap = new HashMap<Integer, Double>();
            laiMap.put(1, laiJan);
            laiMap.put(2, laiFeb);
            laiMap.put(3, laiMar);
            laiMap.put(4, laiApr);
            laiMap.put(5, laiMay);
            laiMap.put(6, laiGiu);
            laiMap.put(7, laiJul);
            laiMap.put(8, laiAug);
            laiMap.put(9, laiSep);
            laiMap.put(10, laiOct);
            laiMap.put(11, laiNov);
            laiMap.put(12, laiDec);
            vegindex2laiMap.put(id, laiMap);
            // ALBEDO - for now not used
            // ,15-jan_albedo,16-feb_albedo,
            // 17-mar_albedo,18-apr_albedo,19-maj_albedo,20-jun_albedo,21-jul_albedo,
            // 22-ago_albedo,23-sep_albedo,24-oct_albedo,25-nov_albedo,26-dec_albedo,

            // 27-rough_jan,28-rough_feb,29-rough_mar,30-rough_apr,31-rough_maj,
            // 32-rough_jun,33-rough_jul,34-rough_ago,35-rough_sep,36-rough_oct,
            // 37-rough_nov,38-rough_dec
            double roughnessJan = vegetationLibScalarSet.get(i + 27);
            double roughnessFeb = vegetationLibScalarSet.get(i + 28);
            double roughnessMar = vegetationLibScalarSet.get(i + 29);
            double roughnessApr = vegetationLibScalarSet.get(i + 30);
            double roughnessMay = vegetationLibScalarSet.get(i + 31);
            double roughnessGiu = vegetationLibScalarSet.get(i + 32);
            double roughnessJul = vegetationLibScalarSet.get(i + 33);
            double roughnessAug = vegetationLibScalarSet.get(i + 34);
            double roughnessSep = vegetationLibScalarSet.get(i + 35);
            double roughnessOct = vegetationLibScalarSet.get(i + 36);
            double roughnessNov = vegetationLibScalarSet.get(i + 37);
            double roughnessDec = vegetationLibScalarSet.get(i + 38);
            HashMap<Integer, Double> roughnessMap = new HashMap<Integer, Double>();
            roughnessMap.put(1, roughnessJan);
            roughnessMap.put(2, roughnessFeb);
            roughnessMap.put(3, roughnessMar);
            roughnessMap.put(4, roughnessApr);
            roughnessMap.put(5, roughnessMay);
            roughnessMap.put(6, roughnessGiu);
            roughnessMap.put(7, roughnessJul);
            roughnessMap.put(8, roughnessAug);
            roughnessMap.put(9, roughnessSep);
            roughnessMap.put(10, roughnessOct);
            roughnessMap.put(11, roughnessNov);
            roughnessMap.put(12, roughnessDec);
            vegindex2roughnessMap.put(id, roughnessMap);

            // 39-displ_jan,40-displ_feb,41-displ_mar,
            // 42-displ_apr,43-displ_maj,44-displ_jun,45-displ_jul,46-displ_ago,
            // 47-displ_sep,48-displ_oct,49-displ_nov,50-displ_dec,
            double displacementJan = vegetationLibScalarSet.get(i + 39);
            double displacementFeb = vegetationLibScalarSet.get(i + 40);
            double displacementMar = vegetationLibScalarSet.get(i + 41);
            double displacementApr = vegetationLibScalarSet.get(i + 42);
            double displacementMay = vegetationLibScalarSet.get(i + 43);
            double displacementGiu = vegetationLibScalarSet.get(i + 44);
            double displacementJul = vegetationLibScalarSet.get(i + 45);
            double displacementAug = vegetationLibScalarSet.get(i + 46);
            double displacementSep = vegetationLibScalarSet.get(i + 47);
            double displacementOct = vegetationLibScalarSet.get(i + 48);
            double displacementNov = vegetationLibScalarSet.get(i + 49);
            double displacementDec = vegetationLibScalarSet.get(i + 50);
            HashMap<Integer, Double> displacementMap = new HashMap<Integer, Double>();
            displacementMap.put(1, displacementJan);
            displacementMap.put(2, displacementFeb);
            displacementMap.put(3, displacementMar);
            displacementMap.put(4, displacementApr);
            displacementMap.put(5, displacementMay);
            displacementMap.put(6, displacementGiu);
            displacementMap.put(7, displacementJul);
            displacementMap.put(8, displacementAug);
            displacementMap.put(9, displacementSep);
            displacementMap.put(10, displacementOct);
            displacementMap.put(11, displacementNov);
            displacementMap.put(12, displacementDec);
            vegindex2displacementMap.put(id, displacementMap);

            // 51-wind_height,
            // 52-rgl
            double rgl = vegetationLibScalarSet.get(i + 52);
            vegindex2RGLMap.put(id, rgl);
            // 53-rad_atten,54-wind_atten,55-trunk_ratio,
        }
    }

}
