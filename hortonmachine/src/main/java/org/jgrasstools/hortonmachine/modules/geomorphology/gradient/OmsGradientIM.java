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
package org.jgrasstools.hortonmachine.modules.geomorphology.gradient;

import static java.lang.Math.atan;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_doDegrees_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_outSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_pMode_DESCRIPTION;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralEnvelope;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.SldUtilities;
import org.jgrasstools.gears.utils.colors.ColorTables;
import org.jgrasstools.gears.utils.colors.RasterStyleUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

@Description(OMSGRADIENT_DESCRIPTION)
@Documentation(OMSGRADIENT_DOCUMENTATION)
@Author(name = OMSGRADIENT_AUTHORNAMES, contact = OMSGRADIENT_AUTHORCONTACTS)
@Keywords(OMSGRADIENT_KEYWORDS)
@Label(OMSGRADIENT_LABEL)
@Name("gradient_im")
@Status(OMSGRADIENT_STATUS)
@License(OMSGRADIENT_LICENSE)
public class OmsGradientIM extends JGTModel {
    @Description("The imagemosaic map of the digital elevation model (DEM or pit).")
    @In
    public String inElev = null;

    @Description(OMSGRADIENT_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSGRADIENT_doDegrees_DESCRIPTION)
    @In
    public boolean doDegrees = false;

    @Description("Lower slope threshold")
    @In
    public double pLowerThres = 0.0;

    @Description("Upper slope threshold")
    @In
    public double pUpperThres = 0.0;

    @Description(OMSGRADIENT_outSlope_DESCRIPTION)
    @Out
    public String outSlope = null;

    private int cellBuffer = 1;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int nCols;
    private double xRes;
    private int nRows;
    private double yRes;

