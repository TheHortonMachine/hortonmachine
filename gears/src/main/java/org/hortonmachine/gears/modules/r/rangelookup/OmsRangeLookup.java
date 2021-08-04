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
package org.hortonmachine.gears.modules.r.rangelookup;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_P_CLASSES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_P_RANGES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_STATUS;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.util.HashMap;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROIShape;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.rlookup.RangeLookupTable;
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

@Description(OMSRANGELOOKUP_DESCRIPTION)
@Documentation(OMSRANGELOOKUP_DOCUMENTATION)
@Author(name = OMSRANGELOOKUP_AUTHORNAMES, contact = OMSRANGELOOKUP_AUTHORCONTACTS)
@Keywords(OMSRANGELOOKUP_KEYWORDS)
@Label(OMSRANGELOOKUP_LABEL)
@Name(OMSRANGELOOKUP_NAME)
@Status(OMSRANGELOOKUP_STATUS)
@License(OMSRANGELOOKUP_LICENSE)
public class OmsRangeLookup extends HMModel {

    @Description(OMSRANGELOOKUP_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRANGELOOKUP_P_RANGES_DESCRIPTION)
    @In
    public String pRanges;

    @Description(OMSRANGELOOKUP_P_CLASSES_DESCRIPTION)
    @In
    public String pClasses;

    @Description(OMSRANGELOOKUP_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        JAIExt.initJAIEXT(true); // FIXME remove when the jaitools rangelookup is not pulled fro
                                 // raster process anymore

        checkNull(inRaster, pRanges, pClasses);

        double novalue = HMConstants.getNovalue(inRaster);

        RenderedImage inRI = inRaster.getRenderedImage();

        RangeLookupTable.Builder<Double, Double> builder = new RangeLookupTable.Builder<Double, Double>();

        String[] rangesSplit = pRanges.trim().split(",");
        String[] classesSplit = pClasses.trim().split(",");
        if (rangesSplit.length != classesSplit.length) {
            throw new ModelsIllegalargumentException("Ranges and classes must be in pairs!", this, pm);
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
                if (split[0].equals("null")) {
                    min = Double.NEGATIVE_INFINITY;
                } else {
                    min = Double.parseDouble(split[0]);
                }
            } catch (Exception e) {
                // can be null
            }
            Double max = null;
            try {
                if (split[1].equals("null")) {
                    max = Double.POSITIVE_INFINITY;
                } else {
                    max = Double.parseDouble(split[1]);
                }
            } catch (Exception e) {
                // can be null
            }

            Range r = RangeFactory.create(min, minIncluded, max, maxIncluded);
            builder.add(r, classNum);
        }
        // List<org.jaitools.numeric.Range> ranges;
        // new RangeLookupProcess().execute(inRaster, 0, ranges, null);
        RangeLookupTable<Double, Double> table = builder.build();

        ROIShape roi = new ROIShape(new Rectangle(0, 0, inRI.getWidth(), inRI.getHeight()));

        ParameterBlockJAI pb = new ParameterBlockJAI("RLookup");
        pb.setSource("source0", inRI);
        pb.setParameter("table", table);
        pb.setParameter("roi", roi);
        pb.setParameter("default", (Double) novalue);
        RenderedImage lookupImg = JAI.create("RLookup", pb);

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        outRaster = CoverageUtilities.buildCoverageWithNovalue("rangelookup", lookupImg, regionMap,
                inRaster.getCoordinateReferenceSystem(), novalue);
    }
}
