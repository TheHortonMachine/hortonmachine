package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

import java.util.HashMap;

import org.locationtech.jts.geom.Coordinate;

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
        
        while( mc.doProcess ) {
            mc.process();
            
            HashMap<Integer, Coordinate> StationIds2CoordinateMap = mc.outStationIds2CoordinateMap;
            HashMap<Integer, double[]> dataMap = mc.outStationIds2ValueMap;
            HashMap<Integer, Coordinate> targetPointsIds2CoordinateMap = mc.outTargetPointsIds2CoordinateMap;
            HashMap<Integer, TargetPointAssociation> TargetPointId2AssociationMap = mc.outTargetPointId2AssociationMap;
            
            
            
        }
    }
}
