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
package org.jgrasstools.gears.modules.r.transformer;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERRESOLUTIONRESAMPLER_DOCUMENTATION;
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

import javax.media.jai.Interpolation;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.JGTProcessingRegion;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

@Description(OMSRASTERRESOLUTIONRESAMPLER_DESCRIPTION)
@Documentation(OMSRASTERRESOLUTIONRESAMPLER_DOCUMENTATION)
@Author(name = OMSRASTERRESOLUTIONRESAMPLER_AUTHORNAMES, contact = OMSRASTERRESOLUTIONRESAMPLER_AUTHORCONTACTS)
@Keywords(OMSRASTERRESOLUTIONRESAMPLER_KEYWORDS)
@Label(OMSRASTERRESOLUTIONRESAMPLER_LABEL)
@Name(OMSRASTERRESOLUTIONRESAMPLER_NAME)
@Status(OMSRASTERRESOLUTIONRESAMPLER_STATUS)
@License(OMSRASTERRESOLUTIONRESAMPLER_LICENSE)
public class OmsRasterResolutionResampler extends JGTModel {

    @Description(OMSRASTERRESOLUTIONRESAMPLER_inGeodata_DESCRIPTION)
    @In
    public GridCoverage2D inGeodata;

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
    @Out
    public GridCoverage2D outGeodata;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        checkNull(inGeodata, pXres);
        if (pYres == null) {
            pYres = pXres;
        }
        JGTProcessingRegion region = new JGTProcessingRegion(inGeodata);
        region.setWEResolution(pXres);
        region.setNSResolution(pYres);

        GridGeometry2D newGridGeometry = region.getGridGeometry(inGeodata.getCoordinateReferenceSystem());

        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        if (pInterpolation.equals(BILINEAR)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        } else if (pInterpolation.equals(BICUBIC)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
        }

        pm.beginTask("Resampling...", IJGTProgressMonitor.UNKNOWN);
        outGeodata = (GridCoverage2D) Operations.DEFAULT.resample(inGeodata, inGeodata.getCoordinateReferenceSystem(),
                newGridGeometry, interpolation);
        pm.done();
    }

}
