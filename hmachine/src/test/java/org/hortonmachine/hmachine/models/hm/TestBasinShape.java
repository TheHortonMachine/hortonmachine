/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.basin.basinshape.OmsBasinShape;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
/**
 * Test {@link OmsBasinShape}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestBasinShape extends HMTestCase {

    private static final String geom1Txt = "POLYGON ((1640710 5139990, 1640710 5139960, 1640680 5139960, 1640680 5139840, 1640710 5139840, 1640710 5139810, 1640800 5139810, 1640800 5139990, 1640710 5139990))";
    private static final String geom2Txt = "POLYGON ((1640800 5139990, 1640800 5139810, 1640920 5139810, 1640920 5139990, 1640800 5139990))";

    @SuppressWarnings("nls")
    public void testBasinShape() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs, true);
        double[][] basinsData = HMTestMaps.basinShapeData;
        GridCoverage2D basinsCoverage = CoverageUtilities.buildCoverage("basins", basinsData, envelopeParams, crs, true);

        OmsBasinShape basin = new OmsBasinShape();
        basin.inElev = pitCoverage;
        basin.inBasins = basinsCoverage;
        basin.pm = pm;

        basin.process();

        SimpleFeatureCollection basinsFC = basin.outBasins;

        FeatureIterator<SimpleFeature> basinsIter = basinsFC.features();
        while( basinsIter.hasNext() ) {
            SimpleFeature feature = basinsIter.next();
            Geometry line = (Geometry) feature.getDefaultGeometry();

            int numGeometries = line.getNumGeometries();
            for( int i = 0; i < numGeometries; i++ ) {
                Geometry geometryN = line.getGeometryN(i);
                int length = geometryN.getCoordinates().length;
                if (length == 9) {
                    Geometry g1 = new WKTReader().read(geom1Txt);
                    assertTrue(geometryN.equals(g1));
                }
                if (length == 5) {
                    Geometry g2 = new WKTReader().read(geom2Txt);
                    assertTrue(geometryN.equals(g2));
                }
            }
        }
        basinsIter.close();

    }

}
