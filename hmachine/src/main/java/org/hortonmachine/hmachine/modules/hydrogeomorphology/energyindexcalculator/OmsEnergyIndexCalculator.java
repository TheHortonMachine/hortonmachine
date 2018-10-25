/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.energyindexcalculator;

import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static org.hortonmachine.gears.libs.modules.HMConstants.intNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.omega;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inAspect_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inBasins_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inCurvatures_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inSlope_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_outAltimetry_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_outArea_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_outEnergy_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_pDt_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_pEi_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_pEs_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.eicalculator.EIAltimetry;
import org.hortonmachine.gears.io.eicalculator.EIAreas;
import org.hortonmachine.gears.io.eicalculator.EIEnergy;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.opengis.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Coordinate;

@Description(OMSENERGYINDEXCALCULATOR_DESCRIPTION)
@Author(name = OMSENERGYINDEXCALCULATOR_AUTHORNAMES, contact = OMSENERGYINDEXCALCULATOR_AUTHORCONTACTS)
@Keywords(OMSENERGYINDEXCALCULATOR_KEYWORDS)
@Label(OMSENERGYINDEXCALCULATOR_LABEL)
@Name(OMSENERGYINDEXCALCULATOR_NAME)
@Status(OMSENERGYINDEXCALCULATOR_STATUS)
@License(OMSENERGYINDEXCALCULATOR_LICENSE)
public class OmsEnergyIndexCalculator extends HMModel {

    @Description(OMSENERGYINDEXCALCULATOR_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSENERGYINDEXCALCULATOR_inBasins_DESCRIPTION)
    @In
    public GridCoverage2D inBasins = null;

    @Description(OMSENERGYINDEXCALCULATOR_inCurvatures_DESCRIPTION)
    @In
    public GridCoverage2D inCurvatures = null;

    @Description(OMSENERGYINDEXCALCULATOR_inAspect_DESCRIPTION)
    @In
    public GridCoverage2D inAspect = null;

    @Description(OMSENERGYINDEXCALCULATOR_inSlope_DESCRIPTION)
    @In
    public GridCoverage2D inSlope = null;

    @Description(OMSENERGYINDEXCALCULATOR_pEs_DESCRIPTION)
    @In
    public int pEs = -1;

    @Description(OMSENERGYINDEXCALCULATOR_pEi_DESCRIPTION)
    @In
    public int pEi = -1;

    @Description(OMSENERGYINDEXCALCULATOR_pDt_DESCRIPTION)
    @In
    public double pDt = -1;

    @Description(OMSENERGYINDEXCALCULATOR_outAltimetry_DESCRIPTION)
    @Out
    public List<EIAltimetry> outAltimetry;

    @Description(OMSENERGYINDEXCALCULATOR_outEnergy_DESCRIPTION)
    @Out
    public List<EIEnergy> outEnergy;

    @Description(OMSENERGYINDEXCALCULATOR_outArea_DESCRIPTION)
    @Out
    public List<EIAreas> outArea;

    private static final int NOVALUE = intNovalue;
    private HashMap<Integer, Integer> id2indexMap = null;
    private HashMap<Integer, Integer> index2idMap = null;

    private double avgLatitude = -1;
    private double dx;
    private double dy;
    private int[][] eibasinID;
    private int[][] outputShadow;
    private double[][][] eibasinEmonth;
    private double[][] eibasinESrange;
    private double[][] eibasinES;
    private double[][] eibasinEI_mean;
    private double[][][] eibasinEI;
    private double[][] eibasinE;
    private double[][][] eibasinA;
    private final GeomorphUtilities geomorphUtilities = new GeomorphUtilities();
    private int eibasinNum;

    private RandomIter idbasinImageIterator;

    private RandomIter elevImageIterator;

    private WritableRaster curvatureImage;

    private RandomIter aspectImageIterator;

    private RandomIter slopeImageIterator;

    private int rows;

