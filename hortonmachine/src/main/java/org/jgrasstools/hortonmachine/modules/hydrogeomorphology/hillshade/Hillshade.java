/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hillshade;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("This class evalutate the hillshade of a DEM.")
@Author(name = "Daniele Andreis and Riccardo Rigon")
@Keywords("Hydrology, radiation")
@Bibliography("Corripio, J. G.: 2003," + " Vectorial algebra algorithms for calculating terrain parameters"
        + "from DEMs and the position of the sun for solar radiation modelling in mountainous terrain"
        + ", International Journal of Geographical Information Science 17(1), 1–23. and"
        + "Iqbal, M., 1983. An Introduction to solar radiation. In: , Academic Press, New York")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/")
public class Hillshade extends JGTModel {
    @Description("The map of the elevation.")
    @Role(Role.INPUT)
    @In
    public GridCoverage2D inElevation = null;

    @Description("The output map")
    @Out
    public GridCoverage2D outMap;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();


    @Description(" minimum diffuse insolation 0 to 1")
    @Role(Role.PARAMETER)
    @In
    public double defaultMinDiffuse = 0.0;

    @Description("azimuth")
    @Role(Role.PARAMETER)
    @In
    public double defaultAzimuth = 360;

    @Description("the sun elevation")
    @Role(Role.PARAMETER)
    @In
    public double defaultElevation = 90;

    private ModelsEngine engine = new ModelsEngine();
    private final static double doubleNoValue = JGTConstants.doubleNovalue;
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        // Check on the input parameters
        if (defaultAzimuth < 0.0 || defaultAzimuth > 360.0) {
            System.err.println(msg.message("hillshade.errAzimuth"));
        }
        if (defaultElevation < 0.0 || defaultElevation > 90.0) {
            System.err.println(msg.message("hillshade.errElevation"));
        }
        RenderedImage pitRI = inElevation.getRenderedImage();
        WritableRaster pitWR = CoverageUtilities.replaceNovalue(pitRI, -9999.0);
        // extract some attributes of the dem
        HashMap<String, Double> attribute = CoverageUtilities.getRegionParamsFromGridCoverage(inElevation);
        double dx = attribute.get(CoverageUtilities.XRES);
        int width = pitRI.getWidth();
        int height = pitRI.getHeight();
        pitRI = null;

        WritableRaster hillshadeWR = CoverageUtilities.createDoubleWritableRaster(width, height, null, pitWR.getSampleModel(),
                0.0);
        WritableRaster gradientWR = normalVector(pitWR, dx);

        calchillshade(pitWR, hillshadeWR, gradientWR, dx);

        // re-set the value to NaN
        setNoValueBorder(pitWR, width, height, hillshadeWR);

