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
import jaitools.jiffle.runtime.JiffleEvent;
import jaitools.jiffle.runtime.JiffleEventListener;
import jaitools.jiffle.runtime.JiffleExecutor;
import jaitools.jiffle.runtime.JiffleExecutorResult;
import jaitools.jiffle.runtime.JiffleProgressListener;

import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
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
public class Mapcalc extends JGTModel implements JiffleEventListener {

    @Description("The maps that are used in the calculation.")
    @In
    public HashMap<String, GridCoverage2D> inMaps;

    @Description("The function to process.")
    @UI(JGTConstants.MULTILINE_UI_HINT + "5")
    @In
    public String pFunction;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The resulting map.")
    @Out
    public GridCoverage2D outMap = null;

    private static final String resultName = "result"; //$NON-NLS-1$

    public static final String MAPWRAPPER = "\"";
    private int previousProgress = 0;
    private boolean hasFinished = false;
    private HashMap<String, Double> regionParameters = null;

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }

        /*
         * prepare the function to be used by jiffle
         */
        pFunction = pFunction.replaceAll(MAPWRAPPER, "");
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
        script= script.trim();
        
        // create the executor
        JiffleExecutor executor = new JiffleExecutor(1);
        executor.addEventListener(this);

        // gather maps
        HashMap<String, RenderedImage> imgParams = new HashMap<String, RenderedImage>();
        // ad roles
        Map<String, Jiffle.ImageRole> imgRoles = CollectionFactory.map();

        Set<String> mapNamesSet = inMaps.keySet();
        for( String name : mapNamesSet ) {
            GridCoverage2D gridCoverage = inMaps.get(name);
            if (regionParameters == null) {
                regionParameters = CoverageUtilities.getRegionParamsFromGridCoverage(gridCoverage);
                crs = gridCoverage.getCoordinateReferenceSystem();
            }
            RenderedImage renderedImage = gridCoverage.getRenderedImage();
            // add map
            imgParams.put(name, renderedImage);
            // add role
            imgRoles.put(name, Jiffle.ImageRole.SOURCE);
        }
        if (regionParameters == null) {
            throw new ModelsIllegalargumentException("No map has been supplied.", this.getClass().getSimpleName());
        }
        int nCols = regionParameters.get(CoverageUtilities.COLS).intValue();
        int nRows = regionParameters.get(CoverageUtilities.ROWS).intValue();
        long pixelsNum = (long) nCols * nRows;

        // add the output map
        imgParams.put(resultName, ImageUtils.createConstantImage(nCols, nRows, Double.valueOf(0d)));

        // build the jiffle
        imgRoles.put(resultName, Jiffle.ImageRole.DEST);

        final long updateInterval = pixelsNum / 100;

        Jiffle jiffle = new Jiffle(script, imgRoles);
        if (jiffle.isCompiled()) {
            executor.submit(jiffle, imgParams, new JiffleProgressListener(){

                public void update( long done ) {
                    pm.worked(1);
                }

                public void start() {
                    pm.beginTask("Processing maps...", 100);
                }

                public void setUpdateInterval( double propPixels ) {

                }

                public void setUpdateInterval( long numPixels ) {

                }

                public void setTaskSize( long numPixels ) {
                }

                public long getUpdateInterval() {
                    return updateInterval;
                }

                public void finish() {
                    pm.done();
                }
            });
        }

        // try{
        // } catch (JiffleCompilationException e) {
        // String message =
        // "An error occurred during the compilation of the function. Please check your function.";
        // throw new ModelsRuntimeException(message, this);
        // } catch (JiffleInterpreterException e) {
        // String message =
        // "An error occurred during the interpretation of the function. Please check your function.";
        // throw new ModelsRuntimeException(message, this);
        // }

    }

    public void onCompletionEvent( JiffleEvent ev ) {
        JiffleExecutorResult result = ev.getResult();
        RenderedImage resultImage = result.getImages().get(resultName);
        try {
            outMap = CoverageUtilities.buildCoverage(resultName, resultImage, regionParameters, crs);
        } finally {
            pm.done();
            hasFinished = true;
        }
    }

    public void onFailureEvent( JiffleEvent ev ) {
        String msg = ev.toString();
        hasFinished = true;
        throw new ModelsRuntimeException(msg, this);
    }
}
