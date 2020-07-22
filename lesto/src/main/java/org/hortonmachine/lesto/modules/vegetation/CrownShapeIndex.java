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
package org.hortonmachine.lesto.modules.vegetation;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.coverage.ProfilePoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("A Crown Shape Index based single tree detection.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("las, tree")
@Label(HMConstants.LESTO + "/vegetation")
@Name("CrownShapeIndex")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class CrownShapeIndex extends HMModel {

	@Description("Input CHM.")
	@UI(HMConstants.FILEIN_UI_HINT_RASTER)
	@In
	public String inChm;

	@Description("Search area size.")
	@In
	public double pSearchAreaSize = 40.0;

	@Description("Skip border.")
	@In
	public boolean doSkipborder = false;

	@Description("Output grid areas with volume definitions.")
	@UI(HMConstants.FILEOUT_UI_HINT)
	@In
	public String outTrees = null;

	@Description("Output CSI raster.")
	@UI(HMConstants.FILEOUT_UI_HINT)
	@In
	public String outCsi = null;

	@Description("Output Positive Opennes raster.")
	@UI(HMConstants.FILEOUT_UI_HINT)
	@In
	public String outPositiveOpennes = null;

	@Description("Output Negative Openness raster.")
	@UI(HMConstants.FILEOUT_UI_HINT)
	@In
	public String outNegativeOpenness = null;

	@Execute
	public void process() throws Exception {
		checkNull(inChm);

		GridCoverage2D inChmGC = null;
		CoordinateReferenceSystem crs = null;

		inChmGC = getRaster(inChm);
		crs = inChmGC.getCoordinateReferenceSystem();

		/// create the output featurecollection
		DefaultFeatureCollection outTreesFC = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("trees");
		b.setCRS(crs);
		b.add("the_geom", Point.class);
		b.add("csi", Double.class);
		b.add("height", Double.class);
		SimpleFeatureType type = b.buildFeatureType();
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

		RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inChmGC);
		int nCols = regionMap.getCols();
		int nRows = regionMap.getRows();
		double xRes = regionMap.getXres();
		double yRes = regionMap.getYres();
		RandomIter chmIter = CoverageUtilities.getRandomIterator(inChmGC);
		GridGeometry2D gridGeometry = inChmGC.getGridGeometry();

		WritableRaster csiWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null,
				HMConstants.doubleNovalue);

		WritableRaster positiveOpennesWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null,
				HMConstants.doubleNovalue);

		WritableRaster negativeOpennesWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null,
				HMConstants.doubleNovalue);

		int skipCols = (int) Math.ceil(pSearchAreaSize / 2 / xRes);
		int skipRows = (int) Math.ceil(pSearchAreaSize / 2 / yRes);
		if (!doSkipborder) {
			skipCols = 0;
			skipRows = 0;
		}

		int searchCells = (int) Math.ceil(pSearchAreaSize / xRes);

		pm.beginTask("Calculating Crown Shape Index...", (nRows - 2 * skipRows) * (nCols - 2 * skipCols));
		for (int row = skipRows; row < nRows - skipRows; row++) {
			for (int col = skipCols; col < nCols - skipCols; col++) {

				GridNode node = new GridNode(chmIter, nCols, nRows, xRes, yRes, col, row);
				if (!node.isValid()) {
					continue;
				}
				Coordinate nodeCoord = CoverageUtilities.coordinateFromColRow(col, row, gridGeometry);

				GridNode[][] window = node.getWindow(searchCells);
				int rows = window.length;
				int cols = window[0].length;
				int centerX = (int) (Math.ceil(cols / 2));
				int centerY = (int) (Math.ceil(rows / 2));

				List<double[]> phi12List = new ArrayList<>();
				// get the right cells
				GridNode nw = window[0][0];
				if (nw.isValid()) {
					double[] phi12NW = getNodePhi12(chmIter, gridGeometry, nodeCoord, nw);
					phi12List.add(phi12NW);
				}
				GridNode n = window[0][centerX];
				if (n.isValid()) {
					double[] phi12N = getNodePhi12(chmIter, gridGeometry, nodeCoord, n);
					phi12List.add(phi12N);
				}
				GridNode ne = window[0][cols - 1];
				if (ne.isValid()) {
					double[] phi12NE = getNodePhi12(chmIter, gridGeometry, nodeCoord, ne);
					phi12List.add(phi12NE);
				}
				GridNode w = window[centerY][0];
				if (w.isValid()) {
					double[] phi12W = getNodePhi12(chmIter, gridGeometry, nodeCoord, w);
					phi12List.add(phi12W);
				}
				GridNode e = window[centerY][cols - 1];
				if (e.isValid()) {
					double[] phi12E = getNodePhi12(chmIter, gridGeometry, nodeCoord, e);
					phi12List.add(phi12E);
				}
				GridNode sw = window[rows - 1][0];
				if (sw.isValid()) {
					double[] phi12SW = getNodePhi12(chmIter, gridGeometry, nodeCoord, sw);
					phi12List.add(phi12SW);
				}
				GridNode s = window[rows - 1][centerX];
				if (s.isValid()) {
					double[] phi12S = getNodePhi12(chmIter, gridGeometry, nodeCoord, s);
					phi12List.add(phi12S);
				}
				GridNode se = window[rows - 1][cols - 1];
				if (se.isValid()) {
					double[] phi12SE = getNodePhi12(chmIter, gridGeometry, nodeCoord, se);
					phi12List.add(phi12SE);
				}

				boolean allPhi1Minor90 = !phi12List.stream().anyMatch(phi12 -> phi12[0] >= 90);
				int count = phi12List.size();

				double finalPhi3;
				if (allPhi1Minor90) {
					finalPhi3 = (179.9 - 0.1) / 2;
				} else {
					// calc phi3 and average
					double sum = 0;
					for (double[] phi12 : phi12List) {
						sum = sum + (phi12[0] - phi12[1]) / 2.0;
					}
					finalPhi3 = sum / count;
				}

				csiWR.setSample(col, row, 0, finalPhi3);

//              Create the output for the original values of positive and negative openness (average on 8 directions)
				double finalPhi1 = 0;
				double finalPhi2 = 0;
				for (double[] phi12 : phi12List) {
					finalPhi1 = finalPhi1 + phi12[2];
					finalPhi2 = finalPhi2 + phi12[3];
				}
				finalPhi1 = finalPhi1 / count;
				finalPhi2 = finalPhi2 / count;
				positiveOpennesWR.setSample(col, row, 0, finalPhi1);
				negativeOpennesWR.setSample(col, row, 0, finalPhi2);

			}
			pm.worked(1);
		}
		pm.done();

		WritableRandomIter csiIter = CoverageUtilities.getWritableRandomIterator(csiWR);
