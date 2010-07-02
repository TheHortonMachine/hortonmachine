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

import jaitools.imageutils.ImageUtils;
import jaitools.jiffle.Jiffle;
import jaitools.jiffle.JiffleCompilationException;
import jaitools.jiffle.runtime.JiffleCompletionEvent;
import jaitools.jiffle.runtime.JiffleEventListener;
import jaitools.jiffle.runtime.JiffleFailureEvent;
import jaitools.jiffle.runtime.JiffleInterpreter;
import jaitools.jiffle.runtime.JiffleInterpreterException;
import jaitools.jiffle.runtime.JiffleProgressEvent;

import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Set;

import javax.media.jai.TiledImage;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
@Description("Module for doing raster map algebra")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Mapcalc, Raster")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Mapcalc extends JGTModel {

    @Description("The maps that are used in the calculation.")
    @In
    public HashMap<String, GridCoverage2D> inMaps;

    @Description("The function to process.")
    @In
    public String pFunction;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The resulting map.")
    @Out
    public GridCoverage2D outMap = null;

    private static final String result = "MAPCALC_RESULT";

    public static final String MAPWRAPPER = "\"";
    private int previousProgress = 0;
    private boolean hasFinished = false;
    private JiffleInterpreter interp;
    private HashMap<String, Double> regionMap = null;

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }

        HashMap<String, RenderedImage> imgParams = new HashMap<String, RenderedImage>();
        Set<String> mapNamesSet = inMaps.keySet();
        for( String name : mapNamesSet ) {
            GridCoverage2D gridCoverage = inMaps.get(name);
            if (regionMap == null) {
                regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(gridCoverage);
                crs = gridCoverage.getCoordinateReferenceSystem();
            }
            RenderedImage renderedImage = gridCoverage.getRenderedImage();
            imgParams.put(name, renderedImage);
        }
        if (regionMap == null) {
            throw new ModelsIllegalargumentException("No map has been supplied.", this.getClass()
                    .getSimpleName());
        }
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();

        pm.beginTask("Processing maps...", 100);
        TiledImage returnImage = ImageUtils.createConstantImage(nCols, nRows, 0.0);
        imgParams.put(result, returnImage);

        /*
         * prepare the function to be used by jiffle
         */
        pFunction = pFunction.replaceAll(MAPWRAPPER, "");
        String script = null;
        String regex = result + "[\\s+]=";
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
            script = result + "=" + pFunction + "\n";
        }
        previousProgress = 0;

        JiffleEventListener listener = new JiffleEventListener(){
            public void onCompletionEvent( JiffleCompletionEvent ev ) {
                RenderedImage resultImage = ev.getJiffle().getImage(result);
                try {
                    outMap = CoverageUtilities.buildCoverage(result, resultImage, regionMap, crs);
                } finally {
                    pm.done();
                    hasFinished = true;
                }
            }

            public void onFailureEvent( JiffleFailureEvent arg0 ) {
                String msg = arg0.toString();
                hasFinished = true;
                throw new ModelsRuntimeException(msg, this);
            }

            public void onProgressEvent( JiffleProgressEvent arg0 ) {
                float progress = arg0.getProgress();
                int current = (int) (progress * 100f);
                int delta = current - previousProgress;
                pm.worked(delta);
                previousProgress = current;
            }
        };

        try {
            Jiffle j = new Jiffle(script, imgParams);
            interp = new JiffleInterpreter();
            interp.addEventListener(listener);
            if (j.isCompiled()) {
                interp.submit(j);
            }

            while( !hasFinished ) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (JiffleCompilationException e) {
            String message = "An error occurred during the compilation of the function. Please check your function.";
            throw new ModelsRuntimeException(message, this);
        } catch (JiffleInterpreterException e) {
            String message = "An error occurred during the interpretation of the function. Please check your function.";
            throw new ModelsRuntimeException(message, this);
        }

    }

}
