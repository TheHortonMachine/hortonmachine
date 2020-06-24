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

    @Description("Output grid areas with volume definitions.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTrees = null;

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

        int skipCols = (int) Math.ceil(pSearchAreaSize / 2 / xRes);
        int skipRows = (int) Math.ceil(pSearchAreaSize / 2 / yRes);

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

                // get the right cells
                GridNode nw = window[0][0];
                double[] phi12NW = getNodePhi12(chmIter, gridGeometry, nodeCoord, nw);
                GridNode n = window[0][centerX];
                double[] phi12N = getNodePhi12(chmIter, gridGeometry, nodeCoord, n);
                GridNode ne = window[0][cols - 1];
                double[] phi12NE = getNodePhi12(chmIter, gridGeometry, nodeCoord, ne);

                GridNode w = window[centerY][0];
                double[] phi12W = getNodePhi12(chmIter, gridGeometry, nodeCoord, w);
                GridNode e = window[centerY][cols - 1];
                double[] phi12E = getNodePhi12(chmIter, gridGeometry, nodeCoord, e);

                GridNode sw = window[rows - 1][0];
                double[] phi12SW = getNodePhi12(chmIter, gridGeometry, nodeCoord, sw);
                GridNode s = window[rows - 1][centerX];
                double[] phi12S = getNodePhi12(chmIter, gridGeometry, nodeCoord, s);
                GridNode se = window[rows - 1][cols - 1];
                double[] phi12SE = getNodePhi12(chmIter, gridGeometry, nodeCoord, se);

                double phi1NW = phi12NW[0];
                double phi1N = phi12N[0];
                double phi1NE = phi12NE[0];
                double phi1W = phi12W[0];
                double phi1E = phi12E[0];
                double phi1SW = phi12SW[0];
                double phi1S = phi12S[0];
                double phi1SE = phi12SE[0];

                double phi2NW = phi12NW[1];
                double phi2N = phi12N[1];
                double phi2NE = phi12NE[1];
                double phi2W = phi12W[1];
                double phi2E = phi12E[1];
                double phi2SW = phi12SW[1];
                double phi2S = phi12S[1];
                double phi2SE = phi12SE[1];

                double finalPhi3;
                if (phi1NW < 90 && phi1N < 90 && phi1NE < 90 && phi1W < 90 && phi1E < 90 && phi1SW < 90 && phi1S < 90
                        && phi1SE < 90) {
                    finalPhi3 = (179.9 - 0.1) / 2;
                } else {
                    // calc phi3 and average
                    double phi3NW = (phi1NW - phi2NW) / 2;
                    double phi3N = (phi1N - phi2N) / 2;
                    double phi3NE = (phi1NE - phi2NE) / 2;
                    double phi3W = (phi1W - phi2W) / 2;
                    double phi3E = (phi1E - phi2E) / 2;
                    double phi3SW = (phi1SW - phi2SW) / 2;
                    double phi3S = (phi1S - phi2S) / 2;
                    double phi3SE = (phi1SE - phi2SE) / 2;
                    finalPhi3 = (phi3NW + phi3N + phi3NE + phi3W + phi3E + phi3SW + phi3S + phi3SE) / 8;
                }

                csiWR.setSample(col, row, 0, finalPhi3);
            }
            pm.worked(1);
        }
        pm.done();

        WritableRandomIter csiIter = CoverageUtilities.getWritableRandomIterator(csiWR);
        pm.beginTask("Finding local maxima...", (nRows - 2 * skipRows) * (nCols - 2 * skipCols));
        for (int row = skipRows; row < nRows - skipRows; row++) {
            for (int col = skipCols; col < nCols - skipCols; col++) {
                GridNode node = new GridNode(csiIter, nCols, nRows, xRes, yRes, col, row);
                List<GridNode> validSurroundingNodes = node.getValidSurroundingNodes();
                if (node.isValid()) {
                    boolean isLocalMaxima = true;
                    double centerElev = node.elevation;
                    for (GridNode n : validSurroundingNodes) {
                        double elev = n.elevation;
                        if (elev > centerElev) {
                            isLocalMaxima = false;
                            break;
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
        dumpVector(outTreesFC, outTrees);
    }

    private double[] getNodePhi12(RandomIter iter, GridGeometry2D gridGeometry, Coordinate nodeCoord, GridNode node)
            throws TransformException {
        Coordinate nwCoord = CoverageUtilities.coordinateFromColRow(node.col, node.row, gridGeometry);
        List<ProfilePoint> profilePoints = CoverageUtilities.doProfile(iter, gridGeometry, nodeCoord, nwCoord);

        ProfilePoint startPoint = profilePoints.remove(0);

        // calculate phi1
        double phi1 = calculatePhi(profilePoints, startPoint);

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

        double phi2 = calculatePhi(reversedProfilePoints, reversedStartPoint);

        // apply criteria
        if (150 <= phi1) {
            phi1 = 150;
        } else if (phi1 >= 90 && phi1 < 150) {
            phi1 = 150;
        } else if (phi1 >= 30 && phi1 < 90) {
            phi1 = 30;
        }
        if (phi2 <= 30) {
            phi2 = 30;
        } else if (phi2 > 30 && phi2 <= 90) {
            phi2 = 30;
        } else if (phi2 > 90 && phi2 <= 150) {
            phi2 = 150;
        }

        return new double[] { phi1, phi2 };
    }

    private double calculatePhi(List<ProfilePoint> profilePoints, ProfilePoint startPoint) {
        double phi;
        double startElev = startPoint.getElevation();
        double lastElev = profilePoints.get(profilePoints.size() - 1).getElevation();

        if (lastElev >= startElev) {
            double maxAngle = Double.NEGATIVE_INFINITY;
            for (ProfilePoint pp : profilePoints) {
                double pElev = pp.getElevation();
                double tanAngle = (pElev - startElev) / (pp.getProgressive() - startPoint.getProgressive());
                double angle = Math.atan(tanAngle);

                maxAngle= Math.max(angle, maxAngle);
            }
            phi = 90 - maxAngle;
        } else {
            // in this case the minimum angle is chosen
            double minAngle = Double.POSITIVE_INFINITY;
            for (ProfilePoint pp : profilePoints) {
                double pElev = pp.getElevation();
                double tanAngle = (pElev - startElev) / (pp.getProgressive() - startPoint.getProgressive());
                double angle = Math.atan(tanAngle);

                minAngle = Math.min(angle, minAngle);
            }
            phi = 90 + minAngle;
        }
        return phi;
    }

    public static void main(String[] args) throws Exception {
        String inDsm = "/Users/hydrologis/TMP/VEGTEST/plot_77_dsm.asc";
        String inDtm = "/Users/hydrologis/TMP/VEGTEST/plot_77_dtm.asc";
        String chm = "/Users/hydrologis/TMP/VEGTEST/plot_77_chm.asc";
        String outShp = "/Users/hydrologis/TMP/VEGTEST/out.shp";
        double area = 5;

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
        abv.process();
    }

}
