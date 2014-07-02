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
import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.OmsShapefileFeatureReader;
import org.jgrasstools.gears.utils.PrintUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.network.netnumbering.OmsNetNumbering;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link OmsNetNumbering}.
 * 
 * @author Andrea Antonello (www.hydrologis.com), Daniele Andreis
 */
public class TestNetNumbering extends HMTestCase {

    public void testNetnumberingMode0() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] netData = HMTestMaps.extractNet1Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);

        OmsNetNumbering netNumbering = new OmsNetNumbering();
        netNumbering.inFlow = flowCoverage;
        netNumbering.inNet = netCoverage;
        netNumbering.pMode = 0;
        netNumbering.pm = pm;

        netNumbering.process();

        GridCoverage2D netnumberingCoverage = netNumbering.outNetnum;
        GridCoverage2D subbasinsCoverage = netNumbering.outBasins;

        checkMatrixEqual(netnumberingCoverage.getRenderedImage(), HMTestMaps.netNumberingChannelDataNN0, 0);
        checkMatrixEqual(subbasinsCoverage.getRenderedImage(), HMTestMaps.basinDataNN0, 0);
    }

    public void testNetnumberingMode1() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] netData = HMTestMaps.extractNet1Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);

        OmsNetNumbering netNumbering = new OmsNetNumbering();
        netNumbering.inFlow = flowCoverage;
        netNumbering.inNet = netCoverage;
        netNumbering.inTca = tcaCoverage;
        netNumbering.pMode = 1;
        netNumbering.pThres = 2.0;
        netNumbering.pm = pm;

        netNumbering.process();

        GridCoverage2D netnumberingCoverage = netNumbering.outNetnum;
        GridCoverage2D subbasinsCoverage = netNumbering.outBasins;

        checkMatrixEqual(netnumberingCoverage.getRenderedImage(), HMTestMaps.netNumberingChannelDataNN1, 0);
        checkMatrixEqual(subbasinsCoverage.getRenderedImage(), HMTestMaps.basinDataNN1, 0);
    }

    public void testNetnumberingMode2() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        URL pointURL = this.getClass().getClassLoader().getResource("netNumbering_Point.shp");
        File pointsFile = new File(pointURL.toURI());

        OmsShapefileFeatureReader pointsReader = new OmsShapefileFeatureReader();
        pointsReader.file = pointsFile.getAbsolutePath();
        pointsReader.readFeatureCollection();
        SimpleFeatureCollection pointsFC = pointsReader.geodata;
        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] netData = HMTestMaps.extractNet1Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);

        OmsNetNumbering netNumbering = new OmsNetNumbering();
        netNumbering.inFlow = flowCoverage;
        netNumbering.inNet = netCoverage;
        netNumbering.fPointId = "RETE_ID";
        netNumbering.pMode = 2;
        netNumbering.inPoints = pointsFC;
        netNumbering.pm = pm;

        netNumbering.process();

        GridCoverage2D netnumberingCoverage = netNumbering.outNetnum;
        GridCoverage2D subbasinsCoverage = netNumbering.outBasins;

        PrintUtilities.printCoverageData(netnumberingCoverage);
        System.out.println("***************");
        PrintUtilities.printMatrixData( HMTestMaps.netNumberingChannelDataNN2);
        
        checkMatrixEqual(netnumberingCoverage.getRenderedImage(), HMTestMaps.netNumberingChannelDataNN2, 0);
        checkMatrixEqual(subbasinsCoverage.getRenderedImage(), HMTestMaps.basinDataNN2, 0);
    }

    // public void testNetnumberingMode3() throws Exception {
    // HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
    // CoordinateReferenceSystem crs = HMTestMaps.getCrs();
    // URL pointURL = this.getClass().getClassLoader().getResource("netNumbering_Point.shp");
    // File pointsFile = new File(pointURL.toURI());
    //
    // OmsShapefileFeatureReader pointsReader = new OmsShapefileFeatureReader();
    // pointsReader.file = pointsFile.getAbsolutePath();
    // pointsReader.readFeatureCollection();
    // SimpleFeatureCollection pointsFC = pointsReader.geodata;
    // double[][] flowData = HMTestMaps.mflowDataBorder;
    // GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
    // envelopeParams, crs, true);
    // double[][] netData = HMTestMaps.extractNet1Data;
    // GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams,
    // crs, true);
    // double[][] tcaData = HMTestMaps.tcaData;
    // GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams,
    // crs, true);
    //
    // OmsNetNumbering netNumbering = new OmsNetNumbering();
    // netNumbering.inFlow = flowCoverage;
    // netNumbering.inNet = netCoverage;
    // netNumbering.inTca = tcaCoverage;
    // netNumbering.pMode = 3;
    // netNumbering.inPoints = pointsFC;
    // netNumbering.pThres = 2.0;
    // netNumbering.pm = pm;
    //
    // netNumbering.process();
    //
    // GridCoverage2D netnumberingCoverage = netNumbering.outNetnum;
    // GridCoverage2D subbasinsCoverage = netNumbering.outBasins;
    //
    // checkMatrixEqual(netnumberingCoverage.getRenderedImage(),
    // HMTestMaps.netNumberingChannelDataNN3, 0);
    // checkMatrixEqual(subbasinsCoverage.getRenderedImage(), HMTestMaps.basinDataNN3, 0);
    // }

}
