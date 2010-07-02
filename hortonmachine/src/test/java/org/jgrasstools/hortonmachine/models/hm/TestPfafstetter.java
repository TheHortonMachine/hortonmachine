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
package org.jgrasstools.hortonmachine.models.hm;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureWriter;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.modules.network.pfafstetter.Pfafstetter;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Test the {@link Pfafstetter} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestPfafstetter extends HMTestCase {

    public void testPfafstetter() throws Exception {

        GridCoverage2D flowCoverage = null;
        GridCoverage2D pitCoverage = null;
        GridCoverage2D hackCoverage = null;
        GridCoverage2D netnumCoverage = null;
        String outputPath = null;

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        Pfafstetter pfafstetter = new Pfafstetter();
        pfafstetter.pm = pm;
        pfafstetter.inFlow = flowCoverage;
        pfafstetter.inPit = pitCoverage;
        pfafstetter.inHackstream = hackCoverage;
        pfafstetter.inNetnum = netnumCoverage;
        pfafstetter.pMode = 0;

        pfafstetter.process();
        
        FeatureCollection<SimpleFeatureType, SimpleFeature> pfafstetterFC = pfafstetter.outPfaf;
        
        ShapefileFeatureWriter.writeShapefile(outputPath, pfafstetterFC);

    }

}