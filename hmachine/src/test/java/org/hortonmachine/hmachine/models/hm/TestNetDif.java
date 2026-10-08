package org.hortonmachine.hmachine.models.hm;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;

import org.eclipse.imagen.TiledImage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.network.netdiff.OmsNetDiff;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

public class TestNetDif extends HMTestCase {

    public void testNetDif() {
        double[][] flowData = HMTestMaps.flowData;
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        double[][] strahlerData = HMTestMaps.strahlerData;
        GridCoverage2D strahlerCoverage = CoverageUtilities.buildCoverage("net", strahlerData, envelopeParams, crs, true);

        double[][] pitfillerData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pit", pitfillerData, envelopeParams, crs, true);

        OmsNetDiff netDif = new OmsNetDiff();
        netDif.inFlow = flowCoverage;
        netDif.inStream = strahlerCoverage;
        netDif.inRaster = pitfillerCoverage;
        netDif.process();

        GridCoverage2D netDifCoverage = netDif.outDiff;
        checkMatrixEqual(netDifCoverage.getRenderedImage(), HMTestMaps.diff_forPit);

    }

    /**
     * The drainage directions as an integer image of tiles smaller than the map, as read from a
     * tiled GeoTIFF: the output was created with their sample model, so it had the size of a tile.
     */
    public void testNetDifWithTiledFlow() {
        double[][] flowData = HMTestMaps.flowData;
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        int rows = flowData.length;
        int cols = flowData[0].length;
        SampleModel tileSampleModel = new ComponentSampleModel(DataBuffer.TYPE_INT, 4, 4, 1, 4, new int[]{0});
        ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
                Transparency.OPAQUE, DataBuffer.TYPE_INT);
        TiledImage flowImage = new TiledImage(0, 0, cols, rows, 0, 0, tileSampleModel, colorModel);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                flowImage.setSample(c, r, 0, (int) flowData[r][c]);
            }
        }
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowImage, envelopeParams, crs);
        GridCoverage2D strahlerCoverage = CoverageUtilities.buildCoverage("net", HMTestMaps.strahlerData, envelopeParams,
                crs, true);
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pit", HMTestMaps.pitData, envelopeParams, crs,
                true);

        OmsNetDiff netDif = new OmsNetDiff();
        netDif.inFlow = flowCoverage;
        netDif.inStream = strahlerCoverage;
        netDif.inRaster = pitfillerCoverage;
        netDif.process();

        checkMatrixEqual(netDif.outDiff.getRenderedImage(), HMTestMaps.diff_forPit);
    }

}
