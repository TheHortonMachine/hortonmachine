package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import eu.hydrologis.jgrass.hortonmachine.modules.network.extractnetwork.ExtractNetwork;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;
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
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs);
        double[][] slopeData = HMTestMaps.slopeData;
        GridCoverage2D slopeCoverage = CoverageUtilities.buildCoverage("slope", slopeData, envelopeParams, crs);

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
        
        FeatureCollection<SimpleFeatureType, SimpleFeature> networkFC = extractNetwork.outNetfc;
        
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
