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
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Module for raster rangelookup.")
@Documentation("RangeLookup.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Raster, Rangelookup")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.CERTIFIED)
@Name("rrangelookup")
@License("General Public License Version 3 (GPLv3)")
public class RangeLookup extends JGTModel {

    @Description("The raster that has to be processed.")
    @In
    public GridCoverage2D inRaster;

    @Description("The ranges in the form [r1l r1h),[r2l r2h]")
    @In
    public String pRanges;

    @Description("The classes to substitute in the same order of the ranges (in the form 1,2)")
    @In
    public String pClasses;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The processed raster.")
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
        outRaster = CoverageUtilities.buildCoverage("rangelookup", lookupImg, regionMap,
                inRaster.getCoordinateReferenceSystem());
    }
}
