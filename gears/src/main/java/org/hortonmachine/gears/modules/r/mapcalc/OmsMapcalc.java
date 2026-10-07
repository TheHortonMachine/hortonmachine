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
package org.hortonmachine.gears.modules.r.mapcalc;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_IN_RASTERS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_P_FUNCTION_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_STATUS;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.media.jiffle.Jiffle;
import org.eclipse.imagen.media.jiffle.runtime.AffineCoordinateTransform;
import org.eclipse.imagen.media.jiffle.runtime.CoordinateTransform;
import org.eclipse.imagen.media.jiffle.runtime.JiffleDirectRuntime;
import org.eclipse.imagen.media.jiffle.runtime.NullProgressListener;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.utils.jaitools.ImageUtils;
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
import oms3.annotations.UI;

@Description(OMSMAPCALC_DESCRIPTION)
@Documentation(OMSMAPCALC_DOCUMENTATION)
@Author(name = OMSMAPCALC_AUTHORNAMES, contact = OMSMAPCALC_AUTHORCONTACTS)
@Keywords(OMSMAPCALC_KEYWORDS)
@Label(OMSMAPCALC_LABEL)
@Name(OMSMAPCALC_NAME)
@Status(OMSMAPCALC_STATUS)
@License(OMSMAPCALC_LICENSE)
public class OmsMapcalc extends HMModel {

    @Description(OMSMAPCALC_IN_RASTERS_DESCRIPTION)
    @In
    public List<GridCoverage2D> inRasters;

    @Description(OMSMAPCALC_P_FUNCTION_DESCRIPTION)
    @UI(HMConstants.MULTILINE_UI_HINT + "10," + HMConstants.MAPCALC_UI_HINT)
    @In
    public String pFunction;

    @Description(OMSMAPCALC_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    private RegionMap regionParameters = null;

    private CoordinateReferenceSystem crs;

    private Rectangle2D worldBounds;

    private long updateInterval;
    private long totalCount = 100;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        String script = pFunction;
        script = script.trim();

        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
        JiffleDirectRuntime jiffleRuntime = jiffle.getRuntimeInstance();

        CoordinateTransform jiffleCRS = null;

        // gather maps
        for( GridCoverage2D mapGC : inRasters ) {
            if (regionParameters == null) {
                regionParameters = CoverageUtilities.getRegionParamsFromGridCoverage(mapGC);
                crs = mapGC.getCoordinateReferenceSystem();

                ReferencedEnvelope envelope2d = mapGC.getEnvelope2D();
                
//                TODO check this!
				worldBounds = new Rectangle2D.Double(envelope2d.getMinX(), envelope2d.getMinY(),
						envelope2d.getWidth(), envelope2d.getHeight());
                Rectangle gridBounds = mapGC.getGridGeometry().getGridRange2D().getBounds();

                jiffleCRS = getTransform(worldBounds, gridBounds);

                double xRes = regionParameters.xres;
                double yRes = regionParameters.yres;
                jiffleRuntime.setWorldByResolution(worldBounds, xRes, yRes);
            }
            // the novalues of the map become NaN, the null of Jiffle
            RenderedImage renderedImage = novalueToNaN(mapGC);
            // add map
            String name = mapGC.getName().toString();
            jiffleRuntime.setSourceImage(name, renderedImage, jiffleCRS);
        }
        if (regionParameters == null) {
            throw new ModelsIllegalargumentException("No map has been supplied.", this.getClass().getSimpleName(), pm);
        }
        int nCols = regionParameters.cols;
        int nRows = regionParameters.rows;
        long pixelsNum = (long) nCols * nRows;

        if (pixelsNum < totalCount) {
            totalCount = pixelsNum;
        }
        updateInterval = pixelsNum / totalCount;

        String destName = jiffleRuntime.getDestinationVarNames()[0];
        if (!assignsVariable(script, destName)) {
            throw new ModelsIllegalargumentException("The script never assigns a value to the output map " + destName
                    + ", which would be all 0. Assign it in the script, as in: " + destName + " = ...;",
                    this.getClass().getSimpleName(), pm);
        }
        WritableRenderedImage destImg = ImageUtils.createConstantImage(nCols, nRows, 0d);
        jiffleRuntime.setDestinationImage(destName, destImg, jiffleCRS);
        
        jiffleRuntime.evaluateAll(new NullProgressListener());

        // the null results of Jiffle become the novalue of the HortonMachine
        WritableRaster outWR = destImg.getData().createCompatibleWritableRaster();
        outWR.setRect(destImg.getData());
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                if (Double.isNaN(outWR.getSampleDouble(c, r, 0))) {
                    outWR.setSample(c, r, 0, HMConstants.doubleNovalue);
                }
            }
        }
        outRaster = CoverageUtilities.buildCoverage(destName, outWR, regionParameters, crs);

