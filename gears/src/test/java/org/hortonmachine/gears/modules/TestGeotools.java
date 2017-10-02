package org.hortonmachine.gears.modules;
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
//package org.hortonmachine.gears.modules;
//
//import static java.lang.Double.NaN;
//
//import java.awt.image.ComponentSampleModel;
//import java.awt.image.DataBuffer;
//import java.awt.image.WritableRaster;
//
//import javax.media.jai.Interpolation;
//import javax.media.jai.RasterFactory;
//
//import org.geotools.coverage.CoverageFactoryFinder;
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.coverage.grid.GridCoverageFactory;
//import org.geotools.coverage.grid.GridEnvelope2D;
//import org.geotools.coverage.grid.GridGeometry2D;
//import org.geotools.coverage.grid.ViewType;
//import org.geotools.coverage.processing.CoverageProcessor;
//import org.geotools.geometry.Envelope2D;
//import org.geotools.referencing.CRS;
//import org.geotools.resources.image.ImageUtilities;
//import org.hortonmachine.gears.utils.HMTestCase;
//import org.opengis.coverage.processing.Operation;
//import org.opengis.geometry.Envelope;
//import org.opengis.parameter.ParameterValueGroup;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
///**
// * Test for the reprojection modules.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestGeotools extends HMTestCase {
//
//    public void testCoverageReprojector() throws Exception {
//        double n = 5140020.0;
//        double s = 5139780.0;
//        double w = 1640650.0;
//        double e = 1640950.0;
//        int rows = 8;
//        int cols = 10;
//
//        double[][] elevationData = new double[][]{//
//        {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
//                {600, NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
//                {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
//                {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
//                {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
//                {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
//                {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
//                {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};
//
//        CoordinateReferenceSystem crs = null;
//        try {
//            crs = CRS.decode("EPSG:32632");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        int height = elevationData.length;
//        int width = elevationData[0].length;
//        int dataType = DataBuffer.TYPE_DOUBLE;
//
//        ComponentSampleModel sampleModel = new ComponentSampleModel(dataType, width, height, 1, width, new int[]{0});
//        WritableRaster raster = RasterFactory.createWritableRaster(sampleModel, null);
//        for( int y = 0; y < height; y++ ) {
//            for( int x = 0; x < width; x++ ) {
//                raster.setSample(x, y, 0, elevationData[y][x]);
//            }
//        }
//
//        Envelope2D writeEnvelope = new Envelope2D(crs, w, s, e - w, n - s);
//        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
//        GridCoverage2D elevationCoverage = factory.create("to_resample", raster, writeEnvelope);
//
//        CoverageProcessor processor = CoverageProcessor.getInstance();
//        Operation resampleOp = processor.getOperation("Resample"); //$NON-NLS-1$
//
//        Envelope envelope = new Envelope2D(crs, w, s, e - w, n - s);
//        GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, cols / 2, rows / 2);
//        GridGeometry2D newGridGeometry = new GridGeometry2D(gridRange, envelope);
//
//        ParameterValueGroup param = resampleOp.getParameters();
//        param.parameter("Source").setValue(elevationCoverage.view(ViewType.GEOPHYSICS));
//        param.parameter("GridGeometry").setValue(newGridGeometry);
//        param.parameter("CoordinateReferenceSystem").setValue(crs);
//
//        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
//        String interpolationType = ImageUtilities.getInterpolationName(interpolation);
//        param.parameter("InterpolationType").setValue(interpolationType);
//
//        GridCoverage2D outGeodata = (GridCoverage2D) processor.doOperation(param);
//        outGeodata.getRenderedImage().getData();
//    }
//
//}
