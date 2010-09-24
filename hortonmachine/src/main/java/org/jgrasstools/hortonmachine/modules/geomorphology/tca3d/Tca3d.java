package org.jgrasstools.hortonmachine.modules.geomorphology.tca3d;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("The OMS3 component representation of the tca model. The upslope catchment (or simply contributing) areas represent the planar projection of the areas afferent to a point in the basin. Once the drainage directions have been defined, it is possible to calculate, for each site, the total drainage area afferent to it, indicated as TCA (Total Contributing Area).")
@Author(name = "Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo")
@Bibliography("Take this from the Horton Manual")
@Status(Status.DRAFT)
@License("GPL3")
public class Tca3d extends JGTModel {
    /*
     * EXTERNAL VARIABLES
     */
    @Description("The depitted elevation model.")
    @In
    public GridCoverage2D inPit = null;

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of total contributing areas 3d.")
    @Out
    public GridCoverage2D outTca3d = null;

    /*
     * INTERNAL VARIABLES
     */
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private static final double NaN = doubleNovalue;

    private int cols;
    private int rows;
    private double xRes;
    private double yRes;
    /**
     * Calculates total contributing areas
     * 
     * @throws Exception
     */
    @Execute
    public void process() throws Exception {
        if (!concatOr(outTca3d == null, doReset)) {
            return;
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage pitfillerRI = inPit.getRenderedImage();
        WritableRaster pitWR = CoverageUtilities.renderedImage2WritableRaster(pitfillerRI, false);
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);

        pm.message(msg.message("tca3d.initializematrix")); //$NON-NLS-1$

        // Initialize new RasterData and set value
        WritableRaster tca3dWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, NaN);

        tca3dWR = area3d(pitWR, flowWR, tca3dWR);
        outTca3d = CoverageUtilities.buildCoverage("tca3d", tca3dWR, regionMap, //$NON-NLS-1$
                inPit.getCoordinateReferenceSystem());

    }

    private WritableRaster area3d( WritableRaster pitImage, WritableRaster flowImage, WritableRaster tca3dImage ) {
        int[][] tri = {{0, 0}, {1, 2}, /* tri 012 */
        {3, 2}, /* tri 023 */
        {3, 4}, /* tri 034 |4|3|2| */
        {5, 4}, /* tri 045 |5|0|1| */
        {5, 6}, /* tri 056 |6|7|8| */
        {7, 6}, /* tri 067 */
        {7, 8}, /* tri 078 */
        {1, 8} /* tri 089 */};

        int[][] dir = ModelsSupporter.DIR_WITHFLOW_EXITING_INVERTED;
        int nnov = 0;

        double dx = xRes;
        double dy = yRes;
        double semiptr = 0.0, area = 0.0, areamed = 0.0;

        // areatr contains areas of 8 triangles having vertex in the 8 pixel
        // around

        double[] grid = new double[11];
        // grid contains the dimension of pixels according with flow directions
        grid[0] = grid[9] = grid[10] = 0;
        grid[1] = grid[5] = abs(dx);
        grid[3] = grid[7] = abs(dy);
        grid[2] = grid[4] = grid[6] = grid[8] = sqrt(dx * dx + dy * dy);

        // contains the triangle's side
        double latitr[] = new double[3];

        // per ogni lato del triangolo contiene il dislivello e la distanza
        // planimetrica
        double[][] dzdiff = new double[3][2];

        RandomIter pitIter = RandomIterFactory.create(pitImage, null);
        WritableRandomIter tca3dIter = RandomIterFactory.createWritable(tca3dImage, null);

        pm.message(msg.message("tca3d.woringon")); //$NON-NLS-1$

        for( int j = 1; j < rows - 1; j++ ) {
            for( int i = 1; i < cols - 1; i++ ) {
                double pitAtIJ = pitIter.getSampleDouble(i, j, 0);
                nnov = 0;
                area = 0;
                areamed = 0;
                final double[] areatr = new double[9];
                if (!isNovalue(pitAtIJ)) {
                    // calculates the area of the triangle
                    for( int k = 1; k <= 8; k++ ) {
                        double pitAtK0 = pitIter.getSampleDouble(i + dir[tri[k][0]][0], j + dir[tri[k][0]][1], 0);
                        double pitAtK1 = pitIter.getSampleDouble(i + dir[tri[k][1]][0], j + dir[tri[k][1]][1], 0);

                        if (!isNovalue(pitAtK0) && !isNovalue(pitAtK1)) {
                            nnov++;
                            // calcola per ogni lato del triangolo in dislivello
                            // e la distanza planimetrica tra i pixel
                            // considerati.
                            dzdiff[0][0] = abs(pitAtIJ - pitAtK0);
                            dzdiff[0][1] = grid[dir[tri[k][0]][2]];
                            dzdiff[1][0] = abs(pitAtIJ - pitAtK1);
                            dzdiff[1][1] = grid[dir[tri[k][1]][2]];
                            dzdiff[2][0] = abs(pitAtK0 - pitAtK1);
                            dzdiff[2][1] = grid[1];
                            // calcola i lati del tringolo considerato
                            latitr[0] = sqrt(pow(dzdiff[0][0], 2) + pow(dzdiff[0][1], 2));
                            latitr[1] = sqrt(pow(dzdiff[1][0], 2) + pow(dzdiff[1][1], 2));
                            latitr[2] = sqrt(pow(dzdiff[2][0], 2) + pow(dzdiff[2][1], 2));
                            // calcola il semiperimetro del triangolo
                            semiptr = 0.5 * (latitr[0] + latitr[1] + latitr[2]);
                            // calcola l'area di ciascun triangolo
                            areatr[k] = sqrt(semiptr * (semiptr - latitr[0]) * (semiptr - latitr[1]) * (semiptr - latitr[2]));
                        }
                    }
                    if (nnov == 8)
                    // calcolo l'area del pixel sommando le aree degli 8
                    // triangoli.
                    {
                        for( int k = 1; k <= 8; k++ ) {
                            area = area + areatr[k] / 4;
                        }
                        tca3dIter.setSample(i, j, 0, area);
                    } else
                    // se il pixel e' circondato da novalue, non e' possibile
                    // comporre
                    // 8 triangoli, si calcola quindi l'area relativa ai
                    // triangoli completi
                    // si calcola la media dei loro valori e quindi si spalma il
                    // valore
                    // ottenuto sul pixel.
                    {
                        for( int k = 1; k <= 8; k++ ) {
                            area = area + areatr[k] / 4;
                        }
                        areamed = area / nnov;
                        tca3dIter.setSample(i, j, 0, areamed * 8);
                    }
                } else
                    tca3dIter.setSample(i, j, 0, NaN);
            }
        }
        pm.message(msg.message("tca3d.summ")); //$NON-NLS-1$
        RandomIter flowIter = RandomIterFactory.create(flowImage, null);
        return ModelsEngine.sumDownstream(flowIter, tca3dIter, cols, rows, pm);
    }
}