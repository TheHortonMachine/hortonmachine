package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

import eu.hydrologis.jgrass.hortonmachine.modules.demmanipulation.pitfiller.Pitfiller;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;

/**
 * Test the {@link Pitfiller} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestPitfiller extends HMTestCase {
    public void testPitfiller() throws Exception {

        // Locale.setDefault(Locale.ITALIAN);

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation",
                elevationData, envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        Pitfiller pitfiller = new Pitfiller();
        pitfiller.inDem = elevationCoverage;
        pitfiller.pm = pm;

        pitfiller.process();

        GridCoverage2D pitfillerCoverage = pitfiller.outPit;

        checkMatrixEqual(pitfillerCoverage.getRenderedImage(), HMTestMaps.outPitData, 0);
    }

    public void testPitfillerGeotoolsProcessMode() throws Exception {

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation",
                elevationData, envelopeParams, crs);

        Pitfiller pitfiller = new Pitfiller();
        Map<String, Object> inputMap = new HashMap<String, Object>();
        inputMap.put("inDem", elevationCoverage);

        // quite dummy listener
        ProgressListener listener = new ProgressListener(){
            public void warningOccurred( String source, String location, String warning ) {
                System.out.println(warning);
            }
            
            public void started() {
                System.out.println("Started");
            }
            
            public void setTask( InternationalString task ) {
            }
            
            public void setDescription( String description ) {
                System.out.println(description);
            }
            
            public void setCanceled( boolean cancel ) {
            }
            
            public void progress( float percent ) {
                System.out.println("Worked: " + percent);
            }
            
            public boolean isCanceled() {
                return false;
            }
            
            public InternationalString getTask() {
                return null;
            }
            
            public float getProgress() {
                return 0;
            }
            
            public String getDescription() {
                return null;
            }
            
            public void exceptionOccurred( Throwable exception ) {
            }
            
            public void dispose() {
            }
            
            public void complete() {
                System.out.println("Finished");
            }
        };
        
        Map<String, Object> outputMap = pitfiller.execute(inputMap, listener);

        GridCoverage2D pitfillerCoverage = (GridCoverage2D) outputMap.get("outPit");

        checkMatrixEqual(pitfillerCoverage.getRenderedImage(), HMTestMaps.outPitData, 0);
    }

}
