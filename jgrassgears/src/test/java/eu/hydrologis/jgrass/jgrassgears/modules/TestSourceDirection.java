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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

import eu.hydrologis.jgrass.jgrassgears.modules.v.sourcesdirection.SourcesDirectionCalculator;
import eu.hydrologis.jgrass.jgrassgears.utils.HMTestCase;
import eu.hydrologis.jgrass.jgrassgears.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;

/**
 * Test for the {@link SourcesDirectionCalculator} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestSourceDirection extends HMTestCase {
    public void testSourceDirection() throws Exception {

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", //$NON-NLS-1$
                elevationData, envelopeParams, crs);

        List<GridCoverage2D> demList = new ArrayList<GridCoverage2D>();
        demList.add(elevationCoverage);

        double res = 30.0;
        double newRes = 60.0;

        double n = 5140020.0 - 4 * res; // 4 pixels down
        double w = 1640650.0 + 5 * res; // 5 pixels right
        Coordinate coord = new Coordinate(w, n);
        FeatureCollection<SimpleFeatureType, SimpleFeature> pointFC = HMTestMaps.createFcFromPoint(
                coord, crs);

        SourcesDirectionCalculator sourceDirection = new SourcesDirectionCalculator();
        sourceDirection.inSources = pointFC;
        sourceDirection.inDems = demList;
        sourceDirection.pRes = newRes;
        sourceDirection.process();

        FeatureCollection<SimpleFeatureType, SimpleFeature> outSources = sourceDirection.outSources;
        SimpleFeature feature = outSources.features().next();
        double azimuth = ((Number) feature.getAttribute("azimuth")).doubleValue(); //$NON-NLS-1$
        assertEquals(270.0, azimuth);

    }

}
