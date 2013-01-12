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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_doFlipHorizontal_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_doFlipVertical_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_outBounds_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pAngle_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pInterpolation_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pScaleX_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pScaleY_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pTransX_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pTransY_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.Variables.BICUBIC;
import static org.jgrasstools.gears.libs.modules.Variables.BILINEAR;
import static org.jgrasstools.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
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
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.transformer.OmsRasterTransformer;

@Description(OMSRASTERTRANSFORMER_DESCRIPTION)
@Documentation(OMSRASTERTRANSFORMER_DOCUMENTATION)
@Author(name = OMSRASTERTRANSFORMER_AUTHORNAMES, contact = OMSRASTERTRANSFORMER_AUTHORCONTACTS)
@Keywords(OMSRASTERTRANSFORMER_KEYWORDS)
@Label(OMSRASTERTRANSFORMER_LABEL)
@Name("_" + OMSRASTERTRANSFORMER_NAME)
@Status(OMSRASTERTRANSFORMER_STATUS)
@License(OMSRASTERTRANSFORMER_LICENSE)
public class RasterTransformer extends JGTModel {
    @Description(OMSRASTERTRANSFORMER_inRaster_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster;

    @Description(OMSRASTERTRANSFORMER_pInterpolation_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSRASTERTRANSFORMER_pTransX_DESCRIPTION)
    @Unit("m")
    @In
    public Double pTransX;

    @Description(OMSRASTERTRANSFORMER_pTransY_DESCRIPTION)
    @Unit("m")
    @In
    public Double pTransY;

    @Description(OMSRASTERTRANSFORMER_pScaleX_DESCRIPTION)
    @In
    public Double pScaleX;

    @Description(OMSRASTERTRANSFORMER_pScaleY_DESCRIPTION)
    @In
    public Double pScaleY;

    @Description(OMSRASTERTRANSFORMER_doFlipHorizontal_DESCRIPTION)
    @In
    public boolean doFlipHorizontal;

    @Description(OMSRASTERTRANSFORMER_doFlipVertical_DESCRIPTION)
    @In
    public boolean doFlipVertical;

    @Description(OMSRASTERTRANSFORMER_pNorth_DESCRIPTION)
    @UI(JGTConstants.NORTHING_UI_HINT)
    @In
    public Double pNorth;

    @Description(OMSRASTERTRANSFORMER_pEast_DESCRIPTION)
    @UI(JGTConstants.EASTING_UI_HINT)
    @In
    public Double pEast;

    @Description(OMSRASTERTRANSFORMER_pAngle_DESCRIPTION)
    @Unit("degrees")
    @In
    public Double pAngle;

    @Description(OMSRASTERTRANSFORMER_outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outRaster = null;

    @Description(OMSRASTERTRANSFORMER_outBounds_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
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
