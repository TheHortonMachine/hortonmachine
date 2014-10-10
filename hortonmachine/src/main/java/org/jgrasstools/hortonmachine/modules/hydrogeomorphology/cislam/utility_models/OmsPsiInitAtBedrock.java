/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been entirely contributed by Marco Foi (www.mcfoi.it)
 * 
 * "CI-slam module" is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models;

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSPSIINITATBEDROCK_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSPSIINITATBEDROCK_KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSPSIINITATBEDROCK_NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSPSIINITATBEDROCK_outPsiInitAtBedrock_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSPSIINITATBEDROCK_pPsiInitAtBedrockConstant_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSPSIINITATBEDROCK_STATUS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_inSoilThickness_DESCRIPTION;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description(OMSCISLAM_OMSPSIINITATBEDROCK_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSPSIINITATBEDROCK_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSPSIINITATBEDROCK_NAME)
@Status(OMSCISLAM_OMSPSIINITATBEDROCK_STATUS)
@License(OMSCISLAM_LICENSE)
public class OmsPsiInitAtBedrock extends JGTModel {

    public final double DEFAULT_PSI_CONSTANT = 0.05;

    @Description(OMSCISLAM_inPit_DESCRIPTION)
    @Unit("m")
    @In
    public GridCoverage2D inPit = null;

    @Description(OMSCISLAM_inSoilThickness_DESCRIPTION)
    @Unit("m")
    @In
    public GridCoverage2D inSoilThickness = null;

    @Description(OMSCISLAM_OMSPSIINITATBEDROCK_pPsiInitAtBedrockConstant_DESCRIPTION)
    @Unit("m")
    @In
    public double pPsiInitAtBedrockConstant = 0.0;

    @Description(OMSCISLAM_OMSPSIINITATBEDROCK_outPsiInitAtBedrock_DESCRIPTION)
    @Unit("m")
    @Out
    public GridCoverage2D outPsiInitAtBedrock = null;

    @Execute
    public void process() {
        if (!concatOr(outPsiInitAtBedrock == null, doReset)) {
            return;
        }
        // Either Pit or Soil Thickness are required to compute RegionMap and CRS
        if (inPit == null && inSoilThickness == null) {
            throw new ModelsIllegalargumentException("Either Pit or Soil Thickness are required to run the model. Check your input...",
                    this.getClass().getSimpleName());
        }

        GridCoverage2D refmap;
        if (inPit != null) {
            refmap = inPit;
        } else {
            refmap = inSoilThickness;
        }
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(refmap);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        WritableRaster mPsiInitAtBedrockWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null,
                JGTConstants.doubleNovalue);
        WritableRandomIter mPsiInitAtBedrockIter = RandomIterFactory.createWritable(mPsiInitAtBedrockWR, null);

        if (inSoilThickness == null && (pPsiInitAtBedrockConstant == 0 || pPsiInitAtBedrockConstant == JGTConstants.doubleNovalue)) {
            pm.message("No soil map nor constant Psi was provided: the model will fall back on using a predefinde Psi constant of 0.05m for the whole basin.");
            RandomIter pitIter = RandomIterFactory.create(inPit.getRenderedImage(), null);
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    if (!Double.isNaN(pitIter.getSampleDouble(c, r, 0))) {
                        mPsiInitAtBedrockIter.setSample(c, r, 0, DEFAULT_PSI_CONSTANT);
                    }
                }
            }
        } else if (inSoilThickness == null && pPsiInitAtBedrockConstant != 0) {
            pm.message("The module will produce a map with the provided constant Psi value.");
            RandomIter pitIter = RandomIterFactory.create(inPit.getRenderedImage(), null);
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    if (!Double.isNaN(pitIter.getSampleDouble(c, r, 0))) {
                        mPsiInitAtBedrockIter.setSample(c, r, 0, pPsiInitAtBedrockConstant);
                    }
                }
            }
        } else if (inSoilThickness != null) {
            pm.message("Since a soil map has been provided, the Psi map will be computed using the formula [1-(soil-thickness)]");
            RandomIter soilThicknessIter = RandomIterFactory.create(inSoilThickness.getRenderedImage(), null);
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double sT = soilThicknessIter.getSampleDouble(c, r, 0);
                    if ( !Double.isNaN(sT) ) {
                        mPsiInitAtBedrockIter.setSample(c, r, 0, (1.0 - sT));
                    }
                }
            }
        }

        outPsiInitAtBedrock = CoverageUtilities.buildCoverage("PsiInitAtBedrock", mPsiInitAtBedrockWR, regionMap,
                refmap.getCoordinateReferenceSystem());

    }

}