    @Execute
    public void process() throws Exception {
        checkNull(inElev);

        File inFile = new File(inElev);

        File outFile = new File(outSlope);
        File outParentFolder = outFile.getParentFile();
        String outBaseName = FileUtilities.getNameWithoutExtention(outFile);

        File propertiesFile = FileUtilities.substituteExtention(inFile, "properties");
        HashMap<String, String> propertiesMap = FileUtilities.readFileToHasMap(propertiesFile.getAbsolutePath(), null, false);

        String xyREs = propertiesMap.get("Levels");
        String[] split = xyREs.split(",");
        double xRes = Double.parseDouble(split[0]);
        double yRes = Double.parseDouble(split[1]);

        String locationField = propertiesMap.get("LocationAttribute");

        ImageMosaicReader imReader = new ImageMosaicReader(inElev);
        CoordinateReferenceSystem crs = imReader.getCrs();

        GeneralEnvelope originalEnvelope = imReader.getOriginalEnvelope();
        double[] llCorner = originalEnvelope.getLowerCorner().getCoordinate();
        double[] urCorner = originalEnvelope.getUpperCorner().getCoordinate();

        SimpleFeatureCollection vectorBounds = OmsVectorReader.readVector(inElev);
        List<Geometry> boundsGeometries = FeatureUtilities.featureCollectionToGeometriesList(vectorBounds, true, locationField);

        int size = boundsGeometries.size();
        int count = 0;
        pm.beginTask("processing...", size);
        for( Geometry boundGeometry : boundsGeometries ) {
            count++;
            Envelope writeEnv = boundGeometry.getEnvelopeInternal();

            double writeEast = writeEnv.getMaxX();
            double writeWest = writeEnv.getMinX();
            double writeNorth = writeEnv.getMaxY();
            double writeSouth = writeEnv.getMinY();
            int writeCols = (int) ((writeEast - writeWest) / xRes);
            int writeRows = (int) ((writeNorth - writeSouth) / yRes);

            Envelope readEnv = new Envelope(writeEnv);
            readEnv.expandBy(cellBuffer * xRes, cellBuffer * yRes);

            double readEast = readEnv.getMaxX();
            double readWest = readEnv.getMinX();
            double readNorth = readEnv.getMaxY();
            double readSouth = readEnv.getMinY();
            // int readCols = (int) ((readEast - readWest) / xRes);
            // int readRows = (int) ((readNorth - readSouth) / yRes);

            GridGeometry2D writeGridGeometry = CoverageUtilities.gridGeometryFromRegionValues(writeNorth, writeSouth, writeEast,
                    writeWest, writeCols, writeRows, crs);
            WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(writeCols, writeRows, null, null,
                    JGTConstants.doubleNovalue);
            RegionMap writeParams = CoverageUtilities.gridGeometry2RegionParamsMap(writeGridGeometry);
            GridCoverage2D writeGC = CoverageUtilities.buildCoverage("tile", outWR, writeParams, crs);

            GeneralParameterValue[] readGeneralParameterValues = CoverageUtilities.createGridGeometryGeneralParameter(xRes, yRes,
                    readNorth, readSouth, readEast, readWest, crs);
            GridCoverage2D readGC = imReader.read(readGeneralParameterValues);
            GridGeometry2D readGridGeometry = readGC.getGridGeometry();

            GridCoordinates2D llGrid = readGridGeometry.worldToGrid(new DirectPosition2D(llCorner[0], llCorner[1]));
            GridCoordinates2D urGrid = readGridGeometry.worldToGrid(new DirectPosition2D(urCorner[0], urCorner[1]));
            int minX = llGrid.x;
            int maxY = llGrid.y; // y grid is inverse
            int maxX = urGrid.x;
            int minY = urGrid.y;
            // is there a gridrange shift?
            GridEnvelope2D gridRange2D = readGridGeometry.getGridRange2D();
            minY = minY + gridRange2D.y;
            maxY = maxY + gridRange2D.y;
            minX = minX + gridRange2D.x;
            maxX = maxX + gridRange2D.x;

            pm.message("X range: " + minX + " -> " + maxX);
            pm.message("Y range: " + minY + " -> " + maxY);

            // read raster at once, since a randomiter is way slower
            Raster readRaster = readGC.getRenderedImage().getData();

            WritableRandomIter writeIter = CoverageUtilities.getWritableRandomIterator(outWR);

            for( int writeCol = 0; writeCol < writeCols; writeCol++ ) {
                for( int writeRow = 0; writeRow < writeRows; writeRow++ ) {
                    DirectPosition writeGridToWorld = writeGridGeometry.gridToWorld(new GridCoordinates2D(writeCol, writeRow));
                    GridCoordinates2D worldToReadGrid = readGridGeometry.worldToGrid(writeGridToWorld);
                    int readCol = worldToReadGrid.x;
                    int readRow = worldToReadGrid.y;
                    int x = readCol;
                    int y = readRow;

                    if (x + cellBuffer > maxX || x - cellBuffer < minX || y + cellBuffer > maxY || y - cellBuffer < minY) {
                        continue;
                    }

                    // double read = readRaster.getSampleDouble(readCol, readRow, 0);

                    // extract the value to use for the algoritm. It is the finite difference
                    // approach.
                    double elevIJ = readRaster.getSampleDouble(x, y, 0);
                    double elevIJipre = readRaster.getSampleDouble(x - 1, y, 0);
                    double elevIJipost = readRaster.getSampleDouble(x + 1, y, 0);
                    double elevIJjpre = readRaster.getSampleDouble(x, y - 1, 0);
                    double elevIJjpost = readRaster.getSampleDouble(x, y + 1, 0);
                    if (!isNovalue(elevIJ) && !isNovalue(elevIJipre) && !isNovalue(elevIJipost) && !isNovalue(elevIJjpre)
                            && !isNovalue(elevIJjpost)) {
                        double xGrad = 0.5 * (elevIJipost - elevIJipre) / xRes;
                        double yGrad = 0.5 * (elevIJjpre - elevIJjpost) / yRes;
                        double grad = sqrt(pow(xGrad, 2) + pow(yGrad, 2));
                        if (doDegrees) {
                            grad = transform(grad);
                        }
                        if (grad >= pLowerThres && grad <= pUpperThres)
                            writeIter.setSample(writeCol, writeRow, 0, grad);
                    }
                }
            }

            File outTileFile = new File(outParentFolder, outBaseName + "_" + count + ".tiff");
            OmsRasterWriter.writeRaster(outTileFile.getAbsolutePath(), writeGC);
            File outStyleFile = new File(outParentFolder, outBaseName + "_" + count + ".sld");
            String styleForColortable = RasterStyleUtilities.createStyleForColortable(ColorTables.greyscaleinverse.name(),
                    pLowerThres, pUpperThres, null, 1);
            FileUtilities.writeFile(styleForColortable, outStyleFile);
            pm.worked(1);
        }
        pm.done();

        // checkNull(inElev);
        // HashMap<String, Double> regionMap =
        // CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        // nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        // nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        // xRes = regionMap.get(CoverageUtilities.XRES);
        // yRes = regionMap.get(CoverageUtilities.YRES);
        //
        // RenderedImage elevationRI = inElev.getRenderedImage();
        // RandomIter elevationIter = RandomIterFactory.create(elevationRI, null);
        // WritableRaster gradientWR = null;
        // if (pMode == 1) {
        // pm.message("Using Horn formula");
        // gradientWR = gradientHorn(elevationIter);
        // } else if (pMode == 2) {
        // pm.message("Using Evans formula");
        // gradientWR = gradientEvans(elevationIter);
        // } else {
        // pm.message("Using finite differences");
        // gradientWR = gradientDiff(elevationIter);
        // }
        // outSlope = CoverageUtilities.buildCoverage("gradient", gradientWR, regionMap,
        // inElev.getCoordinateReferenceSystem());
    }

