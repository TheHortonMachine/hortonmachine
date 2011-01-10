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
package org.jgrasstools.gears.modules.r.cutout;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Module for raster thresholding and masking")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Raster, Threshold")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class CutOut extends JGTModel {

    @Description("The coverage that has to be processed.")
    @In
    public GridCoverage2D inGeodata;

    @Description("The masking coverage.")
    @In
    public GridCoverage2D inMask;

    @Description("The upper threshold")
    @In
    public Double pMax;

    @Description("The lower threshold")
    @In
    public Double pMin;

    @Description("Switch for doing estraction of the mask area or the inverse (negative). if set to true, extracts the mask area.")
    @In
    public boolean doInverse = false;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The processed coverage.")
    @Out
    public GridCoverage2D outGeodata = null;

    private RandomIter maskIter;
    private boolean doMax = false;
    private boolean doMin = false;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outGeodata == null, doReset)) {
            return;
        }

        double max = -1;
        double min = -1;
        // do autoboxing only once
        if (pMax != null) {
            max = pMax;
            doMax = true;
        }
        if (pMin != null) {
            min = pMin;
            doMin = true;
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inGeodata);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage geodataRI = inGeodata.getRenderedImage();
        RandomIter geodataIter = RandomIterFactory.create(geodataRI, null);

        if (inMask != null) {
            RenderedImage maskRI = inMask.getRenderedImage();
            maskIter = RandomIterFactory.create(maskRI, null);
        }

        WritableRaster outWR = CoverageUtilities.renderedImage2WritableRaster(geodataRI, false);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        pm.beginTask("Processing map...", nRows);
        for( int i = 0; i < nRows; i++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int j = 0; j < nCols; j++ ) {
                if (maskIter != null) {
                    double maskValue = maskIter.getSampleDouble(j, i, 0);
                    if (!doInverse) {
                        if (isNovalue(maskValue)) {
                            outIter.setSample(j, i, 0, JGTConstants.doubleNovalue);
                            continue;
                        }
                    } else {
                        if (!isNovalue(maskValue)) {
                            outIter.setSample(j, i, 0, JGTConstants.doubleNovalue);
                            continue;
                        }
                    }
                }
                double value = geodataIter.getSampleDouble(j, i, 0);
                if (doMax && value > max) {
                    outIter.setSample(j, i, 0, JGTConstants.doubleNovalue);
                    continue;
                }
                if (doMin && value < min) {
                    outIter.setSample(j, i, 0, JGTConstants.doubleNovalue);
                    continue;
                }
            }
            pm.worked(1);
        }
        pm.done();

        outGeodata = CoverageUtilities.buildCoverage("pitfiller", outWR, regionMap, inGeodata.getCoordinateReferenceSystem());

    }
}
