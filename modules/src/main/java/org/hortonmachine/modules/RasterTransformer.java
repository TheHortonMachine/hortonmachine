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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_DO_FLIP_HORIZONTAL_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_DO_FLIP_VERTICAL_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_OUT_BOUNDS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_P_ANGLE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_P_EAST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_P_INTERPOLATION_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_P_NORTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_P_SCALE_X_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_P_SCALE_Y_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_P_TRANS_X_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_P_TRANS_Y_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_STATUS;
import static org.hortonmachine.gears.libs.modules.Variables.BICUBIC;
import static org.hortonmachine.gears.libs.modules.Variables.BILINEAR;
import static org.hortonmachine.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer;

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
import oms3.annotations.Unit;

@Description(OMSRASTERTRANSFORMER_DESCRIPTION)
@Author(name = OMSRASTERTRANSFORMER_AUTHORNAMES, contact = OMSRASTERTRANSFORMER_AUTHORCONTACTS)
@Keywords(OMSRASTERTRANSFORMER_KEYWORDS)
@Label(OMSRASTERTRANSFORMER_LABEL)
@Name("_" + OMSRASTERTRANSFORMER_NAME)
@Status(OMSRASTERTRANSFORMER_STATUS)
@License(OMSRASTERTRANSFORMER_LICENSE)
public class RasterTransformer extends HMModel {
    @Description(OMSRASTERTRANSFORMER_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OMSRASTERTRANSFORMER_P_INTERPOLATION_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSRASTERTRANSFORMER_P_TRANS_X_DESCRIPTION)
    @Unit("m")
    @In
    public Double pTransX;

    @Description(OMSRASTERTRANSFORMER_P_TRANS_Y_DESCRIPTION)
    @Unit("m")
    @In
    public Double pTransY;

    @Description(OMSRASTERTRANSFORMER_P_SCALE_X_DESCRIPTION)
    @In
    public Double pScaleX;

    @Description(OMSRASTERTRANSFORMER_P_SCALE_Y_DESCRIPTION)
    @In
    public Double pScaleY;

    @Description(OMSRASTERTRANSFORMER_DO_FLIP_HORIZONTAL_DESCRIPTION)
    @In
    public boolean doFlipHorizontal;

    @Description(OMSRASTERTRANSFORMER_DO_FLIP_VERTICAL_DESCRIPTION)
    @In
    public boolean doFlipVertical;

    @Description(OMSRASTERTRANSFORMER_P_NORTH_DESCRIPTION)
    @UI(HMConstants.NORTHING_UI_HINT)
    @In
    public Double pNorth;

    @Description(OMSRASTERTRANSFORMER_P_EAST_DESCRIPTION)
    @UI(HMConstants.EASTING_UI_HINT)
    @In
    public Double pEast;

    @Description(OMSRASTERTRANSFORMER_P_ANGLE_DESCRIPTION)
    @Unit("degrees")
    @In
    public Double pAngle;

    @Description(OMSRASTERTRANSFORMER_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Description(OMSRASTERTRANSFORMER_OUT_BOUNDS_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBounds = null;

    @Execute
    public void process() throws Exception {
        OmsRasterTransformer rastertransformer = new OmsRasterTransformer();
        rastertransformer.inRaster = getRaster(inRaster);
        rastertransformer.pInterpolation = pInterpolation;
        rastertransformer.pTransX = pTransX;
        rastertransformer.pTransY = pTransY;
        rastertransformer.pScaleX = pScaleX;
        rastertransformer.pScaleY = pScaleY;
        rastertransformer.doFlipHorizontal = doFlipHorizontal;
        rastertransformer.doFlipVertical = doFlipVertical;
        rastertransformer.pNorth = pNorth;
        rastertransformer.pEast = pEast;
        rastertransformer.pAngle = pAngle;
        rastertransformer.pm = pm;
        rastertransformer.doProcess = doProcess;
        rastertransformer.doReset = doReset;
        rastertransformer.process();
        dumpRaster(rastertransformer.outRaster, outRaster);
        dumpVector(rastertransformer.outBounds, outBounds);
    }
}
