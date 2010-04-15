/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.jgrassgears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import eu.hydrologis.jgrass.jgrassgears.io.shapefile.ShapefileFeatureReader;
import eu.hydrologis.jgrass.jgrassgears.io.shapefile.ShapefileFeatureWriter;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.modules.r.coveragereprojector.CoverageReprojector;
import eu.hydrologis.jgrass.jgrassgears.modules.v.contourlabels.ContourLinesLabeler;
import eu.hydrologis.jgrass.jgrassgears.modules.v.featurereprojector.FeatureReprojector;
import eu.hydrologis.jgrass.jgrassgears.utils.HMTestCase;
import eu.hydrologis.jgrass.jgrassgears.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;

/**
 * Test for the {@link ContourLinesLabeler} modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCountourLinesLabeler extends HMTestCase {
    public void testCountourLinesLabeler() throws Exception {

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        String contours = "/home/moovida/data/serviziogeologico_tn/ServizioGeologico/contourlines_labels/cl_28100_2d_50.shp";
        String lines = "/home/moovida/data/serviziogeologico_tn/ServizioGeologico/contourlines_labels/lines.shp";
        String points = "/home/moovida/data/serviziogeologico_tn/ServizioGeologico/contourlines_labels/points.shp";

        ShapefileFeatureReader reader = new ShapefileFeatureReader();
        reader.file = contours;
        reader.readFeatureCollection();
        FeatureCollection<SimpleFeatureType, SimpleFeature> contoursFC = reader.geodata;

        reader = new ShapefileFeatureReader();
        reader.file = lines;
        reader.readFeatureCollection();
        FeatureCollection<SimpleFeatureType, SimpleFeature> linesFC = reader.geodata;

        ContourLinesLabeler labeler = new ContourLinesLabeler();
        labeler.pm = pm;
        labeler.inContour = contoursFC;
        labeler.inLines = linesFC;
        labeler.fElevation = "CONTOUR";
        labeler.buffer = 10.0;
        labeler.process();

        FeatureCollection<SimpleFeatureType, SimpleFeature> outPoints = labeler.outPoints;

        ShapefileFeatureWriter writer = new ShapefileFeatureWriter();
        writer.file = points;
        writer.geodata = outPoints;
        writer.writeFeatureCollection();

    }

}
