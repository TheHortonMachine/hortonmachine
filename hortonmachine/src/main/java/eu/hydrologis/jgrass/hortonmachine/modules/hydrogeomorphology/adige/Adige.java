///*
// * JGrass - Free Open Source Java GIS http://www.jgrass.org 
// * (C) HydroloGIS - www.hydrologis.com 
// * 
// * This library is free software; you can redistribute it and/or modify it under
// * the terms of the GNU Library General Public License as published by the Free
// * Software Foundation; either version 2 of the License, or (at your option) any
// * later version.
// * 
// * This library is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
// * details.
// * 
// * You should have received a copy of the GNU Library General Public License
// * along with this library; if not, write to the Free Foundation, Inc., 59
// * Temple Place, Suite 330, Boston, MA 02111-1307 USA
// */
//package eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige;
//
//import java.io.PrintStream;
//import java.text.DateFormat;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//
//import oms3.annotations.Description;
//import oms3.annotations.In;
//import oms3.annotations.Role;
//import oms3.annotations.Unit;
//
//import org.geotools.feature.FeatureCollection;
//import org.geotools.feature.FeatureIterator;
//import org.joda.time.DateTime;
//import org.opengis.feature.simple.SimpleFeature;
//import org.opengis.feature.simple.SimpleFeatureType;
//
//import static eu.hydrologis.jgrass.hortonmachine.libs.models.HMConstants.*;
//import eu.hydrologis.jgrass.hortonmachine.libs.exceptions.ModelsIOException;
//import eu.hydrologis.jgrass.hortonmachine.libs.exceptions.ModelsIllegalargumentException;
//import eu.hydrologis.jgrass.hortonmachine.libs.models.HMModel;
//import eu.hydrologis.jgrass.hortonmachine.libs.monitor.PrintStreamProgressMonitor;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.Dams;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.DischargeContributor;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.HillSlope;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.Hydrometers;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.NetBasinsManager;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.Offtakes;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.PfafstetterNumber;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core.Tributaries;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.duffy.DischargeDistributor;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.duffy.DuffyModel;
//import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.duffy.RungeKuttaFelberg;
//
///**
// * The adige model.
// * 
// * @author Silvia Franceschi (www.hydrologis.com)
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class Adige extends HMModel {
//
//    @Description("The hillslope data.")
//    @In
//    public FeatureCollection<SimpleFeatureType, SimpleFeature> hillslopeFC;
//
//    @Description("The a field name of the netnum attribute in the hillslope data.")
//    @In
//    public String netNumAttributeName = null;
//
//    @Description("The a field name of the baricenter elevation attribute in the hillslope data.")
//    @In
//    public String baricenterAttributeName = null;
//
//    @Description("The a field name of the soil use attribute in the hillslope data.")
//    @In
//    public String vegetationAttributeName = null;
//
//    @Description("The a field name of the avg_sub attribute in the hillslope data.")
//    @In
//    public String PARAMS_AVG_SUB = null;
//
//    @Description("The a field name of the var_sub attribute in the hillslope data.")
//    @In
//    public String PARAMS_VAR_SUB = null;
//
//    @Description("The a field name of the avg_sup_10 attribute in the hillslope data.")
//    @In
//    public String PARAMS_AVG_SUP_10 = null;
//
//    @Description("The a field name of the var_sup_10 attribute in the hillslope data.")
//    @In
//    public String PARAMS_VAR_SUP_10 = null;
//
//    @Description("The a field name of the avg_sup_30 attribute in the hillslope data.")
//    @In
//    public String PARAMS_AVG_SUP_30 = null;
//
//    @Description("The a field name of the var_sup_30 attribute in the hillslope data.")
//    @In
//    public String PARAMS_VAR_SUP_30 = null;
//
//    @Description("The a field name of the avg_sup_60 attribute in the hillslope data.")
//    @In
//    public String PARAMS_AVG_SUP_60 = null;
//
//    @Description("The a field name of the var_sup_60 attribute in the hillslope data.")
//    @In
//    public String PARAMS_VAR_SUP_60 = null;
//
//    @Description("The average speed for superficial runoff.")
//    @In
//    public double PARAMS_V_SUP = -1;
//
//    @Description("The average speed for sub-superficial runoff.")
//    @In
//    public double PARAMS_V_SUB = -1;
//
//    @Description("The hydrometers monitoring point data.")
//    @In
//    public FeatureCollection<SimpleFeatureType, SimpleFeature> hydrometersFC;
//
//    @Description("The dams monitoring point data.")
//    @In
//    public FeatureCollection<SimpleFeatureType, SimpleFeature> damsFC;
//
//    @Description("The tributary monitoring point data.")
//    @In
//    public FeatureCollection<SimpleFeatureType, SimpleFeature> tributaryFC;
//
//    @Description("The offtakes monitoring point data.")
//    @In
//    public FeatureCollection<SimpleFeatureType, SimpleFeature> offtakesFC;
//
//    @Description("Comma separated list of pfafstetter ids, in which to generate the output")
//    @In
//    public String outPfafIdsStrings = null;
//
//    @Description("The a field name of the monitoring point's id attribute in the monitoring points data.")
//    @In
//    public String monPointIdAttributeName = null;
//
//    @Description("The network data.")
//    @In
//    public FeatureCollection<SimpleFeatureType, SimpleFeature> netpfafFeatureCollection;
//
//    @Description("The a field name of the pfafstetter enumeration attribute in the network data.")
//    @In
//    public String pfaffAttributeName = null;
//
//    @Description("The a field name of the elevation of the starting point of a link in the network data.")
//    @In
//    public String startNetElevAttributeName = null;
//
//    @Description("The a field name of the elevation of the end point of a link in the network data.")
//    @In
//    public String endNetElevAttributeName = null;
//
//    @Role(Role.PARAMETER)
//    @Description("The routing model type to use.")
//    @In
//    public int routingType = 3;
//
//    @Role(Role.PARAMETER)
//    @Description("A constant value of rain intensity.")
//    @Unit("mm/h")
//    @In
//    public double rainIntensity;
//
//    @Role(Role.PARAMETER)
//    @Description("The duration of the constant rain.")
//    @Unit("min")
//    @In
//    public double rainDurationInMinutes;
//
//    @Role(Role.PARAMETER)
//    @Description("Switch to activate additional logging to file.")
//    @In
//    public boolean doLog = false;
//
//    
//    public DateTime currentTime;
//    // public String startDateArg = null;
//    // public String endDateArg = null;
//    // public double deltaTArg = null;
//
//    /*
//     * ATTRIBUTES FIELDS
//     */
//    private Date startDate;
//    private Date endDate;
//    private double rainEndDateInMinutes = -1;
//    private double deltaTinMilliSeconds;
//    private double deltaTinMinutes = -1;
//
//    /** the running rain array */
//    private double[] rainArray = null;
//    private double[] radiationArray;
//    private double[] netshortArray;
//    private double[] temperatureArray;
//    private double[] humidityArray;
//    private double[] windspeedArray;
//    private double[] pressureArray;
//    private double[] snowWaterEquivalentArray;
//    /** the running discharge array, which at the begin holds the initial conditions */
//    private double[] initialConditions = null;
//
//    private RungeKuttaFelberg rainRunoffRaining;
//    private List<PfafstetterNumber> netPfaffsList;
//    private DuffyModel duffyEvaluator;
//
//    // hydrometers
//    private DischargeContributor hydrometersHandler;
//    private HashMap<String, Integer> hydrometer_pfaff2idMap;
//    private HashMap<Integer, Double> hydrometer_id2valuesMap;
//
//    // dams
//    private DischargeContributor damsHandler;
//    private HashMap<String, Integer> dams_pfaff2idMap;
//    private HashMap<Integer, Double> dams_id2valuesQMap;
//
//    // tributaries
//    private DischargeContributor tributaryHandler;
//    private HashMap<String, Integer> tributary_pfaff2idMap;
//    private HashMap<Integer, Double> tributary_id2valuesQMap;
//
//    // offtakes
//    private DischargeContributor offtakesHandler;
//    private HashMap<String, Integer> offtakes_pfaff2idMap;
//    private HashMap<Integer, Double> offtakes_id2valuesQMap;
//
//    private HashMap<Integer, Integer> basinid2Index;
//    private HashMap<Integer, Integer> index2Basinid;
//    
//    private int hillsSlopeNum;
//    private int outletHillslopeId = -1;
//    private HashMap<String, Integer> pfaff2Index;
//    private int[] indexesArray;
//    private List<HillSlope> orderedHillslopes;
//
//    public void executeAdige() throws Exception {
//
//        if (rainIntensity != -1 && rainDurationInMinutes != -1) {
//            rainEndDateInMinutes = startDate.getTime() * MStM + rainDurationInMinutes;
//        } else if (rainIntensity != -1 && rainDurationInMinutes == -1) {
//            throw new ModelsIllegalargumentException(
//                    "In the case of usage of a constant rainintensity it is necessary to define also its duration.\nCheck your arguments, probably the --rainduration flag is missing.",
//                    this);
//        }
//
//        if (netNumAttributeName == null || netNumAttributeName.length() < 1) {
//            throw new ModelsIllegalargumentException("Missing net num attribute name.", this);
//        }
//        if (pfaffAttributeName == null || pfaffAttributeName.length() < 1) {
//            throw new ModelsIllegalargumentException("Missing pfafstetter attribute name.", this);
//        }
//        if (monPointIdAttributeName == null || monPointIdAttributeName.length() < 1) {
//            throw new ModelsIllegalargumentException("Missing monitoring point id attribute name.",
//                    this);
//        }
//        if (baricenterAttributeName == null || baricenterAttributeName.length() < 1) {
//            throw new ModelsIllegalargumentException("Missing basin centroid attribute name.", this);
//        }
//        if (startNetElevAttributeName == null || startNetElevAttributeName.length() < 1) {
//            throw new ModelsIllegalargumentException("Missing start net elevation attribute name.",
//                    this);
//        }
//        if (endNetElevAttributeName == null || endNetElevAttributeName.length() < 1) {
//            throw new ModelsIllegalargumentException("Missing start net elevation attribute name.",
//                    this);
//        }
//
//        // hydrometers input
//        vegetationInputEI = ModelsConstants.createDummyInputExchangeItem(this);
//        netpfafInputEI = ModelsConstants.createFeatureCollectionInputExchangeItem(this, null);
//        hillslopeInputEI = ModelsConstants.createFeatureCollectionInputExchangeItem(this, null);
//        hydrometersFeaturesInputEI = ModelsConstants.createFeatureCollectionInputExchangeItem(this,
//                null);
//        hydrometersDataInputEI = ModelsConstants.createDummyInputExchangeItem(this);
//        damsFeaturesInputEI = ModelsConstants.createFeatureCollectionInputExchangeItem(this, null);
//        damsOverflowDischargeInputEI = ModelsConstants.createDummyInputExchangeItem(this);
//        tributaryFeaturesInputEI = ModelsConstants.createFeatureCollectionInputExchangeItem(this,
//                null);
//        tributaryDischargeInputEI = ModelsConstants.createDummyInputExchangeItem(this);
//        offtakesFeaturesInputEI = ModelsConstants.createFeatureCollectionInputExchangeItem(this,
//                null);
//        offtakesDischargeInputEI = ModelsConstants.createDummyInputExchangeItem(this);
//        rainfallInputEI = ModelsConstants.createDummyInputExchangeItem(this);
//        boundaryInputEI = ModelsConstants.createDummyInputExchangeItem(this);
//        boundaryOutputEI = ModelsConstants.createDummyOutputExchangeItem(this);
//        dischargeOutputEI = ModelsConstants.createDummyOutputExchangeItem(this);
//        s1OutputEI = ModelsConstants.createDummyOutputExchangeItem(this);
//        s2OutputEI = ModelsConstants.createDummyOutputExchangeItem(this);
//        s3OutputEI = ModelsConstants.createDummyOutputExchangeItem(this);
//        basinrainOutputEI = ModelsConstants.createDummyOutputExchangeItem(this);
//    }
//
//    public IValueSet safeGetValues( ITime time, String linkID ) throws Exception {
//        double runningDateInMinutes = -1;
//        HydrologisDate tmpTime = null;
//        if (time instanceof HydrologisDate) {
//            tmpTime = (HydrologisDate) time;
//        } else {
//            throw new ModelsIllegalargumentException(
//                    "The model was launched without time interval or something is wrong in the time setting.",
//                    this);
//        }
//        /*
//         * check if the time is null or if it is the first implementation of the new timestep
//         */
//        if (currentTime == null || !(tmpTime.compareTo(currentTime) == 0)) {
//            currentTime = new HydrologisDate();
//            currentTime.setTime(tmpTime.getTime());
//            runningDateInMinutes = ((HydrologisDate) currentTime).getTime() * MStM;
//
//            // hydrometers
//            if (hydrometersFeaturesInputLink != null && hydrometersDataInputLink != null) {
//                if (hydrometersHandler == null) {
//                    out
//                            .println("Reading hydrometers geometries and mapping them to the network...");
//                    hydrometer_pfaff2idMap = new HashMap<String, Integer>();
//                    hydrometer_id2valuesMap = new HashMap<Integer, Double>();
//                    hydrometersHandler = new Hydrometers(hydrometer_pfaff2idMap,
//                            hydrometer_id2valuesMap);
//
//                    IValueSet hydrometersValueSet = hydrometersFeaturesInputLink
//                            .getSourceComponent().getValues(null,
//                                    hydrometersFeaturesInputLink.getID());
//                    if (hydrometersValueSet instanceof JGrassFeatureValueSet) {
//                        hydrometersFeatureCollection = ((JGrassFeatureValueSet) hydrometersValueSet)
//                                .getFeatureCollection();
//                    } else {
//                        throw new ModelsIllegalargumentException(
//                                "An error occurred while retrieving the hydrometers data at date: "
//                                        + dateFormatter.format(time), this);
//                    }
//                    if (hydrometersFeatureCollection == null) {
//                        throw new ModelsIllegalargumentException(
//                                "An error occurred while reading the hydrometers geometries", this);
//                    }
//                    FeatureIterator<SimpleFeature> hydrometersIterator = hydrometersFeatureCollection
//                            .features();
//                    int pfaffIndex = -1;
//                    int monIdIndex = -1;
//                    while( hydrometersIterator.hasNext() ) {
//                        SimpleFeature hydrometer = hydrometersIterator.next();
//                        if (pfaffIndex == -1) {
//                            SimpleFeatureType featureType = hydrometer.getFeatureType();
//                            pfaffIndex = featureType.indexOf(pfaffAttributeName);
//                            if (pfaffIndex == -1) {
//                                throw new ModelsIllegalargumentException(
//                                        "The hydrometer features are missing the pafaffstetter attribute field: "
//                                                + pfaffAttributeName, this);
//                            }
//                            monIdIndex = featureType.indexOf(monPointIdAttributeName);
//                            if (monIdIndex == -1) {
//                                throw new ModelsIllegalargumentException(
//                                        "The hydrometer features are missing the id attribute field: "
//                                                + monPointIdAttributeName, this);
//                            }
//                        }
//
//                        String pNumberStr = (String) hydrometer.getAttribute(pfaffIndex);
//                        int id = ((Number) hydrometer.getAttribute(monIdIndex)).intValue();
//                        hydrometer_pfaff2idMap.put(pNumberStr, id);
//                    }
//                }
//                // hydrometers values
//                IValueSet hydrometerValueSet = hydrometersDataInputLink.getSourceComponent()
//                        .getValues(time, hydrometersDataInputLink.getID());
//                if (hydrometerValueSet != null && hydrometerValueSet instanceof ScalarSet) {
//                    hydrometerScalarSet = (ScalarSet) hydrometerValueSet;
//                    hydrometer_id2valuesMap.clear();
//                    for( int i = 1; i < hydrometerScalarSet.size(); i = i + 2 ) {
//                        int id = hydrometerScalarSet.get(i).intValue();
//                        Double value = hydrometerScalarSet.get(i + 1);
//                        hydrometer_id2valuesMap.put(id, value);
//                    }
//                } else {
//                    throw new ModelsIllegalargumentException(
//                            "An error occurred while retrieving the hydrometers data at date: "
//                                    + dateFormatter.format(time), this);
//                }
//            }
//
//            // dams
//            if (damsFeaturesInputLink != null) {
//                if (damsHandler == null) {
//                    out.println("Reading dams geometries and mapping them to the network...");
//                    dams_pfaff2idMap = new HashMap<String, Integer>();
//                    dams_id2valuesQMap = new HashMap<Integer, Double>();
//                    damsHandler = new Dams(dams_pfaff2idMap, dams_id2valuesQMap);
//
//                    IValueSet damsValueSet = damsFeaturesInputLink.getSourceComponent().getValues(
//                            null, damsFeaturesInputLink.getID());
//                    if (damsValueSet instanceof JGrassFeatureValueSet) {
//                        damsFeatureCollection = ((JGrassFeatureValueSet) damsValueSet)
//                                .getFeatureCollection();
//                    } else {
//                        throw new ModelsIllegalargumentException(
//                                "An error occurred while retrieving the dams data at date: "
//                                        + dateFormatter.format(time), this);
//                    }
//                    if (damsFeatureCollection == null) {
//                        throw new ModelsIllegalargumentException(
//                                "An error occurred while reading the dams geometries", this);
//                    }
//                    FeatureIterator<SimpleFeature> damsIterator = damsFeatureCollection.features();
//                    int pfaffIndex = -1;
//                    int monIdIndex = -1;
//                    while( damsIterator.hasNext() ) {
//                        SimpleFeature dam = damsIterator.next();
//                        if (pfaffIndex == -1) {
//                            SimpleFeatureType featureType = dam.getFeatureType();
//                            pfaffIndex = featureType.indexOf(pfaffAttributeName);
//                            if (pfaffIndex == -1) {
//                                throw new ModelsIllegalargumentException(
//                                        "The dams features are missing the pfaffstetter attribute field: "
//                                                + pfaffAttributeName, this);
//                            }
//                            monIdIndex = featureType.indexOf(monPointIdAttributeName);
//                            if (monIdIndex == -1) {
//                                throw new ModelsIllegalargumentException(
//                                        "The dams features are missing the id attribute field: "
//                                                + monPointIdAttributeName, this);
//                            }
//                        }
//
//                        String pNumberStr = (String) dam.getAttribute(pfaffIndex);
//                        int id = ((Number) dam.getAttribute(monIdIndex)).intValue();
//                        dams_pfaff2idMap.put(pNumberStr, id);
//                    }
//                }
//                // dams discharge values
//                IValueSet damsDischargeValueSet = damsOverflowDischargeInputLink
//                        .getSourceComponent().getValues(time,
//                                damsOverflowDischargeInputLink.getID());
//                if (damsDischargeValueSet != null && damsDischargeValueSet instanceof ScalarSet) {
//                    damsQScalarSet = (ScalarSet) damsDischargeValueSet;
//                    dams_id2valuesQMap.clear();
//                    for( int i = 1; i < damsQScalarSet.size(); i = i + 2 ) {
//                        int id = damsQScalarSet.get(i).intValue();
//                        Double value = damsQScalarSet.get(i + 1);
//                        dams_id2valuesQMap.put(id, value);
//                    }
//                } else {
//                    throw new ModelsIllegalargumentException(
//                            "An error occurred while retrieving the dams discharge data at date: "
//                                    + dateFormatter.format(time), this);
//                }
//            }
//
//            // tributary
//            if (tributaryFeaturesInputLink != null) {
//                if (tributaryHandler == null) {
//                    out.println("Reading tributary geometries and mapping them to the network...");
//                    tributary_pfaff2idMap = new HashMap<String, Integer>();
//                    tributary_id2valuesQMap = new HashMap<Integer, Double>();
//                    tributaryHandler = new Tributaries(tributary_pfaff2idMap,
//                            tributary_id2valuesQMap);
//
//                    IValueSet tributaryValueSet = tributaryFeaturesInputLink.getSourceComponent()
//                            .getValues(null, tributaryFeaturesInputLink.getID());
//                    if (tributaryValueSet instanceof JGrassFeatureValueSet) {
//                        tributaryFeatureCollection = ((JGrassFeatureValueSet) tributaryValueSet)
//                                .getFeatureCollection();
//                    } else {
//                        throw new ModelsIllegalargumentException(
//                                "An error occurred while retrieving the tributary data at date: "
//                                        + dateFormatter.format(time), this);
//                    }
//                    if (tributaryFeatureCollection == null) {
//                        throw new ModelsIllegalargumentException(
//                                "An error occurred while reading the tributary geometries", this);
//                    }
//                    FeatureIterator<SimpleFeature> tributaryIterator = tributaryFeatureCollection
//                            .features();
//                    int pfaffIndex = -1;
//                    int monIdIndex = -1;
//                    while( tributaryIterator.hasNext() ) {
//                        SimpleFeature tributary = tributaryIterator.next();
//                        if (pfaffIndex == -1) {
//                            SimpleFeatureType featureType = tributary.getFeatureType();
//                            pfaffIndex = featureType.indexOf(pfaffAttributeName);
//                            if (pfaffIndex == -1) {
//                                throw new ModelsIllegalargumentException(
//                                        "The tributary features are missing the pfaffstetter attribute field: "
//                                                + pfaffAttributeName, this);
//                            }
//                            monIdIndex = featureType.indexOf(monPointIdAttributeName);
//                            if (monIdIndex == -1) {
//                                throw new ModelsIllegalargumentException(
//                                        "The tributary features are missing the id attribute field: "
//                                                + monPointIdAttributeName, this);
//                            }
//                        }
//
//                        String pNumberStr = (String) tributary.getAttribute(pfaffIndex);
//                        int id = ((Number) tributary.getAttribute(monIdIndex)).intValue();
//                        tributary_pfaff2idMap.put(pNumberStr, id);
//                    }
//                }
//                // dams discharge values
//                IValueSet tributaryDischargeValueSet = tributaryDischargeInputLink
//                        .getSourceComponent().getValues(time, tributaryDischargeInputLink.getID());
//                if (tributaryDischargeValueSet != null
//                        && tributaryDischargeValueSet instanceof ScalarSet) {
//                    tributaryQScalarSet = (ScalarSet) tributaryDischargeValueSet;
//                    tributary_id2valuesQMap.clear();
//                    for( int i = 1; i < tributaryQScalarSet.size(); i = i + 2 ) {
//                        int id = tributaryQScalarSet.get(i).intValue();
//                        Double value = tributaryQScalarSet.get(i + 1);
//                        tributary_id2valuesQMap.put(id, value);
//                    }
//                } else {
//                    throw new ModelsIllegalargumentException(
//                            "An error occurred while retrieving the tributary discharge data at date: "
//                                    + dateFormatter.format(time), this);
//                }
//            }
//
//            // offtakes
//            if (offtakesFeaturesInputLink != null) {
//                if (offtakesHandler == null) {
//                    out.println("Reading offtakes geometries and mapping them to the network...");
//                    offtakes_pfaff2idMap = new HashMap<String, Integer>();
//                    offtakes_id2valuesQMap = new HashMap<Integer, Double>();
//                    offtakesHandler = new Offtakes(offtakes_pfaff2idMap, offtakes_id2valuesQMap,
//                            out);
//
//                    IValueSet offtakesValueSet = offtakesFeaturesInputLink.getSourceComponent()
//                            .getValues(null, offtakesFeaturesInputLink.getID());
//                    if (offtakesValueSet instanceof JGrassFeatureValueSet) {
//                        offtakesFeatureCollection = ((JGrassFeatureValueSet) offtakesValueSet)
//                                .getFeatureCollection();
//                    } else {
//                        throw new ModelsIllegalargumentException(
//                                "An error occurred while retrieving the offtakes data at date: "
//                                        + dateFormatter.format(time), this);
//                    }
//                    if (offtakesFeatureCollection == null) {
//                        throw new ModelsIllegalargumentException(
//                                "An error occurred while reading the offtakes geometries", this);
//                    }
//                    FeatureIterator<SimpleFeature> offtakesIterator = offtakesFeatureCollection
//                            .features();
//                    int pfaffIndex = -1;
//                    int monIdIndex = -1;
//                    while( offtakesIterator.hasNext() ) {
//                        SimpleFeature offtakes = offtakesIterator.next();
//                        if (pfaffIndex == -1) {
//                            SimpleFeatureType featureType = offtakes.getFeatureType();
//                            pfaffIndex = featureType.indexOf(pfaffAttributeName);
//                            if (pfaffIndex == -1) {
//                                throw new ModelsIllegalargumentException(
//                                        "The offtakes features are missing the pfaffstetter attribute field: "
//                                                + pfaffAttributeName, this);
//                            }
//                            monIdIndex = featureType.indexOf(monPointIdAttributeName);
//                            if (monIdIndex == -1) {
//                                throw new ModelsIllegalargumentException(
//                                        "The offtakes features are missing the id attribute field: "
//                                                + monPointIdAttributeName, this);
//                            }
//                        }
//
//                        String pNumberStr = (String) offtakes.getAttribute(pfaffIndex);
//                        int id = ((Number) offtakes.getAttribute(monIdIndex)).intValue();
//                        offtakes_pfaff2idMap.put(pNumberStr, id);
//                    }
//                }
//                // dams discharge values
//                IValueSet offtakesDischargeValueSet = offtakesDischargeInputLink
//                        .getSourceComponent().getValues(time, offtakesDischargeInputLink.getID());
//                if (offtakesDischargeValueSet != null
//                        && offtakesDischargeValueSet instanceof ScalarSet) {
//                    offtakesQScalarSet = (ScalarSet) offtakesDischargeValueSet;
//                    offtakes_id2valuesQMap.clear();
//                    for( int i = 1; i < offtakesQScalarSet.size(); i = i + 2 ) {
//                        int id = offtakesQScalarSet.get(i).intValue();
//                        Double value = offtakesQScalarSet.get(i + 1);
//                        offtakes_id2valuesQMap.put(id, value);
//                    }
//                } else {
//                    throw new ModelsIllegalargumentException(
//                            "An error occurred while retrieving the offtakes discharge data at date: "
//                                    + dateFormatter.format(time), this);
//                }
//            }
//
//            // netpfaf
//            netpfafFeatureCollection = ModelsConstants.getFeatureCollectionFromLink(
//                    netpfafInputLink, time, err);
//            // hillslope
//            hillslopeFeatureCollection = ModelsConstants.getFeatureCollectionFromLink(
//                    hillslopeInputLink, time, err);
//            hillsSlopeNum = hillslopeFeatureCollection.size();
//
//            if (netPfaffsList == null) {
//                ScalarSet vegetationLibScalarSet = ModelsConstants.getScalarSetFromLink(
//                        vegetationInputLink, time, err);
//                HashMap<Integer, HashMap<Integer, Double>> vegindex2laiMap = new HashMap<Integer, HashMap<Integer, Double>>();
//                HashMap<Integer, HashMap<Integer, Double>> vegindex2displacementMap = new HashMap<Integer, HashMap<Integer, Double>>();
//                HashMap<Integer, HashMap<Integer, Double>> vegindex2roughnessMap = new HashMap<Integer, HashMap<Integer, Double>>();
//                HashMap<Integer, Double> vegindex2RGLMap = new HashMap<Integer, Double>();
//                HashMap<Integer, Double> vegindex2rsMap = new HashMap<Integer, Double>();
//                HashMap<Integer, Double> vegindex2rarcMap = new HashMap<Integer, Double>();
//
//                readVegetationLibrary(vegetationLibScalarSet, vegindex2laiMap,
//                        vegindex2displacementMap, vegindex2roughnessMap, vegindex2RGLMap,
//                        vegindex2rsMap, vegindex2rarcMap);
//
//                // at the first round create the hillslopes and network hierarchy
//                NetBasinsManager nbMan = new NetBasinsManager();
//                orderedHillslopes = nbMan.operateOnLayers(netpfafFeatureCollection,
//                        hillslopeFeatureCollection, netNumAttributeName, pfaffAttributeName,
//                        startNetElevAttributeName, endNetElevAttributeName,
//                        baricenterAttributeName, vegetationAttributeName, out);
//                HashMap<Integer, DischargeDistributor> hillslopeId2DischargeDistributor = new HashMap<Integer, DischargeDistributor>();
//                outletHillslopeId = orderedHillslopes.get(0).getHillslopeId();
//                netPfaffsList = new ArrayList<PfafstetterNumber>();
//                pfaff2Index = new HashMap<String, Integer>();
//                basinid2Index = new HashMap<Integer, Integer>();
//                index2Basinid = new HashMap<Integer, Integer>();
//                PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(out);
//                pm.beginTask("Analaysing hillslopes and calculating distribution curves...",
//                        orderedHillslopes.size());
//                for( int i = 0; i < orderedHillslopes.size(); i++ ) {
//                    HillSlope hillSlope = orderedHillslopes.get(i);
//                    if (vegindex2laiMap.size() > 0)
//                        hillSlope.parameters.setVegetationLibrary(vegindex2laiMap,
//                                vegindex2displacementMap, vegindex2roughnessMap, vegindex2RGLMap,
//                                vegindex2rsMap, vegindex2rarcMap);
//                    PfafstetterNumber pfafstetterNumber = hillSlope.getPfafstetterNumber();
//                    netPfaffsList.add(pfafstetterNumber);
//                    int hillslopeId = hillSlope.getHillslopeId();
//                    basinid2Index.put(hillslopeId, i);
//                    index2Basinid.put(i, hillslopeId);
//                    pfaff2Index.put(pfafstetterNumber.toString(), i);
//                    // the distributor
//                    HashMap<Integer, Double> params = fillParameters(hillSlope);
//                    System.out.println("Bacino: " + hillslopeId);
//                    hillslopeId2DischargeDistributor.put(hillslopeId, DischargeDistributor
//                            .createDischargeDistributor(DischargeDistributor.DISTRIBUTOR_TYPE_NASH,
//                                    startDate.getTime(), endDate.getTime(),
//                                    (long) deltaTinMilliSeconds, params));
//                    pm.worked(1);
//                }
//                pm.done();
//
//                duffyEvaluator = new DuffyModel(orderedHillslopes, routingType, out,
//                        deltaTinMinutes, doLog);
//                if (hydrometersHandler != null) {
//                    duffyEvaluator.addDischargeContributor(hydrometersHandler);
//                }
//                if (damsHandler != null) {
//                    duffyEvaluator.addDischargeContributor(damsHandler);
//                }
//                if (tributaryHandler != null) {
//                    duffyEvaluator.addDischargeContributor(tributaryHandler);
//                }
//                if (offtakesHandler != null) {
//                    duffyEvaluator.addDischargeContributor(offtakesHandler);
//                }
//                duffyEvaluator.addDischargeDistributor(hillslopeId2DischargeDistributor);
//                /*
//                 * read the initial conditions. 
//                 */
//                initialConditions = new double[hillsSlopeNum * 4];
//                ScalarSet boundaryInputSet = null;
//                if (boundaryInputLink != null) {
//                    boundaryInputSet = ModelsConstants.getScalarSetFromLink(boundaryInputLink,
//                            time, err);
//                }
//
//                if (boundaryInputSet != null) {
//                    int dataIndex = 0;
//                    for( int i = 0; i < (boundaryInputSet.size() - 1) / 5; i++ ) {
//                        Double idHillslope = boundaryInputSet.get(dataIndex + 1);
//                        Integer index = basinid2Index.get(idHillslope.intValue());
//                        if (index == null)
//                            continue;
//                        initialConditions[index] = boundaryInputSet.get(dataIndex + 2);
//                        initialConditions[index + hillsSlopeNum] = boundaryInputSet
//                                .get(dataIndex + 3);
//                        initialConditions[index + 2 * hillsSlopeNum] = boundaryInputSet
//                                .get(dataIndex + 4);
//                        initialConditions[index + 3 * hillsSlopeNum] = boundaryInputSet
//                                .get(dataIndex + 5);
//                        dataIndex = dataIndex + 5;
//                    }
//                } else {
//                    double dischargePerUnitArea = 0.01; // m3/s per km2 of upstream drainage area
//                    for( int i = 0; i < orderedHillslopes.size(); i++ ) {
//                        HillSlope currentHillslope = orderedHillslopes.get(i);
//                        // initialize with a default discharge per unit of drainage area in km2
//                        double hillslopeTotalDischarge = currentHillslope.getUpstreamArea(null)
//                                / 1000000.0 * dischargePerUnitArea;
//                        initialConditions[i] = 0.3 * hillslopeTotalDischarge;
//                        // initial subsuperficial flow is setted at a percentage of the total
//                        // discharge
//                        initialConditions[i + hillsSlopeNum] = 0.7 * hillslopeTotalDischarge;
//                        // initial water content in the saturated hillslope volume is setted to
//                        // have:
//                        // saturation surface at the 10% of the total area
//                        double maxSaturatedVolume = currentHillslope.parameters.getS2max();
//                        // initial water content in the non saturated hillslope volume is setted to
//                        initialConditions[i + 2 * hillsSlopeNum] = 0.2 * maxSaturatedVolume;
//                        initialConditions[i + 3 * hillsSlopeNum] = 0.25 * maxSaturatedVolume;
//                    }
//                }
//
//                // print of the initial conditions values, just for check
//                System.out.println("bacino\tQ\tQs\tS1\tS2");
//                for( int i = 0; i < hillsSlopeNum; i++ ) {
//                    int currentBasinId = index2Basinid.get(i);
//                    System.out.println(currentBasinId + "\t" + initialConditions[i] + "\t"
//                            + initialConditions[i + hillsSlopeNum] + "\t"
//                            + initialConditions[i + 2 * hillsSlopeNum] + "\t"
//                            + initialConditions[i + 3 * hillsSlopeNum]);
//                }
//
//                rainRunoffRaining = new RungeKuttaFelberg(duffyEvaluator, 1e-2, 10 / 60., out,
//                        doLog);
//            }
//
//            // deal with rain
//            if (rainIntensity != -1) {
//                /*
//                 * in the case of constant rain the array is build once and then used every time.
//                 * The only thing that changes, is that after the rainEndDate, the rain intensity is
//                 * set to 0.
//                 */
//                if (runningDateInMinutes > rainEndDateInMinutes) {
//                    rainArray = new double[netPfaffsList.size()];
//                    Arrays.fill(rainArray, 0);
//                } else {
//                    rainArray = new double[netPfaffsList.size()];
//                    Arrays.fill(rainArray, rainIntensity);
//                }
//
//            } else {
//                // read rainfall from input link scalar set and transform into a rainfall intensity
//                // [mm/h]
//                ScalarSet rainfallScalarSet = ModelsConstants.getScalarSetFromLink(
//                        rainfallInputLink, time, err);
//
//                rainArray = new double[hillsSlopeNum];
//                radiationArray = new double[hillsSlopeNum];
//                netshortArray = new double[hillsSlopeNum];
//                temperatureArray = new double[hillsSlopeNum];
//                humidityArray = new double[hillsSlopeNum];
//                windspeedArray = new double[hillsSlopeNum];
//                pressureArray = new double[hillsSlopeNum];
//                snowWaterEquivalentArray = new double[hillsSlopeNum];
//                for( int i = 1; i < rainfallScalarSet.size(); i = i + 9 ) {
//                    // rain
//                    Double basinId = rainfallScalarSet.get(i);
//                    Integer index = basinid2Index.get(basinId.intValue());
//                    if (index == null) {
//                        // System.out.println("Per il bacino " + basinId
//                        // + " non e' stata trovata una corrispondenza tra rete e bacini.");
//                        continue;
//                    }
//                    double rValue = rainfallScalarSet.get(i + 1);
//                    if (JGrassConstants.isNovalue(rValue)) {
//                        rValue = 0.0;
//                    }
//                    rainArray[index] = rValue / (deltaTinMinutes / 60.0);
//                    // radiation
//                    rValue = rainfallScalarSet.get(i + 2);
//                    radiationArray[index] = rValue;
//                    // netshort
//                    rValue = rainfallScalarSet.get(i + 3);
//                    netshortArray[index] = rValue;
//                    // temperature
//                    rValue = rainfallScalarSet.get(i + 4);
//                    temperatureArray[index] = rValue;
//                    // humidity
//                    rValue = rainfallScalarSet.get(i + 5);
//                    humidityArray[index] = rValue;
//                    // windspeed
//                    rValue = rainfallScalarSet.get(i + 6);
//                    windspeedArray[index] = rValue;
//                    // pressure
//                    rValue = rainfallScalarSet.get(i + 7);
//                    pressureArray[index] = rValue;
//                    // snow water equivalent
//                    rValue = rainfallScalarSet.get(i + 8);
//                    snowWaterEquivalentArray[index] = rValue;
//                }
//
//            }
//
//            double intervalStartTimeInMinutes = runningDateInMinutes;
//            double intervalEndTimeInMinutes = runningDateInMinutes + deltaTinMinutes;
//
//            rainRunoffRaining.solve(intervalStartTimeInMinutes, intervalEndTimeInMinutes, 1,
//                    initialConditions, rainArray, radiationArray, netshortArray, temperatureArray,
//                    humidityArray, windspeedArray, pressureArray, snowWaterEquivalentArray);
//            initialConditions = rainRunoffRaining.getFinalCond();
//            rainRunoffRaining.setBasicTimeStep(10 / 60.);
//        }
//        // return the output link -> create a chart with average rainfall and outlet discharge
//        if (linkID.equals(dischargeOutputLink.getID())) {
//            // Calculate the average rain on the basin
//            double avgRain = 0;
//            for( int i = 0; i < rainArray.length; i++ ) {
//                avgRain = avgRain + rainArray[i];
//            }
//            avgRain = avgRain / rainArray.length;
//
//            ScalarSet ret = new ScalarSet();
//            if (outPfafIdsStrings != null) {
//                int outNum = outPfafIdsStrings.length;
//                ret.add((double) (outNum + 1));
//
//                if (indexesArray == null) {
//                    indexesArray = new int[outNum];
//                    for( int i = 0; i < outPfafIdsStrings.length; i++ ) {
//                        String pfaf = outPfafIdsStrings[i];
//                        indexesArray[i] = pfaff2Index.get(pfaf);
//                    }
//                }
//                int pfafindex = 0;
//                for( int index : indexesArray ) {
//                    double dischargeToPrint = initialConditions[index]
//                            + initialConditions[index + hillsSlopeNum];
//                    ret.add(dischargeToPrint);
//                    double supdischargetoprint = initialConditions[index];
//                    ret.add(supdischargetoprint);
//                    double subdischargetoprint = initialConditions[index + hillsSlopeNum];
//                    ret.add(subdischargetoprint);
//                    System.out.println("Bacino: " + outPfafIdsStrings[pfafindex]
//                            + " Outlet Discharge " + initialConditions[index] + " qsub "
//                            + initialConditions[index + hillsSlopeNum] + " S1 "
//                            + initialConditions[index + 2 * hillsSlopeNum] + " S2 "
//                            + initialConditions[index + 3 * hillsSlopeNum] + " rain "
//                            + rainArray[index]);
//                    pfafindex++;
//                }
//            } else {
//                ret.add(4.0);
//                if (indexesArray == null) {
//                    indexesArray = new int[1];
//                    indexesArray[0] = basinid2Index.get(outletHillslopeId);
//                }
//                ret.add(initialConditions[indexesArray[0]]
//                        + initialConditions[indexesArray[0] + hillsSlopeNum]);
//                double supdischargetoprint = initialConditions[indexesArray[0]];
//                ret.add(supdischargetoprint);
//                double subdischargetoprint = initialConditions[indexesArray[0] + hillsSlopeNum];
//                ret.add(subdischargetoprint);
//                System.out.println("Outlet Discharge " + initialConditions[indexesArray[0]]
//                        + " qsub " + initialConditions[indexesArray[0] + hillsSlopeNum] + " S1 "
//                        + initialConditions[indexesArray[0] + 2 * hillsSlopeNum] + " S2 "
//                        + initialConditions[indexesArray[0] + 3 * hillsSlopeNum] + " rain "
//                        + avgRain);
//            }
//
//            // add also the average rainfall
//            // ret.add(avgRain);
//            return ret;
//        } else if (boundaryOutputLink != null && linkID.equals(boundaryOutputLink.getID())) {
//            ScalarSet outputBoundarySet = new ScalarSet();
//            outputBoundarySet.add((initialConditions.length / 4.0) * 5.0);
//            for( int i = 0; i < initialConditions.length / 4; i++ ) {
//                Integer basinId = index2Basinid.get(i);
//                outputBoundarySet.add(basinId.doubleValue());
//                outputBoundarySet.add(initialConditions[i]);
//                outputBoundarySet.add(initialConditions[i + hillsSlopeNum]);
//                outputBoundarySet.add(initialConditions[i + 2 * hillsSlopeNum]);
//                outputBoundarySet.add(initialConditions[i + 3 * hillsSlopeNum]);
//            }
//            return outputBoundarySet;
//        } else if (s1OutputLink != null && linkID.equals(s1OutputLink.getID())) {
//            ScalarSet outputS1Set = new ScalarSet();
//            if (outPfafIdsStrings != null) {
//                int outNum = outPfafIdsStrings.length;
//                outputS1Set.add((double) (outNum));
//                if (indexesArray == null) {
//                    indexesArray = new int[outNum];
//                    for( int i = 0; i < outPfafIdsStrings.length; i++ ) {
//                        String pfaf = outPfafIdsStrings[i];
//                        indexesArray[i] = pfaff2Index.get(pfaf);
//                    }
//                }
//                for( int index : indexesArray ) {
//                    outputS1Set.add(initialConditions[index + 2 * hillsSlopeNum]);
//                }
//            } else {
//                outputS1Set.add(1.0);
//                Integer outletIndex = basinid2Index.get(outletHillslopeId);
//                outputS1Set.add(initialConditions[outletIndex + 2 * hillsSlopeNum]);
//            }
//            return outputS1Set;
//        } else if (s2OutputLink != null && linkID.equals(s2OutputLink.getID())) {
//            ScalarSet outputS2Set = new ScalarSet();
//            if (outPfafIdsStrings != null) {
//                int outNum = outPfafIdsStrings.length;
//                outputS2Set.add((double) (outNum));
//                if (indexesArray == null) {
//                    indexesArray = new int[outNum];
//                    for( int i = 0; i < outPfafIdsStrings.length; i++ ) {
//                        String pfaf = outPfafIdsStrings[i];
//                        indexesArray[i] = pfaff2Index.get(pfaf);
//                    }
//                }
//                for( int index : indexesArray ) {
//                    outputS2Set.add(initialConditions[index + 3 * hillsSlopeNum]);
//                }
//            } else {
//                outputS2Set.add(1.0);
//                Integer outletIndex = basinid2Index.get(outletHillslopeId);
//                outputS2Set.add(initialConditions[outletIndex + 3 * hillsSlopeNum]);
//            }
//            return outputS2Set;
//        } else if (s3OutputLink != null && linkID.equals(s3OutputLink.getID())) {
//            ScalarSet outputS3Set = new ScalarSet();
//            if (outPfafIdsStrings != null) {
//                int outNum = outPfafIdsStrings.length;
//                outputS3Set.add((double) (outNum));
//                if (indexesArray == null) {
//                    indexesArray = new int[outNum];
//                    for( int i = 0; i < outPfafIdsStrings.length; i++ ) {
//                        String pfaf = outPfafIdsStrings[i];
//                        indexesArray[i] = pfaff2Index.get(pfaf);
//                    }
//                }
//                for( int index : indexesArray ) {
//                    HillSlope hillSlope = orderedHillslopes.get(index);
//                    outputS3Set.add(hillSlope.parameters.getS2Param()
//                            * (initialConditions[index + 3 * hillsSlopeNum]));
//                }
//            } else {
//                outputS3Set.add(1.0);
//                Integer outletIndex = basinid2Index.get(outletHillslopeId);
//                HillSlope hillSlope = orderedHillslopes.get(outletIndex);
//                outputS3Set.add(hillSlope.parameters.getS2Param()
//                        * (initialConditions[outletIndex + 3 * hillsSlopeNum]));
//            }
//            return outputS3Set;
//        } else if (basinrainOutputLink != null && linkID.equals(basinrainOutputLink.getID())) {
//            ScalarSet basinrainSet = new ScalarSet();
//            if (outPfafIdsStrings != null) {
//                int outNum = outPfafIdsStrings.length;
//                basinrainSet.add((double) (outNum));
//                if (indexesArray == null) {
//                    indexesArray = new int[outNum];
//                    for( int i = 0; i < outPfafIdsStrings.length; i++ ) {
//                        String pfaf = outPfafIdsStrings[i];
//                        indexesArray[i] = pfaff2Index.get(pfaf);
//                    }
//                }
//                for( int index : indexesArray ) {
//                    basinrainSet.add(rainArray[index]);
//                }
//            } else {
//                basinrainSet.add(1.0);
//                Integer outletIndex = basinid2Index.get(outletHillslopeId);
//                basinrainSet.add(rainArray[outletIndex + 3 * hillsSlopeNum]);
//            }
//            return basinrainSet;
//        }
//
//        return null;
//    }
//
//    private HashMap<Integer, Double> fillParameters( HillSlope hillSlope ) {
//        HashMap<Integer, Double> params = new HashMap<Integer, Double>();
//        // Double attribute = (Double)
//        // hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_10);
//        Double attribute = ((Number) hillSlope.getHillslopeFeature()
//                .getAttribute(PARAMS_AVG_SUP_10)).doubleValue();
//
//        params.put(DischargeDistributor.PARAMS_AVG_SUP_10, attribute);
//        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_30);
//        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_30))
//                .doubleValue();
//        params.put(DischargeDistributor.PARAMS_AVG_SUP_30, attribute);
//        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_60);
//        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUP_60))
//                .doubleValue();
//        params.put(DischargeDistributor.PARAMS_AVG_SUP_60, attribute);
//        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_10);
//        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_10))
//                .doubleValue();
//        params.put(DischargeDistributor.PARAMS_VAR_SUP_10, attribute);
//        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_30);
//        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_30))
//                .doubleValue();
//        params.put(DischargeDistributor.PARAMS_VAR_SUP_30, attribute);
//        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_60);
//        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUP_60))
//                .doubleValue();
//        params.put(DischargeDistributor.PARAMS_VAR_SUP_60, attribute);
//        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUB);
//        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(PARAMS_AVG_SUB))
//                .doubleValue();
//        params.put(DischargeDistributor.PARAMS_AVG_SUB, attribute);
//        // attribute = (Double) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUB);
//        attribute = ((Number) hillSlope.getHillslopeFeature().getAttribute(PARAMS_VAR_SUB))
//                .doubleValue();
//        params.put(DischargeDistributor.PARAMS_VAR_SUB, attribute);
//        try {
//            double vsup = Double.parseDouble(PARAMS_V_SUP);
//            params.put(DischargeDistributor.PARAMS_V_SUP, vsup);
//            double vsub = Double.parseDouble(PARAMS_V_SUB);
//            params.put(DischargeDistributor.PARAMS_V_SUB, vsub);
//        } catch (NumberFormatException e) {
//            throw new ModelsIllegalargumentException(
//                    "The speed parameters need to be a valid number. Check your syntax.", this);
//        }
//        return params;
//    }
//
//    private void readVegetationLibrary( ScalarSet vegetationLibScalarSet,
//            HashMap<Integer, HashMap<Integer, Double>> vegindex2laiMap,
//            HashMap<Integer, HashMap<Integer, Double>> vegindex2displacementMap,
//            HashMap<Integer, HashMap<Integer, Double>> vegindex2roughnessMap,
//            HashMap<Integer, Double> vegindex2RGLMap, HashMap<Integer, Double> vegindex2rsMap,
//            HashMap<Integer, Double> vegindex2rarcMap ) throws ModelsIOException {
//        Double columns = vegetationLibScalarSet.get(0);
//        int vegIndexesNum = 56;
//        if (columns != vegIndexesNum) {
//            throw new ModelsIOException(
//                    "The vegetation library scalarset contains a wrong number of columns. Check your data.",
//                    this);
//        }
//        for( int i = 1; i < vegetationLibScalarSet.size(); i = i + vegIndexesNum ) {
//            // 0-id,1-architectural_resistance,2-min_stomatal_resistance,
//            int id = vegetationLibScalarSet.get(i + 0).intValue();
//            double archResistance = vegetationLibScalarSet.get(i + 1);
//            vegindex2rarcMap.put(id, archResistance);
//            double minStomatalResistance = vegetationLibScalarSet.get(i + 2);
//            vegindex2rsMap.put(id, minStomatalResistance);
//            // 3-lai_jan,4-lai_feb,5-lai_mar,6-lai_apr,7-lai_maj,8-lai_jun,
//            // 9-lai_jul,9-lai_aug,11-lai_sep,12-lai_oct,13-lai_nov,14-lai_dec
//            double laiJan = vegetationLibScalarSet.get(i + 3);
//            double laiFeb = vegetationLibScalarSet.get(i + 4);
//            double laiMar = vegetationLibScalarSet.get(i + 5);
//            double laiApr = vegetationLibScalarSet.get(i + 6);
//            double laiMay = vegetationLibScalarSet.get(i + 7);
//            double laiGiu = vegetationLibScalarSet.get(i + 8);
//            double laiJul = vegetationLibScalarSet.get(i + 9);
//            double laiAug = vegetationLibScalarSet.get(i + 10);
//            double laiSep = vegetationLibScalarSet.get(i + 11);
//            double laiOct = vegetationLibScalarSet.get(i + 12);
//            double laiNov = vegetationLibScalarSet.get(i + 13);
//            double laiDec = vegetationLibScalarSet.get(i + 14);
//            HashMap<Integer, Double> laiMap = new HashMap<Integer, Double>();
//            laiMap.put(1, laiJan);
//            laiMap.put(2, laiFeb);
//            laiMap.put(3, laiMar);
//            laiMap.put(4, laiApr);
//            laiMap.put(5, laiMay);
//            laiMap.put(6, laiGiu);
//            laiMap.put(7, laiJul);
//            laiMap.put(8, laiAug);
//            laiMap.put(9, laiSep);
//            laiMap.put(10, laiOct);
//            laiMap.put(11, laiNov);
//            laiMap.put(12, laiDec);
//            vegindex2laiMap.put(id, laiMap);
//            // ALBEDO - for now not used
//            // ,15-jan_albedo,16-feb_albedo,
//            // 17-mar_albedo,18-apr_albedo,19-maj_albedo,20-jun_albedo,21-jul_albedo,
//            // 22-ago_albedo,23-sep_albedo,24-oct_albedo,25-nov_albedo,26-dec_albedo,
//
//            // 27-rough_jan,28-rough_feb,29-rough_mar,30-rough_apr,31-rough_maj,
//            // 32-rough_jun,33-rough_jul,34-rough_ago,35-rough_sep,36-rough_oct,
//            // 37-rough_nov,38-rough_dec
//            double roughnessJan = vegetationLibScalarSet.get(i + 27);
//            double roughnessFeb = vegetationLibScalarSet.get(i + 28);
//            double roughnessMar = vegetationLibScalarSet.get(i + 29);
//            double roughnessApr = vegetationLibScalarSet.get(i + 30);
//            double roughnessMay = vegetationLibScalarSet.get(i + 31);
//            double roughnessGiu = vegetationLibScalarSet.get(i + 32);
//            double roughnessJul = vegetationLibScalarSet.get(i + 33);
//            double roughnessAug = vegetationLibScalarSet.get(i + 34);
//            double roughnessSep = vegetationLibScalarSet.get(i + 35);
//            double roughnessOct = vegetationLibScalarSet.get(i + 36);
//            double roughnessNov = vegetationLibScalarSet.get(i + 37);
//            double roughnessDec = vegetationLibScalarSet.get(i + 38);
//            HashMap<Integer, Double> roughnessMap = new HashMap<Integer, Double>();
//            roughnessMap.put(1, roughnessJan);
//            roughnessMap.put(2, roughnessFeb);
//            roughnessMap.put(3, roughnessMar);
//            roughnessMap.put(4, roughnessApr);
//            roughnessMap.put(5, roughnessMay);
//            roughnessMap.put(6, roughnessGiu);
//            roughnessMap.put(7, roughnessJul);
//            roughnessMap.put(8, roughnessAug);
//            roughnessMap.put(9, roughnessSep);
//            roughnessMap.put(10, roughnessOct);
//            roughnessMap.put(11, roughnessNov);
//            roughnessMap.put(12, roughnessDec);
//            vegindex2roughnessMap.put(id, roughnessMap);
//
//            // 39-displ_jan,40-displ_feb,41-displ_mar,
//            // 42-displ_apr,43-displ_maj,44-displ_jun,45-displ_jul,46-displ_ago,
//            // 47-displ_sep,48-displ_oct,49-displ_nov,50-displ_dec,
//            double displacementJan = vegetationLibScalarSet.get(i + 39);
//            double displacementFeb = vegetationLibScalarSet.get(i + 40);
//            double displacementMar = vegetationLibScalarSet.get(i + 41);
//            double displacementApr = vegetationLibScalarSet.get(i + 42);
//            double displacementMay = vegetationLibScalarSet.get(i + 43);
//            double displacementGiu = vegetationLibScalarSet.get(i + 44);
//            double displacementJul = vegetationLibScalarSet.get(i + 45);
//            double displacementAug = vegetationLibScalarSet.get(i + 46);
//            double displacementSep = vegetationLibScalarSet.get(i + 47);
//            double displacementOct = vegetationLibScalarSet.get(i + 48);
//            double displacementNov = vegetationLibScalarSet.get(i + 49);
//            double displacementDec = vegetationLibScalarSet.get(i + 50);
//            HashMap<Integer, Double> displacementMap = new HashMap<Integer, Double>();
//            displacementMap.put(1, displacementJan);
//            displacementMap.put(2, displacementFeb);
//            displacementMap.put(3, displacementMar);
//            displacementMap.put(4, displacementApr);
//            displacementMap.put(5, displacementMay);
//            displacementMap.put(6, displacementGiu);
//            displacementMap.put(7, displacementJul);
//            displacementMap.put(8, displacementAug);
//            displacementMap.put(9, displacementSep);
//            displacementMap.put(10, displacementOct);
//            displacementMap.put(11, displacementNov);
//            displacementMap.put(12, displacementDec);
//            vegindex2displacementMap.put(id, displacementMap);
//
//            // 51-wind_height,
//            // 52-rgl
//            double rgl = vegetationLibScalarSet.get(i + 52);
//            vegindex2RGLMap.put(id, rgl);
//            // 53-rad_atten,54-wind_atten,55-trunk_ratio,
//        }
//    }
//    public void addLink( ILink link ) {
//        String id = link.getID();
//
//        if (id.equals(dischargeOutputID)) {
//            dischargeOutputLink = link;
//        } else if (id.equals(hydrometersDataInputID)) {
//            hydrometersDataInputLink = link;
//        } else if (id.equals(hydrometersFeaturesInputID)) {
//            hydrometersFeaturesInputLink = link;
//        } else if (id.equals(netpfafInputID)) {
//            netpfafInputLink = link;
//        } else if (id.equals(hillslopeInputID)) {
//            hillslopeInputLink = link;
//        } else if (id.equals(rainfallInputID)) {
//            rainfallInputLink = link;
//        } else if (id.equals(boundaryInputID)) {
//            boundaryInputLink = link;
//        } else if (id.equals(boundaryOutputID)) {
//            boundaryOutputLink = link;
//        } else if (id.equals(damsFeaturesInputID)) {
//            damsFeaturesInputLink = link;
//        } else if (id.equals(damsOverflowDischargeInputID)) {
//            damsOverflowDischargeInputLink = link;
//        } else if (id.equals(tributaryFeaturesInputID)) {
//            tributaryFeaturesInputLink = link;
//        } else if (id.equals(tributaryDischargeInputID)) {
//            tributaryDischargeInputLink = link;
//        } else if (id.equals(offtakesFeaturesInputLink)) {
//            offtakesFeaturesInputLink = link;
//        } else if (id.equals(offtakesDischargeInputID)) {
//            offtakesDischargeInputLink = link;
//        } else if (id.equals(vegetationInputID)) {
//            vegetationInputLink = link;
//        } else if (id.equals(s1OutputID)) {
//            s1OutputLink = link;
//        } else if (id.equals(s2OutputID)) {
//            s2OutputLink = link;
//        } else if (id.equals(s3OutputID)) {
//            s3OutputLink = link;
//        } else if (id.equals(basinrainOutputID)) {
//            basinrainOutputLink = link;
//        }
//    }
//
//    public void finish() {
//    }
//
//    public IInputExchangeItem getInputExchangeItem( int inputExchangeItemIndex ) {
//        if (inputExchangeItemIndex == 0) {
//            return hillslopeInputEI;
//        }
//        if (inputExchangeItemIndex == 1) {
//            return netpfafInputEI;
//        }
//        if (inputExchangeItemIndex == 2) {
//            return rainfallInputEI;
//        }
//        if (inputExchangeItemIndex == 3) {
//            return boundaryInputEI;
//        }
//        if (inputExchangeItemIndex == 4) {
//            return hydrometersDataInputEI;
//        }
//        if (inputExchangeItemIndex == 5) {
//            return hydrometersFeaturesInputEI;
//        }
//        if (inputExchangeItemIndex == 6) {
//            return damsOverflowDischargeInputEI;
//        }
//        if (inputExchangeItemIndex == 7) {
//            return damsFeaturesInputEI;
//        }
//        if (inputExchangeItemIndex == 8) {
//            return tributaryDischargeInputEI;
//        }
//        if (inputExchangeItemIndex == 9) {
//            return tributaryFeaturesInputEI;
//        }
//        if (inputExchangeItemIndex == 10) {
//            return offtakesDischargeInputEI;
//        }
//        if (inputExchangeItemIndex == 11) {
//            return offtakesFeaturesInputEI;
//        }
//        if (inputExchangeItemIndex == 12) {
//            return vegetationInputEI;
//        }
//        return null;
//    }
//
//    public int getInputExchangeItemCount() {
//        return 13;
//    }
//
//    public String getModelDescription() {
//        return modelParameters;
//    }
//
//    public IOutputExchangeItem getOutputExchangeItem( int outputExchangeItemIndex ) {
//        if (outputExchangeItemIndex == 0) {
//            return dischargeOutputEI;
//        } else if (outputExchangeItemIndex == 1) {
//            return boundaryOutputEI;
//        } else if (outputExchangeItemIndex == 2) {
//            return s1OutputEI;
//        } else if (outputExchangeItemIndex == 3) {
//            return s2OutputEI;
//        } else if (outputExchangeItemIndex == 4) {
//            return s3OutputEI;
//        } else if (outputExchangeItemIndex == 5) {
//            return basinrainOutputEI;
//        } else {
//            return null;
//        }
//
//    }
//
//    public int getOutputExchangeItemCount() {
//        return 6;
//    }
//
//    public void removeLink( String linkID ) {
//        if (linkID.equals(hydrometersDataInputLink.getID())) {
//            hydrometersDataInputLink = null;
//        } else if (linkID.equals(hydrometersFeaturesInputLink.getID())) {
//            hydrometersFeaturesInputLink = null;
//        } else if (linkID.equals(dischargeOutputLink.getID())) {
//            dischargeOutputLink = null;
//        } else if (linkID.equals(netpfafInputLink.getID())) {
//            netpfafInputLink = null;
//        } else if (linkID.equals(hillslopeInputLink.getID())) {
//            hillslopeInputLink = null;
//        } else if (linkID.equals(rainfallInputLink.getID())) {
//            rainfallInputLink = null;
//        } else if (linkID.equals(boundaryInputLink.getID())) {
//            boundaryInputLink = null;
//        } else if (linkID.equals(boundaryOutputLink.getID())) {
//            boundaryOutputLink = null;
//        } else if (linkID.equals(damsFeaturesInputLink.getID())) {
//            damsFeaturesInputLink = null;
//        } else if (linkID.equals(damsOverflowDischargeInputLink.getID())) {
//            damsOverflowDischargeInputLink = null;
//        } else if (linkID.equals(tributaryDischargeInputLink.getID())) {
//            tributaryDischargeInputLink = null;
//        } else if (linkID.equals(tributaryFeaturesInputLink.getID())) {
//            tributaryFeaturesInputLink = null;
//        } else if (linkID.equals(offtakesFeaturesInputLink.getID())) {
//            offtakesFeaturesInputLink = null;
//        } else if (linkID.equals(offtakesDischargeInputLink.getID())) {
//            offtakesDischargeInputLink = null;
//        } else if (linkID.equals(vegetationInputLink.getID())) {
//            vegetationInputLink = null;
//        } else if (linkID.equals(s1OutputLink.getID())) {
//            s1OutputLink = null;
//        } else if (linkID.equals(s2OutputLink.getID())) {
//            s2OutputLink = null;
//        } else if (linkID.equals(s3OutputLink.getID())) {
//            s3OutputLink = null;
//        } else if (linkID.equals(basinrainOutputLink.getID())) {
//            basinrainOutputLink = null;
//        }
//    }
//
//}
