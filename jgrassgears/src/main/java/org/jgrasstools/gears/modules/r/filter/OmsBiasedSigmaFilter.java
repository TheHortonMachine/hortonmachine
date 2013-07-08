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
package org.jgrasstools.gears.modules.r.filter;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.jgrasstools.gears.libs.modules.JGTConstants.*;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("A biased sigma filter.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("raster, filter, biased sigma")
@Label(JGTConstants.RASTERPROCESSING)
@Name("sigmafilter")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsBiasedSigmaFilter extends JGTModel {

	@Description("The input raster")
	@In
	public GridCoverage2D inGeodata;

	@Description("The output raster")
	@Out
	public GridCoverage2D outGeodata;

	@Execute
	public void process() throws Exception {
		checkNull(inGeodata);

		RegionMap regionMap = CoverageUtilities
				.getRegionParamsFromGridCoverage(inGeodata);

		int cols = regionMap.getCols();
		int rows = regionMap.getRows();
		double xres = regionMap.getXres();
		double yres = regionMap.getYres();

		RandomIter inIter = CoverageUtilities.getRandomIterator(inGeodata);
		WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(
				cols, rows, null, null, doubleNovalue);
		WritableRandomIter outIter = CoverageUtilities
				.getWritableRandomIterator(outWR);

		pm.beginTask("Processing filter...", cols - 2);
		for (int c = 1; c < cols - 1; c++) {
			for (int r = 1; r < rows - 1; r++) {
				GridNode node = new GridNode(inIter, cols, rows, xres, yres, c,
						r);
				if (node.isValid() && !node.touchesBound()) {
					double[][] window = node.getWindow(3, false);

					double elevation = node.elevation;
					double sumUpper = 0;
					double countUpper = 0;
					double sumLower = 0;
					double countLower = 0;

					for (int i = 0; i < window.length; i++) {
						for (int j = 0; j < window[0].length; j++) {
							if (i == 1 && j == 1) {
								continue;
							}
							if (window[i][j] >= elevation) {
								sumUpper = sumUpper + window[i][j];
								countUpper++;
							} else {
								sumLower = sumLower + window[i][j];
								countLower++;
							}
						}
					}

					double avgUpper = sumUpper / countUpper;
					double avgLower = sumLower / countLower;
					double value = 0;
					if (countUpper == 0) {
						value = avgLower;
					} else if (countLower == 0) {
						value = avgUpper;
					} else {
						double deltaUpper = abs(elevation - avgUpper);
						double deltaLower = abs(elevation - avgLower);
						if (deltaUpper < deltaLower) {
							value = avgUpper;
						} else {
							value = avgLower;
						}
					}
					if (isNovalue(value)) {
						throw new ModelsIllegalargumentException("Found NaN",
								this);
					}
					outIter.setSample(c, r, 0, value);
				}
			}
			pm.worked(1);
		}
		pm.done();

		outGeodata = CoverageUtilities.buildCoverage("sigma", outWR, regionMap,
				inGeodata.getCoordinateReferenceSystem());
	}

}