//        WritableRandomIter potitiveIter = CoverageUtilities.getWritableRandomIterator(positiveOpennesWR);
//        WritableRandomIter negativeIter = CoverageUtilities.getWritableRandomIterator(negativeOpennesWR);
		pm.beginTask("Finding local maxima...", (nRows - 2 * skipRows) * (nCols - 2 * skipCols));
		for (int row = skipRows; row < nRows - skipRows; row++) {
			for (int col = skipCols; col < nCols - skipCols; col++) {
				GridNode node = new GridNode(csiIter, nCols, nRows, xRes, yRes, col, row);
				List<GridNode> validSurroundingNodes = node.getValidSurroundingNodes();
				if (node.isValid()) {
					boolean isLocalMaxima = true;
					double centerElev = node.elevation;
					for (GridNode n : validSurroundingNodes) {
						if (n.isValid()) {
							double elev = n.elevation;
							if (elev > centerElev) {
								isLocalMaxima = false;
								break;
							}
						}
					}
					if (isLocalMaxima) {
						double chm = chmIter.getSampleDouble(col, row, 0);
						Point p = gf.createPoint(CoverageUtilities.coordinateFromColRow(col, row, gridGeometry));
						Object[] values = new Object[] { p, centerElev, chm };
						builder.addAll(values);
						SimpleFeature feature = builder.buildFeature(null);
						outTreesFC.add(feature);
					}
				}

			}
			pm.worked(1);
		}
		pm.done();
		chmIter.done();

		GridCoverage2D outCsiGC = CoverageUtilities.buildCoverage("csi", csiWR, regionMap,
				inChmGC.getCoordinateReferenceSystem());

		GridCoverage2D outPositiveOpennessGC = CoverageUtilities.buildCoverage("positiveOpenness", positiveOpennesWR,
				regionMap, inChmGC.getCoordinateReferenceSystem());

		GridCoverage2D outNegativeOpennessGC = CoverageUtilities.buildCoverage("negativeOpenness", negativeOpennesWR,
				regionMap, inChmGC.getCoordinateReferenceSystem());

		dumpRaster(outCsiGC, outCsi);
		dumpRaster(outPositiveOpennessGC, outPositiveOpennes);
		dumpRaster(outNegativeOpennessGC, outNegativeOpenness);

		dumpVector(outTreesFC, outTrees);
	}

	private double[] getNodePhi12(RandomIter iter, GridGeometry2D gridGeometry, Coordinate nodeCoord, GridNode node)
			throws TransformException {
		Coordinate nwCoord = CoverageUtilities.coordinateFromColRow(node.col, node.row, gridGeometry);
		List<ProfilePoint> profilePoints = CoverageUtilities.doProfile(iter, gridGeometry, nodeCoord, nwCoord);

		ProfilePoint startPoint = profilePoints.remove(0);

		// calculate phi1
		double phi1OriginalValue = calculatePhi(profilePoints, startPoint);
//		double[] phi12OriginalValue = calculatePhi2(profilePoints, startPoint);
//		double phi1OriginalValue = phi12OriginalValue[0];
//		double phi2OriginalValue = phi12OriginalValue[1];

		// calculate phi2
		// to do so, we need to invert the profile and do the same as before
		double maxElev = startPoint.getElevation();
		for (ProfilePoint pp : profilePoints) {
			double pElev = pp.getElevation();
			maxElev = Math.max(pElev, maxElev);
		}
		// now redefine the profile by removing the values from the max
		List<ProfilePoint> reversedProfilePoints = new ArrayList<>();
		for (ProfilePoint pp : profilePoints) {
			double pElev = pp.getElevation();

			ProfilePoint newP = new ProfilePoint(pp.getProgressive(), maxElev - pElev, pp.getPosition());
			reversedProfilePoints.add(newP);
		}
		ProfilePoint reversedStartPoint = new ProfilePoint(startPoint.getProgressive(),
				maxElev - startPoint.getElevation(), startPoint.getPosition());

		double phi2OriginalValue = calculatePhi(reversedProfilePoints, reversedStartPoint);

		double phi1 = 0.0;
		double phi2 = 0.0;

		// apply criteria
		if (150 <= phi1OriginalValue) {
			phi1 = 150;
		} else if (phi1OriginalValue >= 90 && phi1OriginalValue < 150) {
			phi1 = 150;
		} else if (phi1OriginalValue >= 30 && phi1OriginalValue < 90) {
			phi1 = 30;
		}
		if (phi2OriginalValue <= 30) {
			phi2 = 30;
		} else if (phi2OriginalValue > 30 && phi2OriginalValue <= 90) {
			phi2 = 30;
		} else if (phi2OriginalValue > 90 && phi2OriginalValue <= 150) {
			phi2 = 150;
		}

		return new double[] { phi1, phi2, phi1OriginalValue, phi2OriginalValue };
	}

	private double calculatePhi(List<ProfilePoint> profilePoints, ProfilePoint startPoint) {
		double phi;
		double startElev = startPoint.getElevation();
		double lastElev = profilePoints.get(profilePoints.size() - 1).getElevation();
		boolean dopositiveAngle = false;

		// check if there is a point in the profile with an elevation greater than
		// startElev
		for (int i = 1; i < profilePoints.size() - 1; i++) {
			double currentElev = profilePoints.get(i).getElevation();
			double diff = currentElev - startElev;

			if (diff >= 0.0) {
				dopositiveAngle = true;
				break;
			}
		}

		// calculate positive angle for the points with an elevation greater than
		// startElev
		if (dopositiveAngle) {
			double maxAngle = Double.NEGATIVE_INFINITY;
			for (ProfilePoint pp : profilePoints) {
				double pElev = pp.getElevation();

				if (pElev > startElev) {
					double tanAngle = (pElev - startElev) / (pp.getProgressive() - startPoint.getProgressive());
					double angleRad = Math.atan(tanAngle);

					maxAngle = Math.max(Math.toDegrees(angleRad), maxAngle);
				}
			}
			phi = 90 - maxAngle;
		} else {
			// the second case is relative to negative values of the elevation angle
			// in this case the minimum angle is chosen
			double minAngle = Double.POSITIVE_INFINITY;
			for (ProfilePoint pp : profilePoints) {
				double pElev = pp.getElevation();
				double tanAngle = Math.abs(pElev - startElev) / (pp.getProgressive() - startPoint.getProgressive());
				double angleRad = Math.atan(tanAngle);

				double angleDeg = Math.toDegrees(angleRad);
				minAngle = Math.min(Math.abs(angleDeg), minAngle);
			}
			phi = 90 + minAngle;
		}
		return phi;
	}

	private double[] calculatePhi2(List<ProfilePoint> profilePoints, ProfilePoint startPoint) {
		double startElev = startPoint.getElevation();

		double maxAngle = Double.NEGATIVE_INFINITY;
		double minAngle = Double.POSITIVE_INFINITY;
		for (ProfilePoint pp : profilePoints) {
			double pElev = pp.getElevation();

			double tanAngle = (pElev - startElev) / (pp.getProgressive() - startPoint.getProgressive());
			double angleRad = Math.atan(tanAngle);

			maxAngle = Math.max(Math.toDegrees(angleRad), maxAngle);
			minAngle = Math.min(Math.toDegrees(angleRad), minAngle);
		}
		double phiMax = 90 - maxAngle;
		double phiMin = 90 + maxAngle;

		return new double[] { phiMax, phiMin };
	}

	public static void main(String[] args) throws Exception {
		String inDsm = "D:\\lavori_tmp\\2020_diadalos\\WP06_VEGETATION\\test_oono/plot_77_dsm.asc";
		String inDtm = "D:\\lavori_tmp\\2020_diadalos\\WP06_VEGETATION\\test_oono/plot_77_dtm.asc";
		String chm = "D:\\lavori_tmp\\2020_diadalos\\WP06_VEGETATION\\test_oono/plot_77_chm.asc";
		String outShp = "D:\\lavori_tmp\\2020_diadalos\\WP06_VEGETATION\\test_oono/out.shp";
		String outCsi = "D:\\lavori_tmp\\2020_diadalos\\WP06_VEGETATION\\test_oono/plot_77_csi.asc";
		String outPositiveOpenness = "D:\\lavori_tmp\\2020_diadalos\\WP06_VEGETATION\\test_oono/plot_77_positiveopenness_4.asc";
		String outNegativeOpenness = "D:\\lavori_tmp\\2020_diadalos\\WP06_VEGETATION\\test_oono/plot_77_negativeopenness_4.asc";
		double area = 40;

		OmsRasterDiff rd = new OmsRasterDiff();
		rd.inRaster1 = OmsRasterReader.readRaster(inDsm);
		rd.inRaster2 = OmsRasterReader.readRaster(inDtm);
		rd.doNegatives = false;
		rd.process();
		GridCoverage2D chmGC = rd.outRaster;
		OmsRasterWriter.writeRaster(chm, chmGC);

		CrownShapeIndex abv = new CrownShapeIndex();
		abv.inChm = chm;
		abv.pSearchAreaSize = area;
		abv.outTrees = outShp;
		abv.outCsi = outCsi;
		abv.outPositiveOpennes = outPositiveOpenness;
		abv.outNegativeOpenness = outNegativeOpenness;
		abv.process();
	}

}