        outMap = CoverageUtilities
                .buildCoverage("insolation", hillshadeWR, attribute, inElevation.getCoordinateReferenceSystem());
    }

    /*
     * Re-set the no value to NaN (I have set it to -9999.0 in order to use this
     * value in the equation) and set the border to 0.
     */
    private void setNoValueBorder( WritableRaster pitWR, int width, int height, WritableRaster hillshadeWR ) {
        for( int y = 2; y < height - 2; y++ ) {
            for( int x = 2; x < width - 2; x++ ) {
                if (pitWR.getSampleDouble(x, y, 0) == -9999.0) {
                    hillshadeWR.setSample(x, y, 0,doubleNoValue);
                }
            }
        }

        // maybe NaN instead of 0
        for( int y = 0; y < height; y++ ) {
            hillshadeWR.setSample(0, y, 0, 0);
            hillshadeWR.setSample(1, y, 0, 0);
            hillshadeWR.setSample(width - 2, y, 0, 0);
            hillshadeWR.setSample(width - 1, y, 0, 0);
        }

        for( int x = 2; x < width - 2; x++ ) {
            hillshadeWR.setSample(x, 0, 0, 0);
            hillshadeWR.setSample(x, 1, 0, 0);
            hillshadeWR.setSample(x, height - 2, 0, 0);
            hillshadeWR.setSample(x, height - 1, 0, 0);
        }
    }

    /**
     * Evaluate the hillshade.
     * 
     * @param pitWR
     *            the raster of elevation.
     * @param hillshadeWR
     *            the WR where store the result.
     * @param gradientWR
     *            the raster of the gradient value of the dem.
     * @param dx
     *            the resolution of the dem. .
     */
    private void calchillshade( WritableRaster pitWR, WritableRaster hillshadeWR, WritableRaster gradientWR, double dx ) {

        defaultAzimuth = Math.toRadians(defaultAzimuth);
        defaultElevation = Math.toRadians(defaultElevation);

        double[] sunVector = calcSunVector();
        double[] normalSunVector = engine.calcNormalSunVector(sunVector);
        double[] inverseSunVector = engine.calcInverseSunVector(sunVector);
        int rows = pitWR.getHeight();
        int cols = pitWR.getWidth();
        WritableRaster sOmbraWR = engine.calculateFactor(rows, cols, sunVector, inverseSunVector, normalSunVector, pitWR, dx);
        pm.beginTask(msg.message("hillshade.calculating"), rows * cols);
        for( int j = 1; j < rows - 1; j++ ) {
            for( int i = 1; i < cols - 1; i++ ) {

                double[] ng = gradientWR.getPixel(i, j, new double[3]);
                double cosinc = engine.scalarProduct(sunVector, ng);
                if (cosinc < 0) {
                    sOmbraWR.setSample(i, j, 0, 0);
                }
                hillshadeWR.setSample(i, j, 0, (int) (212.5 * (cosinc * sOmbraWR.getSample(i, j, 0) + defaultMinDiffuse)));
                pm.worked(1);
            }
        }
        pm.done();
    }


    protected double[] calcSunVector() {
        double[] sunVector = new double[3];
        sunVector[0] = Math.sin(defaultAzimuth) * Math.cos(defaultElevation);
        sunVector[1] = -Math.cos(defaultAzimuth) * Math.cos(defaultElevation);
        sunVector[2] = Math.sin(defaultElevation);
        return sunVector;

    }


    protected WritableRaster normalVector( WritableRaster pitWR, double res ) {
        int minX = pitWR.getMinX();
        int minY = pitWR.getMinY();
        int rows = pitWR.getHeight();
        int cols = pitWR.getWidth();

        RandomIter pitIter = RandomIterFactory.create(pitWR, null);
        /*
         * Initialize the Image of the normal vector in the central point of the
         * cells, which have 3 components so the Image have 3 bands..
         */
        SampleModel sm = RasterFactory.createBandedSampleModel(5, cols, rows, 3);
        WritableRaster tmpNormalVectorWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, sm, 0.0);
        WritableRandomIter tmpNormaIter = RandomIterFactory.createWritable(tmpNormalVectorWR, null);
        /*
         * apply the corripio's formula (is the formula (3) in the article)
         */
        for( int j = minY; j < minX + rows - 1; j++ ) {
            for( int i = minX; i < minX + cols - 1; i++ ) {
                double zij = pitIter.getSampleDouble(i, j, 0);
                double zidxj = pitIter.getSampleDouble(i + 1, j, 0);
                double zijdy = pitIter.getSampleDouble(i, j + 1, 0);
                double zidxjdy = pitIter.getSampleDouble(i + 1, j + 1, 0);
                double firstComponent = 0.5 * res * (zij - zidxj + zijdy - zidxjdy);
                double secondComponent = 0.5 * res * (zij + zidxj - zijdy - zidxjdy);
                double thirthComponent = (res * res);
                double den = Math.sqrt(firstComponent * firstComponent + secondComponent * secondComponent + thirthComponent
                        * thirthComponent);
                tmpNormaIter.setPixel(i, j, new double[]{firstComponent / den, secondComponent / den, thirthComponent / den});

            }
        }
        pitIter.done();

        return tmpNormalVectorWR;

    }
}
