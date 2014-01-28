package org.jgrasstools.gears;

import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
/**
 * Test FeatureUtils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestFeatureUtils extends HMTestCase {

    @SuppressWarnings("nls")
    public void testFeatureUtils() throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("typename");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        b.add("AttrName", String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{GeometryUtilities.gf().createPoint(new Coordinate(0, 0)), "test"};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(type.getTypeName());

        Object attr = FeatureUtilities.getAttributeCaseChecked(feature, "attrname");
        assertEquals("test", attr.toString());
        attr = FeatureUtilities.getAttributeCaseChecked(feature, "attrnam");
        assertNull(attr);
    }

    public void testGridCellGeoms() throws Exception {
        double[][] mapData = HMTestMaps.mapData;
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        GridCoverage2D inElev = CoverageUtilities.buildCoverage("elevation", mapData, envelopeParams, crs, true); //$NON-NLS-1$
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        double east = regionMap.getEast();
        double north = regionMap.getNorth();
        double south = regionMap.getSouth();
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        List<Polygon> cellPolygons = FeatureUtilities.gridcoverageToCellPolygons(inElev);
        int size = nCols * nRows;
        assertEquals(size, cellPolygons.size());

        Polygon polygon = cellPolygons.get(9);
        Envelope env = polygon.getEnvelopeInternal();
        assertEquals(east, env.getMaxX(), DELTA);
        assertEquals(north, env.getMaxY(), DELTA);

        polygon = cellPolygons.get(size - 1);
        env = polygon.getEnvelopeInternal();
        assertEquals(east, env.getMaxX(), DELTA);
        assertEquals(south, env.getMinY(), DELTA);

    }

}
