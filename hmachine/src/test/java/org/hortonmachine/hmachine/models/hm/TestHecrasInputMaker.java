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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.hecras.OmsHecrasInputBuilder;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.OmsRiverSectionsExtractor;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Test for the {@link OmsHecrasInputBuilder} module, on the sections of {@link OmsRiverSectionsExtractor}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestHecrasInputMaker extends HMTestCase {

    public void testEndpointsGoFromUpstreamToDownstream() throws Exception {
        RegionMap region = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        // a V shaped valley in the middle column, falling to the south
        double centerX = region.west + (region.cols / 2 + 0.5) * region.xres;
        double[][] data = new double[region.rows][region.cols];
        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                double x = region.west + (c + 0.5) * region.xres;
                double y = region.north - (r + 0.5) * region.yres;
                data[r][c] = 1000 + (y - region.south) * 0.1 + Math.abs(x - centerX) * 0.5;
            }
        }

        // the river drawn from upstream, in the north, to downstream
        LineString river = GeometryUtilities.gf().createLineString(new Coordinate[]{
                new Coordinate(centerX, region.north - region.yres), new Coordinate(centerX, region.south + region.yres)});

        OmsRiverSectionsExtractor extractor = new OmsRiverSectionsExtractor();
        extractor.inElev = CoverageUtilities.buildCoverage("elevation", data, region, crs, true);
        extractor.inRiver = FeatureUtilities.featureCollectionFromGeometry(crs, river);
        extractor.pSectionsIntervalDistance = 2 * region.yres;
        extractor.pSectionsWidth = 2 * region.xres;
        extractor.pm = pm;
        extractor.process();
        SimpleFeatureCollection sections = extractor.outSections;
        assertTrue(sections.size() > 1);

        File hecrasFile = File.createTempFile("hm_hecras", ".sdf");
        hecrasFile.deleteOnExit();
        OmsHecrasInputBuilder hecras = new OmsHecrasInputBuilder();
        hecras.inRiverPoints = extractor.outRiverPoints;
        hecras.inSections = sections;
        hecras.inSectionPoints = extractor.outSectionPoints;
        hecras.pTitle = "test";
        hecras.outHecras = hecrasFile.getAbsolutePath();
        hecras.pm = pm;
        hecras.process();

        // ENDPOINT: x,y,z, id -> the upstream end is FROM POINT 1, the downstream one TO POINT 2
        List<double[]> endpoints = new ArrayList<>();
        for( String line : FileUtilities.readFile(hecrasFile).split("\\R") ) {
            if (line.startsWith("ENDPOINT:")) {
                String[] split = line.substring("ENDPOINT:".length()).trim().split(",");
                endpoints.add(new double[]{Double.parseDouble(split[1]), Double.parseDouble(split[2])});
            }
        }
        assertEquals(2, endpoints.size());
        double[] upstream = endpoints.get(0);
        double[] downstream = endpoints.get(1);
        assertTrue(upstream[0] > downstream[0]);
        assertTrue(upstream[1] > downstream[1]);
    }
}