    public static void main( String[] args ) throws Exception {
        OmsGradientIM g = new OmsGradientIM();
        g.inElev = "/media/lacntfs/oceandtm/q1swb_2008_export_043_xyz2_2m/q1swb_2008_export_043_xyz2_2m.shp";
        g.outSlope = "/media/lacntfs/oceandtm/testout/q1swb_2008_export_043_xyz2_2m_gradient.shp";
        g.pLowerThres = 0.0;
        g.pUpperThres = 0.3;
        g.process();

    }

    /**
    * Computes the gradient algorithm. p=f_{x}^{2}+f_{y}^{2}
    *  
    * The derivatives can be calculate with the  the horn formula:
    * <p>
    * f<sub>x</sub>=(2*f<sub>(x+1,y)</sub>+f<sub>(x+1,y-1)</sub>+
    *   f<sub>(x+1,y+1)</sub>-2*f<sub>(x-1,y)</sub>-f<sub>(x-1,y+1)</sub>-
    *   f<sub>(x-1,y-1)</sub>)/(8 &#916 x)
    * <br> 
    * f<sub>y</sub>=(2*f<sub>(x,y+1)</sub>+f<sub>(x+1,y+1)</sub>+
    * f<sub>(x-1,y+1)</sub>-2*f<sub>(x,y-1)</sub>-f<sub>(x+1,y-1)</sub>+
    * f<sub>(x-1,y-1)</sub>)/(8 &#916 y)
    * <p>
    * The kernel is compound of 9 cell (8 around the central pixel) and the numeration is:
    * <pre>
    * 1   2   3
    * 4   5   6
    * 7   8   9
    * </pre>
    * 
    * <p>
    * This numeration is used to extract the appropriate elevation value (es elev1 an so on)
    */
    private WritableRaster gradientHorn( RandomIter elevationIter ) {

        WritableRaster gradientWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("gradient.working"), nRows);
        for( int y = 1; y < nRows - 1; y++ ) {
            if (isCanceled(pm)) {
                return null;
            }
            for( int x = 1; x < nCols - 1; x++ ) {
                // extract the value to use for the algoritm. It is the finite difference approach.
                double elev5 = elevationIter.getSampleDouble(x, y, 0);
                double elev4 = elevationIter.getSampleDouble(x - 1, y, 0);
                double elev6 = elevationIter.getSampleDouble(x + 1, y, 0);
                double elev2 = elevationIter.getSampleDouble(x, y - 1, 0);
                double elev8 = elevationIter.getSampleDouble(x, y + 1, 0);
                double elev9 = elevationIter.getSampleDouble(x + 1, y + 1, 0);
                double elev1 = elevationIter.getSampleDouble(x - 1, y - 1, 0);
                double elev3 = elevationIter.getSampleDouble(x + 1, y - 1, 0);
                double elev7 = elevationIter.getSampleDouble(x - 1, y + 1, 0);

                if (isNovalue(elev5) || isNovalue(elev1) || isNovalue(elev2) || isNovalue(elev3) || isNovalue(elev4)
                        || isNovalue(elev6) || isNovalue(elev7) || isNovalue(elev8) || isNovalue(elev9)) {
                    gradientWR.setSample(x, y, 0, doubleNovalue);
                } else {
                    double fu = 2 * elev6 + elev9 + elev3;
                    double fd = 2 * elev4 + elev7 + elev1;
                    double xGrad = (fu - fd) / (8 * xRes);
                    fu = 2 * elev8 + elev7 + elev9;
                    fd = 2 * elev2 + elev1 + elev3;
                    double yGrad = (fu - fd) / (8 * yRes);
                    double grad = sqrt(pow(xGrad, 2) + pow(yGrad, 2));
                    if (doDegrees) {
                        grad = transform(grad);
                    }
                    gradientWR.setSample(x, y, 0, grad);
                }
            }
            pm.worked(1);
        }
        pm.done();

