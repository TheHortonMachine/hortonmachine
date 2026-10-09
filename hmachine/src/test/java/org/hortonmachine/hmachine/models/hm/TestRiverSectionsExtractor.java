/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.OmsRiverSectionsExtractor;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

/**
 * Test for the {@link OmsRiverSectionsExtractor} module.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestRiverSectionsExtractor extends HMTestCase {

    public void testTheLongestOfSeparateLinesIsTheRiver() throws Exception {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        RegionMap region = new RegionMap();
        region.west = 600000;
        region.xres = 10;
        region.cols = 50;
        region.east = region.west + region.cols * region.xres;
        region.south = 5000000;
        region.yres = 10;
        region.rows = 100;
        region.north = region.south + region.rows * region.yres;

        // a V shaped valley in the middle, falling to the south
        double centerX = region.west + region.cols * region.xres / 2.0;
        double[][] data = new double[region.rows][region.cols];
        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                double x = region.west + (c + 0.5) * region.xres;
                double y = region.north - (r + 0.5) * region.yres;
                data[r][c] = 100 + 0.01 * (y - region.south) + 0.2 * Math.abs(x - centerX);
            }
        }

        // the river, from upstream, and a short line along the southern border, that does not touch it
        GeometryFactory gf = GeometryUtilities.gf();
        LineString river = gf.createLineString(
                new Coordinate[]{new Coordinate(centerX, region.north - 50), new Coordinate(centerX, region.south + 50)});
        LineString border = gf.createLineString(new Coordinate[]{new Coordinate(region.west + 20, region.south + 15),
                new Coordinate(region.west + 120, region.south + 15)});

        OmsRiverSectionsExtractor extractor = new OmsRiverSectionsExtractor();
        extractor.inElev = CoverageUtilities.buildCoverage("elevation", data, region, crs, true);
        extractor.inRiver = FeatureUtilities.featureCollectionFromGeometry(crs, border, river);
        extractor.pSectionsIntervalDistance = 100;
        extractor.pSectionsWidth = 50;
        extractor.pm = pm;
        extractor.process();

        // the sections cross the river, every 100 m along its 900 m
        assertEquals(10, extractor.outSections.size());
        for( SimpleFeature section : FeatureUtilities.featureCollectionToList(extractor.outSections) ) {
            Geometry sectionLine = (Geometry) section.getDefaultGeometry();
            assertTrue(sectionLine.intersects(river));
        }
    }
}
