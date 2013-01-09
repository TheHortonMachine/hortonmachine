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
package org.jgrasstools.modules;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_inGeodata_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_outGeodata_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pCols_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pMode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pRows_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pXstep_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pYstep_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.WritableRaster;

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
import org.jaitools.imageutils.iterator.WindowIterator;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description(OMSWINDOWSAMPLER_DESCRIPTION)
@Documentation(OMSWINDOWSAMPLER_DOCUMENTATION)
@Author(name = OMSWINDOWSAMPLER_AUTHORNAMES, contact = OMSWINDOWSAMPLER_AUTHORCONTACTS)
@Keywords(OMSWINDOWSAMPLER_KEYWORDS)
@Label(OMSWINDOWSAMPLER_LABEL)
@Name(OMSWINDOWSAMPLER_NAME)
@Status(OMSWINDOWSAMPLER_STATUS)
@License(OMSWINDOWSAMPLER_LICENSE)
public class OmsWindowSampler extends JGTModel {

    @Description(OMSWINDOWSAMPLER_inGeodata_DESCRIPTION)
    @In
    public GridCoverage2D inGeodata;

    @Description(OMSWINDOWSAMPLER_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSWINDOWSAMPLER_pRows_DESCRIPTION)
    @In
    public int pRows = 3;

    @Description(OMSWINDOWSAMPLER_pCols_DESCRIPTION)
    @In
    public int pCols = 3;

    @Description(OMSWINDOWSAMPLER_pXstep_DESCRIPTION)
    @In
    public Integer pXstep;

    @Description(OMSWINDOWSAMPLER_pYstep_DESCRIPTION)
    @In
    public Integer pYstep;

    @Description(OMSWINDOWSAMPLER_outGeodata_DESCRIPTION)
    @Out
    public GridCoverage2D outGeodata;

    @Execute
    public void process() throws Exception {
        checkNull(inGeodata);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inGeodata);

        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        int xstep = pCols;
        int ystep = pRows;
        if (pXstep != null) {
            xstep = pXstep;
        }
        if (pYstep != null) {
            ystep = pYstep;
        }

        // new rows and cols are all that have space rounding down
        int newRows = (int) ceil((double) rows / (double) ystep);
        int newCols = (int) ceil((double) cols / (double) xstep);
        WritableRaster outputWR = CoverageUtilities.createDoubleWritableRaster(newCols, newRows, null, null,
                JGTConstants.doubleNovalue);

        WindowIterator iter = new WindowIterator(inGeodata.getRenderedImage(), null, new Dimension(pCols, pRows),
                new Point(0, 0), xstep, ystep, JGTConstants.doubleNovalue);

        for( int r = 0; r < newRows; r++ ) {
            for( int c = 0; c < newCols; c++ ) {
                double[][] window = iter.getWindowDouble(null);
                double newValue = calculateValue(window);
                iter.next();

                outputWR.setSample(c, r, 0, newValue);
            }
        }

        outGeodata = CoverageUtilities
                .buildCoverage("downsampled", outputWR, regionMap, inGeodata.getCoordinateReferenceSystem());

    }

    private double calculateValue( double[][] window ) {
        switch( pMode ) {
        case 0:
            double avg = 0;
            int num = 0;
            for( int i = 0; i < window.length; i++ ) {
                for( int j = 0; j < window[0].length; j++ ) {
                    if (!isNovalue(window[i][j])) {
                        avg = avg + window[i][j];
                        num++;
                    }
                }
            }
            avg = avg / num;
            return avg;
        case 1:
            double sum = 0;
            for( int i = 0; i < window.length; i++ ) {
                for( int j = 0; j < window[0].length; j++ ) {
                    if (!isNovalue(window[i][j])) {
                        sum = sum + window[i][j];
                    }
                }
            }
            return sum;
        case 2:
            double max = Double.NEGATIVE_INFINITY;
            for( int i = 0; i < window.length; i++ ) {
                for( int j = 0; j < window[0].length; j++ ) {
                    if (!isNovalue(window[i][j])) {
                        max = max(window[i][j], max);
                    }
                }
            }
            return max;
        case 3:
            double min = Double.POSITIVE_INFINITY;
            for( int i = 0; i < window.length; i++ ) {
                for( int j = 0; j < window[0].length; j++ ) {
                    if (!isNovalue(window[i][j])) {
                        min = min(window[i][j], min);
                    }
                }
            }
            return min;
        default:
            throw new ModelsIllegalargumentException("Mode not recognized: " + pMode, this);
        }
    }

}