//        // create the executor
//        JiffleExecutor executor = new JiffleExecutor();
//        JiffleEventListener listener = new JiffleEventListener(){
//            
//            @Override
//            public void onFailureEvent( JiffleEvent event ) {
//             
//            }
//            
//            @Override
//            public void onCompletionEvent( JiffleEvent event ) {
//                JiffleExecutorResult result = event.getResult();
//                Map<String, RenderedImage> imgMap = result.getImages();
//                RenderedImage destImage = imgMap.get(destName);
//                outRaster = CoverageUtilities.buildCoverage(destName, destImage, regionParameters, crs);
//                executor.shutdown();
//            }
//        };
//        executor.addEventListener(listener);
//
//        executor.submit(jiffleRuntime, new JiffleProgressListener(){
//            private long count = 0;
//            public void update( long done ) {
//                if (count == done) {
//                    pm.worked(1);
//                    count = count + updateInterval;
//                }
//            }
//
//            public void start() {
//                pm.beginTask("Processing maps...", (int) totalCount);
//            }
//
//            public void setUpdateInterval( double propPixels ) {
//            }
//
//            public void setUpdateInterval( long numPixels ) {
//            }
//
//            public void setTaskSize( long numPixels ) {
//                count = updateInterval;
//            }
//
//            public long getUpdateInterval() {
//                if (updateInterval == 0) {
//                    return 1;
//                }
//                return updateInterval;
//            }
//
//            public void finish() {
//                pm.done();
//            }
//        });


    }

    /**
     * Checks if the statements of a script assign a variable, outside of its comments and of the
     * images block, where the output map is declared. Jiffle accepts an output map that is never
     * assigned, and leaves it at the constant value of the destination image.
     *
     * @param script the Jiffle script.
     * @param name the name of the variable.
     * @return <code>true</code> if the variable is assigned somewhere.
     */
    public static boolean assignsVariable( String script, String name ) {
        String statements = script.replaceAll("(?s)/\\*.*?\\*/", " ") //
                .replaceAll("//[^\\n]*", " ") //
                .replaceAll("(?s)\\bimages\\s*\\{.*?\\}", " ");
        Pattern assignment = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*([-+*/%]?=(?!=)|\\+\\+|--)");
        return assignment.matcher(statements).find();
    }

    /**
     * The transform from the world positions of the script to the image cells: the rows grow
     * towards the south, so north is up, and each position goes to the cell that contains it.
     * 
     * <p>
     * Jiffle rounds the transformed positions to the nearest cell, and walks the world from the
     * south-west corner of each cell: the half cell shift makes the rounding pick the cell that
     * contains a position, and the epsilon keeps the corners of the walk, on the edges between
     * two cells, in their cell.
     */
    private static CoordinateTransform getTransform( Rectangle2D worldBounds, Rectangle imageBounds ) {
        if (worldBounds == null || worldBounds.isEmpty()) {
            throw new IllegalArgumentException("worldBounds must not be null or empty");
        }
        if (imageBounds == null || imageBounds.isEmpty()) {
            throw new IllegalArgumentException("imageBounds must not be null or empty");
        }
        double epsilon = 1E-6;

        double xscale = (imageBounds.getMaxX() - imageBounds.getMinX()) / (worldBounds.getMaxX() - worldBounds.getMinX());
        double xoff = imageBounds.getMinX() - xscale * worldBounds.getMinX() - 0.5 + epsilon;

        double yscale = (imageBounds.getMaxY() - imageBounds.getMinY()) / (worldBounds.getMaxY() - worldBounds.getMinY());
        double yoff = imageBounds.getMinY() + yscale * worldBounds.getMaxY() - 0.5 - epsilon;

        return new AffineCoordinateTransform(new AffineTransform(xscale, 0, 0, -yscale, xoff, yoff));
    }

    /**
     * @return the image of the map as doubles, with its novalues set to NaN, the null of Jiffle.
     */
    private static RenderedImage novalueToNaN( GridCoverage2D map ) {
        double novalue = HMConstants.getNovalue(map);
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(map);
        int cols = region.getCols();
        int rows = region.getRows();
        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, Double.class, null, null);
        RandomIter iter = CoverageUtilities.getRandomIterator(map);
        try {
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = iter.getSampleDouble(c, r, 0);
                    if (HMConstants.isNovalue(value, novalue)) {
                        value = Double.NaN;
                    }
                    outWR.setSample(c, r, 0, value);
                }
            }
        } finally {
            iter.done();
        }
        return CoverageUtilities.buildCoverage("map", outWR, region, map.getCoordinateReferenceSystem()).getRenderedImage();
    }

}
