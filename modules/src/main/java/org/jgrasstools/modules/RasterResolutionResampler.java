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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_inGeodata_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_outGeodata_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_pInterpolation_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_pXres_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_pYres_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.Variables.BICUBIC;
import static org.jgrasstools.gears.libs.modules.Variables.BILINEAR;
import static org.jgrasstools.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.transformer.OmsRasterResolutionResampler;

@Description(OMSRASTERRESOLUTIONRESAMPLER_DESCRIPTION)
@Author(name = OMSRASTERRESOLUTIONRESAMPLER_AUTHORNAMES, contact = OMSRASTERRESOLUTIONRESAMPLER_AUTHORCONTACTS)
@Keywords(OMSRASTERRESOLUTIONRESAMPLER_KEYWORDS)
@Label(OMSRASTERRESOLUTIONRESAMPLER_LABEL)
@Name("_" + OMSRASTERRESOLUTIONRESAMPLER_NAME)
@Status(OMSRASTERRESOLUTIONRESAMPLER_STATUS)
@License(OMSRASTERRESOLUTIONRESAMPLER_LICENSE)
public class RasterResolutionResampler extends JGTModel {

    @Description(OMSRASTERRESOLUTIONRESAMPLER_inGeodata_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inGeodata;

    @Description(OMSRASTERRESOLUTIONRESAMPLER_pInterpolation_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSRASTERRESOLUTIONRESAMPLER_pXres_DESCRIPTION)
    @In
    public Double pXres;

    @Description(OMSRASTERRESOLUTIONRESAMPLER_pYres_DESCRIPTION)
    @In
    public Double pYres;

    @Description(OMSRASTERRESOLUTIONRESAMPLER_outGeodata_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outGeodata;

    @Execute
    public void process() throws Exception {
        OmsRasterResolutionResampler rasterresolutionresampler = new OmsRasterResolutionResampler();
        rasterresolutionresampler.inGeodata = getRaster(inGeodata);
        rasterresolutionresampler.pInterpolation = pInterpolation;
        rasterresolutionresampler.pXres = pXres;
        rasterresolutionresampler.pYres = pYres;
        rasterresolutionresampler.pm = pm;
        rasterresolutionresampler.doProcess = doProcess;
        rasterresolutionresampler.doReset = doReset;
        rasterresolutionresampler.process();
        dumpRaster(rasterresolutionresampler.outGeodata, outGeodata);
    }
}
