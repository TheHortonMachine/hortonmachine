package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.hydrologis.jgrass.hortonmachine.modules.statistics.kriging.Kriging;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.jgrassgears.io.shapefile.ShapefileFeatureReader;
import eu.hydrologis.jgrass.jgrassgears.io.timedependent.TimeseriesByStepReaderId2Value;
import eu.hydrologis.jgrass.jgrassgears.io.timedependent.TimeseriesByStepWriterId2Value;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
/**
 * Test the kriging model.
 * 
 * @author daniele andreis
 *
 */
public class TestKriging extends HMTestCase {

    public void testKriging2() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        URL stazioniUrl = this.getClass().getClassLoader().getResource("rainstations.shp");
        File stazioniFile = new File(stazioniUrl.toURI());
        URL puntiUrl = this.getClass().getClassLoader().getResource("basins_passirio_width0.shp");
        File puntiFile = new File(puntiUrl.toURI());
        URL krigingRainUrl = this.getClass().getClassLoader().getResource("rain_test.csv");
        File krigingRainFile = new File(krigingRainUrl.toURI());

        ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
        stationsReader.file = stazioniFile.getAbsolutePath();
        stationsReader.readFeatureCollection();
        FeatureCollection<SimpleFeatureType, SimpleFeature> stationsFC = stationsReader.geodata;

        ShapefileFeatureReader interpolatedPointsReader = new ShapefileFeatureReader();
        interpolatedPointsReader.file = puntiFile.getAbsolutePath();
        interpolatedPointsReader.readFeatureCollection();
        FeatureCollection<SimpleFeatureType, SimpleFeature> interpolatedPointsFC = interpolatedPointsReader.geodata;

        TimeseriesByStepReaderId2Value reader = new TimeseriesByStepReaderId2Value();
        reader.file = krigingRainFile.getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tTimestep = 60;
        //reader.tEnd = "2000-01-01 00:00";
        reader.fileNovalue = "-9999";
        
        reader.startTicking();

       

        Kriging kriging = new Kriging();
        kriging.pm = pm;

        kriging.inStations = stationsFC;
        kriging.fStationsid = "ID_PUNTI_M";

        kriging.inInterpolate = interpolatedPointsFC;
        kriging.fInterpolateid = "netnum";

       

        // it doesn't execute the model with log value.
        kriging.doLogarithmic = false;
        /*
         * Set up the model in order to use the variogram with an explicit integral scale and variance.
         */
        kriging.pVariance = 0.5;
        kriging.pIntegralscale = new double[]{10000, 10000, 100};
        /*
         * Set up the model in order to run with a FeatureCollection as point to interpolated. In this case only 2D.
         */
        kriging.pMode = 0;


        File interpolatedRainFile = new File(krigingRainFile.getParentFile(),
                "kriging_interpolated.csv");
        String interpolatedRainPath = interpolatedRainFile.getAbsolutePath();
        interpolatedRainPath = interpolatedRainPath.replaceFirst("target", "src" + File.separator
                + File.separator + "test");
        interpolatedRainPath = interpolatedRainPath.replaceFirst("test-classes", "resources");
        TimeseriesByStepWriterId2Value writer = new TimeseriesByStepWriterId2Value();
        writer.file = interpolatedRainPath;
        
        writer.tStart = reader.tStart;
        writer.tTimestep = reader.tTimestep;

        
        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.data;
            kriging.inData = id2ValueMap;
            kriging.executeKriging();
            /*
             * Extract the result.
             */
            HashMap<Integer, double[]> result = kriging.outData;
            writer.data = result;
            writer.writeNextLine();
        }
        
        
        
        reader.close();
        writer.close();
    }

}