    private int cols;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inBasins);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        dx = regionMap.get(CoverageUtilities.XRES);
        dy = regionMap.get(CoverageUtilities.YRES);
        double n = regionMap.get(CoverageUtilities.NORTH);
        double s = regionMap.get(CoverageUtilities.SOUTH);
        double w = regionMap.get(CoverageUtilities.WEST);
        double e = regionMap.get(CoverageUtilities.EAST);

        double meanX = w + (e - w) / 2.0;
        double meanY = s + (n - s) / 2.0;

        Coordinate tmp = new Coordinate(meanX, meanY);
        MathTransform mathTransform = CRS.findMathTransform(inAspect.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84);
        Coordinate newC = JTS.transform(tmp, null, mathTransform);
        avgLatitude = newC.y;

        RenderedImage idbasinImage = inBasins.getRenderedImage();
        idbasinImageIterator = RandomIterFactory.create(idbasinImage, null);

        RenderedImage elevImage = inElev.getRenderedImage();
        elevImageIterator = RandomIterFactory.create(elevImage, null);

        RenderedImage tmpImage = inCurvatures.getRenderedImage();
        curvatureImage = CoverageUtilities
                .createWritableRaster(tmpImage.getWidth(), tmpImage.getHeight(), null, null, null);
        RandomIter tmpIterator = RandomIterFactory.create(tmpImage, null);
        // TODO check what this is for?!?!?
        for( int i = 0; i < tmpImage.getHeight(); i++ ) {
            for( int j = 0; j < tmpImage.getWidth(); j++ ) {
                double value = tmpIterator.getSampleDouble(j, i, 0);
                curvatureImage.setSample(j, i, 0, value);
            }
        }

        RenderedImage aspectImage = inAspect.getRenderedImage();
        aspectImageIterator = RandomIterFactory.create(aspectImage, null);

        RenderedImage slopeImage = inSlope.getRenderedImage();
        slopeImageIterator = RandomIterFactory.create(slopeImage, null);

        avgLatitude *= (PI / 180.0);

        pm.message(msg.message("eicalculator.preparing_inputs")); //$NON-NLS-1$
        eibasinNum = prepareInputsOutputs();

        pm.beginTask(msg.message("eicalculator.computing"), 6); //$NON-NLS-1$
        for( int m = 0; m < 6; m++ ) {
            pm.worked(1);
            compute_EI(m + 1);
        }
        pm.done();

        average_EI(10, 6);

        pm.beginTask(msg.message("eicalculator.calc_areas"), eibasinNum); //$NON-NLS-1$
        for( int i = 0; i < eibasinNum; i++ ) {
            pm.worked(1);
            area(i);
        }
        pm.done();

        /*
         * putting the results together
         */
        outAltimetry = new ArrayList<EIAltimetry>();
        outEnergy = new ArrayList<EIEnergy>();
        outArea = new ArrayList<EIAreas>();
        for( int i = 0; i < eibasinNum; i++ ) {
            int realBasinId = index2idMap.get(i + 1);
            /*
             * ENERGY BANDS
             * 
             * Cycle over the virtual months:
             * 0: 22 DICEMBRE - 20 GENNAIO
             * 1: 21 GENNAIO - 20 FEBBRAIO
             * 2: 21 FEBBRAIO - 22 MARZO
             * 3: 23 MARZO - 22 APRILE
             * 4: 23 APRILE - 22 MAGGIO
             * 5: 23 MAGGIO - 22 GIUGNO
             */
            for( int j = 0; j < 6; j++ ) {
                for( int k = 0; k < pEi; k++ ) {
                    EIEnergy tmpEi = new EIEnergy();
                    // the basin id
                    tmpEi.basinId = realBasinId;
                    tmpEi.energeticBandId = k;
                    tmpEi.virtualMonth = j;
                    tmpEi.energyValue = eibasinEI[0][k][i];
                    outEnergy.add(tmpEi);
                }
            }

            /*
             * ALTIMETRIC BANDS
             */
            for( int k = 0; k < pEs; k++ ) {
                EIAltimetry tmpAl = new EIAltimetry();
                tmpAl.basinId = realBasinId;
                tmpAl.altimetricBandId = k;
                tmpAl.elevationValue = eibasinES[k][i];
                tmpAl.bandRange = eibasinESrange[k][i];
                outAltimetry.add(tmpAl);
            }

            /*
             * AREAS
             */
            for( int j = 0; j < pEs; j++ ) {
                for( int k = 0; k < pEi; k++ ) {
                    EIAreas tmpAr = new EIAreas();
                    tmpAr.basinId = realBasinId;
                    tmpAr.altimetricBandId = j;
                    tmpAr.energyBandId = k;
                    tmpAr.areaValue = eibasinA[j][k][i];
                    outArea.add(tmpAr);
                }
            }

        }
    }

    private int prepareInputsOutputs() {

        List<Integer> idList = new ArrayList<Integer>();
        eibasinID = new int[rows][cols];

        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                // get the value
                if (isNovalue(idbasinImageIterator.getSampleDouble(c, r, 0))) {
                    eibasinID[r][c] = NOVALUE;
                } else {
                    eibasinID[r][c] = (int) idbasinImageIterator.getSampleDouble(c, r, 0);
                    // put the value in the id list, if it isn't already there
                    if (!idList.contains(eibasinID[r][c])) {
                        idList.add(eibasinID[r][c]);
                    }
                }

            }
        }
        // sort the id list
        Collections.sort(idList);
        /*
         * now the number of involved subbasins is known
         */
        int eibasinNum = idList.size();

        /*
         * now substitute the numbers in the ID matrix with a sequential index without wholes
         */
        // first create the mapping
        id2indexMap = new HashMap<Integer, Integer>();
        index2idMap = new HashMap<Integer, Integer>();
        for( int i = 1; i <= idList.size(); i++ ) {
            id2indexMap.put(idList.get(i - 1), i);
            index2idMap.put(i, idList.get(i - 1));
        }
        for( int r = 0; r < eibasinID.length; r++ ) {
            for( int c = 0; c < eibasinID[0].length; c++ ) {
                if (eibasinID[r][c] != NOVALUE) {
                    eibasinID[r][c] = id2indexMap.get(eibasinID[r][c]);
                }
            }
        }

        pm.message(msg.message("eicalculator.subbasinsnum") + eibasinNum); //$NON-NLS-1$

        /*
         * prepare outputs
         */
        outputShadow = new int[rows][cols];
        for( int r = 0; r < outputShadow.length; r++ ) {
            for( int c = 0; c < outputShadow[0].length; c++ ) {
                outputShadow[r][c] = NOVALUE;
            }
        }

        eibasinE = new double[rows][cols];
        for( int r = 0; r < eibasinE.length; r++ ) {
            for( int c = 0; c < eibasinE[0].length; c++ ) {
                eibasinE[r][c] = NOVALUE;
            }
        }

        eibasinEmonth = new double[6][rows][cols];
        for( int r = 0; r < eibasinEmonth.length; r++ ) {
            for( int c = 0; c < eibasinEmonth[0].length; c++ ) {
                for( int t = 0; t < eibasinEmonth[0][0].length; t++ ) {
                    eibasinEmonth[r][c][t] = NOVALUE;
                }
            }
        }

        eibasinES = new double[pEs][eibasinNum];
        eibasinESrange = new double[pEs][eibasinNum];
        eibasinEI_mean = new double[pEi][eibasinNum];

        eibasinEI = new double[6][pEi][eibasinNum];

        eibasinA = new double[pEs][pEi][eibasinNum];

        return eibasinNum;
    }

    private void compute_EI( int month ) {

        int[] day_beg = new int[1], day_end = new int[1], daymonth = new int[1], monthyear = new int[1];
        int day;
        double hour;
        double[] Rad_morpho = new double[1], Rad_flat = new double[1];
        double[] E0 = new double[1], alpha = new double[1], direction = new double[1];
        double[][] Rad_morpho_cum, Rad_flat_cum;

        find_days(month, day_beg, day_end);

        hour = 0.5 * pDt;
        day = day_beg[0];

        Rad_morpho_cum = new double[rows][cols];
        Rad_flat_cum = new double[rows][cols];

        get_date(day, monthyear, daymonth);

        printReport(daymonth, monthyear, hour);

        do {
            sun(hour, day, E0, alpha, direction);

            for( int r = 0; r < eibasinID.length; r++ ) {
                for( int c = 0; c < eibasinID[0].length; c++ ) {
                    if (eibasinID[r][c] != NOVALUE) {
                        radiation(Rad_morpho, Rad_flat, E0[0], alpha[0], direction[0],
                                aspectImageIterator.getSampleDouble(c, r, 0), slopeImageIterator.getSampleDouble(c, r, 0),
                                outputShadow[r][c]);
                        Rad_morpho_cum[r][c] += Rad_morpho[0];
                        Rad_flat_cum[r][c] += Rad_flat[0];
                    }
                }
            }

            hour += pDt;
            if (hour >= 24) {
                hour -= 24.0;
                day += 1;
            }

            get_date(day, monthyear, daymonth);

            printReport(daymonth, monthyear, hour);

        } while( day <= day_end[0] );

        for( int r = 0; r < eibasinID.length; r++ ) {
            for( int c = 0; c < eibasinID[0].length; c++ ) {
                if (eibasinID[r][c] != NOVALUE) {
                    pm.message("Bacino: " + index2idMap.get(eibasinID[r][c]));
                    if (Rad_flat_cum[r][c] == 0) {
                        pm.message("Rad flat nulla");
                        Rad_morpho_cum[r][c] = 1;
                        Rad_flat_cum[r][c] = 1;
                    }
                    eibasinEmonth[month - 1][r][c] = Rad_morpho_cum[r][c] / Rad_flat_cum[r][c];
                    pm.message("Rad morfo: " + Rad_morpho_cum[r][c]);
                    pm.message("Rad flat: " + Rad_flat_cum[r][c]);
                } else {
                    eibasinEmonth[month - 1][r][c] = NOVALUE;
                }
            }
        }

    }

    private void printReport( int[] daymonth, int[] monthyear, double hour ) {
        StringBuilder sb = new StringBuilder();
        sb.append(msg.message("hm.day")); //$NON-NLS-1$
        sb.append(": "); //$NON-NLS-1$
        sb.append(daymonth[0]);
        sb.append("/"); //$NON-NLS-1$
        sb.append(monthyear[0]);
        sb.append(msg.message("hm.hour")); //$NON-NLS-1$
        sb.append(": "); //$NON-NLS-1$
        sb.append(hour);
        if ((hour - (long) hour) * 60 < 10) {
            sb.append(":0"); //$NON-NLS-1$
        } else {
            sb.append(":"); //$NON-NLS-1$
        }
        sb.append(((hour - (long) hour) * 60));
        pm.message(sb.toString());
    }

    private void find_days( int month, int[] day_begin, int[] day_end ) {

        if (month == 1) {
            day_begin[0] = -9;
            day_end[0] = 20;
        } else if (month == 2) {
            day_begin[0] = 21;
            day_end[0] = 51;
        } else if (month == 3) {
            day_begin[0] = 52;
            day_end[0] = 81;
        } else if (month == 4) {
            day_begin[0] = 82;
            day_end[0] = 112;
        } else if (month == 5) {
            day_begin[0] = 113;
            day_end[0] = 142;
        } else if (month == 6) {
            day_begin[0] = 143;
            day_end[0] = 173;
        } else {
            throw new ModelsIllegalargumentException("Incorrect in find_days", "OmsEnergyIndexCalculator", pm);
        }
    }

    private void get_date( int julianday, int[] month, int[] daymonth ) {

        if (julianday <= 0) {
            month[0] = 12;
            daymonth[0] = 31 + julianday;
        } else if (julianday >= 1 && julianday <= 31) {
            month[0] = 1;
            daymonth[0] = julianday;
        } else if (julianday >= 32 && julianday <= 59) {
            month[0] = 2;
            daymonth[0] = julianday - 31;
        } else if (julianday >= 60 && julianday <= 90) {
            month[0] = 3;
            daymonth[0] = julianday - 59;
        } else if (julianday >= 91 && julianday <= 120) {
            month[0] = 4;
            daymonth[0] = julianday - 90;
        } else if (julianday >= 121 && julianday <= 151) {
            month[0] = 5;
            daymonth[0] = julianday - 120;
        } else if (julianday >= 152 && julianday <= 181) {
            month[0] = 6;
            daymonth[0] = julianday - 151;
        } else if (julianday >= 182 && julianday <= 212) {
            month[0] = 7;
            daymonth[0] = julianday - 181;
        } else if (julianday >= 213 && julianday <= 243) {
            month[0] = 8;
            daymonth[0] = julianday - 212;
        } else if (julianday >= 244 && julianday <= 273) {
            month[0] = 9;
            daymonth[0] = julianday - 243;
        } else if (julianday >= 274 && julianday <= 304) {
            month[0] = 10;
            daymonth[0] = julianday - 273;
        } else if (julianday >= 305 && julianday <= 334) {
            month[0] = 11;
            daymonth[0] = julianday - 304;
        } else if (julianday >= 335 && julianday <= 365) {
            month[0] = 12;
            daymonth[0] = julianday - 334;
        }

    }

    private void sun( double hour, int day, double[] E0, double[] alpha, double[] direction ) {

        // latitudine, longitudine in [rad]

        double G, Et, local_hour, D, Thr, beta;

        // correction sideral time
        G = 2.0 * PI * (day - 1) / 365.0;
        Et = 0.000075 + 0.001868 * cos(G) - 0.032077 * sin(G) - 0.014615 * cos(2 * G) - 0.04089 * sin(2 * G);

        // local time
        local_hour = hour + Et / omega; // Iqbal: formula 1.4.2

        // earth-sun distance correction
        E0[0] = 1.00011 + 0.034221 * cos(G) + 0.00128 * sin(G) + 0.000719 * cos(2 * G) + 0.000077 * sin(2 * G);

        // solar declination
        D = 0.006918 - 0.399912 * cos(G) + 0.070257 * sin(G) - 0.006758 * cos(2 * G) + 0.000907 * sin(2 * G) - 0.002697
                * cos(3 * G) + 0.00148 * sin(3 * G);

        // Sunrise and sunset with respect to midday [hour]
        Thr = (acos(-tan(D) * tan(avgLatitude))) / omega;

        if (local_hour >= 12.0 - Thr && local_hour <= 12.0 + Thr) {

            // alpha: solar height (complementar to zenith angle), [rad]
            alpha[0] = asin(sin(avgLatitude) * sin(D) + cos(avgLatitude) * cos(D) * cos(omega * (12.0 - local_hour)));

            // direction: azimuth angle (0 Nord, clockwise) [rad]
            if (local_hour <= 12) {
                if (alpha[0] == PI / 2.0) { /* sole allo zenit */
                    direction[0] = PI / 2.0;
                } else {
                    direction[0] = PI - acos((sin(alpha[0]) * sin(avgLatitude) - sin(D)) / (cos(alpha[0]) * cos(avgLatitude)));
                }
            } else {
                if (alpha[0] == PI / 2.0) { /* sole allo zenit */
                    direction[0] = 3 * PI / 2.0;
                } else {
                    direction[0] = PI + acos((sin(alpha[0]) * sin(avgLatitude) - sin(D)) / (cos(alpha[0]) * cos(avgLatitude)));
                }
            }

            // CALCOLO OMBRE
            /*
             * Chiama Orizzonte# Inputs: dx: dim. pixel (funziona solo per pixel quadrati)
             * 2(basin.Z.nch + basin.Z.nrh): dimensione matrice alpha: altezza solare Z: matrice
             * elevazioni curv: matrice curvature beta: azimuth +#PI/4 NOVALUE: novalue per Z0
             * Outputs: shadow: matrice ombre (1 ombra 0 sole)
             */

            if (direction[0] >= 0. && direction[0] <= PI / 4.) {
                beta = direction[0];
                geomorphUtilities.orizzonte1(dx, 2 * (cols + rows), beta, alpha[0], elevImageIterator, curvatureImage,
                        outputShadow);

            } else if (direction[0] > PI / 4. && direction[0] <= PI / 2.) {
                beta = (PI / 2. - direction[0]);
                geomorphUtilities.orizzonte2(dx, 2 * (cols + rows), beta, alpha[0], elevImageIterator, curvatureImage,
                        outputShadow);

            } else if (direction[0] > PI / 2. && direction[0] <= PI * 3. / 4.) {
                beta = (direction[0] - PI / 2.);

                geomorphUtilities.orizzonte3(dx, 2 * (cols + rows), beta, alpha[0], elevImageIterator, curvatureImage,
                        outputShadow);

            } else if (direction[0] > PI * 3. / 4. && direction[0] <= PI) {
                beta = (PI - direction[0]);
                geomorphUtilities.orizzonte4(dx, 2 * (cols + rows), beta, alpha[0], elevImageIterator, curvatureImage,
                        outputShadow);

            } else if (direction[0] > PI && direction[0] <= PI * 5. / 4.) {
                beta = (direction[0] - PI);
                geomorphUtilities.orizzonte5(dx, 2 * (cols + rows), beta, alpha[0], elevImageIterator, curvatureImage,
                        outputShadow);

            } else if (direction[0] > PI * 5. / 4. && direction[0] <= PI * 3. / 2.) {
                beta = (PI * 3. / 2. - direction[0]);
                geomorphUtilities.orizzonte6(dx, 2 * (cols + rows), beta, alpha[0], elevImageIterator, curvatureImage,
                        outputShadow);

            } else if (direction[0] > PI * 3. / 2. && direction[0] <= PI * 7. / 4.) {
                beta = (direction[0] - PI * 3. / 2.);
                geomorphUtilities.orizzonte7(dx, 2 * (cols + rows), beta, alpha[0], elevImageIterator, curvatureImage,
                        outputShadow);

            } else if (direction[0] > PI * 7. / 4. && direction[0] < 2. * PI) {
                beta = (2. * PI - direction[0]);
                /*
                 * here we should have Orizzonte8, but the routine has an error. So the 1 is called.
                 * Explanation: quello è un errore dovuto al fatto che la routine orizzonte8 è
                 * sbagliata e dà errore, allora ci ho messo una pezza e ho richiamato la
                 * orizzonte1, invece che la orizzonte 8. tuttavia le orizzonte 1 e 8 vengono
                 * chiamate solo quando il sole è a nord e da noi questo non capita mai.. quindi
                 * puoi lasciare così com'è
                 */
                geomorphUtilities.orizzonte1(dx, 2 * (cols + rows), beta, alpha[0], elevImageIterator, curvatureImage,
                        outputShadow);
                // error!!!
            }

        } else {

            for( int r = 0; r < eibasinID.length; r++ ) {
                for( int c = 0; c < eibasinID[0].length; c++ ) {
                    if (eibasinID[r][c] != (int) NOVALUE)
                        outputShadow[r][c] = 1;
                }
            }
            alpha[0] = 0.0;
            direction[0] = 0.0;

        }

    }

    private void radiation( double[] Rad_morpho, double[] Rad_flat, double E0, double alpha, double direction, double aspect,
            double slope, int shadow ) {

        Rad_flat[0] = E0 * sin(alpha);
        if (shadow == 1 || alpha == 0.0) { // in ombra o di notte
            Rad_morpho[0] = 0.0;
        } else {
            Rad_morpho[0] = E0 * (cos(slope) * sin(alpha) + sin(slope) * cos(alpha) * cos(-aspect + direction));
        }

        if (Rad_morpho[0] < 0)
            Rad_morpho[0] = 0.0;
    }

    private void average_EI( int month_begin, int month_end ) {

        int m, month;

        if (month_end < month_begin)
            month_end += 12;

        for( int r = 0; r < eibasinID.length; r++ ) {
            for( int c = 0; c < eibasinID[0].length; c++ ) {
                if (eibasinID[r][c] != NOVALUE) {
                    eibasinE[r][c] = 0.0;
                    for( m = month_begin - 1; m < month_end; m++ ) {
                        if (m > 11) {
                            month = m - 12;
                        } else {
                            month = m;
                        }
                        if (month < 6) {
                            eibasinE[r][c] += eibasinEmonth[month][r][c];
                        } else {
                            eibasinE[r][c] += eibasinEmonth[12 - month][r][c];
                        }
                    }
                    eibasinE[r][c] /= (double) (month_end - month_begin + 1);
                }
            }
        }

    }

    private void area( int i ) {

        double minES, maxES, minEI, maxEI;

        maxES = Double.NEGATIVE_INFINITY;
        minES = Double.POSITIVE_INFINITY;
        maxEI = Double.NEGATIVE_INFINITY;
        minEI = Double.POSITIVE_INFINITY;

        for( int r = 0; r < eibasinID.length; r++ ) {
            for( int c = 0; c < eibasinID[0].length; c++ ) {
                if (eibasinID[r][c] != NOVALUE) {
                    // System.out.println("Bacino: " + eibasinID[r][c]);
                    if (eibasinID[r][c] == i + 1) {
                        double value = elevImageIterator.getSampleDouble(c, r, 0);
                        if (value < minES)
                            minES = value;
                        if (value > maxES)
                            maxES = value;
                        if (eibasinE[r][c] < minEI)
                            minEI = eibasinE[r][c];
                        if (eibasinE[r][c] > maxEI)
                            maxEI = eibasinE[r][c];
                    }
                }
            }
            // System.out.println("minEi: " + minEI);
            // System.out.println("maxEi: " + maxEI);
        }

        for( int r = 0; r < eibasinID.length; r++ ) {
            for( int c = 0; c < eibasinID[0].length; c++ ) {
                if (eibasinID[r][c] == (i + 1)) {
                    for( int j = 0; j < pEs; j++ ) {
                        double minCurrentAltimetricBand = minES + (j) * (maxES - minES) / (double) pEs;
                        double maxCurrentAltimetricBand = minES + (j + 1) * (maxES - minES) / (double) pEs;
                        double value = elevImageIterator.getSampleDouble(c, r, 0);
                        if ((value > minCurrentAltimetricBand && value <= maxCurrentAltimetricBand) || (j == 0 && value == minES)) {
                            for( int k = 0; k < pEi; k++ ) {
                                double minCurrentEnergeticBand = minEI + (k) * (maxEI - minEI) / (double) pEi;
                                double maxCurrentEnergeticBand = minEI + (k + 1) * (maxEI - minEI) / (double) pEi;
                                if ((eibasinE[r][c] > minCurrentEnergeticBand && eibasinE[r][c] <= maxCurrentEnergeticBand)
                                        || (k == 0 && eibasinE[r][c] == minEI)) {
                                    eibasinA[j][k][i] += 1.0;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        for( int j = 0; j < pEs; j++ ) {
            for( int k = 0; k < pEi; k++ ) {
                eibasinA[j][k][i] *= (dx * dy * 1.0E-6); // in [km2]
            }
        }

        for( int j = 0; j < pEs; j++ ) {
            eibasinESrange[j][i] = (maxES - minES) / (double) pEs;
            eibasinES[j][i] = minES + (j + 1 - 0.5) * (maxES - minES) / (double) pEs;
        }

        int cont = 0;
        for( int m = 0; m < 6; m++ ) {
            for( int k = 0; k < pEi; k++ ) {
                cont = 0;
                for( int r = 0; r < eibasinID.length; r++ ) {
                    for( int c = 0; c < eibasinID[0].length; c++ ) {
                        if (eibasinID[r][c] != NOVALUE) {
                            double test1 = minEI + (k) * (maxEI - minEI) / (double) pEi;
                            double test2 = minEI + (k + 1) * (maxEI - minEI) / (double) pEi;
                            if ((eibasinE[r][c] > test1 && eibasinE[r][c] <= test2) || (k == 0 && eibasinE[r][c] == minEI)) {
                                cont += 1;
                                eibasinEI[m][k][i] += eibasinEmonth[m][r][c];
                            }
                        }
                    }
                }
                if (cont == 0) {
                    eibasinEI[m][k][i] = 0.00001;
                } else {
                    eibasinEI[m][k][i] /= (double) cont;
                }
            }
        }

    }

    // private void output( int i ) {
    //
    // String filename = outputPath + formatter.format(reverseidMappings.get(i + 1)) + ".txt";
    //
    // out.println(MessageFormat.format("Writing output {0} to file {1}", i, filename));
    // if (new File(filename).exists()) {
    // // copy file to backup
    // try {
    // // Create channel on the source
    // FileChannel srcChannel = new FileInputStream(filename).getChannel();
    //
    // // Create channel on the destination
    // FileChannel dstChannel = new FileOutputStream(filename + ".old").getChannel();
    //
    // // Copy file contents from source to destination
    // dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
    //
    // // Close the channels
    // srcChannel.close();
    // dstChannel.close();
    //
    // // remove the file
    // boolean success = (new File(filename)).delete();
    // if (!success) {
    // out.println("Cannot remove file: " + filename + "!");
    // return;
    // }
    //
    // } catch (IOException e) {
    // e.printStackTrace();
    // return;
    // }
    // }
    //
    // BufferedWriter bw;
    // try {
    // bw = new BufferedWriter(new FileWriter(filename, true));
    //
    // bw.write("# generated by EIcalculator*/");
    // bw.write("\n@15");
    // bw.write("\n");
    // bw
    // .write("\n# 1 block - INDICE ENERGETICO PER LE BANDE ENERGETICHE PER OGNI MESE DELL'ANNO (-)")
    // ;
    // bw.write("\n");
    //
    // bw.write("\n# 22 DICEMBRE - 20 GENNAIO ");
    // bw.write("\n% " + numEi + "\n");
    // for( int k = 0; k < numEi - 1; k++ ) {
    // bw.write(eibasinEI[0][k][i] + " ");
    // }
    // bw.write("" + eibasinEI[0][numEi - 1][i]);
    // bw.write("\n");
    //
    // bw.write("\n# 21 GENNAIO - 20 FEBBRAIO");
    // bw.write("\n% " + numEi + "\n");
    // for( int k = 0; k < numEi - 1; k++ ) {
    // bw.write(eibasinEI[1][k][i] + " ");
    // }
    // bw.write("" + eibasinEI[1][numEi - 1][i]);
    // bw.write("\n");
    //
    // bw.write("\n# 21 FEBBRAIO - 22 MARZO");
    // bw.write("\n% " + numEi + "\n");
    // for( int k = 0; k < numEi - 1; k++ ) {
    // bw.write(eibasinEI[2][k][i] + " ");
    // }
    // bw.write("" + eibasinEI[2][numEi - 1][i]);
    // bw.write("\n");
    //
    // bw.write("\n# 23 MARZO - 22 APRILE");
    // bw.write("\n% " + numEi + "\n");
    // for( int k = 0; k < numEi - 1; k++ ) {
    // bw.write(eibasinEI[3][k][i] + " ");
    // }
    // bw.write("" + eibasinEI[3][numEi - 1][i]);
    // bw.write("\n");
    //
    // bw.write("\n# 23 APRILE - 22 MAGGIO");
    // bw.write("\n% " + numEi + "\n");
    // for( int k = 0; k < numEi - 1; k++ ) {
    // bw.write(eibasinEI[4][k][i] + " ");
    // }
    // bw.write("" + eibasinEI[4][numEi - 1][i]);
    // bw.write("\n");
    //
    // bw.write("\n#23 MAGGIO - 22 GIUGNO");
    // bw.write("\n% " + numEi + "\n");
    // for( int k = 0; k < numEi - 1; k++ ) {
    // bw.write(eibasinEI[5][k][i] + " ");
    // }
    // bw.write("" + eibasinEI[5][numEi - 1][i]);
    // bw.write("\n");
    //
    // bw.write("\n");
    // bw.write("\n");
    // bw.write("\n");
    // bw
    // .write(
    // "\n# 2 block - QUOTA DEL BARICENTRO DELLE FASCIE ALTIMETRICHE e RANGE DI QUOTA PER OGNI FASCIA (m)"
    // );
    // bw.write("\n");
    //
    // bw.write("\n% " + numEs + "\n");
    // for( int j = 0; j < numEs - 1; j++ ) {
    // bw.write(eibasinES[j][i] + " ");
    // }
    // bw.write("" + eibasinES[numEs - 1][i]);
    //
    // bw.write("\n% " + numEs + "\n");
    // for( int j = 0; j < numEs - 1; j++ ) {
    // bw.write(eibasinESrange[j][i] + " ");
    // }
    // bw.write("" + eibasinESrange[numEs - 1][i]);
    //
    // bw.write("\n");
    // bw.write("\n");
    // bw.write("\n");
    //
    // bw
    // .write("\n# 3 block - AREE PER FASCIA ALTIMETRICA (riga) E BANDA ENERGETICA (colonna) (km2)");
    // bw.write("\n% " + numEs + " " + numEi + "\n");
    // for( int j = 0; j < numEs; j++ ) {
    // for( int k = 0; k < numEi; k++ ) {
    // bw.write(eibasinA[j][k][i] + " ");
    // }
    // bw.write("\n");
    // }
    //
    // bw.close();
    //
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    //
    // }
}
