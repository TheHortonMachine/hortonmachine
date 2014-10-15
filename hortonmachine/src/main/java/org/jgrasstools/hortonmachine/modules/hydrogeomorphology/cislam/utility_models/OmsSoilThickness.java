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

//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSHALSTAB_inSlope_DESCRIPTION;
//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_doRound_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSLOPEFORCISLAM_inSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSOILTHICKNESS_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSOILTHICKNESS_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSOILTHICKNESS_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSOILTHICKNESS_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSOILTHICKNESS_outSoilThickness_DESCRIPTION;

import java.awt.image.RenderedImage;
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
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
//import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils.MapPreprocessingUtilities;

@Description(OMSCISLAM_OMSSOILTHICKNESS_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSSOILTHICKNESS_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSSOILTHICKNESS_NAME)
@Status(OMSCISLAM_OMSSOILTHICKNESS_STATUS)
@License(OMSCISLAM_LICENSE)
public class OmsSoilThickness extends JGTModel {

	@Description(OMSCISLAM_OMSSLOPEFORCISLAM_inSlope_DESCRIPTION)
	@Unit("m/m")
	@In
	public GridCoverage2D inSlope = null;

	@Description(OMSCISLAM_OMSSOILTHICKNESS_outSoilThickness_DESCRIPTION)
	@Unit("m")
	@Out
	public GridCoverage2D outSoilThickness = null;

	private HortonMessageHandler msg = HortonMessageHandler.getInstance();

	@Execute
	public void process() throws Exception {
		if (!concatOr(outSoilThickness == null, doReset)) {
			return;
		}
		checkNull(inSlope);
		
		// Check if input map is valid for computation
		if (!MapPreprocessingUtilities.isValidSlopeMap(inSlope, pm)) {
			String errorMessage = "Input Slope map does not meet requirements. Consider using OmsSlopeForCislam module.";
			pm.errorMessage(errorMessage);
			throw new ModelsIllegalargumentException(errorMessage, this);
		} else {
			pm.message(msg.message("cislam.omssoilthickness.validslopemapprovided"));
		}

		RegionMap regionMap = CoverageUtilities
				.getRegionParamsFromGridCoverage(inSlope);
		int cols = regionMap.getCols();
		int rows = regionMap.getRows();
		double xRes = regionMap.getXres();
		double yRes = regionMap.getYres();

		// Prepare tools for iterating over the slope map
		RenderedImage slopeRI = inSlope.getRenderedImage();
		RandomIter slopeRandomIter = RandomIterFactory.create(slopeRI, null);

		// Prepare data structures for storing soil thickness
		WritableRaster soilThicknessWR = CoverageUtilities
				.createDoubleWritableRaster(cols, rows, null, null,
						JGTConstants.doubleNovalue);
		WritableRandomIter soilThicknessIter = RandomIterFactory
				.createWritable(soilThicknessWR, null);

		// Send message to progress monitor
		pm.beginTask("Calculating soil thickness...", rows);

		double slopeCurrent;
		double soilthickness = 0;
		// Cycling into the valid region.
		for (int r = 1; r < rows - 1; r++) {
			for (int c = 1; c < cols - 1; c++) {
				// GridNode node = new GridNode(slopeRandomIter, cols, rows,
				// xRes, yRes, c, r);
				slopeCurrent = slopeRandomIter.getSampleDouble(c, r, 0);
				// The following will not work as getSampleDouble returns a
				// Double.NaN
				// if (slopeCurrent != JGTConstants.doubleNovalue) {
				if (Double.isInfinite(slopeCurrent)) {
					pm.errorMessage("Infinite value spotted in slope map: fix before proceeding. Now aborting!" );
					return;
				}
				if (slopeCurrent==0.0) {
					pm.errorMessage("Zero value spotted in slope map: fix with OmsSlopeForCislam before proceeding. Now aborting!" );
					return;
				}
					if (!Double.isNaN(slopeCurrent)) {
						soilthickness = calculateSoilThickness(slopeCurrent);
					} else {
						soilthickness = JGTConstants.doubleNovalue;
					}
				
				// Use the iterator to write in the writable raster.
				soilThicknessIter.setSample(c, r, 0, soilthickness);

			}
			pm.worked(1);
		}
		pm.done();

		CoverageUtilities.setNovalueBorder(soilThicknessWR);
		outSoilThickness = CoverageUtilities.buildCoverage("soilthikness",
				soilThicknessWR, regionMap,
				inSlope.getCoordinateReferenceSystem());

	}

	/**
	 * Calculates the soil thickness in a given {@link GridNode}. Implements
	 * logic from Equation 20 of reference paper: Lanni et Al. (2012) on Hydrol.
	 * Earth Syst. Sci., 16, 3959-3971 Note that
	 * 
	 * @param slopeCurrent
	 *            the current grid node.
	 * @param slope
	 *            (tan_beta) at the current grid node
	 * @return the value of aspect.
	 */
	public static double calculateSoilThickness(double slopeCurrent) {

		double soilThickness;
		if (slopeCurrent <= Math.tan(90.0 / 180.0 * Math.PI)) {
			soilThickness = 1.006 - 0.85 * slopeCurrent;
			if (soilThickness < 0.1)
				soilThickness = 0.1;
		} else {
			soilThickness = 0.0;
		}
		return soilThickness;
	}

}
