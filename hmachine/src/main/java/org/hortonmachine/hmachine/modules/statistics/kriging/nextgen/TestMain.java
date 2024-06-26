package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

import static org.hortonmachine.gears.libs.modules.Variables.KRIGING_EXPERIMENTAL_VARIOGRAM;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class TestMain {
    public static void main( String[] args ) throws Exception {
        String stations = "/home/hydrologis/TMP/UNITN/newGenKriging/shapefile/stations_utm.shp";
        String targets = "/home/hydrologis/TMP/UNITN/newGenKriging/shapefile/centroids_utm.shp";
//        String data = "/home/hydrologis/TMP/UNITN/newGenKriging/01_input_kriging_hourly_allnodata.csv";
//        String data = "/home/hydrologis/TMP/UNITN/newGenKriging/04_input_kriging_hourly_twovalues.csv";
//        String data = "/home/hydrologis/TMP/UNITN/newGenKriging/03_input_kriging_hourly_onevalue.csv";
//        String data = "/home/hydrologis/TMP/UNITN/newGenKriging/05_input_kriging_hourly_allconstantvalue.csv";
        String data = "/home/hydrologis/TMP/UNITN/newGenKriging/06_input_kriging_hourly_morevalues.csv";

        OmsMeasurementsDataCoach mc = new OmsMeasurementsDataCoach();
        mc.inStations = stations;
        mc.inInterpolate = targets;
        mc.inMeasurements = data;
        mc.fStationsid = "ID";
        mc.fStationsZ = "elev_m";
        mc.fInterpolateid = "basinid";
        mc.fInterpolatedZ = "elev_m";

        mc.pMaxDistKriging = 10_000.0;
        mc.pMaxDistIdw = 100_000.0;
        mc.pMaxClosestStationsNum = 10;

        mc.initProcess();

        OmsKrigingInterpolator interpolator = new OmsKrigingInterpolator();
        interpolator.pMode = KRIGING_EXPERIMENTAL_VARIOGRAM;

        int timestepCount = 1;

        while( mc.doProcess ) {
            mc.process();
            StringBuilder sbStats = new StringBuilder();
            sbStats.append("Timestep " + timestepCount + "\n");

            interpolator.inStationIds2CoordinateMap = mc.outStationIds2CoordinateMap;
            interpolator.inStationIds2ValueMap = mc.outStationIds2ValueMap;
            interpolator.inPreviousStationIds2ValueMaps = mc.outPreviousStationIds2ValueMaps;
            interpolator.inTargetPointId2AssociationMap = mc.outTargetPointId2AssociationMap;
            interpolator.inTargetPointsIds2CoordinateMap = mc.outTargetPointsIds2CoordinateMap;

            interpolator.process();

            HashMap<Integer, double[]> interpolatedTargetDataMap = interpolator.outTargetIds2ValueMap;

            if (interpolatedTargetDataMap.size() > 0) {
                StringBuilder sbData = new StringBuilder();
                for( Entry<Integer, double[]> entry : interpolatedTargetDataMap.entrySet() ) {
                    Integer targetId = entry.getKey();
                    double value = entry.getValue()[0];
                    sbStats.append("\tInvolved stations with values for target id:").append(targetId).append("\n");

                    TargetPointAssociation association = mc.outTargetPointId2AssociationMap.get(targetId);
                    sbStats.append("\tInterpolation method used:").append(association.interpolationType).append("\n");

                    sbData.append("Target point: ").append(targetId).append(";");
                    sbData.append("Interpolated value: ").append(value).append("\n");
                    List<Integer> stationIds = association.stationIds;
                    List<Double> distances = association.stationDistances;
                    for( int i = 0; i < stationIds.size(); i++ ) {
                        Integer stationId = stationIds.get(i);
                        double distance = distances.get(i);
                        double stationValue = mc.outStationIds2ValueMap.get(stationId)[0];
                        sbStats.append("\t\tstatid:").append(stationId).append("\tvalue: ").append(stationValue)
                                .append("\tdistance: ").append(distance).append("\n");
                    }
                    sbStats.append("\n");
                }
            } else {
                sbStats.append("\tNo valid data found\n");
            }
            System.out.println(sbStats);
            timestepCount++;
        }
    }
}
