package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

import java.util.ArrayList;
import java.util.List;



public class TargetPointAssociation {
    public int targetPointId;
    
    public List<Integer> stationIds = new ArrayList<>();
    
    public List<Double> stationDistances = new ArrayList<>();
    
    public InterpolationType interpolationType;

}
