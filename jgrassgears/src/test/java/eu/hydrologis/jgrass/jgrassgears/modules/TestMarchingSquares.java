package eu.hydrologis.jgrass.jgrassgears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.jgrassgears.io.shapefile.ShapefileFeatureWriter;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.modules.r.marchingsquares.MarchingSquaresVectorializer;
import eu.hydrologis.jgrass.jgrassgears.utils.HMTestCase;
import eu.hydrologis.jgrass.jgrassgears.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;

public class TestMarchingSquares extends HMTestCase {
    public void testMarchingSquares() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        double[][] extractNet1Data = HMTestMaps.extractNet1Data;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net",
                extractNet1Data, envelopeParams, crs);
        GridCoverage2D geodata = netCoverage;

        MarchingSquaresVectorializer squares = new MarchingSquaresVectorializer();
        squares.inGeodata = geodata;
        squares.pValue = 0;
        squares.pm = pm;

        squares.process();

        FeatureCollection<SimpleFeatureType, SimpleFeature> outGeodata = squares.outGeodata;

        ShapefileFeatureWriter writer = new ShapefileFeatureWriter();
        writer.file = "/home/moovida/data/geosolutions/datitest/WSM.shp";
        writer.geodata = outGeodata;
        writer.writeFeatureCollection();

    }

}
