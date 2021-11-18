package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class TestMain {
    public static void main( String[] args ) throws Exception {
        String stations = "";
        String targets= "";
        String data = "";
        
        OmsMeasurementsDataCoach mc = new OmsMeasurementsDataCoach();
        mc.inStations = stations;
        mc.inInterpolate = targets;
        mc.inMeasurements = data;
        mc.fStationsid = "";
        mc.fStationsZ = "";
        mc.fInterpolateid = "";
        mc.fInterpolatedZ = "";
        
        mc.pMaxDist = 1000.0;
        mc.pMaxClosestStationsNum = 10;
        
        OmsKrigingInterpolator interpolator = new OmsKrigingInterpolator();
        
        while( mc.doProcess ) {
            mc.process();
            
            interpolator.inStationIds2CoordinateMap = mc.outStationIds2CoordinateMap;
            interpolator.inStationIds2ValueMap = mc.outStationIds2ValueMap;
            interpolator.inTargetPointId2AssociationMap = mc.outTargetPointId2AssociationMap;
            interpolator.inTargetPointsIds2CoordinateMap = mc.outTargetPointsIds2CoordinateMap;
            
            interpolator.process();
            
            HashMap<Integer, double[]> interpolatedTargetDataMap = interpolator.outTargetIds2ValueMap;
            
            StringBuilder sb = new StringBuilder();
            for( Entry<Integer, double[]> entry : interpolatedTargetDataMap.entrySet() ) {
                Integer targetId = entry.getKey();
                double value = entry.getValue()[0];
                
                sb.append("Target point: ").append(targetId).append("\n");
                sb.append("Interpolated value: ").append(value).append("\n");
                sb.append("Involved stations with values:").append("\n");
                TargetPointAssociation association = mc.outTargetPointId2AssociationMap.get(targetId);
                List<Integer> stationIds = association.stationIds;
                List<Double> distances = association.stationDistances;
                for( int i = 0; i < stationIds.size(); i++ ) {
                    Integer stationId = stationIds.get(i);
                    double distance = distances.get(i);
                    double stationValue = mc.outStationIds2ValueMap.get(stationId)[0];
                    sb.append("\tid:").append(stationId).append("\tvalue: ").append(stationValue).append("\tdistance").append(distance).append("\n");
                }
            }
        }
    }
}
