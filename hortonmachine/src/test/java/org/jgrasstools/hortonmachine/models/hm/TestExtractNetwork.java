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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
import org.jgrasstools.hortonmachine.modules.network.extractnetwork.ExtractNetwork;
import org.jgrasstools.hortonmachine.modules.network.networkattributes.NetworkAttributesBuilder;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
/**
 * It test the {@link ExtractNetwork} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestExtractNetwork extends HMTestCase {

    public static void main( String[] args ) throws Exception {

        String base = "D:/Dropbox/hydrologis/lavori/2012_03_27_finland_forestry/data/grassdata/tm35fin/lidar/cell/";
        String inFlow = base + "flow";
        String inTca = base + "tca";
        String inNet = base + "net_200";
        String outHack = base + "hack";
        String out = "D:/TMP/net.shp";

        NetworkAttributesBuilder extract = new NetworkAttributesBuilder();
        extract.inFlow = RasterReader.readRaster(inFlow);
        extract.inTca = RasterReader.readRaster(inTca);
        extract.inNet = RasterReader.readRaster(inNet);
        extract.process();

        SimpleFeatureCollection net = extract.outNet;
        VectorWriter.writeVector(out, net);
        GridCoverage2D hack = extract.outHack;
        RasterWriter.writeRaster(outHack, hack);

    }

    // /**
    // * Test module with mode=0.
    // */
    // public void testExtractNetwork0() throws Exception {
    // HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
    // CoordinateReferenceSystem crs = HMTestMaps.crs;
    //
    // double[][] flowData = HMTestMaps.flowData;
    // GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
    // envelopeParams, crs, true);
    // double[][] tcaData = HMTestMaps.tcaData;
    // GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams,
    // crs, true);
    //
    // ExtractNetwork extractNetwork = new ExtractNetwork();
    // extractNetwork.pm = pm;
    // extractNetwork.inFlow = flowCoverage;
    // extractNetwork.inTca = tcaCoverage;
    // extractNetwork.pMode = Variables.TCA;
    // extractNetwork.pThres = 5;
    // extractNetwork.process();
    //
    // GridCoverage2D networkCoverage = extractNetwork.outNet;
    // checkMatrixEqual(networkCoverage.getRenderedImage(), HMTestMaps.extractNet0Data, 0.01);
    // }
    //
    // /**
    // * Test module with mode=1.
    // */
    // public void testExtractNetwork1() throws Exception {
    //
    // HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
    // CoordinateReferenceSystem crs = HMTestMaps.crs;
    //
    // double[][] flowData = HMTestMaps.flowData;
    // GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
    // envelopeParams, crs, true);
    // double[][] tcaData = HMTestMaps.tcaData;
    // GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams,
    // crs, true);
    // double[][] slopeData = HMTestMaps.slopeData;
    // GridCoverage2D slopeCoverage = CoverageUtilities.buildCoverage("slope", slopeData,
    // envelopeParams, crs, true);
    //
    // ExtractNetwork extractNetwork = new ExtractNetwork();
    // extractNetwork.pm = pm;
    // extractNetwork.inFlow = flowCoverage;
    // extractNetwork.inTca = tcaCoverage;
    // extractNetwork.inSlope = slopeCoverage;
    // extractNetwork.pMode = Variables.TCA_SLOPE;
    // extractNetwork.pThres = 8;
    // extractNetwork.process();
    //
    // GridCoverage2D networkCoverage = extractNetwork.outNet;
    //
    // checkMatrixEqual(networkCoverage.getRenderedImage(), HMTestMaps.extractNet1Data, 0.01);
    // }
    //
    // public void testExtractVectorNetwork() throws Exception {
    // HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
    // CoordinateReferenceSystem crs = HMTestMaps.crs;
    //
    // double[][] flowData = HMTestMaps.flowData;
    // GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
    // envelopeParams, crs, true);
    // double[][] netData = HMTestMaps.extractNet0Data;
    // GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams,
    // crs, true);
    //
    // NetworkAttributesBuilder extractNetwork = new NetworkAttributesBuilder();
    // extractNetwork.pm = pm;
    // extractNetwork.inFlow = flowCoverage;
    // extractNetwork.inNet = netCoverage;
    // extractNetwork.process();
    // SimpleFeatureCollection networkFC = extractNetwork.outNet;
    //
    // FeatureIterator<SimpleFeature> featureIterator = networkFC.features();
    // while( featureIterator.hasNext() ) {
    // SimpleFeature feature = featureIterator.next();
    // Geometry geometry = (Geometry) feature.getDefaultGeometry();
    // if (geometry.getCoordinates().length > 2) {
    // assertTrue(geometry
    // .toText()
    // .equals("LINESTRING (1640695 5139915, 1640725 5139885, 1640755 5139885, 1640785 5139885, 1640815 5139885, 1640845 5139885)"));
    // } else {
    // assertTrue(geometry.toText().startsWith("LINESTRING (1640845 5139885, 1640875 "));
    // }
    // }
    // featureIterator.close();
    // }

}
