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

import java.io.File;
import java.net.URL;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
/**
 * Test for the {@link OmsDxfConverter}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestDxfConverter extends HMTestCase {

    @SuppressWarnings("nls")
    public void testVectorFilter() throws Exception {
        URL testUrl = this.getClass().getClassLoader().getResource("test.dxf");
        String dxfFile = new File(testUrl.toURI()).getAbsolutePath();

        OmsDxfConverter reader = new OmsDxfConverter();
        reader.file = dxfFile;
        reader.process();

        SimpleFeatureCollection pointsFC = reader.pointsVector;
        SimpleFeatureCollection linesFC = reader.lineVector;

        assertTrue(pointsFC.size() == 0);
        assertTrue(linesFC.size() == 1);

        SimpleFeature feature = linesFC.features().next();
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        Coordinate[] coordinates = geometry.getCoordinates();

        double delta = 0.000001;
        assertEquals(coordinates[0].x, 0.0, delta);
        assertEquals(coordinates[0].y, 0.0, delta);
        assertEquals(coordinates[1].x, 10.0, delta);
        assertEquals(coordinates[1].y, 10.0, delta);

    }

    /**
     * Points and texts, lines and open polylines, closed polylines as polygons, the other entities
     * ignored, with the crs of the code when there is no prj.
     */
    public void testEntities() throws Exception {
        String dxf = String.join("\n", //
                "0", "SECTION", "2", "ENTITIES", //
                "0", "POINT", "8", "wells", "62", "3", "10", "1.0", "20", "2.0", "30", "350.0", //
                "0", "TEXT", "8", "labels", "10", "5.0", "20", "5.0", "30", "0.0", "40", "2.5", "1", "Rio Torto", //
                "0", "LINE", "8", "roads", "10", "0.0", "20", "0.0", "30", "0.0", "11", "10.0", "21", "0.0", "31", "0.0", //
                "0", "LWPOLYLINE", "8", "roads", "90", "3", "70", "0", //
                "10", "0.0", "20", "0.0", "10", "5.0", "20", "5.0", "10", "10.0", "20", "0.0", //
                "0", "LWPOLYLINE", "8", "parcels", "90", "4", "70", "1", //
                "10", "0.0", "20", "0.0", "10", "10.0", "20", "0.0", "10", "10.0", "20", "10.0", "10", "0.0", "20", "10.0", //
                "0", "CIRCLE", "8", "trees", "10", "3.0", "20", "3.0", "30", "0.0", "40", "1.0", //
                "0", "ENDSEC", "0", "EOF", "");
        File dxfFile = File.createTempFile("hm_dxf_test", ".dxf");
        try {
            FileUtilities.writeFile(dxf, dxfFile);

            OmsDxfConverter reader = new OmsDxfConverter();
            reader.file = dxfFile.getAbsolutePath();
            reader.pCode = "EPSG:3003";
            reader.process();

            List<SimpleFeature> points = FeatureUtilities.featureCollectionToList(reader.pointsVector);
            List<SimpleFeature> lines = FeatureUtilities.featureCollectionToList(reader.lineVector);
            List<SimpleFeature> polygons = FeatureUtilities.featureCollectionToList(reader.polygonVector);
            assertEquals(2, points.size());
            assertEquals(2, lines.size());
            assertEquals(1, polygons.size());

            for( SimpleFeature point : points ) {
                if ("wells".equals(point.getAttribute("LAYER"))) {
                    assertEquals(3, point.getAttribute("COLOR"));
                    assertEquals(1.0, ((Geometry) point.getDefaultGeometry()).getCoordinate().x, 1E-9);
                } else {
                    assertEquals("labels", point.getAttribute("LAYER"));
                    assertEquals("Rio Torto", point.getAttribute("TEXT"));
                    assertEquals(2.5, (Double) point.getAttribute("TEXT_HEIGHT"), 1E-9);
                }
            }
            for( SimpleFeature line : lines ) {
                assertEquals("roads", line.getAttribute("LAYER"));
            }
            Geometry parcel = (Geometry) polygons.get(0).getDefaultGeometry();
            assertEquals("parcels", polygons.get(0).getAttribute("LAYER"));
            assertEquals(100.0, parcel.getArea(), 1E-9);

            assertEquals("EPSG:3003",
                    HMCrsRegistry.getCodeFromCrs(reader.polygonVector.getSchema().getCoordinateReferenceSystem()));
        } finally {
            dxfFile.delete();
        }
    }
}
