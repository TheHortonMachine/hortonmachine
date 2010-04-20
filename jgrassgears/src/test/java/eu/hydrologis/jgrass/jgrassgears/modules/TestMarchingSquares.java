package eu.hydrologis.jgrass.jgrassgears.modules;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.hydrologis.jgrass.jgrassgears.io.shapefile.ShapefileFeatureWriter;
import eu.hydrologis.jgrass.jgrassgears.io.tiff.GeoTiffCoverageReader;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.modules.r.marchingsquares.MarchingSquaresVectorializer;
import eu.hydrologis.jgrass.jgrassgears.utils.HMTestCase;

public class TestMarchingSquares extends HMTestCase {
    public void testMarchingSquares() throws Exception {

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
        String tiff = "/home/moovida/data/geosolutions/datitest/WSM_SS_20100416_233249_4676_2.dim.tif";

        GeoTiffCoverageReader reader = new GeoTiffCoverageReader();
        reader.file = tiff;
        reader.pm = pm;

        reader.readCoverage();

        GridCoverage2D geodata = reader.geodata;

        // double[][] extractNet1Data = HMTestMaps.extractNet1Data;
        // HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        // CoordinateReferenceSystem crs = HMTestMaps.crs;
        // GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation",
        // extractNet1Data, envelopeParams, crs);
        // GridCoverage2D geodata = elevationCoverage;

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
