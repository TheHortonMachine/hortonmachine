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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.modules.r.profile.OmsProfile;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
/**
 * Test for the {@link OmsProfile}
 * <
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestProfile extends HMTestCase {
    private Coordinate westNorth = HMTestMaps.getWestNorth();
    private Coordinate eastSouth = HMTestMaps.getEastSouth();
    private Coordinate centerCoord = HMTestMaps.getCenterCoord();
    private RegionMap ep = HMTestMaps.getEnvelopeparams();

    @SuppressWarnings("nls")
    public void testProfile() throws Exception {
        double[][] elevationData = HMTestMaps.mapData;
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, ep, crs, true);

        String coords = (westNorth.x + ep.getXres() / 2) + "," + (centerCoord.y + ep.getYres() / 2) + ","
                + (eastSouth.x - ep.getXres() / 2) + "," + (centerCoord.y + ep.getYres() / 2);

        OmsProfile profile = new OmsProfile();
        profile.inRaster = elevationCoverage;
        profile.pm = pm;
        profile.inCoordinates = coords;
        profile.process();
        double[][] outProfile = profile.outProfile;
        double[] result = {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500};
        for( int i = 0; i < result.length; i++ ) {
            assertEquals(result[i], outProfile[i][1], 0.000001);
        }

        profile = new OmsProfile();
        profile.inRaster = elevationCoverage;
        profile.pm = pm;
        profile.inVector = doCollection();
        profile.process();
        outProfile = profile.outProfile;
        for( int i = 0; i < result.length; i++ ) {
            assertEquals(result[i], outProfile[i][1], 0.000001);
        }

    }

    private SimpleFeatureCollection doCollection() {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("typename");
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        Coordinate one = new Coordinate(westNorth.x + ep.getXres() / 2, centerCoord.y + ep.getYres() / 2);
        Coordinate two = new Coordinate(eastSouth.x - ep.getXres() / 2, centerCoord.y + ep.getYres() / 2);

        LineString lineString = GeometryUtilities.gf().createLineString(new Coordinate[]{one, two});
        Object[] values = new Object[]{lineString};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(null);
        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        newCollection.add(feature);
        return newCollection;
    }
}
