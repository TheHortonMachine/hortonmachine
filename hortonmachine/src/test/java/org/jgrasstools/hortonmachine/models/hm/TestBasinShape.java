package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.basin.basinshape.BasinShape;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
/**
 * Test basinshape.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestBasinShape extends HMTestCase {

    @SuppressWarnings("nls")
    public void testBasinShape() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs, true);
        double[][] basinsData = HMTestMaps.basinShapeData;
        GridCoverage2D basinsCoverage = CoverageUtilities.buildCoverage("basins", basinsData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        BasinShape basin = new BasinShape();
        basin.inPit = pitCoverage;
        basin.inBasins = basinsCoverage;
        basin.pm = pm;

        basin.process();

        FeatureCollection<SimpleFeatureType, SimpleFeature> basinsFC = basin.outBasins;

        FeatureIterator<SimpleFeature> basinsIter = basinsFC.features();
        while( basinsIter.hasNext() ) {
            SimpleFeature feature = basinsIter.next();
            Geometry line = (Geometry) feature.getDefaultGeometry();

            int numGeometries = line.getNumGeometries();
            for( int i = 0; i < numGeometries; i++ ) {
                Geometry geometryN = line.getGeometryN(i);
                System.out.println(geometryN.toText());
            }
        }
        basinsIter.close();

    }

}
