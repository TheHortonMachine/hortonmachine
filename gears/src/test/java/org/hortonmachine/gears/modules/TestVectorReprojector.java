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
package org.hortonmachine.gears.modules;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * Test for the {@link OmsVectorReprojector}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorReprojector extends HMTestCase {
    private double delta = 0.0000001;

    public void testVectorReprojector() throws Exception {

        SimpleFeatureCollection testFC = HMTestMaps.getTestFC();

        OmsVectorReprojector reprojector = new OmsVectorReprojector();
        reprojector.inVector = testFC;
        reprojector.pCode = "EPSG:4326";
        reprojector.pm = pm;
        reprojector.process();

        CoordinateReferenceSystem sourceCRS = HMTestMaps.getCrs();
        CoordinateReferenceSystem targetCRS = CrsUtilities.getCrsFromSrid(4326);

        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);

        SimpleFeatureCollection outFC = reprojector.outVector;
        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        SimpleFeatureIterator originalFeatureIterator = testFC.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();

            SimpleFeature originalFeature = originalFeatureIterator.next();
            Coordinate origCoord = ((Geometry) originalFeature.getDefaultGeometry()).getCoordinate();
            Coordinate reprojected = JTS.transform(origCoord, null, transform);

            assertEquals(reprojected.x, coordinate.x, delta);
            assertEquals(reprojected.y, coordinate.y, delta);
        }
        featureIterator.close();

    }
}
