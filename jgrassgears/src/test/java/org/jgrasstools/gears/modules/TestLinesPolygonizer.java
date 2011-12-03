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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.vectorreader.VectorReader;
import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
import org.jgrasstools.gears.modules.v.polygonize.LinesPolygonizer;
import org.jgrasstools.gears.utils.HMTestCase;

/**
 * Test for {@link LinesPolygonizer}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestLinesPolygonizer extends HMTestCase {
    public void testLinesPolygonizer() throws Exception {

        SimpleFeatureCollection lines = VectorReader.readVector("D:\\data\\dwg_dxf\\polygonizer\\pranzo_line.shp");
        SimpleFeatureCollection points = VectorReader.readVector("D:\\data\\dwg_dxf\\polygonizer\\pranzo_point.shp");

        LinesPolygonizer vectorizer = new LinesPolygonizer();
        vectorizer.pm = pm;
        vectorizer.inMap = lines;
        vectorizer.inPoints = points;
        vectorizer.fId = "Text";
        vectorizer.fNewId = "id";
        vectorizer.process();

        SimpleFeatureCollection outPolygons = vectorizer.outMap;

        VectorWriter.writeVector("D:\\data\\dwg_dxf\\polygonizer\\pranzo_poligoni.shp", outPolygons);

    }

}
