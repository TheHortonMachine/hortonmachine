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
package org.jgrasstools.hortonmachine.modules.geomorphology.aspect;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_doRadiants_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_doRound_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_outAspect_DESCRIPTION;

import java.awt.image.Raster;
import java.io.File;

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

import org.jgrasstools.gears.libs.modules.JGTModelIM;
import org.jgrasstools.gears.modules.r.imagemosaic.OmsImageMosaicCreator;
import org.jgrasstools.gears.utils.colors.ColorTables;
import org.jgrasstools.gears.utils.math.NumericsUtilities;

@Description(OMSASPECT_DESCRIPTION)
@Documentation(OMSASPECT_DOCUMENTATION)
@Author(name = OMSASPECT_AUTHORNAMES, contact = OMSASPECT_AUTHORCONTACTS)
@Keywords(OMSASPECT_KEYWORDS)
@Label(OMSASPECT_LABEL)
@Name(OMSASPECT_NAME)
@Status(OMSASPECT_STATUS)
@License(OMSASPECT_LICENSE)
public class OmsAspectIM extends JGTModelIM {
    @Description(OMSASPECT_inElev_DESCRIPTION)
    @In
    public String inElev = null;

    @Description(OMSASPECT_doRadiants_DESCRIPTION)
    @In
    public boolean doRadiants = false;

    @Description(OMSASPECT_doRound_DESCRIPTION)
    @In
    public boolean doRound = false;

    @Description(OMSASPECT_outAspect_DESCRIPTION)
    @Out
    public String outAspect = null;

    private double radtodeg;

    @Execute
    public void process() throws Exception {
        checkNull(inElev);
        radtodeg = NumericsUtilities.RADTODEG;
        if (doRadiants) {
            radtodeg = 1.0;
        }

        cellBuffer = 1;
        addSource(new File(inElev));
        setOutput(outAspect);

        processTiles();

        makeMosaicWithStyle(ColorTables.aspect, 0, 360);
    }

    @Override
    protected void processCell( int readCol, int readRow, int writeCol, int writeRow ) {
        double aspect = doubleNovalue;
        // the value of the x and y derivative
        double aData = 0.0;
        double bData = 0.0;

        Raster elevRaster = inRasters.get(0);
        double centralValue = elevRaster.getSampleDouble(readCol, readRow, 0);
        double wValue = elevRaster.getSampleDouble(readCol - 1, readRow, 0);
        double eValue = elevRaster.getSampleDouble(readCol + 1, readRow, 0);
        double sValue = elevRaster.getSampleDouble(readCol, readRow - 1, 0);
        double nValue = elevRaster.getSampleDouble(readCol, readRow + 1, 0);

        // double centralValue = node.elevation;
        // double nValue = node.getNorthElev();
        // double sValue = node.getSouthElev();
        // double wValue = node.getWestElev();
        // double eValue = node.getEastElev();

        if (!isNovalue(centralValue)) {
            boolean sIsNovalue = isNovalue(sValue);
            boolean nIsNovalue = isNovalue(nValue);
            boolean wIsNovalue = isNovalue(wValue);
            boolean eIsNovalue = isNovalue(eValue);

            if (!sIsNovalue && !nIsNovalue) {
                aData = atan((nValue - sValue) / (2 * yRes));
            } else if (nIsNovalue && !sIsNovalue) {
                aData = atan((centralValue - sValue) / (yRes));
            } else if (!nIsNovalue && sIsNovalue) {
                aData = atan((nValue - centralValue) / (yRes));
            } else if (nIsNovalue && sIsNovalue) {
                aData = doubleNovalue;
            } else {
                // can't happen
                throw new RuntimeException();
            }
            if (!wIsNovalue && !eIsNovalue) {
                bData = atan((wValue - eValue) / (2 * xRes));
            } else if (wIsNovalue && !eIsNovalue) {
                bData = atan((centralValue - eValue) / (xRes));
            } else if (!wIsNovalue && eIsNovalue) {
                bData = atan((wValue - centralValue) / (xRes));
            } else if (wIsNovalue && eIsNovalue) {
                bData = doubleNovalue;
            } else {
                // can't happen
                throw new RuntimeException();
            }

            double delta = 0.0;
            // calculate the aspect value
            if (aData < 0 && bData > 0) {
                delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                aspect = delta * radtodeg;
            } else if (aData > 0 && bData > 0) {
                delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                aspect = (PI - delta) * radtodeg;
            } else if (aData > 0 && bData < 0) {
                delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                aspect = (PI + delta) * radtodeg;
            } else if (aData < 0 && bData < 0) {
                delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                aspect = (2 * PI - delta) * radtodeg;
            } else if (aData == 0 && bData > 0) {
                aspect = (PI / 2.) * radtodeg;
            } else if (aData == 0 && bData < 0) {
                aspect = (PI * 3. / 2.) * radtodeg;
            } else if (aData > 0 && bData == 0) {
                aspect = PI * radtodeg;
            } else if (aData < 0 && bData == 0) {
                aspect = 2.0 * PI * radtodeg;
            } else if (aData == 0 && bData == 0) {
                aspect = 0.0;
            } else if (isNovalue(aData) || isNovalue(bData)) {
                aspect = doubleNovalue;
            } else {
                // can't happen
                throw new RuntimeException();
            }
            if (doRound) {
                aspect = round(aspect);
            }
        }

        outDataIter.setSample(writeCol, writeRow, 0, aspect);
    }

    public static void main( String[] args ) throws Exception {
//        OmsAspectIM g = new OmsAspectIM();
//        g.inElev = "/media/lacntfs/oceandtm/q1swb_2008_export_043_xyz2_2m/q1swb_2008_export_043_xyz2_2m.shp";
//        g.outAspect = "/media/lacntfs/oceandtm/q1swb_2008_export_043_xyz2_2m_aspect/q1swb_2008_export_043_xyz2_2m_aspect.shp";
//        g.doRadiants = false;
//        g.process();
        
        OmsImageMosaicCreator im = new OmsImageMosaicCreator();
        im.inFolder = "/media/lacntfs/oceandtm/q1swb_2008_export_043_xyz2_2m_aspect/";
        im.process();
    }

}
