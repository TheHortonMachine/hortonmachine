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
package org.jgrasstools.gears.modules.r.mapcalc;

import jaitools.CollectionFactory;
import jaitools.imageutils.ImageUtils;
import jaitools.jiffle.Jiffle;
import jaitools.jiffle.runtime.JiffleExecutor;
import jaitools.jiffle.runtime.JiffleExecutorResult;
import jaitools.jiffle.runtime.JiffleProgressListener;

import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Module for doing raster map algebra")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Mapcalc, Raster")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Mapcalc extends JGTModel {

    @Description("The maps (file paths) that are used in the calculation.")
    @In
    public List<GridCoverage2D> inMaps;

    @Description("The function to process.")
    @UI(JGTConstants.MULTILINE_UI_HINT + "5," + JGTConstants.MAPCALC_UI_HINT)
    @In
    public String pFunction;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The resulting map.")
    @Out
    public GridCoverage2D outMap = null;

    private static final String resultName = "result"; //$NON-NLS-1$

    private HashMap<String, Double> regionParameters = null;

    private CoordinateReferenceSystem crs;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }

        /*
         * prepare the function to be used by jiffle
         */
        String script = null;
        String regex = resultName + "[\\s+]=";
        String[] split = pFunction.split(regex);
        if (split.length > 1) {
            /*
             * if there is the result inside the function,
             * then 
             */
            script = pFunction + "\n";
        } else {
            /*
             * if there is no result inside, then we
             * assume a form of:
             * result = function
             */
            script = resultName + "=" + pFunction + "\n";
        }
        script = script.trim();

        // gather maps
        Map<String, RenderedImage> images = CollectionFactory.map();
        // ad roles
        Map<String, Jiffle.ImageRole> imageParams = CollectionFactory.map();

        for( GridCoverage2D mapGC : inMaps ) {
            if (regionParameters == null) {
                regionParameters = CoverageUtilities.getRegionParamsFromGridCoverage(mapGC);
                crs = mapGC.getCoordinateReferenceSystem();
            }
            RenderedImage renderedImage = mapGC.getRenderedImage();
            // add map
            String name = mapGC.getName().toString();
            images.put(name, renderedImage);
            // add role
            imageParams.put(name, Jiffle.ImageRole.SOURCE);
        }
        if (regionParameters == null) {
            throw new ModelsIllegalargumentException("No map has been supplied.", this.getClass().getSimpleName());
        }
        int nCols = regionParameters.get(CoverageUtilities.COLS).intValue();
        int nRows = regionParameters.get(CoverageUtilities.ROWS).intValue();
        long pixelsNum = (long) nCols * nRows;

        // add the output map
        images.put(resultName, ImageUtils.createConstantImage(nCols, nRows, 0d));

        // build the jiffle
        imageParams.put(resultName, Jiffle.ImageRole.DEST);

        final long updateInterval = pixelsNum / 100;

        Jiffle jiffle = new Jiffle(script, imageParams);
        // jiffle.compile();

        // create the executor
        JiffleExecutor executor = new JiffleExecutor();
        WaitingListener listener = new WaitingListener();
        executor.addEventListener(listener);
        listener.setNumTasks(1);

        int jobID = executor.submit(jiffle, images, new JiffleProgressListener(){
            private long count = 0;
            public void update( long done ) {
                if (count == done) {
                    pm.worked(1);
                    count = count + updateInterval;
                }
            }

            public void start() {
                pm.beginTask("Processing maps...", 100);
            }

            public void setUpdateInterval( double propPixels ) {
            }

            public void setUpdateInterval( long numPixels ) {
            }

            public void setTaskSize( long numPixels ) {
                count = updateInterval;
            }

            public long getUpdateInterval() {
                if (updateInterval == 0) {
                    return 1;
                }
                return updateInterval;
            }

            public void finish() {
                pm.done();
            }
        });
        listener.await();

        JiffleExecutorResult result = listener.getResults().get(0);
        RenderedImage resultImage = result.getImages().get(resultName);
        outMap = CoverageUtilities.buildCoverage(resultName, resultImage, regionParameters, crs);
        executor.shutdown();
    }

}
