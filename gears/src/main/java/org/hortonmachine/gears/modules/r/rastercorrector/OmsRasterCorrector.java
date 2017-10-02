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
package org.hortonmachine.gears.modules.r.rastercorrector;

import static org.hortonmachine.gears.i18n.GearsMessages.*;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

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
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

@Description(OMSRASTERCORRECTOR_DESCRIPTION)
@Documentation(OMSRASTERCORRECTOR_DOCUMENTATION)
@Author(name = OMSRASTERCORRECTOR_AUTHORNAMES, contact = OMSRASTERCORRECTOR_AUTHORCONTACTS)
@Keywords(OMSRASTERCORRECTOR_KEYWORDS)
@Label(OMSRASTERCORRECTOR_LABEL)
@Name(OMSRASTERCORRECTOR_NAME)
@Status(OMSRASTERCORRECTOR_STATUS)
@License(OMSRASTERCORRECTOR_LICENSE)
public class OmsRasterCorrector extends HMModel {

    @Description(OMSRASTERCORRECTOR_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERCORRECTOR_P_CORRECTIONS_DESCRIPTION)
    @UI(HMConstants.EASTINGNORTHING_UI_HINT)
    @In
    public String pCorrections;

    @Description(OMSRASTERCORRECTOR_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, pCorrections);

        String[] correctionSplit = pCorrections.split(","); //$NON-NLS-1$
        if (correctionSplit.length % 3 != 0) {
            throw new ModelsIllegalargumentException(
                    "the format of the correction values is: col1,row1,value1,col2,row2,value2...", this, pm);
        }

        RenderedImage inRI = inRaster.getRenderedImage();
        WritableRaster outWR = CoverageUtilities.renderedImage2WritableRaster(inRI, false);

        for( int i = 0; i < correctionSplit.length; i = i + 3 ) {
            int col = Integer.parseInt(correctionSplit[i].trim());
            int row = Integer.parseInt(correctionSplit[i + 1].trim());
            double value = Double.parseDouble(correctionSplit[i + 2].trim());

            outWR.setSample(col, row, 0, value);
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        outRaster = CoverageUtilities.buildCoverage("corrected", outWR, regionMap, inRaster.getCoordinateReferenceSystem());
    }

}
