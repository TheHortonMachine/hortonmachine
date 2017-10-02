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
package org.hortonmachine.hmachine.modules.statistics.cb;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_inRaster1_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_inRaster2_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_outCb_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_pBins_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_pFirst_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_pLast_DESCRIPTION;

import java.awt.image.RenderedImage;

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
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.math.CoupledFieldsMoments;

@Description(OMSCB_DESCRIPTION)
@Author(name = OMSCB_AUTHORNAMES, contact = OMSCB_AUTHORCONTACTS)
@Keywords(OMSCB_KEYWORDS)
@Label(OMSCB_LABEL)
@Name(OMSCB_NAME)
@Status(OMSCB_STATUS)
@License(OMSCB_LICENSE)
public class OmsCb extends HMModel {

    @Description(OMSCB_inRaster1_DESCRIPTION)
    @In
    public GridCoverage2D inRaster1 = null;

    @Description(OMSCB_inRaster2_DESCRIPTION)
    @In
    public GridCoverage2D inRaster2 = null;

    @Description(OMSCB_pBins_DESCRIPTION)
    @In
    public int pBins = 100;

    @Description(OMSCB_pFirst_DESCRIPTION)
    @In
    public int pFirst = 1;

    @Description(OMSCB_pLast_DESCRIPTION)
    @In
    public int pLast = 2;

    @Description(OMSCB_outCb_DESCRIPTION)
    @Out
    public double[][] outCb;

    private int binmode = 1;

    // private int bintype;
    // private float base;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outCb == null, doReset)) {
            return;
        }
        checkNull(inRaster1);
        RenderedImage map1RI = inRaster1.getRenderedImage();
        RenderedImage map2RI = null;
        if (inRaster2 == null) {
            map2RI = map1RI;
        } else {
            map2RI = inRaster2.getRenderedImage();
        }

        outCb = new CoupledFieldsMoments().process(map1RI, map2RI, pBins, pFirst, pLast, pm, binmode);
    }
    
    public static void main( String[] args ) throws Exception {
        OmsCb cb = new OmsCb();
        cb.inRaster1 = OmsRasterReader.readRaster("/home/hydrologis/Dropbox/TMP/test_jgtools/Archive/rainfall.asc");
        cb.inRaster2 = OmsRasterReader.readRaster("/home/hydrologis/Dropbox/TMP/test_jgtools/Archive/subbasin.asc");
        cb.process();
        
        
    }

}
