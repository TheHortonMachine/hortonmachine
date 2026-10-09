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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.ARiverSectionsExtractor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.OmsRiverSectionsExtractor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.Result;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Test for the {@link OmsSaintGeo} module, on the sections of a synthetic valley.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestSaintGeo extends HMTestCase {

    private static final int STEPS = 300;

    /**
     * A straight valley falling to the south: a channel 2 m deep and 20 m wide at the bottom,
     * in a flat floodplain 200 m wide, between sides of 20%.
     *
     * @param slope the slope of the valley.
     * @param gullyOffset if not 0, the valley is instead a narrow V shaped gully, with sides of
     *          400%, at this distance from the river line.
     */
    private OmsRiverSectionsExtractor sections( double slope, double gullyOffset ) throws Exception {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        RegionMap region = new RegionMap();
        region.west = 600000;
        region.xres = 5;
        region.cols = 60;
        region.east = region.west + region.cols * region.xres;
        region.south = 5000000;
        region.yres = 5;
        region.rows = 220;
        region.north = region.south + region.rows * region.yres;

        double centerX = region.west + region.cols * region.xres / 2.0;
        double[][] data = new double[region.rows][region.cols];
        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                double x = region.west + (c + 0.5) * region.xres;
                double y = region.north - (r + 0.5) * region.yres;
                double d = Math.abs(x - centerX);
                double shape;
                if (d <= 10) {
                    shape = -2;
                } else if (d <= 20) {
                    shape = -2 + 0.2 * (d - 10);
                } else if (d <= 100) {
                    shape = 0;
                } else {
                    shape = 0.2 * (d - 100);
                }
                if (gullyOffset != 0) {
                    shape = 4 * Math.abs(x - (centerX + gullyOffset));
                }
                data[r][c] = 100 + slope * (y - region.south) + shape;
            }
        }

        // the river drawn from upstream, in the north, to downstream
        LineString river = GeometryUtilities.gf().createLineString(new Coordinate[]{
                new Coordinate(centerX, region.north - 25), new Coordinate(centerX, region.south + 25)});

        OmsRiverSectionsExtractor extractor = new OmsRiverSectionsExtractor();
        extractor.inElev = CoverageUtilities.buildCoverage("elevation", data, region, crs, true);
        extractor.inRiver = FeatureUtilities.featureCollectionFromGeometry(crs, river);
        extractor.pSectionsIntervalDistance = 50;
        extractor.pSectionsWidth = 120;
        extractor.pm = pm;
        extractor.process();
        return extractor;
    }

    private OmsSaintGeo saintGeo( OmsRiverSectionsExtractor extractor, double discharge ) {
        OmsSaintGeo saintGeo = new OmsSaintGeo();
        saintGeo.inRiverPoints = extractor.outRiverPoints;
        saintGeo.inSections = extractor.outSections;
        saintGeo.inSectionPoints = extractor.outSectionPoints;
        saintGeo.inDischarge = new double[STEPS];
        Arrays.fill(saintGeo.inDischarge, discharge);
        saintGeo.pKs = 30.0;
        saintGeo.pm = pm;
        return saintGeo;
    }

    /**
     * @return the ids of the sections, from upstream to downstream.
     */
    private List<Integer> sectionIds( OmsRiverSectionsExtractor extractor ) {
        TreeMap<Double, Integer> progressive2Id = new TreeMap<>();
        for( SimpleFeature section : FeatureUtilities.featureCollectionToList(extractor.outSections) ) {
            double progressive = ((Number) section.getAttribute(ARiverSectionsExtractor.FIELD_PROGRESSIVE)).doubleValue();
            int id = ((Number) section.getAttribute(ARiverSectionsExtractor.FIELD_SECTION_ID)).intValue();
            progressive2Id.put(progressive, id);
        }
        return new ArrayList<>(progressive2Id.values());
    }

    private static List<Result> lastResults( OmsSaintGeo saintGeo ) {
        return saintGeo.outResultsMap.get(STEPS - 1);
    }

    public void testSteadyDischargeFlowsThrough() throws Exception {
        OmsSaintGeo saintGeo = saintGeo(sections(0.002, 0), 5.0);
        saintGeo.process();

        assertEquals(STEPS, saintGeo.outResultsMap.size());
        for( Result result : lastResults(saintGeo) ) {
            double depth = result.waterLevel - result.minSectionElevation;
            assertTrue(depth > 0 && depth < 2);
            assertEquals(5.0, result.discharge, 0.05);
        }
    }

    public void testDownstreamLevelIsImposed() throws Exception {
        OmsRiverSectionsExtractor extractor = sections(0.002, 0);
        OmsSaintGeo free = saintGeo(extractor, 5.0);
        free.process();
        List<Result> freeResults = lastResults(free);
        Result freeLast = freeResults.get(freeResults.size() - 1);

        // a level 1.5 m over the bottom of the last section, as behind a weir
        double level = freeLast.minSectionElevation + 1.5;
        OmsSaintGeo imposed = saintGeo(extractor, 5.0);
        imposed.inDownstreamLevel = new double[STEPS];
        Arrays.fill(imposed.inDownstreamLevel, level);
        imposed.process();
        List<Result> imposedResults = lastResults(imposed);

        assertEquals(level, imposedResults.get(imposedResults.size() - 1).waterLevel, 1E-9);
        assertTrue(freeLast.waterLevel < level - 0.5);
        // the backwater raises the level also upstream of the last section
        int beforeLast = imposedResults.size() - 2;
        assertTrue(imposedResults.get(beforeLast).waterLevel > freeResults.get(beforeLast).waterLevel);
    }

    public void testLateralAndConfluenceInflowsAreBothAdded() throws Exception {
        OmsRiverSectionsExtractor extractor = sections(0.002, 0);
        List<Integer> ids = sectionIds(extractor);

        double[] lateral = new double[STEPS];
        Arrays.fill(lateral, 1.0);
        double[] confluence = new double[STEPS];
        Arrays.fill(confluence, 2.0);

        OmsSaintGeo saintGeo = saintGeo(extractor, 5.0);
        saintGeo.inLateralId2DischargeMap = new HashMap<>();
        saintGeo.inLateralId2DischargeMap.put(ids.get(3), lateral);
        saintGeo.inConfluenceId2DischargeMap = new HashMap<>();
        saintGeo.inConfluenceId2DischargeMap.put(ids.get(6), confluence);
        saintGeo.process();

        List<Result> results = lastResults(saintGeo);
        assertEquals(5.0, results.get(1).discharge, 0.1);
        assertEquals(8.0, results.get(results.size() - 2).discharge, 0.1);
    }

    public void testSectionsWithTheBottomNearTheirEnds() throws Exception {
        // the sections reach 120 m from the river line: the gully is in their last meters,
        // on the side of the last section points
        OmsSaintGeo saintGeo = saintGeo(sections(0.002, -112.5), 5.0);
        saintGeo.process();
        for( Result result : lastResults(saintGeo) ) {
            assertTrue(Double.isFinite(result.waterLevel));
            assertTrue(result.wettedArea > 0);
        }
    }

    public void testDivergenceIsReported() throws Exception {
        OmsRiverSectionsExtractor extractor = sections(0.002, 0);
        // a missing value in the lateral inflow, as an empty cell of its file
        double[] lateral = new double[STEPS];
        Arrays.fill(lateral, 1.0);
        lateral[10] = Double.NaN;

        OmsSaintGeo saintGeo = saintGeo(extractor, 5.0);
        saintGeo.inLateralId2DischargeMap = new HashMap<>();
        saintGeo.inLateralId2DischargeMap.put(sectionIds(extractor).get(3), lateral);
        try {
            saintGeo.process();
            fail("The diverged solution should be reported.");
        } catch (ModelsRuntimeException e) {
            assertTrue(e.getMessage().contains("diverged at time step 10"));
        }
    }
}
