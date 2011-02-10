package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.flow.FlowDirections;
import org.jgrasstools.hortonmachine.modules.geomorphology.slope.Slope;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Tests the {@link Slope} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestSlope extends HMTestCase {

    public void testSlope() throws Exception {

        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("elevation", pitData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        // first create the needed map of flowdirections
        FlowDirections flow = new FlowDirections();
        flow.pm = pm;
        flow.inPit = pitfillerCoverage;
        flow.process();

        // then create the slope map mode 0
        Slope slope = new Slope();
        slope.inDem = pitfillerCoverage;
        slope.inFlow = flow.outFlow;
        slope.doRadiants=false;
        slope.pm = pm;

        slope.process();

        GridCoverage2D slopeCoverage_mode0 = slope.outSlope;
        checkMatrixEqual(slopeCoverage_mode0.getRenderedImage(), HMTestMaps.slopeData, 0.01);
        
        double [][] slope_data_in_radiant=new double [HMTestMaps.slopeData.length][HMTestMaps.slopeData[0].length];
        for(int i=0;i<HMTestMaps.slopeData.length;i++){
        	for(int j=0;j<HMTestMaps.slopeData[0].length;j++){
        		
        		if(HMTestMaps.slopeData[i][j]!=HMTestMaps.slopeData[i][j]){
        			
        			slope_data_in_radiant[i][j]=doubleNovalue;
        		}
        		else{
        			slope_data_in_radiant[i][j]=Math.atan(HMTestMaps.slopeData[i][j])*Math.PI/180.0;
        			
        		}
        		
        	}
        	
        }	
            Slope slope1 = new Slope();
            slope1.inDem = pitfillerCoverage;
            slope1.inFlow = flow.outFlow;
            slope1.doRadiants=true;
            slope1.pm = pm;

            slope1.process();

            GridCoverage2D slopeCoverage_mode1 = slope1.outSlope;
            checkMatrixEqual(slopeCoverage_mode1.getRenderedImage(), slope_data_in_radiant, 0.01);        	
        
        
        
        
    }

}
