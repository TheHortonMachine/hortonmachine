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
package org.hortonmachine.hmachine.modules.geomorphology.tca3d;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_DOCUMENTATION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_inPit_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_outTca_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.modules.ModelsSupporter;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

@Description(OMSTCA3D_DESCRIPTION)
@Documentation(OMSTCA3D_DOCUMENTATION)
@Author(name = OMSTCA3D_AUTHORNAMES, contact = OMSTCA3D_AUTHORCONTACTS)
@Keywords(OMSTCA3D_KEYWORDS)
@Label(OMSTCA3D_LABEL)
@Name(OMSTCA3D_NAME)
@Status(OMSTCA3D_STATUS)
@License(OMSTCA3D_LICENSE)
public class OmsTca3d extends HMModel {
    @Description(OMSTCA3D_inPit_DESCRIPTION)
    @In
    public GridCoverage2D inPit = null;

    @Description(OMSTCA3D_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSTCA3D_outTca_DESCRIPTION)
    @Out
    public GridCoverage2D outTca = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

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
        if (!concatOr(outTca == null, doReset)) {
            return;
        }
        checkNull(inPit, inFlow);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage pitfillerRI = inPit.getRenderedImage();
        WritableRaster pitWR = CoverageUtilities.renderedImage2WritableRaster(pitfillerRI, false);
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);

        // Initialize new RasterData and set value
        WritableRaster tca3dWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, doubleNovalue);

        tca3dWR = area3d(pitWR, flowWR, tca3dWR);
        outTca = CoverageUtilities.buildCoverage("tca3d", tca3dWR, regionMap, //$NON-NLS-1$
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

        pm.beginTask(msg.message("tca3d.woringon"), rows - 2); //$NON-NLS-1$
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
                    tca3dIter.setSample(i, j, 0, doubleNovalue);
            }
            pm.worked(1);
        }
        pm.done();
        RandomIter flowIter = RandomIterFactory.create(flowImage, null);
        return ModelsEngine.sumDownstream(flowIter, tca3dIter, cols, rows, null, null, pm);
    }
}