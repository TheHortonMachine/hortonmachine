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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.insolation;

import static org.hortonmachine.gears.libs.modules.ModelsEngine.calcInverseSunVector;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.calcNormalSunVector;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.calculateFactor;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.scalarProduct;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_outIns_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_tEndDate_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_tStartDate_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.RasterFactory;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

@Description(OMSINSOLATION_DESCRIPTION)
@Author(name = OMSINSOLATION_AUTHORNAMES, contact = OMSINSOLATION_AUTHORCONTACTS)
@Keywords(OMSINSOLATION_KEYWORDS)
@Label(OMSINSOLATION_LABEL)
@Name(OMSINSOLATION_NAME)
@Status(OMSINSOLATION_STATUS)
@License(OMSINSOLATION_LICENSE)
public class OmsInsolation extends HMModel {

    @Description(OMSINSOLATION_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSINSOLATION_tStartDate_DESCRIPTION)
    @In
    public String tStartDate = null;

    @Description(OMSINSOLATION_tEndDate_DESCRIPTION)
    @In
    public String tEndDate = null;

    @Description(OMSINSOLATION_outIns_DESCRIPTION)
    @Out
    public GridCoverage2D outIns;

    private static final double pCmO3 = 0.3;

    private static final double pRH = 0.4;

    private static final double pLapse = -.0065;

    private static final double pVisibility = 60;

    /**
     * The solar constant.
     */
    private static final double SOLARCTE = 1368.0;

    /**
     * The atmosphere pressure.
     */
    private static final double ATM = 1013.25;

    private double lambda;

    private double delta;

    private double omega;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception { // transform the
        checkNull(inElev, tStartDate, tEndDate);
        // extract some attributes of the map
        HashMap<String, Double> attribute = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        double dx = attribute.get(CoverageUtilities.XRES);

        /*
         * The models use only one value of the latitude. So I have decided to
         * set it to the center of the raster. Extract the CRS of the
         * GridCoverage and transform the value of a WGS84 latitude.
         */
        CoordinateReferenceSystem sourceCRS = inElev.getCoordinateReferenceSystem2D();
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;

        double srcPts[] = new double[]{attribute.get(CoverageUtilities.EAST), attribute.get(CoverageUtilities.SOUTH)};

        Coordinate source = new Coordinate(srcPts[0], srcPts[1]);
        Point[] so = new Point[]{GeometryUtilities.gf().createPoint(source)};
        CrsUtilities.reproject(sourceCRS, targetCRS, so);
        // the latitude value
        lambda = Math.toRadians(so[0].getY());

        /*
         * transform the start and end date in an int value (the day in the
         * year, from 1 to 365)
         */
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.UTC);
        DateTime currentDatetime = formatter.parseDateTime(tStartDate);
        int startDay = currentDatetime.getDayOfYear();
        currentDatetime = formatter.parseDateTime(tEndDate);
        int endDay = currentDatetime.getDayOfYear();
        CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        RenderedImage pitTmpRI = inElev.getRenderedImage();
        int width = pitTmpRI.getWidth();
        int height = pitTmpRI.getHeight();
        WritableRaster pitWR = CoverageUtilities.replaceNovalue(pitTmpRI, -9999.0);
        pitTmpRI = null;

        WritableRaster insolationWR = CoverageUtilities.createWritableRaster(width, height, null, pitWR.getSampleModel(),
                0.0);
        WritableRandomIter insolationIterator = RandomIterFactory.createWritable(insolationWR, null);

        WritableRaster gradientWR = normalVector(pitWR, dx);

        pm.beginTask(msg.message("insolation.calculating"), endDay - startDay);

        for( int i = startDay; i <= endDay; i++ ) {
            calcInsolation(lambda, pitWR, gradientWR, insolationWR, i, dx);
            pm.worked(i - startDay);
        }
        pm.done();
        for( int y = 2; y < height - 2; y++ ) {
            for( int x = 2; x < width - 2; x++ ) {
                if (HMConstants.isNovalue(pitWR.getSampleDouble(x, y, 0))) {
                    insolationIterator.setSample(x, y, 0, HMConstants.doubleNovalue);

                }
            }
        }

