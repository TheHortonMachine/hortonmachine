/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.modules.r.profile.OmsProfile;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
/**
 * Test for the {@link OmsProfile}
 * <
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestProfile extends HMTestCase {
    private Coordinate westNorth = HMTestMaps.westNorth;
    private Coordinate eastSouth = HMTestMaps.eastSouth;
    private Coordinate centerCoord = HMTestMaps.centerCoord;
    private RegionMap ep = HMTestMaps.envelopeParams;

    @SuppressWarnings("nls")
    public void testProfile() throws Exception {
        double[][] elevationData = HMTestMaps.mapData;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
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
        CoordinateReferenceSystem crs = HMTestMaps.crs;
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
        SimpleFeatureCollection newCollection = FeatureCollections.newCollection();
        newCollection.add(feature);
        return newCollection;
    }
}
