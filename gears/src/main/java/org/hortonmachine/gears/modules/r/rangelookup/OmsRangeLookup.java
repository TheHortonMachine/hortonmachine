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

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup.OMSRANGELOOKUP_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup.OMSRANGELOOKUP_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup.OMSRANGELOOKUP_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup.OMSRANGELOOKUP_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup.OMSRANGELOOKUP_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup.OMSRANGELOOKUP_LABEL;
import static org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup.OMSRANGELOOKUP_LICENSE;
import static org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup.OMSRANGELOOKUP_NAME;
import static org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup.OMSRANGELOOKUP_STATUS;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;

import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.ParameterBlockImageN;
import org.eclipse.imagen.ROIShape;
import org.eclipse.imagen.media.range.RangeFactory;
import org.eclipse.imagen.media.rlookup.RangeLookupTable;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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

    public static final String OMSRANGELOOKUP_DESCRIPTION = "Module for raster rangelookup.";
    public static final String OMSRANGELOOKUP_DOCUMENTATION = """
            The ranges are separated by commas, each with its lower and upper bound separated by a space and enclosed \
            in brackets: a square bracket includes the bound, a round one excludes it, and `null` leaves the range \
            open on that side. The classes, also separated by commas, go to the ranges in the same order. For example \
            `[0 10),[10 20),[20 null)` with `1,2,3` gives 1 to the values from 0 to below 10, 2 from 10 to below 20 \
            and 3 from 20 up. The cells that fall in no range become no-data, and the no-data cells of the input \
            stay no-data, also in a range open downwards.
            """;
    public static final String OMSRANGELOOKUP_KEYWORDS = "Raster, Rangelookup";
    public static final String OMSRANGELOOKUP_LABEL = RASTERPROCESSING;
    public static final String OMSRANGELOOKUP_NAME = "rrangelookup";
    public static final int OMSRANGELOOKUP_STATUS = 40;
    public static final String OMSRANGELOOKUP_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRANGELOOKUP_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRANGELOOKUP_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRANGELOOKUP_IN_RASTER_DESCRIPTION = "The raster that has to be processed.";
    public static final String OMSRANGELOOKUP_P_RANGES_DESCRIPTION = "The ranges in the form [r1l r1h),[r2l r2h]";
    public static final String OMSRANGELOOKUP_P_CLASSES_DESCRIPTION = "The classes to substitute in the same order of the ranges (in the form 1,2)";
    public static final String OMSRANGELOOKUP_OUT_RASTER_DESCRIPTION = "The processed raster.";

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

//        JAIExt.initJAIEXT(true); // FIXME remove when the jaitools rangelookup is not pulled fro
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

            var r = RangeFactory.create(min, minIncluded, max, maxIncluded);
            builder.add(r, classNum);
        }
        // List<org.jaitools.numeric.Range> ranges;
        // new RangeLookupProcess().execute(inRaster, 0, ranges, null);
        RangeLookupTable<Double, Double> table = builder.build();

        ROIShape roi = new ROIShape(new Rectangle(0, 0, inRI.getWidth(), inRI.getHeight()));

        ParameterBlockImageN pb = new ParameterBlockImageN("RLookup");
        pb.setSource("source0", inRI);
        pb.setParameter("table", table);
        pb.setParameter("roi", roi);
        pb.setParameter("default", (Double) novalue);
        RenderedImage lookupImg = ImageN.create("RLookup", pb);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        GridCoverage2D lookupCoverage = CoverageUtilities.buildCoverageWithNovalue("rangelookup", lookupImg, regionMap,
                inRaster.getCoordinateReferenceSystem(), novalue);

        // the lookup classes also the novalue cells, if a range contains the novalue
        try (HMRaster inData = HMRaster.fromGridCoverage(inRaster);
                HMRaster lookupData = HMRaster.fromGridCoverage(lookupCoverage);
                HMRaster outData = new HMRasterWritableBuilder().setName("rangelookup").setTemplate(inRaster)
                        .setNoValue(novalue).build()) {
            inData.process(pm, "Classifying ranges...", ( col, row, value, cols, rows ) -> {
                if (!inData.isNovalue(value)) {
                    outData.setValue(col, row, lookupData.getValue(col, row));
                }
            });
            outRaster = outData.buildCoverage();
        }
    }
}
