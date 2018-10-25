package org.hortonmachine.gears.modules;
///*
// * This file is part of HortonMachine (http://www.hortonmachine.org)
// * (C) HydroloGIS - www.hydrologis.com 
// * 
// * The HortonMachine is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.hortonmachine.gears.modules;
//
//import java.io.File;
//import java.net.URL;
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.hortonmachine.gears.modules.v.vectorconverter.OmsDwgConverter;
//import org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter;
//import org.hortonmachine.gears.utils.HMTestCase;
//import org.opengis.feature.simple.SimpleFeature;
//
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.Geometry;
///**
// * Test for the {@link OmsDwgConverter}
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestDwgConverter extends HMTestCase {
//
//    @SuppressWarnings("nls")
//    public void testVectorFilter() throws Exception {
//        URL testUrl = this.getClass().getClassLoader().getResource("test.dxf");
//        String dxfFile = new File(testUrl.toURI()).getAbsolutePath();
//
//        OmsDwgConverter reader = new OmsDwgConverter();
//        reader.file = dxfFile;
//        reader.readFeatureCollection();
//
//        SimpleFeatureCollection pointsFC = reader.pointsFC;
//        SimpleFeatureCollection linesFC = reader.lineFC;
//
//        assertTrue(pointsFC.size() == 0);
//        assertTrue(linesFC.size() == 1);
//
//        SimpleFeature feature = linesFC.features().next();
//        Geometry geometry = (Geometry) feature.getDefaultGeometry();
//        Coordinate[] coordinates = geometry.getCoordinates();
//
//        double delta = 0.000001;
//        assertEquals(coordinates[0].x, 0.0, delta);
//        assertEquals(coordinates[0].y, 0.0, delta);
//        assertEquals(coordinates[1].x, 10.0, delta);
//        assertEquals(coordinates[1].y, 10.0, delta);
//
//    }
// }
