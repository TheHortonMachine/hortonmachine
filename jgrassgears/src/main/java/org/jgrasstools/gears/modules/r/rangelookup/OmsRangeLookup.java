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
package org.jgrasstools.gears.modules.r.rangelookup;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_pClasses_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRANGELOOKUP_pRanges_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.util.HashMap;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;

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
import org.jaitools.media.jai.rangelookup.RangeLookupTable;
import org.jaitools.numeric.Range;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description(OMSRANGELOOKUP_DESCRIPTION)
@Documentation(OMSRANGELOOKUP_DOCUMENTATION)
@Author(name = OMSRANGELOOKUP_AUTHORNAMES, contact = OMSRANGELOOKUP_AUTHORCONTACTS)
@Keywords(OMSRANGELOOKUP_KEYWORDS)
@Label(OMSRANGELOOKUP_LABEL)
@Name(OMSRANGELOOKUP_NAME)
@Status(OMSRANGELOOKUP_STATUS)
@License(OMSRANGELOOKUP_LICENSE)
public class OmsRangeLookup extends JGTModel {

    @Description(OMSRANGELOOKUP_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRANGELOOKUP_pRanges_DESCRIPTION)
    @In
    public String pRanges;

    @Description(OMSRANGELOOKUP_pClasses_DESCRIPTION)
    @In
    public String pClasses;

    @Description(OMSRANGELOOKUP_outRaster_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        checkNull(inRaster, pRanges, pClasses);

        RenderedImage inRI = inRaster.getRenderedImage();

        RangeLookupTable<Double, Double> table = new RangeLookupTable<Double, Double>(JGTConstants.doubleNovalue);

        String[] rangesSplit = pRanges.trim().split(",");
        String[] classesSplit = pClasses.trim().split(",");
        if (rangesSplit.length != classesSplit.length) {
            throw new ModelsIllegalargumentException("Ranges and classes must be in pairs!", this);
        }
        for( int i = 0; i < rangesSplit.length; i++ ) {
            String classStr = classesSplit[i].trim();
            double classNum = Double.parseDouble(classStr);

            String range = rangesSplit[i].trim();
            boolean minIncluded = false;
            boolean maxIncluded = false;
            if (range.startsWith("[")) {
                minIncluded = true;
            }
            if (range.endsWith("]")) {
                maxIncluded = true;
            }
            String rangeNoBrac = range.replaceAll("\\[|\\]|\\(|\\)", "");
            String[] split = rangeNoBrac.trim().split("\\s+");

            Double min = null;
            try {
                min = Double.parseDouble(split[0]);
            } catch (Exception e) {
                // can be null
            }
            Double max = null;
            try {
                max = Double.parseDouble(split[1]);
            } catch (Exception e) {
                // can be null
            }

            Range<Double> r = new Range<Double>(min, minIncluded, max, maxIncluded);
            table.add(r, classNum);
        }

        ParameterBlockJAI pb = new ParameterBlockJAI("RangeLookup");
        pb.setSource("source0", inRI);
        pb.setParameter("table", table);
        RenderedImage lookupImg = JAI.create("RangeLookup", pb);

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        outRaster = CoverageUtilities.buildCoverage("rangelookup", lookupImg, regionMap, inRaster.getCoordinateReferenceSystem());
    }
}
