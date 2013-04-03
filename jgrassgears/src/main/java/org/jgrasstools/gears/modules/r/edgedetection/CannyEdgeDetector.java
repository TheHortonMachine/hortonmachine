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
package org.jgrasstools.gears.modules.r.edgedetection;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Edge detection operations")
@Author(name = "Tom Gibara, Andrea Antonello", contact = "http://www.tomgibara.com/computer-vision/canny-edge-detector, www.hydrologis.com")
@Keywords("Edge detection, Raster")
@Label(JGTConstants.RASTERPROCESSING)
@Name("canny")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class CannyEdgeDetector extends JGTModel {

    @Description("The map on which to perform edge detection.")
    @In
    public GridCoverage2D inMap = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The low threshold for the algorithm (default = 2.5).")
    @In
    public Float pLowthres = 2.5f;

    @Description("The hight threshold for the algorithm (default = 7.5).")
    @In
    public Float pHighthres = 7.5f;

    @Description("The radius of the gaussian kernel (default = 2).")
    @In
    public Float pRadiusgauss = 2f;

    @Description("The width of the gaussian kernel (default= 16).")
    @In
    public Integer pWidthgauss = 16;

    @Description("Switch to normalize the contrast.")
    @In
    public boolean doNormcontrast = false;

    @Description("The resulting map.")
    @Out
    public GridCoverage2D outMap = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }

        final RenderedImage renderedImage = inMap.getRenderedImage();
        Canny canny = new Canny(pLowthres, pHighthres, pRadiusgauss, pWidthgauss, doNormcontrast, renderedImage);
        pm.beginTask("Processsing edgedetection...", -1);
        canny.process();
        pm.done();

        WritableRaster edgesWR = canny.getEdgesRaster();
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inMap);
        outMap = CoverageUtilities.buildCoverage("canny", edgesWR, regionMap, inMap.getCoordinateReferenceSystem()); //$NON-NLS-1$
    }

}