        outIns = CoverageUtilities.buildCoverage("insolation", insolationWR, attribute, inElev.getCoordinateReferenceSystem());
    }

    /**
     * Evaluate the radiation.
     * 
     * @param lambda
     *            the latitude.
     * @param demWR
     *            the raster of elevation
     * @param gradientWR
     *            the raster of the gradient value of the dem.
     * @param insolationWR
     *            the wr where to store the result.
     * @param the
     *            day in the year.
     * @paradx the resolutiono of the dem.
     */
    private void calcInsolation( double lambda, WritableRaster demWR, WritableRaster gradientWR, WritableRaster insolationWR,
            int day, double dx ) {
        // calculating the day angle
        // double dayang = 2 * Math.PI * (day - 1) / 365.0;
        double dayangb = (360 / 365.25) * (day - 79.436);
        dayangb = Math.toRadians(dayangb);
        // Evaluate the declination of the sun.
        delta = getDeclination(dayangb);
        // Evaluate the radiation in this day.
        double ss = Math.acos(-Math.tan(delta) * Math.tan(lambda));
        double hour = -ss + (Math.PI / 48.0);
        while( hour <= ss - (Math.PI / 48) ) {
            omega = hour;
            // calculating the vector related to the sun
            double sunVector[] = calcSunVector();
            double zenith = calcZenith(sunVector[2]);
            double[] inverseSunVector = calcInverseSunVector(sunVector);
            double[] normalSunVector = calcNormalSunVector(sunVector);

            int height = demWR.getHeight();
            int width = demWR.getWidth();
            WritableRaster sOmbraWR = calculateFactor(height, width, sunVector, inverseSunVector, normalSunVector, demWR, dx);
            double mr = 1 / (sunVector[2] + 0.15 * Math.pow((93.885 - zenith), (-1.253)));
            for( int j = 0; j < height; j++ ) {
                for( int i = 0; i < width; i++ ) {
                    // evaluate the radiation.
                    calcRadiation(i, j, demWR, sOmbraWR, insolationWR, sunVector, gradientWR, mr);
                }
            }
            hour = hour + Math.PI / 24.0;
        }
    }

    /*
     * Evaluate the declination.
     */
    private double getDeclination( double dayangb ) {
        double delta = .3723 + 23.2567 * Math.sin(dayangb) - .758 * Math.cos(dayangb) + .1149 * Math.sin(2 * dayangb) + .3656
                * Math.cos(2 * dayangb) - .1712 * Math.sin(3 * dayangb) + .0201 * Math.cos(3 * dayangb);
        return Math.toRadians(delta);
    }

    /*
     * evaluate several component of the radiation and then multiply by the
     * sOmbra factor.
     */
    private void calcRadiation( int i, int j, WritableRaster demWR, WritableRaster sOmbraWR, WritableRaster insolationWR,
            double[] sunVector, WritableRaster gradientWR, double mr ) {
        double z = demWR.getSampleDouble(i, j, 0);
        double pressure = ATM * Math.exp(-0.0001184 * z);
        double ma = mr * pressure / ATM;
        double temp = 273 + pLapse * (z - 4000);
        double vap_psat = Math.exp(26.23 - 5416.0 / temp);
        double wPrec = 0.493 * pRH * vap_psat / temp;
        double taur = Math.exp((-.09030 * Math.pow(ma, 0.84)) * (1.0 + ma - Math.pow(ma, 1.01)));
        double d = pCmO3 * mr;
        double tauo = 1 - (0.1611 * d * Math.pow(1.0 + 139.48 * d, -0.3035) - 0.002715 * d)
                / (1.0 + 0.044 * d + 0.0003 * Math.pow(d, 2));
        double taug = Math.exp(-0.0127 * Math.pow(ma, 0.26));
        double tauw = 1 - 2.4959 * (wPrec * mr) / (1.0 + 79.034 * (wPrec * mr) * 0.6828 + 6.385 * (wPrec * mr));
        double taua = Math.pow((0.97 - 1.265 * Math.pow(pVisibility, (-0.66))), Math.pow(ma, 0.9));

        double In = 0.9751 * SOLARCTE * taur * tauo * taug * tauw * taua;

        double cosinc = scalarProduct(sunVector, gradientWR.getPixel(i, j, new double[3]));

        if (cosinc < 0) {
            cosinc = 0;
        }
        double tmp = insolationWR.getSampleDouble(i, j, 0);
        insolationWR.setSample(i, j, 0, In * cosinc * sOmbraWR.getSampleDouble(i, j, 0) / 1000 + tmp);
    }

    protected double[] calcSunVector() {
        double sunVector[] = new double[3];
        sunVector[0] = -Math.sin(omega) * Math.cos(delta);
        sunVector[1] = Math.sin(lambda) * Math.cos(omega) * Math.cos(delta) - Math.cos(lambda) * Math.sin(delta);
        sunVector[2] = Math.cos(lambda) * Math.cos(omega) * Math.cos(delta) + Math.sin(lambda) * Math.sin(delta);

        return sunVector;

    }

    protected WritableRaster normalVector( WritableRaster pitWR, double res ) {

        int minX = pitWR.getMinX();
        int minY = pitWR.getMinY();
        int rows = pitWR.getHeight();
        int cols = pitWR.getWidth();

        RandomIter pitIter = RandomIterFactory.create(pitWR, null);
        /*
         * Initializa the Image of the normal vector in the central point of the
         * cells, which have 3 components so the Image have 3 bands..
         */
        SampleModel sm = RasterFactory.createBandedSampleModel(5, cols, rows, 3);
        WritableRaster tmpNormalVectorWR = CoverageUtilities.createWritableRaster(cols, rows, null, sm, 0.0);
        WritableRandomIter tmpNormalIter = RandomIterFactory.createWritable(tmpNormalVectorWR, null);
        /*
         * apply the corripio's formula (is the formula (3) in the article)
         */
        for( int j = minY; j < minX + rows - 1; j++ ) {
            for( int i = minX; i < minX + cols - 1; i++ ) {
                double zij = pitIter.getSampleDouble(i, j, 0);
                double zidxj = pitIter.getSampleDouble(i + 1, j, 0);
                double zijdy = pitIter.getSampleDouble(i, j + 1, 0);
                double zidxjdy = pitIter.getSampleDouble(i + 1, j + 1, 0);
                double firstComponent = res * (zij - zidxj + zijdy - zidxjdy);
                double secondComponent = res * (zij + zidxj - zijdy - zidxjdy);
                double thirthComponent = 2 * (res * res);
                double den = Math.sqrt(firstComponent * firstComponent + secondComponent * secondComponent + thirthComponent
                        * thirthComponent);
                tmpNormalIter.setPixel(i, j, new double[]{firstComponent / den, secondComponent / den, thirthComponent / den});

            }
        }
        pitIter.done();

        return tmpNormalVectorWR;

    }
    private double calcZenith( double sunVector2 ) {
        return Math.acos(sunVector2);
    }
}
