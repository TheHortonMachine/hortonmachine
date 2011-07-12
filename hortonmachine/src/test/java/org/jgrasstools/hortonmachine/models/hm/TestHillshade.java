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
package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hillshade.Hillshade;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.sun.org.apache.regexp.internal.ReaderCharacterIterator;

/**
 * Test the {@link Hillshade} module.
 * 
 * @author Daniele Andreis
 */
public class TestHillshade extends HMTestCase {

    public void testHillshade() throws Exception {

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs3004;
       
        RasterReader rasterR = new RasterReader();
        rasterR.fileNovalue = -9999.0;
        rasterR.file="/home/daniele/Downloads/dtm000191_wor/dtm000191_wor.asc";
        rasterR.process();
        GridCoverage2D elevationCoverage = rasterR.outRaster;

        Hillshade hillshade = new Hillshade();
        hillshade.inElev = elevationCoverage;
        hillshade.pElev = 45.0;
        hillshade.pAzimuth = 315;

        hillshade.pm = pm;

        hillshade.process();

        GridCoverage2D hillshadeCoverage = hillshade.outHill;
        
        RasterWriter write = new RasterWriter();
        write.inRaster=hillshadeCoverage;
       write.pType="asc";
        write.file="/home/daniele/Downloads/dtm000191_wor/shade";
        write.process();
        
        checkMatrixEqual(hillshadeCoverage.getRenderedImage(), HMTestMaps.outHillshade, 0.1);
    }

}
