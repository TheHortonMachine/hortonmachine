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
package org.jgrasstools.gears.modules;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.modules.r.coveragereconverter.CoverageConverter;
import org.jgrasstools.gears.utils.HMTestCase;
/**
 * Test for the reprojection modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCoverageConverter extends HMTestCase {
    @SuppressWarnings("nls")
    public void testCoverageConverter() throws Exception {

        String inPath = "/media/32_00_00/dati_geologico/DTM/dtm001725/w001001x.adf";
        String outPath = "/home/moovida/data/serviziogeologico_tn/ServizioGeologico/sorgenti/sorgenti/dtm001725.asc";

        CoverageConverter converter = new CoverageConverter();
        converter.inputFile = inPath;
        converter.outputFile = outPath;
        converter.pType = JGTConstants.ESRIGRID;
        converter.process();

    }

}
