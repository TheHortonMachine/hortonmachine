package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.network.extractnetwork.ExtractNetwork;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
/**
 * It test the {@link ExtractNetwork} module with mode=1.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestExtractNetwork1 extends HMTestCase{
    public void testExtractNetworks1() throws Exception {
        
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);
        double[][] slopeData = HMTestMaps.slopeData;
        GridCoverage2D slopeCoverage = CoverageUtilities.buildCoverage("slope", slopeData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        ExtractNetwork extractNetwork = new ExtractNetwork();
        extractNetwork.pm = pm;
        extractNetwork.inFlow = flowCoverage;
        extractNetwork.inTca = tcaCoverage;
        extractNetwork.inSlope = slopeCoverage;
        extractNetwork.pMode = 1;
        extractNetwork.pThres = 10;
        extractNetwork.doNetfc = true;
        
        extractNetwork.process();

        GridCoverage2D networkCoverage = extractNetwork.outNet;
        checkMatrixEqual(networkCoverage.getRenderedImage(), HMTestMaps.extractNet1Data, 0.01);
        
        SimpleFeatureCollection networkFC = extractNetwork.outNetfc;
        
        FeatureIterator<SimpleFeature> featureIterator = networkFC.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coordinate[] coordinates = geometry.getCoordinates();
            System.out.println("Coords of feature: " + feature.getID());
            for( Coordinate coordinate : coordinates ) {
                System.out.println(coordinate);
            }
        }
        networkFC.close(featureIterator);
    }

}