        return gradientWR;
    }

    /**
     * Transform the gradient value into degrees.
     * 
     * @param value the radiant based gradient.
     * @return the degree gradient.
     */
    private double transform( double value ) {
        return toDegrees(atan(value));
    }

    /**
     * Estimate the gradient (p=f_{x}^{2}+f_{y}^{2}) with a finite difference formula:
     * 
     * <pre>
     *  p=&radic{f<sub>x</sub>&sup2;+f<sub>y</sub>&sup2;}
     * f<sub>x</sub>=(f(x+1,y)-f(x-1,y))/(2 &#916 x) 
     * f<sub>y</sub>=(f(x,y+1)-f(x,y-1))/(2 &#916 y)
     * </pre>
     * 
    */
    private WritableRaster gradientDiff( RandomIter elevationIter ) {

        WritableRaster gradientWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("gradient.working"), nRows);
        for( int y = 1; y < nRows - 1; y++ ) {

            for( int x = 1; x < nCols - 1; x++ ) {
                // extract the value to use for the algoritm. It is the finite difference approach.
                double elevIJ = elevationIter.getSampleDouble(x, y, 0);
                double elevIJipre = elevationIter.getSampleDouble(x - 1, y, 0);
                double elevIJipost = elevationIter.getSampleDouble(x + 1, y, 0);
                double elevIJjpre = elevationIter.getSampleDouble(x, y - 1, 0);
                double elevIJjpost = elevationIter.getSampleDouble(x, y + 1, 0);
                if (isNovalue(elevIJ) || isNovalue(elevIJipre) || isNovalue(elevIJipost) || isNovalue(elevIJjpre)
                        || isNovalue(elevIJjpost)) {
                    gradientWR.setSample(x, y, 0, doubleNovalue);
                } else if (!isNovalue(elevIJ) && !isNovalue(elevIJipre) && !isNovalue(elevIJipost) && !isNovalue(elevIJjpre)
                        && !isNovalue(elevIJjpost)) {
                    double xGrad = 0.5 * (elevIJipost - elevIJipre) / xRes;
                    double yGrad = 0.5 * (elevIJjpre - elevIJjpost) / yRes;
                    double grad = sqrt(pow(xGrad, 2) + pow(yGrad, 2));
                    if (doDegrees) {
                        grad = transform(grad);
                    }
                    gradientWR.setSample(x, y, 0, grad);
                } else {
                    throw new ModelsIllegalargumentException("Error in gradient", this);
                }
            }
            pm.worked(1);
        }
        pm.done();

        return gradientWR;
    }

    /** estimate the gradient using the Horn formula.
     * <p>
     * Where the gradient is:
     * </p>
     * <pre>
     *  p=&radic{f<sub>x</sub>&sup2;+f<sub>y</sub>&sup2;}
     *  
     *  and the derivatives can be calculate with the  the Evans formula:
     * f<sub>x</sub>=(f(x+1,y)+f(x+1,y-1)+f(x+1,y+1)-f(x-1,y)-f(x-1,y+1)-f(x-1,y-1))/(6 &#916 x) 
     * f<sub>y</sub>=(f(x,y+1)+f(x+1,y+1)+f(x-1,y+1)-f(x,y-1)-f(x+1,y-1)+f(x-1,y-1))/(6 &#916 y)
     * <p>
     * The kernel is compound of 9 cell (8 around the central pixel) and the numeration is:
     * 
     * 1   2   3
     * 4   5   6
     * 7   8   9
     * 
     * This enumeration is used to extract the appropriate elevation value (es elev1 an so on)
     * 
     * </p>
     *
     */
    private WritableRaster gradientEvans( RandomIter elevationIter ) {

        WritableRaster gradientWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("gradient.working"), nRows);
        for( int y = 1; y < nRows - 1; y++ ) {

            for( int x = 1; x < nCols - 1; x++ ) {

                // extract the value to use for the algoritm. It is the finite difference approach.
                double elev5 = elevationIter.getSampleDouble(x, y, 0);
                double elev4 = elevationIter.getSampleDouble(x - 1, y, 0);
                double elev6 = elevationIter.getSampleDouble(x + 1, y, 0);
                double elev2 = elevationIter.getSampleDouble(x, y - 1, 0);
                double elev8 = elevationIter.getSampleDouble(x, y + 1, 0);
                double elev9 = elevationIter.getSampleDouble(x + 1, y + 1, 0);
                double elev1 = elevationIter.getSampleDouble(x - 1, y - 1, 0);
                double elev3 = elevationIter.getSampleDouble(x + 1, y - 1, 0);
                double elev7 = elevationIter.getSampleDouble(x - 1, y + 1, 0);

                if (isNovalue(elev5) || isNovalue(elev1) || isNovalue(elev2) || isNovalue(elev3) || isNovalue(elev4)
                        || isNovalue(elev6) || isNovalue(elev7) || isNovalue(elev8) || isNovalue(elev9)) {
                    gradientWR.setSample(x, y, 0, doubleNovalue);
                } else {
                    double fu = elev6 + elev9 + elev3;
                    double fd = elev4 + elev7 + elev1;
                    double xGrad = (fu - fd) / (6 * xRes);
                    fu = elev8 + elev7 + elev9;
                    fd = elev2 + elev1 + elev3;

                    double yGrad = (fu - fd) / (6 * yRes);
                    double grad = sqrt(pow(xGrad, 2) + pow(yGrad, 2));
                    if (doDegrees) {
                        grad = transform(grad);
                    }
                    gradientWR.setSample(x, y, 0, grad);
                }
            }

            pm.worked(1);
        }
        pm.done();

        return gradientWR;
    }

}
